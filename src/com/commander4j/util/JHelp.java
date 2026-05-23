package com.commander4j.util;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;

import javax.swing.JButton;
import javax.swing.JMenuItem;

import com.commander4j.sys.Common;

public class JHelp
{
	private String helpURL;
	private static boolean HelpAvailable = false;
	public static Desktop desktop;

	public JHelp()
	{
		init();
	}

	private void setHelpURL(String value)
	{
		if (value == null || value.isEmpty())
			helpURL = Common.helpURL;
		else
			helpURL = value;
	}

	private void init()
	{
		try
		{
			if (Desktop.isDesktopSupported())
			{
				desktop = Desktop.getDesktop();
				HelpAvailable = true;
			}
			else
			{
				HelpAvailable = false;
			}
		}
		catch (Exception e)
		{
			HelpAvailable = false;
		}
	}

	public void enableHelpOnButton(JButton button, String helpsetID)
	{
		if (HelpAvailable)
		{
			try
			{
				setHelpURL(helpsetID);
				button.addActionListener(new ButtonHandler());
			}
			catch (Exception ex)
			{
				HelpAvailable = false;
			}
		}
	}

	public void enableHelpOnMenuItem(JMenuItem item, String helpsetID)
	{
		if (HelpAvailable)
		{
			try
			{
				setHelpURL(helpsetID);
				item.addActionListener(new ButtonHandler());
			}
			catch (Exception ex)
			{
				HelpAvailable = false;
			}
		}
	}

	private class ButtonHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
			if (HelpAvailable)
			{
				try
				{
					URI uri;
					if (helpURL.contains("http"))
					{
						uri = new URI(helpURL);
						desktop.browse(uri);
					}
					else
					{
						File file = new File(helpURL);
						uri = file.toURI().normalize();
						desktop.browse(uri);
					}
				}
				catch (Exception ex)
				{
				}
			}
		}
	}
}
