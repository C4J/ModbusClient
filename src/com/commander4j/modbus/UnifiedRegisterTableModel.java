package com.commander4j.modbus;

import java.util.function.BiConsumer;

import javax.swing.table.AbstractTableModel;

import com.digitalpetri.modbus.exceptions.ModbusException;

/**
 * Single Swing table model that exposes all four Modbus data tables aligned by
 * a shared zero-based address. Holds parallel snapshots of a contiguous address
 * window populated by the panel's polling thread:
 *
 * <ul>
 *   <li>Coil bit + conventional reference (1..)  - editable</li>
 *   <li>Discrete input bit + conventional reference (10001..)  - read-only on the wire</li>
 *   <li>Input register + reference (30001..) + hex view  - read-only on the wire</li>
 *   <li>Holding register + reference (40001..) + hex view  - editable</li>
 * </ul>
 *
 * <p>Modbus has no client-write function code for discrete inputs or input registers, so
 * those Value columns are intentionally non-editable. Editing a Coil or Holding Register
 * Value column writes through to the {@link ClientController}; the optimistic local
 * snapshot value is reverted if the write throws.
 */
public class UnifiedRegisterTableModel extends AbstractTableModel
{

	private static final long serialVersionUID = 1L;

	public static final int COL_ADDRESS = 0;
	public static final int COL_COIL_REF = 1;
	public static final int COL_COIL_VAL = 2;
	public static final int COL_DISC_REF = 3;
	public static final int COL_DISC_VAL = 4;
	public static final int COL_INPUT_REF = 5;
	public static final int COL_INPUT_VAL = 6;
	public static final int COL_INPUT_HEX = 7;
	public static final int COL_HOLD_REF = 8;
	public static final int COL_HOLD_VAL = 9;
	public static final int COL_HOLD_HEX = 10;

	private static final int COLUMN_COUNT = 11;

	private final ClientController controller;
	private final BiConsumer<String, Throwable> errorLog;

	private int startAddress;
	private int count;

	private boolean[] coils = new boolean[0];
	private boolean[] discrete = new boolean[0];
	private int[] input = new int[0];
	private int[] holding = new int[0];

	public UnifiedRegisterTableModel(ClientController controller, BiConsumer<String, Throwable> errorLog)
	{
		this.controller = controller;
		this.errorLog = errorLog;
		this.startAddress = 0;
		this.count = 64;
		this.coils = new boolean[count];
		this.discrete = new boolean[count];
		this.input = new int[count];
		this.holding = new int[count];
	}

	public int getStartAddress()
	{
		return startAddress;
	}

	public int getCount()
	{
		return count;
	}

	/**
	 * Changes the visible address window. Snapshots are resized to {@code count} and
	 * zero-filled; the next polling tick will populate them with live values.
	 */
	public void setRange(int startAddress, int count)
	{
		this.startAddress = startAddress;
		this.count = count;
		this.coils = new boolean[count];
		this.discrete = new boolean[count];
		this.input = new int[count];
		this.holding = new int[count];
		fireTableDataChanged();
	}

	/**
	 * Applies a freshly polled snapshot. Each kind is compared to the current value;
	 * only rows whose value actually changed are repainted, and a row currently being
	 * edited is skipped so the operator's keystrokes are not lost. Must be called on
	 * the EDT.
	 *
	 * <p>Any of the four arrays may be {@code null} - meaning "this kind was not
	 * polled this tick" - and is left untouched.
	 */
	public void applyPolledData(boolean[] newCoils, boolean[] newDiscrete,
			int[] newInput, int[] newHolding, int editingRow)
	{
		for (int row = 0; row < count; row++)
		{
			if (row == editingRow)
			{
				continue;
			}
			boolean changed = false;
			if (newCoils != null && row < newCoils.length && coils[row] != newCoils[row])
			{
				coils[row] = newCoils[row];
				changed = true;
			}
			if (newDiscrete != null && row < newDiscrete.length && discrete[row] != newDiscrete[row])
			{
				discrete[row] = newDiscrete[row];
				changed = true;
			}
			if (newInput != null && row < newInput.length && input[row] != newInput[row])
			{
				input[row] = newInput[row];
				changed = true;
			}
			if (newHolding != null && row < newHolding.length && holding[row] != newHolding[row])
			{
				holding[row] = newHolding[row];
				changed = true;
			}
			if (changed)
			{
				fireTableRowsUpdated(row, row);
			}
		}
	}

