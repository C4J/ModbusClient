package com.commander4j.modbus;

/**
 * Immutable snapshot of the three Modbus client settings persisted to {@code xml/config/config.xml}:
 * server host (IP), TCP port, and Modbus unit ID. Used by {@link ConfigStore} for load/save and by
 * {@link ClientFrame} as the baseline against which UI edits are compared to determine the
 * "unsaved changes" state.
 */
public record ClientConfig(String host, int port, int unitId)
{
}
