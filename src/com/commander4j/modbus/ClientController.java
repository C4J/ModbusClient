package com.commander4j.modbus;

import java.time.Duration;

import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.exceptions.ModbusException;
import com.digitalpetri.modbus.pdu.ReadCoilsRequest;
import com.digitalpetri.modbus.pdu.ReadCoilsResponse;
import com.digitalpetri.modbus.pdu.ReadDiscreteInputsRequest;
import com.digitalpetri.modbus.pdu.ReadDiscreteInputsResponse;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersResponse;
import com.digitalpetri.modbus.pdu.ReadInputRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadInputRegistersResponse;
import com.digitalpetri.modbus.pdu.WriteMultipleCoilsRequest;
import com.digitalpetri.modbus.pdu.WriteMultipleRegistersRequest;
import com.digitalpetri.modbus.pdu.WriteSingleCoilRequest;
import com.digitalpetri.modbus.pdu.WriteSingleRegisterRequest;
import com.digitalpetri.modbus.tcp.client.NettyTcpClientTransport;

/**
 * Owns the Modbus/TCP client lifecycle. A fresh {@link NettyTcpClientTransport} and
 * {@link ModbusTcpClient} are built on every {@link #connect}; {@link #disconnect}
 * tears them down. The client is re-creatable within the same JVM.
 *
 * <p>All read/write calls are synchronous and throw {@link ModbusException} on any
 * protocol or transport failure. Threading policy is the caller's choice:
 * {@link UnifiedRegisterPanel} drives the periodic reads from its own poll thread,
 * while operator-driven writes run on whatever thread invokes {@link #writeBit} or
 * {@link #writeRegister}. The digitalpetri client serialises in-flight requests by
 * MBAP transaction ID, so concurrent invocation is safe.
 *
 * <p>{@link #writeBit} and {@link #writeRegister} only accept the writable spaces
 * (coils / holding registers). Modbus has no function code for a client to write
 * discrete inputs or input registers, so attempts on those kinds throw rather than
 * generating a request the server is required to reject.
 */
public class ClientController
{

	private volatile int unitId = 0;
	private volatile int responseTimeoutMs = 2000;

	private ModbusTcpClient client; // guarded by this
	private volatile boolean connected;

	public int getUnitId()
	{
		return unitId;
	}

	public void setUnitId(int unitId)
	{
		this.unitId = unitId;
	}

	public int getResponseTimeoutMs()
	{
		return responseTimeoutMs;
	}

	public void setResponseTimeoutMs(int responseTimeoutMs)
	{
		this.responseTimeoutMs = responseTimeoutMs;
	}

	public boolean isConnected()
	{
		return connected;
	}

	/**
	 * Builds a fresh transport + client and opens the TCP connection. Blocks until the
	 * connect attempt completes (or fails). {@code connectPersistent(false)} keeps the
	 * explicit connect/disconnect lifecycle in our hands rather than handing it to the
	 * transport's auto-reconnect state machine - the panel's poll loop already provides
	 * the retry behaviour we want.
	 */
	public synchronized void connect(String host, int port) throws Exception
	{
		if (connected)
		{
			return;
		}
		final int timeout = responseTimeoutMs;
		NettyTcpClientTransport transport = NettyTcpClientTransport.create(cfg ->
		{
			cfg.setHostname(host);
			cfg.setPort(port);
			cfg.setConnectTimeout(Duration.ofMillis(timeout));
			cfg.setConnectPersistent(false);
		});
		ModbusTcpClient started = ModbusTcpClient.create(transport);
		started.connect();
		client = started;
		connected = true;
	}

	/** Closes the TCP connection. Safe to call when already disconnected. */
	public synchronized void disconnect() throws Exception
	{
		if (!connected)
		{
			return;
		}
		try
		{
			client.disconnect();
		}
		finally
		{
			client = null;
			connected = false;
		}
	}

	// --- reads ---------------------------------------------------------------------

	/** Reads {@code count} bit values (coils or discrete inputs) starting at {@code start}. */
	public boolean[] readBits(RegisterKind kind, int start, int count) throws ModbusException
	{
		ModbusTcpClient c = requireConnected();
		byte[] packed;
		if (kind == RegisterKind.COILS)
		{
			ReadCoilsResponse r = c.readCoils(unitId, new ReadCoilsRequest(start, count));
			packed = r.coils();
		}
		else if (kind == RegisterKind.DISCRETE_INPUTS)
		{
			ReadDiscreteInputsResponse r = c.readDiscreteInputs(unitId, new ReadDiscreteInputsRequest(start, count));
			packed = r.inputs();
		}
		else
		{
			throw new IllegalArgumentException("readBits requires a bit-typed kind, got " + kind);
		}
		return unpackBits(packed, count);
	}

