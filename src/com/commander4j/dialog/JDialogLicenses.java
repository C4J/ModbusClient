package com.commander4j.dialog;

import java.awt.BorderLayout;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import com.commander4j.gui.JButton4j;
import com.commander4j.gui.JLabel4j_std;
import com.commander4j.gui.JList4j;
import com.commander4j.sys.Common;
import com.commander4j.util.JUtility;
import com.commander4j.xml.JLicenseInfo;

public class JDialogLicenses extends JDialog
{
	private static final long serialVersionUID = 1L;
	private JPanel contentPanel = new JPanel();
	JList4j<JLicenseInfo> list = new JList4j<JLicenseInfo>();
	private static int widthadjustment = 0;
	private static int heightadjustment = 0;

	public JDialogLicenses(JFrame frame)
	{
		super(frame);
		JDialogLicenses me = this;
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.APPLICATION_MODAL);
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		JUtility.setLookAndFeel("Nimbus");
		setTitle("Libraries");
		setBounds(100, 100, 540, 350);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(0, 24, 521, 246);
		contentPanel.add(scrollPane);

		populateList();

		list.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2 && !list.isSelectionEmpty())
				{
					JLicenseInfo iii = list.getSelectedValue();
					JDialogDisplayLicense dialog = new JDialogDisplayLicense(me, iii);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});

		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPane.setViewportView(list);

		JPanel buttonPane = new JPanel();
		buttonPane.setBounds(0, 271, 521, 39);
		contentPanel.add(buttonPane);
		buttonPane.setLayout(null);

		JButton4j okButton = new JButton4j(Common.icon_ok);
		okButton.setText("Ok");
		okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				dispose();
			}
		});
		okButton.setBounds(200, 0, 128, 32);
		okButton.setActionCommand("OK");
		buttonPane.add(okButton);
		getRootPane().setDefaultButton(okButton);

		JLabel4j_std lblHeader = new JLabel4j_std(
				JUtility.padString("Library", true, JLicenseInfo.width_description, " ")
				+ JUtility.padString("Version", true, JLicenseInfo.width_version, " ")
				+ "Licence");
		lblHeader.setBounds(3, 5, 727, 16);
		lblHeader.setFont(Common.font_list);
		contentPanel.add(lblHeader);

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		widthadjustment = JUtility.getOSWidthAdjustment();
		heightadjustment = JUtility.getOSHeightAdjustment();

		GraphicsDevice gd = JUtility.getGraphicsDevice();
		GraphicsConfiguration gc = gd.getDefaultConfiguration();
		Rectangle screenBounds = gc.getBounds();

		setBounds(screenBounds.x + ((screenBounds.width  - getWidth())  / 2),
		          screenBounds.y + ((screenBounds.height - getHeight()) / 2),
		          getWidth()  + widthadjustment,
		          getHeight() + heightadjustment);
	}

	private void populateList()
	{
		DefaultComboBoxModel<JLicenseInfo> model = new DefaultComboBoxModel<>();
		LinkedList<JLicenseInfo> licenceList = new JLicenseInfo().getLicences();
		for (JLicenseInfo t : licenceList)
			model.addElement(t);
		ListModel<JLicenseInfo> listModel = model;
		list.setModel(listModel);
		if (model.getSize() > 0)
		{
			list.setSelectedIndex(0);
			list.ensureIndexIsVisible(0);
		}
	}
}
