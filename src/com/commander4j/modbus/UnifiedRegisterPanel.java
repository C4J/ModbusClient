package com.commander4j.modbus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.commander4j.gui.JButton4j;
import com.commander4j.sys.Common;
import com.digitalpetri.modbus.exceptions.ModbusException;

/**
 * Single-table view of all four Modbus register spaces, aligned by zero-based
 * protocol address. A shared Start/Count range drives coils, discrete inputs,
 * input registers and holding registers; the panel polls all four kinds in one
 * tick on a background thread and diffs the result against the table model on
 * the EDT so unchanged rows are not repainted and the row currently being
 * edited is left alone.
 *
 * <p>Coil and Holding Register cells are editable when connected; edits write
 * through to the {@link ClientController}. Discrete Input and Input Register
 * cells are read-only at all times because Modbus has no client-write function
 * code for those tables.
 */
public class UnifiedRegisterPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** Upper bound on the number of rows shown at once, to keep the table responsive. */
	private static final int MAX_COUNT = 2000;

	/** Polling cadence, in milliseconds. Each tick reads all four register spaces. */
	private static final int POLL_INTERVAL_MS = 250;

	private static final Color ADDRESS_BG = new Color(155, 77, 133);
	private static final Color ADDRESS_FG = Color.WHITE;
	private static final Color COIL_BG = new Color(168, 208, 141);
	private static final Color DISC_BG = new Color(157, 183, 224);
	private static final Color INPUT_BG = new Color(245, 240, 161);
	private static final Color HOLD_BG = new Color(244, 167, 143);
	private static final Color GROUP_FG = Color.BLACK;

	private final ClientController controller;
	private final Consumer<String> infoLog;
	private final BiConsumer<String, Throwable> errorLog;
	private final Runnable onPollFailed;

	private final UnifiedRegisterTableModel model;
	private final JTable table;

	private final JSpinner startSpinner;
	private final JSpinner countSpinner;
	private final JPopupMenu zeroMenu;

	/** Current visible window. Read by the poll thread, written on the EDT via {@link #applyRange}. */
	private final AtomicReference<Range> range = new AtomicReference<>(new Range(0, 64));

	private volatile Thread pollThread;

	public UnifiedRegisterPanel(ClientController controller,
			Consumer<String> infoLog,
			BiConsumer<String, Throwable> errorLog,
			Runnable onPollFailed)
	{
		super(new BorderLayout(6, 6));
		this.controller = controller;
		this.infoLog = infoLog;
		this.errorLog = errorLog;
		this.onPollFailed = onPollFailed;

		setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

		model = new UnifiedRegisterTableModel(controller, errorLog);

		startSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 65535, 1));
		countSpinner = new JSpinner(new SpinnerNumberModel(64, 1, MAX_COUNT, 1));
		JSpinner.NumberEditor startEditor = new JSpinner.NumberEditor(startSpinner, "#");
		JSpinner.NumberEditor countEditor = new JSpinner.NumberEditor(countSpinner, "#");
		startEditor.getTextField().setColumns(3);
		countEditor.getTextField().setColumns(3);
		startSpinner.setEditor(startEditor);
		countSpinner.setEditor(countEditor);

		JButton4j applyButton = new JButton4j(Common.icon_ok);
		applyButton.setToolTipText("Apply range");
		applyButton.setPreferredSize(new Dimension(36, 36));

		zeroMenu = new JPopupMenu();
		zeroMenu.add(zeroItem("Zero Coils", RegisterKind.COILS));
		zeroMenu.add(zeroItem("Zero Holding Registers", RegisterKind.HOLDING_REGISTERS));
		zeroMenu.addSeparator();
		zeroMenu.add(zeroAllItem());

		JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		bar.add(new JLabel("Start address:"));
		bar.add(startSpinner);
		bar.add(new JLabel("Count:"));
		bar.add(countSpinner);
		bar.add(applyButton);
		add(bar, BorderLayout.SOUTH);

		List<ColumnGroup> groups = List.of(
				new ColumnGroup("", ADDRESS_BG, ADDRESS_FG,
						UnifiedRegisterTableModel.COL_ADDRESS, UnifiedRegisterTableModel.COL_ADDRESS),
				new ColumnGroup("Coil", COIL_BG, GROUP_FG,
						UnifiedRegisterTableModel.COL_COIL_REF, UnifiedRegisterTableModel.COL_COIL_VAL),
				new ColumnGroup("Discrete", DISC_BG, GROUP_FG,
						UnifiedRegisterTableModel.COL_DISC_REF, UnifiedRegisterTableModel.COL_DISC_VAL),
				new ColumnGroup("Input", INPUT_BG, GROUP_FG,
						UnifiedRegisterTableModel.COL_INPUT_REF, UnifiedRegisterTableModel.COL_INPUT_HEX),
				new ColumnGroup("Holding", HOLD_BG, GROUP_FG,
						UnifiedRegisterTableModel.COL_HOLD_REF, UnifiedRegisterTableModel.COL_HOLD_HEX));

		table = new JTable(model)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected JTableHeader createDefaultTableHeader()
			{
				return new GroupableTableHeader(getColumnModel(), groups);
			}
		};
		table.setRowHeight(22);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		table.setShowGrid(true);
		Color gridColour = UIManager.getColor("Table.gridColor");
		if (gridColour != null)
		{
			table.setGridColor(gridColour);
		}

		configureColumnWidths();

		ColourCellRenderer textRenderer = new ColourCellRenderer(groups);
		BooleanCellRenderer boolRenderer = new BooleanCellRenderer(groups);
		for (int i = 0; i < table.getColumnCount(); i++)
		{
			TableColumn col = table.getColumnModel().getColumn(i);
			if (model.getColumnClass(i) == Boolean.class)
			{
				col.setCellRenderer(boolRenderer);
			}
			else
			{
				col.setCellRenderer(textRenderer);
			}
		}
		table.getTableHeader().setDefaultRenderer(new GroupAwareHeaderRenderer(groups));
		table.getTableHeader().setReorderingAllowed(false);

		add(new JScrollPane(table), BorderLayout.CENTER);

		applyButton.addActionListener(_ -> applyRange());
	}

	public JPopupMenu getZeroMenu()
	{
		return zeroMenu;
	}

	/**
	 * Starts or stops the background poll thread. Called by the frame when the
	 * connect/disconnect toggle changes state.
	 */
	public synchronized void setConnected(boolean connected)
	{
		if (connected)
		{
			if (pollThread != null && pollThread.isAlive())
			{
				return;
			}
			Thread t = new Thread(this::pollLoop, "modbus-client-poll");
			t.setDaemon(true);
			pollThread = t;
			t.start();
		}
		else
		{
			Thread t = pollThread;
			pollThread = null;
			if (t != null)
			{
				t.interrupt();
			}
		}
	}

	private JMenuItem zeroItem(String label, RegisterKind kind)
	{
		JMenuItem item = new JMenuItem(label);
		item.addActionListener(_ -> runZero(kind, label));
		return item;
	}

	private JMenuItem zeroAllItem()
	{
		JMenuItem item = new JMenuItem("Zero All");
		item.addActionListener(_ ->
		{
			runZero(RegisterKind.COILS, "Zero Coils");
			runZero(RegisterKind.HOLDING_REGISTERS, "Zero Holding Registers");
		});
		return item;
	}

	private void runZero(RegisterKind kind, String label)
	{
		if (!controller.isConnected())
		{
			infoLog.accept(label + " ignored - not connected");
			return;
		}
		stopEditing();
		Range r = range.get();
		try
		{
			controller.zeroRange(kind, r.start(), r.count());
			infoLog.accept(label + " (" + r.start() + " for " + r.count() + ")");
		}
		catch (ModbusException ex)
		{
			errorLog.accept(label + " failed", ex);
		}
	}

	private void configureColumnWidths()
	{
		setColumnWidth(UnifiedRegisterTableModel.COL_ADDRESS, 75);
		setColumnWidth(UnifiedRegisterTableModel.COL_COIL_REF, 95);
		setColumnWidth(UnifiedRegisterTableModel.COL_COIL_VAL, 60);
		setColumnWidth(UnifiedRegisterTableModel.COL_DISC_REF, 95);
		setColumnWidth(UnifiedRegisterTableModel.COL_DISC_VAL, 60);
		setColumnWidth(UnifiedRegisterTableModel.COL_INPUT_REF, 95);
		setColumnWidth(UnifiedRegisterTableModel.COL_INPUT_VAL, 90);
		setColumnWidth(UnifiedRegisterTableModel.COL_INPUT_HEX, 80);
		setColumnWidth(UnifiedRegisterTableModel.COL_HOLD_REF, 95);
		setColumnWidth(UnifiedRegisterTableModel.COL_HOLD_VAL, 90);
		setColumnWidth(UnifiedRegisterTableModel.COL_HOLD_HEX, 80);
	}

	private void setColumnWidth(int col, int width)
	{
		TableColumn c = table.getColumnModel().getColumn(col);
		c.setMinWidth(40);
		c.setPreferredWidth(width);
	}

	private void applyRange()
	{
		stopEditing();
		try
		{
			startSpinner.commitEdit();
			countSpinner.commitEdit();
		}
		catch (java.text.ParseException ignored)
		{
			// bad text in a spinner - fall back to its last committed value
		}
		int start = (Integer) startSpinner.getValue();
		int count = (Integer) countSpinner.getValue();
		range.set(new Range(start, count));
		model.setRange(start, count);
	}

	private void stopEditing()
	{
		if (table.isEditing())
		{
			table.getCellEditor().stopCellEditing();
		}
	}

	/**
	 * Background polling loop. Runs until the controller disconnects or any read
	 * throws, at which point it notifies the frame via {@link #onPollFailed} so
	 * the UI transitions back to the disconnected state.
	 */
	private void pollLoop()
	{
		while (Thread.currentThread() == pollThread && controller.isConnected())
		{
			final Range r = range.get();
			try
			{
				final boolean[] coils = controller.readBits(RegisterKind.COILS, r.start(), r.count());
				final boolean[] discrete = controller.readBits(RegisterKind.DISCRETE_INPUTS, r.start(), r.count());
				final int[] input = controller.readRegisters(RegisterKind.INPUT_REGISTERS, r.start(), r.count());
				final int[] holding = controller.readRegisters(RegisterKind.HOLDING_REGISTERS, r.start(), r.count());

				SwingUtilities.invokeLater(() ->
				{
					if (!r.equals(range.get()))
					{
						// range changed mid-poll - discard this tick and let the next one populate
						return;
					}
					int editingRow = table.isEditing() ? table.getEditingRow() : -1;
					model.applyPolledData(coils, discrete, input, holding, editingRow);
				});
			}
			catch (ModbusException ex)
			{
				errorLog.accept("Poll failed", ex);
				SwingUtilities.invokeLater(onPollFailed);
				return;
			}
			catch (RuntimeException ex)
			{
				// IllegalStateException from requireConnected() if disconnect raced the poll
				errorLog.accept("Poll stopped", ex);
				SwingUtilities.invokeLater(onPollFailed);
				return;
			}

			try
			{
				Thread.sleep(POLL_INTERVAL_MS);
			}
			catch (InterruptedException interrupted)
			{
				return;
			}
		}
	}

	/** Snapshot of the visible address window shared between the EDT and the poll thread. */
	private record Range(int start, int count)
	{
	}

	// ---- Cell renderers ---------------------------------------------------

	/** Text/number cell renderer that fills the background with the column-group colour. */
	private static final class ColourCellRenderer extends DefaultTableCellRenderer
	{
		private static final long serialVersionUID = 1L;

		private final List<ColumnGroup> groups;

		ColourCellRenderer(List<ColumnGroup> groups)
		{
			this.groups = groups;
			setHorizontalAlignment(CENTER);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column)
		{
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			ColumnGroup g = groupFor(column);
			if (isSelected)
			{
				setBackground(table.getSelectionBackground());
				setForeground(table.getSelectionForeground());
			}
			else if (g != null)
			{
				setBackground(g.colour());
				setForeground(g.textColour());
			}
			return this;
		}

		private ColumnGroup groupFor(int column)
		{
			for (ColumnGroup g : groups)
			{
				if (g.contains(column))
				{
					return g;
				}
			}
			return null;
		}
	}

	/**
	 * Boolean cell renderer that paints its own background so the checkbox sits
	 * on the column-group colour rather than Nimbus's stripe colour.
	 */
	private static final class BooleanCellRenderer extends JCheckBox implements TableCellRenderer
	{
		private static final long serialVersionUID = 1L;

		private final List<ColumnGroup> groups;
		private Color cellBackground = Color.WHITE;

		BooleanCellRenderer(List<ColumnGroup> groups)
		{
			this.groups = groups;
			setHorizontalAlignment(SwingConstants.CENTER);
			setBorderPainted(false);
			setOpaque(false);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column)
		{
			setSelected(Boolean.TRUE.equals(value));
			if (isSelected)
			{
				cellBackground = table.getSelectionBackground();
				setForeground(table.getSelectionForeground());
			}
			else
			{
				ColumnGroup g = groupFor(column);
				cellBackground = g != null ? g.colour() : Color.WHITE;
				setForeground(g != null ? g.textColour() : Color.BLACK);
			}
			return this;
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			g.setColor(cellBackground);
			g.fillRect(0, 0, getWidth(), getHeight());
			super.paintComponent(g);
		}

		private ColumnGroup groupFor(int column)
		{
			for (ColumnGroup grp : groups)
			{
				if (grp.contains(column))
				{
					return grp;
				}
			}
			return null;
		}
	}

	/** Sub-column header renderer that paints the column-group colour behind the label. */
	private static final class GroupAwareHeaderRenderer extends DefaultTableCellRenderer
	{
		private static final long serialVersionUID = 1L;

		private final List<ColumnGroup> groups;

		GroupAwareHeaderRenderer(List<ColumnGroup> groups)
		{
			this.groups = groups;
			setHorizontalAlignment(CENTER);
			setOpaque(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column)
		{
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			ColumnGroup g = groupFor(column);
			if (g != null)
			{
				setBackground(g.colour());
				setForeground(g.textColour());
			}
			setFont(getFont().deriveFont(Font.BOLD));
			Color grid = UIManager.getColor("Table.gridColor");
			setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, grid != null ? grid : Color.GRAY));
			return this;
		}

		private ColumnGroup groupFor(int column)
		{
			for (ColumnGroup g : groups)
			{
				if (g.contains(column))
				{
					return g;
				}
			}
			return null;
		}
	}
}
