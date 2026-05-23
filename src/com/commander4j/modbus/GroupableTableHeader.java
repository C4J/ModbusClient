package com.commander4j.modbus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

/**
 * Table header that paints a two-row layout: a top band of {@link ColumnGroup}
 * cells spanning their member columns, with the standard per-column header row
 * below. Sub-column headers render in the lower portion because
 * {@link #getHeaderRect} returns rectangles offset by {@link #GROUP_HEIGHT};
 * the default UI delegate honours that rect when painting each column header.
 *
 * <p>Column resizing remains available via the resize cursor at column edges
 * within the bottom row; reordering should be disabled by the caller because
 * it would tear apart the static group ranges.
 */
public class GroupableTableHeader extends JTableHeader
{

	private static final long serialVersionUID = 1L;

	/** Height in pixels of the top band that holds group labels. */
	private static final int GROUP_HEIGHT = 26;

	private final List<ColumnGroup> groups;

	public GroupableTableHeader(TableColumnModel columnModel, List<ColumnGroup> groups)
	{
		super(columnModel);
		this.groups = List.copyOf(groups);
	}

	@Override
	public Dimension getPreferredSize()
	{
		Dimension d = super.getPreferredSize();
		d.height += GROUP_HEIGHT;
		return d;
	}

	@Override
	public Rectangle getHeaderRect(int column)
	{
		Rectangle r = super.getHeaderRect(column);
		r.y = GROUP_HEIGHT;
		r.height = Math.max(0, getHeight() - GROUP_HEIGHT);
		return r;
	}

	@Override
	public void paint(Graphics g)
	{
		// Paint the group bands first so the per-column header row (drawn by super
		// in its offset rect) ends up underneath nothing important. Some LAFs
		// route header painting through paint() rather than paintComponent(), so
		// we override paint() to be safe.
		super.paint(g);
		paintGroupBands(g);
	}

	private void paintGroupBands(Graphics g)
	{
		TableColumnModel cm = getColumnModel();
		Font font = getFont().deriveFont(Font.BOLD);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		Color grid = gridColour();

		for (ColumnGroup group : groups)
		{
			int x = 0;
			for (int i = 0; i < group.firstColumn(); i++)
			{
				x += cm.getColumn(i).getWidth();
			}
			int width = 0;
			for (int i = group.firstColumn(); i <= group.lastColumn(); i++)
			{
				width += cm.getColumn(i).getWidth();
			}
			if (width <= 0)
			{
				continue;
			}

			g.setColor(group.colour());
			g.fillRect(x, 0, width, GROUP_HEIGHT);

			g.setColor(grid);
			g.drawLine(x, GROUP_HEIGHT - 1, x + width - 1, GROUP_HEIGHT - 1);
			g.drawLine(x + width - 1, 0, x + width - 1, GROUP_HEIGHT - 1);

			String label = group.label();
			if (label != null && !label.isEmpty())
			{
				g.setColor(group.textColour());
				int textWidth = fm.stringWidth(label);
				int textX = x + Math.max(2, (width - textWidth) / 2);
				int textY = (GROUP_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
				g.drawString(label, textX, textY);
			}
		}
	}

	private static Color gridColour()
	{
		Color c = UIManager.getColor("Table.gridColor");
		return c != null ? c : Color.GRAY;
	}
}
