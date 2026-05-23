package com.commander4j.modbus;

/**
 * The four Modbus data tables exposed by the server.
 *
 * <p>{@link #bit} distinguishes single-bit tables (coils, discrete inputs) from 16-bit
 * register tables (holding, input). {@link #conventionalBase} is the offset added to the
 * zero-based protocol address to obtain the conventional Modbus reference number shown to
 * the operator (e.g. coil 0 is reference 1, holding register 0 is reference 40001).
 */
public enum RegisterKind
{
	COILS("Coils", true, 1),
	DISCRETE_INPUTS("Discrete Inputs", true, 10001),
	HOLDING_REGISTERS("Holding Registers", false, 40001),
	INPUT_REGISTERS("Input Registers", false, 30001);

	/** Human-readable name, used for the tab title. */
	public final String label;

	/** {@code true} for single-bit tables, {@code false} for 16-bit register tables. */
	public final boolean bit;

	/** Offset from zero-based protocol address to the conventional Modbus reference. */
	public final int conventionalBase;

	RegisterKind(String label, boolean bit, int conventionalBase)
	{
		this.label = label;
		this.bit = bit;
		this.conventionalBase = conventionalBase;
	}
}
