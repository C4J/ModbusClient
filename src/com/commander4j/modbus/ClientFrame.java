package com.commander4j.modbus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;

import com.commander4j.dialog.JDialogAbout;
import com.commander4j.dialog.JDialogLicenses;
import com.commander4j.gui.JButton4j;
import com.commander4j.gui.JLabel4j_std;
import com.commander4j.gui.JToggleButton4j;
import com.commander4j.sys.Common;
import com.commander4j.util.JHelp;
import com.commander4j.util.JUtility;

/**
 * Main application window: a connection bar (server host / port / unit ID), the
 * unified register table covering all four Modbus data spaces, and an activity
 * log. The window owns a single {@link ClientController}; connecting and
 * disconnecting is done on a background thread so the TCP call never blocks
 * the event dispatch thread.
 */
public class ClientFrame extends JFrame
{

	private static final long serialVersionUID = 1L;

	private static final Color CONNECTED_COLOR = new Color(0, 140, 0);
	private static final int MAX_LOG_LINES = 400;

	private final ClientController controller = new ClientController();

	private final JTextField hostField = new JTextField("127.0.0.1", 12);
	private final JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(502, 1, 65535, 1));
	private final JSpinner unitSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 247, 1));
	private final JToggleButton4j connectToggle = new JToggleButton4j(Common.icon_disconnected);
	private final JLabel4j_std statusLabel = new JLabel4j_std("Disconnected");

	private final JTextArea logArea = new JTextArea(8, 80);
	private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");

	private final UnifiedRegisterPanel registerPanel;

	private ClientConfig baseline = new ClientConfig("127.0.0.1", 502, 0);
	private File currentConfigFile = ConfigStore.DEFAULT_FILE;
	private boolean loading = false;
	private boolean dirty = false;

	public ClientFrame()
	{
		super(Common.buildTitle(null));
		JUtility.setLookAndFeel("Nimbus");
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		connectToggle.setToolTipText("Connect");
		connectToggle.setPreferredSize(new Dimension(36, 36));
		connectToggle.setMaximumSize(new Dimension(36, 36));
		connectToggle.addActionListener(_ -> toggleConnection());

		add(buildConnectionBar(), BorderLayout.NORTH);

		registerPanel = new UnifiedRegisterPanel(
				controller,
				this::log,
				this::logError,
				this::handlePollFailure);

		logArea.setEditable(false);
		logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		JScrollPane logScroll = new JScrollPane(logArea);
		logScroll.setBorder(BorderFactory.createTitledBorder("Activity"));
		logScroll.setPreferredSize(new Dimension(0, 200));

		JPanel registerArea = new JPanel(new BorderLayout());
		registerArea.add(registerPanel, BorderLayout.CENTER);
		registerArea.add(buildRightToolBar(registerPanel), BorderLayout.EAST);

		JPanel logRow = new JPanel(new BorderLayout());
		logRow.add(logScroll, BorderLayout.CENTER);
		logRow.add(buildLogToolBar(), BorderLayout.EAST);

		JPanel main = new JPanel(new BorderLayout());
		main.add(registerArea, BorderLayout.CENTER);
		main.add(logRow, BorderLayout.SOUTH);
		add(main, BorderLayout.CENTER);

		installDirtyListeners();

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				confirmExit();
			}
		});

		setSize(1020, 860);
		centreOnActiveMonitor();
		log("Ready. Set the server host, port and unit ID, then press Connect.");
		log("Coil and Holding Register cells become editable once connected.");

		loadConfigOnStartup();
	}

	/**
	 * Centre the window on the monitor that currently holds the mouse pointer,
	 * falling back to {@link #setLocationRelativeTo} centring if the screen layout
	 * cannot be queried.
	 */
	private void centreOnActiveMonitor()
	{
		try
		{
			Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

			GraphicsDevice activeDevice = ge.getDefaultScreenDevice();
			for (GraphicsDevice device : ge.getScreenDevices())
			{
				if (device.getDefaultConfiguration().getBounds().contains(mouseLocation))
				{
					activeDevice = device;
					break;
				}
			}

			GraphicsConfiguration gc = activeDevice.getDefaultConfiguration();
			Rectangle screenBounds = gc.getBounds();

			setLocation(screenBounds.x + (screenBounds.width - getWidth()) / 2,
					screenBounds.y + (screenBounds.height - getHeight()) / 2);
		}
		catch (HeadlessException | NullPointerException ex)
		{
			setLocationRelativeTo(null);
		}
	}

	private JPanel buildConnectionBar()
	{
		portSpinner.setEditor(new JSpinner.NumberEditor(portSpinner, "#"));
		unitSpinner.setEditor(new JSpinner.NumberEditor(unitSpinner, "#"));
		statusLabel.setForeground(Color.GRAY);

		JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 8));
		bar.add(new JLabel4j_std("Server host:"));
		bar.add(hostField);
		bar.add(new JLabel4j_std("Port:"));
		bar.add(portSpinner);
		bar.add(new JLabel4j_std("Unit ID:"));
		bar.add(unitSpinner);
		bar.add(Box.createHorizontalStrut(12));
		bar.add(new JLabel4j_std("Status:"));
		bar.add(statusLabel);
		return bar;
	}

	private JToolBar buildRightToolBar(UnifiedRegisterPanel registerPanel)
	{
		JToolBar tb = new JToolBar(JToolBar.VERTICAL);
		tb.setFloatable(false);
		tb.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));

		tb.add(connectToggle);

		tb.add(iconButton(Common.icon_open, "Open settings", _ -> openConfig()));
		tb.add(iconButton(Common.icon_save, "Save settings", _ -> saveConfig()));

		JPopupMenu zeroMenu = registerPanel.getZeroMenu();
		JButton4j zeroButton = new JButton4j(Common.icon_erase);
		zeroButton.setToolTipText("Zero registers");
		zeroButton.setPreferredSize(new Dimension(36, 36));
		zeroButton.setMaximumSize(new Dimension(36, 36));
		zeroButton.addActionListener(_ ->
				zeroMenu.show(zeroButton, -zeroMenu.getPreferredSize().width, 0));
		tb.add(zeroButton);

		tb.add(iconButton(Common.icon_about,   "About",    _ -> new JDialogAbout().setVisible(true)));
		tb.add(iconButton(Common.icon_license, "Licences", _ -> new JDialogLicenses(ClientFrame.this).setVisible(true)));

		JButton4j btnHelp = new JButton4j(Common.icon_help);
		btnHelp.setToolTipText("Help");
		btnHelp.setPreferredSize(new Dimension(36, 36));
		btnHelp.setMaximumSize(new Dimension(36, 36));
		new JHelp().enableHelpOnButton(btnHelp, Common.helpURL);
		tb.add(btnHelp);

		tb.add(iconButton(Common.icon_exit, "Close", _ -> confirmExit()));

		return tb;
	}

	private JToolBar buildLogToolBar()
	{
		JToolBar tb = new JToolBar(JToolBar.VERTICAL);
		tb.setFloatable(false);
		tb.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));

		tb.add(iconButton(Common.icon_save,   "Save log",  _ -> saveLog()));
		tb.add(iconButton(Common.icon_eraser, "Clear log", _ -> clearLog()));

		return tb;
	}

	private void saveLog()
	{
		String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Save activity log");
		chooser.setSelectedFile(new File("modbus-client-log-" + stamp + ".txt"));
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}
		File file = chooser.getSelectedFile();
		try (FileWriter writer = new FileWriter(file))
		{
			writer.write(logArea.getText());
			log("Log saved to " + file.getAbsolutePath());
		}
		catch (IOException ex)
		{
			JOptionPane.showMessageDialog(this,
					"Failed to save log:\n\n" + describe(ex),
					"Save activity log",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void clearLog()
	{
		logArea.setText("");
	}

	private static JButton4j iconButton(javax.swing.ImageIcon icon, String tooltip, java.awt.event.ActionListener a)
	{
		JButton4j b = new JButton4j(icon);
		b.setToolTipText(tooltip);
		b.setPreferredSize(new Dimension(36, 36));
		b.setMaximumSize(new Dimension(36, 36));
		b.addActionListener(a);
		return b;
	}

	private void toggleConnection()
	{
		if (controller.isConnected())
		{
			disconnect();
		}
		else
		{
			connect();
		}
	}

	private void connect()
	{
		String host = hostField.getText().trim();
		if (host.isEmpty())
		{
			host = "127.0.0.1";
		}
		final int port = (Integer) portSpinner.getValue();
		final int unitId = (Integer) unitSpinner.getValue();
		final String targetHost = host;

		setConfigEnabled(false);
		connectToggle.setEnabled(false);
		controller.setUnitId(unitId);
		log("Connecting to " + targetHost + ":" + port + " (unit ID " + unitId + ")...");

		new Thread(() ->
		{
			try
			{
				controller.connect(targetHost, port);
				SwingUtilities.invokeLater(() ->
				{
					statusLabel.setText("Connected to " + targetHost + ":" + port + "  (unit ID " + unitId + ")");
					statusLabel.setForeground(CONNECTED_COLOR);
					connectToggle.setIcon(Common.icon_connected);
					connectToggle.setToolTipText("Disconnect");
					connectToggle.setSelected(true);
					connectToggle.setEnabled(true);
					registerPanel.setConnected(true);
					log("Connected. Polling every 250 ms.");
				});
			}
			catch (Exception ex)
			{
				SwingUtilities.invokeLater(() ->
				{
					String reason = describe(ex);
					log("FAILED to connect: " + reason);
					statusLabel.setText("Disconnected");
					statusLabel.setForeground(Color.GRAY);
					setConfigEnabled(true);
					connectToggle.setIcon(Common.icon_disconnected);
					connectToggle.setToolTipText("Connect");
					connectToggle.setSelected(false);
					connectToggle.setEnabled(true);
					JOptionPane.showMessageDialog(ClientFrame.this,
							"Could not connect to the Modbus server:\n\n" + reason,
							"Modbus Client",
							JOptionPane.ERROR_MESSAGE);
				});
			}
		}, "modbus-client-connect").start();
	}

	private void disconnect()
	{
		connectToggle.setEnabled(false);
		registerPanel.setConnected(false);
		log("Disconnecting...");

		new Thread(() ->
		{
			try
			{
				controller.disconnect();
			}
			catch (Exception ex)
			{
				SwingUtilities.invokeLater(() -> log("Error while disconnecting: " + describe(ex)));
			}
			SwingUtilities.invokeLater(this::applyDisconnectedState);
		}, "modbus-client-disconnect").start();
	}

	/**
	 * Called by {@link UnifiedRegisterPanel} when the poll loop exits unexpectedly
	 * (typically because a read threw). Tears down the connection and returns the
	 * UI to its disconnected state.
	 */
	private void handlePollFailure()
	{
		registerPanel.setConnected(false);
		new Thread(() ->
		{
			try
			{
				controller.disconnect();
			}
			catch (Exception ignored)
			{
				// already in a bad state - swallow secondary errors
			}
			SwingUtilities.invokeLater(() ->
			{
				applyDisconnectedState();
				log("Connection lost.");
			});
		}, "modbus-client-recover").start();
	}

	private void applyDisconnectedState()
	{
		statusLabel.setText("Disconnected");
		statusLabel.setForeground(Color.GRAY);
		setConfigEnabled(true);
		connectToggle.setIcon(Common.icon_disconnected);
		connectToggle.setToolTipText("Connect");
		connectToggle.setSelected(false);
		connectToggle.setEnabled(true);
	}

	private void confirmExit()
	{
		if (dirty)
		{
			int saveChoice = JOptionPane.showConfirmDialog(
					ClientFrame.this,
					"Settings have changed. Save changes to\n" + currentConfigFile.getAbsolutePath() + "?",
					"Unsaved changes",
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE);

			if (saveChoice == JOptionPane.CANCEL_OPTION || saveChoice == JOptionPane.CLOSED_OPTION)
			{
				return;
			}
			if (saveChoice == JOptionPane.YES_OPTION && !writeConfig(currentConfigFile))
			{
				return;
			}
		}
		else
		{
			int choice = JOptionPane.showConfirmDialog(
					ClientFrame.this,
					"Are you sure you want to close " + Common.programName + "?",
					"Confirm Exit",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE);

			if (choice != JOptionPane.YES_OPTION)
			{
				return;
			}
		}

		registerPanel.setConnected(false);
		try
		{
			controller.disconnect();
		}
		catch (Exception ignored)
		{
			// shutting down anyway
		}
		dispose();
		System.exit(0);
	}

	private void setConfigEnabled(boolean enabled)
	{
		hostField.setEnabled(enabled);
		portSpinner.setEnabled(enabled);
		unitSpinner.setEnabled(enabled);
	}

	private static String describe(Throwable ex)
	{
		Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
		String message = cause.getMessage();
		return (message != null && !message.isBlank()) ? message : cause.toString();
	}

	private ClientConfig currentValues()
	{
		return new ClientConfig(
				hostField.getText().trim(),
				(Integer) portSpinner.getValue(),
				(Integer) unitSpinner.getValue());
	}

	private void installDirtyListeners()
	{
		hostField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override public void insertUpdate(DocumentEvent e)  { onUiChange(); }
			@Override public void removeUpdate(DocumentEvent e)  { onUiChange(); }
			@Override public void changedUpdate(DocumentEvent e) { onUiChange(); }
		});
		portSpinner.addChangeListener(_ -> onUiChange());
		unitSpinner.addChangeListener(_ -> onUiChange());
	}

	private void onUiChange()
	{
		if (loading)
		{
			return;
		}
		dirty = !currentValues().equals(baseline);
	}

	private void applyConfig(ClientConfig cfg)
	{
		loading = true;
		try
		{
			hostField.setText(cfg.host());
			portSpinner.setValue(cfg.port());
			unitSpinner.setValue(cfg.unitId());
		}
		finally
		{
			loading = false;
		}
		baseline = new ClientConfig(cfg.host().trim(), cfg.port(), cfg.unitId());
		dirty = false;
	}

	private void loadConfigOnStartup()
	{
		File file = ConfigStore.DEFAULT_FILE;
		if (!file.isFile())
		{
			log("No saved settings at " + file.getAbsolutePath() + " - using defaults.");
			return;
		}
		try
		{
			ClientConfig cfg = ConfigStore.load(file);
			applyConfig(cfg);
			currentConfigFile = file;
			log("Loaded settings from " + file.getAbsolutePath());
		}
		catch (Exception ex)
		{
			log("Failed to load settings from " + file.getAbsolutePath() + ": " + describe(ex));
		}
	}

	private void openConfig()
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Open settings");
		chooser.setFileFilter(new FileNameExtensionFilter("XML files (*.xml)", "xml"));
		File dir = ConfigStore.DEFAULT_FILE.getParentFile();
		if (dir != null && dir.isDirectory())
		{
			chooser.setCurrentDirectory(dir);
		}
		File preset = currentConfigFile.isFile() ? currentConfigFile
				: new File(dir != null ? dir : new File("."), ConfigStore.DEFAULT_FILE.getName());
		chooser.setSelectedFile(preset);
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}
		File file = chooser.getSelectedFile();
		try
		{
			ClientConfig cfg = ConfigStore.load(file);
			applyConfig(cfg);
			currentConfigFile = file;
			log("Loaded settings from " + file.getAbsolutePath());
		}
		catch (Exception ex)
		{
			JOptionPane.showMessageDialog(this,
					"Failed to load settings:\n\n" + describe(ex),
					"Open settings",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void saveConfig()
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Save settings");
		chooser.setFileFilter(new FileNameExtensionFilter("XML files (*.xml)", "xml"));
		File dir = ConfigStore.DEFAULT_FILE.getParentFile();
		if (dir != null)
		{
			if (!dir.isDirectory())
			{
				dir.mkdirs();
			}
			if (dir.isDirectory())
			{
				chooser.setCurrentDirectory(dir);
			}
		}
		File preset = currentConfigFile != null ? currentConfigFile
				: new File(dir != null ? dir : new File("."), ConfigStore.DEFAULT_FILE.getName());
		chooser.setSelectedFile(preset);
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}
		File file = chooser.getSelectedFile();
		if (!file.getName().contains("."))
		{
			file = new File(file.getParentFile(), file.getName() + ".xml");
		}
		writeConfig(file);
	}

	private boolean writeConfig(File file)
	{
		ClientConfig cfg = currentValues();
		try
		{
			ConfigStore.save(file, cfg);
			baseline = cfg;
			dirty = false;
			currentConfigFile = file;
			log("Saved settings to " + file.getAbsolutePath());
			return true;
		}
		catch (Exception ex)
		{
			JOptionPane.showMessageDialog(this,
					"Failed to save settings:\n\n" + describe(ex),
					"Save settings",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	/** Appends a timestamped line to the activity log. Safe to call from any thread. */
	public void log(String message)
	{
		SwingUtilities.invokeLater(() ->
		{
			logArea.append(timeFormat.format(new Date()) + "  " + message + "\n");
			int extra = logArea.getLineCount() - MAX_LOG_LINES;
			if (extra > 0)
			{
				try
				{
					logArea.replaceRange("", 0, logArea.getLineEndOffset(extra - 1));
				}
				catch (BadLocationException ignored)
				{
					// trimming is best-effort
				}
			}
			logArea.setCaretPosition(logArea.getDocument().getLength());
		});
	}

	/** Logs an error with its root cause message. Safe to call from any thread. */
	public void logError(String message, Throwable cause)
	{
		log(message + ": " + describe(cause));
	}
}