	/** Reads {@code count} register values (holding or input) starting at {@code start}. */
	public int[] readRegisters(RegisterKind kind, int start, int count) throws ModbusException
	{
		ModbusTcpClient c = requireConnected();
		byte[] data;
		if (kind == RegisterKind.HOLDING_REGISTERS)
		{
			ReadHoldingRegistersResponse r = c.readHoldingRegisters(unitId, new ReadHoldingRegistersRequest(start, count));
			data = r.registers();
		}
		else if (kind == RegisterKind.INPUT_REGISTERS)
		{
			ReadInputRegistersResponse r = c.readInputRegisters(unitId, new ReadInputRegistersRequest(start, count));
			data = r.registers();
		}
		else
		{
			throw new IllegalArgumentException("readRegisters requires a register-typed kind, got " + kind);
		}
		return unpackRegisters(data, count);
	}

	// --- writes --------------------------------------------------------------------

	/**
	 * Writes a single coil. Discrete inputs are read-only over Modbus and throw
	 * {@link IllegalArgumentException} rather than producing a request the server is
	 * obliged to reject.
	 */
	public void writeBit(RegisterKind kind, int address, boolean value) throws ModbusException
	{
		if (kind != RegisterKind.COILS)
		{
			throw new IllegalArgumentException("writeBit is only valid for COILS, got " + kind);
		}
		ModbusTcpClient c = requireConnected();
		c.writeSingleCoil(unitId, new WriteSingleCoilRequest(address, value));
	}

	/**
	 * Writes a single holding register. Input registers are read-only over Modbus and
	 * throw {@link IllegalArgumentException} for the same reason as {@link #writeBit}.
	 */
	public void writeRegister(RegisterKind kind, int address, int value) throws ModbusException
	{
		if (kind != RegisterKind.HOLDING_REGISTERS)
		{
			throw new IllegalArgumentException("writeRegister is only valid for HOLDING_REGISTERS, got " + kind);
		}
		ModbusTcpClient c = requireConnected();
		c.writeSingleRegister(unitId, new WriteSingleRegisterRequest(address, value & 0xFFFF));
	}

	/**
	 * Clears every address in the given range using multi-write Modbus functions
	 * (FC15 / FC16), chunked to respect the per-request limits in the spec: 1968
	 * coils and 123 registers. Only valid for the writable spaces (coils and
	 * holding registers); other kinds throw.
	 */
	public void zeroRange(RegisterKind kind, int start, int count) throws ModbusException
	{
		if (kind != RegisterKind.COILS && kind != RegisterKind.HOLDING_REGISTERS)
		{
			throw new IllegalArgumentException("zeroRange is only valid for writable spaces, got " + kind);
		}
		ModbusTcpClient c = requireConnected();
		int chunkSize = (kind == RegisterKind.COILS) ? 1968 : 123;
		int remaining = count;
		int address = start;
		while (remaining > 0)
		{
			int n = Math.min(remaining, chunkSize);
			if (kind == RegisterKind.COILS)
			{
				byte[] packed = new byte[(n + 7) / 8];
				c.writeMultipleCoils(unitId, new WriteMultipleCoilsRequest(address, n, packed));
			}
			else
			{
				byte[] values = new byte[n * 2];
				c.writeMultipleRegisters(unitId, new WriteMultipleRegistersRequest(address, n, values));
			}
			address += n;
			remaining -= n;
		}
	}

	private synchronized ModbusTcpClient requireConnected()
	{
		ModbusTcpClient c = client;
		if (c == null || !connected)
		{
			throw new IllegalStateException("Modbus client is not connected");
		}
		return c;
	}

	/** Unpacks a Modbus read-coils/discrete-inputs response: 8 bit values per byte, LSB first. */
	private static boolean[] unpackBits(byte[] packed, int count)
	{
		boolean[] values = new boolean[count];
		if (packed == null)
		{
			return values;
		}
		for (int i = 0; i < count; i++)
		{
			int byteIndex = i >>> 3;
			if (byteIndex >= packed.length)
			{
				break;
			}
			values[i] = ((packed[byteIndex] >> (i & 0x07)) & 0x01) != 0;
		}
		return values;
	}

	/** Unpacks a Modbus read-registers response: two bytes per register, MSB first. */
	private static int[] unpackRegisters(byte[] data, int count)
	{
		int[] values = new int[count];
		if (data == null)
		{
			return values;
		}
		for (int i = 0; i < count; i++)
		{
			int offset = i * 2;
			if (offset + 1 >= data.length)
			{
				break;
			}
			values[i] = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
		}
		return values;
	}
}