	@Override
	public int getRowCount()
	{
		return count;
	}

	@Override
	public int getColumnCount()
	{
		return COLUMN_COUNT;
	}

	@Override
	public String getColumnName(int column)
	{
		return switch (column)
		{
			case COL_ADDRESS -> "Address";
			case COL_COIL_REF, COL_DISC_REF, COL_INPUT_REF, COL_HOLD_REF -> "Modbus Ref";
			case COL_COIL_VAL, COL_DISC_VAL, COL_INPUT_VAL, COL_HOLD_VAL -> "Value";
			case COL_INPUT_HEX, COL_HOLD_HEX -> "Hex";
			default -> "";
		};
	}

	@Override
	public Class<?> getColumnClass(int column)
	{
		return switch (column)
		{
			case COL_COIL_VAL, COL_DISC_VAL -> Boolean.class;
			case COL_INPUT_HEX, COL_HOLD_HEX -> String.class;
			default -> Integer.class;
		};
	}

	@Override
	public boolean isCellEditable(int row, int column)
	{
		if (!controller.isConnected())
		{
			return false;
		}
		return column == COL_COIL_VAL || column == COL_HOLD_VAL;
	}

	@Override
	public Object getValueAt(int row, int column)
	{
		int address = startAddress + row;
		return switch (column)
		{
			case COL_ADDRESS -> address;
			case COL_COIL_REF -> RegisterKind.COILS.conventionalBase + address;
			case COL_COIL_VAL -> coils[row];
			case COL_DISC_REF -> RegisterKind.DISCRETE_INPUTS.conventionalBase + address;
			case COL_DISC_VAL -> discrete[row];
			case COL_INPUT_REF -> RegisterKind.INPUT_REGISTERS.conventionalBase + address;
			case COL_INPUT_VAL -> input[row];
			case COL_INPUT_HEX -> String.format("0x%04X", input[row] & 0xFFFF);
			case COL_HOLD_REF -> RegisterKind.HOLDING_REGISTERS.conventionalBase + address;
			case COL_HOLD_VAL -> holding[row];
			case COL_HOLD_HEX -> String.format("0x%04X", holding[row] & 0xFFFF);
			default -> null;
		};
	}

	@Override
	public void setValueAt(Object value, int row, int column)
	{
		if (!isCellEditable(row, column))
		{
			return;
		}
		int address = startAddress + row;
		switch (column)
		{
			case COL_COIL_VAL ->
			{
				boolean v = Boolean.TRUE.equals(value);
				boolean previous = coils[row];
				coils[row] = v;
				fireTableRowsUpdated(row, row);
				try
				{
					controller.writeBit(RegisterKind.COILS, address, v);
				}
				catch (ModbusException ex)
				{
					coils[row] = previous;
					fireTableRowsUpdated(row, row);
					errorLog.accept("Failed to write Coil " + address, ex);
				}
			}
			case COL_HOLD_VAL ->
			{
				Integer parsed = parseRegister(value);
				if (parsed == null)
				{
					return;
				}
				int previous = holding[row];
				holding[row] = parsed;
				fireTableRowsUpdated(row, row);
				try
				{
					controller.writeRegister(RegisterKind.HOLDING_REGISTERS, address, parsed);
				}
				catch (ModbusException ex)
				{
					holding[row] = previous;
					fireTableRowsUpdated(row, row);
					errorLog.accept("Failed to write Holding Register " + address, ex);
				}
			}
			default ->
			{
				// COL_DISC_VAL and COL_INPUT_VAL are read-only - isCellEditable already
				// excludes them, but the switch is exhaustive in intent.
			}
		}
	}

	private static Integer parseRegister(Object value)
	{
		try
		{
			int n = ((Number) value).intValue();
			return Math.max(0, Math.min(0xFFFF, n));
		}
		catch (RuntimeException notANumber)
		{
			return null;
		}
	}
}
