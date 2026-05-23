package com.commander4j.modbus;

import javax.swing.SwingUtilities;

import com.commander4j.util.JUtility;

/**
 * Entry point for the Commander4j Modbus TCP client tool.
 *
 * <p>The application hosts a Modbus/TCP client (digitalpetri modbus 2.1.5) that
 * polls a configurable address window on a connected server and exposes the
 * coils, discrete inputs, holding registers and input registers in a unified
 * Swing table. Coil and Holding Register cells are editable and write through
 * to the server; discrete inputs and input registers are read-only because
 * Modbus has no client-write function code for those tables.
 */
public final class ModbusClientApp
{

	private ModbusClientApp()
	{
	}

	public static void main(String[] args)
	{
		JUtility.setLookAndFeel("Nimbus");
		SwingUtilities.invokeLater(() -> new ClientFrame().setVisible(true));
	}
}
