package com.commander4j.modbus;

import java.awt.Color;

/**
 * Describes a contiguous range of columns that share a label, background colour
 * and text colour. Used by {@link GroupableTableHeader} to paint a spanning
 * band above the grouped columns, and by the body cell renderers to colour
 * each cell to match its group.
 */
public record ColumnGroup(String label, Color colour, Color textColour, int firstColumn, int lastColumn)
{
	public boolean contains(int column)
	{
		return column >= firstColumn && column <= lastColumn;
	}
}
