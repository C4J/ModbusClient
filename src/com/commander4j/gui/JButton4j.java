package com.commander4j.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

import com.commander4j.sys.Common;

public class JButton4j extends JButton
{
    private static final long serialVersionUID = 1L;

    private void init()
    {
        setFont(Common.font_btn);
        setForeground(Common.color_button_font);
        setOpaque(false);
        setBackground(Common.color_button);
        setBorderPainted(true);
        setContentAreaFilled(true);
        setFocusable(false);

        addMouseListener(new MouseAdapter()
        {
            @Override public void mouseEntered(MouseEvent e) { if (isEnabled()) { setBackground(Common.color_button_hover); setForeground(Common.color_button_font_hover); setFont(Common.font_bold); } }
            @Override public void mouseExited(MouseEvent e)  { setBackground(Common.color_button); setForeground(Common.color_button_font); setFont(Common.font_btn); }
            @Override public void mousePressed(MouseEvent e) { if (isEnabled()) { setBackground(Common.color_button_hover); setForeground(Common.color_button_font_hover); setFont(Common.font_bold); } }
            @Override public void mouseReleased(MouseEvent e){ setBackground(Common.color_button); setForeground(Common.color_button_font); setFont(Common.font_btn); }
            @Override public void mouseClicked(MouseEvent e) { if (isEnabled()) { setBackground(Common.color_button); setForeground(Common.color_button_font); } }
        });
    }

    public JButton4j()                        { super();        init(); }
    public JButton4j(Icon icon)               { super(icon);    init(); }
    public JButton4j(String text)             { super(text);    setToolTipText(text); init(); }
    public JButton4j(Action a)                { super(a);       init(); }
    public JButton4j(String text, Icon icon)  { super(text, icon); setToolTipText(text); init(); }

    @Override
    public void setText(String text) { super.setText(text); setToolTipText(text); }
}
