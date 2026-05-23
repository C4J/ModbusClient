package com.commander4j.dialog;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.commander4j.gui.JButton4j;
import com.commander4j.gui.JLabel4j_std;
import com.commander4j.sys.Common;
import com.commander4j.util.JUtility;

public class JDialogAbout extends JDialog
{
	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private static int widthadjustment = 0;
	private static int heightadjustment = 0;
	private JLabel4j_std jLabelWebPage;

	public JDialogAbout()
	{
		setResizable(false);
		setTitle("About");
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.APPLICATION_MODAL);
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		JUtility.setLookAndFeel("Nimbus");

		setBounds(100, 100, 280, 200);
		getContentPane().setLayout(null);
		contentPanel.setBackground(Common.color_app_window);
		contentPanel.setBounds(0, 0, getWidth(), getHeight());
		contentPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		getContentPane().add(contentPanel);
		contentPanel.setLayout(null);

		JLabel4j_std lbl_program = new JLabel4j_std();
		lbl_program.setFont(new Font("Arial", Font.BOLD, 13));
		lbl_program.setText(Common.programName + " " + Common.version);
		lbl_program.setHorizontalAlignment(SwingConstants.CENTER);
		lbl_program.setBounds(6, 11, 264, 22);
		contentPanel.add(lbl_program);

		JLabel4j_std lbl_description = new JLabel4j_std("Description");
		lbl_description.setFont(new Font("Arial", Font.ITALIC, 13));
		lbl_description.setText("David Garratt");
		lbl_description.setHorizontalAlignment(SwingConstants.CENTER);
		lbl_description.setBounds(6, 52, 264, 22);
		contentPanel.add(lbl_description);

		JButton4j okButton = new JButton4j(Common.icon_ok);
		okButton.setText("Ok");
		okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				dispose();
			}
		});
		okButton.setBounds(85, 122, 103, 30);
		contentPanel.add(okButton);
		okButton.setActionCommand("OK");
		getRootPane().setDefaultButton(okButton);

		jLabelWebPage = new JLabel4j_std();
		jLabelWebPage.setFont(new Font("Arial", Font.PLAIN, 12));
		jLabelWebPage.setHorizontalAlignment(SwingConstants.CENTER);
		jLabelWebPage.setText("https://www.commander4j.com");
		jLabelWebPage.setBounds(2, 86, 269, 14);
		jLabelWebPage.setForeground(new Color(0, 0, 255));
		jLabelWebPage.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt)
			{
				try
				{
					if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
						Desktop.getDesktop().browse(new URI("https://www.commander4j.com"));
				}
				catch (Exception ex)
				{
					JOptionPane.showMessageDialog(JDialogAbout.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}

			public void mouseExited(MouseEvent evt)
			{
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}

			public void mouseEntered(MouseEvent evt)
			{
				setCursor(new Cursor(Cursor.HAND_CURSOR));
			}
		});
		contentPanel.add(jLabelWebPage);

		widthadjustment = JUtility.getOSWidthAdjustment();
		heightadjustment = JUtility.getOSHeightAdjustment();

		GraphicsDevice gd = JUtility.getGraphicsDevice();
		GraphicsConfiguration gc = gd.getDefaultConfiguration();
		Rectangle screenBounds = gc.getBounds();

		setBounds(screenBounds.x + ((screenBounds.width  - getWidth())  / 2),
		          screenBounds.y + ((screenBounds.height - getHeight()) / 2),
		          getWidth()  + widthadjustment,
		          getHeight() + heightadjustment);

		SwingUtilities.invokeLater(() ->
		{
			JLabel4j_std lbl_type = new JLabel4j_std("Type");
			lbl_type.setFont(new Font("Arial", Font.ITALIC, 13));
			lbl_type.setText("Written by");
			lbl_type.setBounds(6, 33, 264, 18);
			lbl_type.setHorizontalAlignment(SwingConstants.CENTER);
			contentPanel.add(lbl_type);
		});
	}
}
