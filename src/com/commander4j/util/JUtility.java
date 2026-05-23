package com.commander4j.util;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

import com.commander4j.sys.Common;


public class JUtility
{
	public static GraphicsDevice getGraphicsDevice()
	{
		GraphicsDevice result;

		Point mouseLocation = MouseInfo.getPointerInfo().getLocation();

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

		GraphicsDevice[] devices;

		try
		{
			devices = ge.getScreenDevices();

			GraphicsDevice currentDevice = null;

			for (GraphicsDevice device : devices)
			{
				Rectangle bounds = device.getDefaultConfiguration().getBounds();
				if (bounds.contains(mouseLocation))
				{
					currentDevice = device;
					break;
				}
			}

			GraphicsDevice[] gs = ge.getScreenDevices();

			String defaultID = currentDevice.getIDstring();

			int monitorIndex = 0;

			for (int x = 0; x < gs.length; x++)
			{
				if (gs[x].getIDstring().equals(defaultID))
				{
					monitorIndex = x;
					break;
				}
			}

			result = gs[monitorIndex];
		}
		catch (HeadlessException ex)
		{
			result = null;
		}

		return result;
	}

	public static int getOSWidthAdjustment()
	{
		int result = 0;
		if (OSValidator.isWindows())
		{
			result = 0;
		}
		if (OSValidator.isMac())
		{
			result = -15;
		}
		if (OSValidator.isSolaris())
		{
			result = 0;
		}
		if (OSValidator.isUnix())
		{
			result = 0;
		}
		return result;
	}

	public static int getOSHeightAdjustment()
	{
		int result = 0;
		if (OSValidator.isWindows())
		{
			result = 0;
		}
		if (OSValidator.isMac())
		{
			result = -13;
		}
		if (OSValidator.isSolaris())
		{
			result = 0;
		}
		if (OSValidator.isUnix())
		{
			result = 0;
		}
		return result;
	}

	public static void adjustForLookandFeel()
	{
		LookAndFeel lf = UIManager.getLookAndFeel();
		if (lf.getName().equals("Mac OS X"))
		{
			Common.LFAdjustWidth = 0;
			Common.LFAdjustHeight = 0;
			Common.LFTreeMenuAdjustWidth = 13;
			Common.LFTreeMenuAdjustHeight = 13;

			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Commander4j");

		}
		else
		{
			Common.LFAdjustWidth = -13;
			Common.LFAdjustHeight = -13;
			Common.LFTreeMenuAdjustWidth = 0;
			Common.LFTreeMenuAdjustHeight = 0;
		}
	}

	public static void setLookandFeel()
	{
		try
		{
			SetLookAndFeel("Metal", "Ocean");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void SetLookAndFeel(String LOOKANDFEEL, String THEME)
	{
		try
		{
			if (LOOKANDFEEL.equals("Metal"))
			{
				if (THEME.equals("DefaultMetal"))
					MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
				else if (THEME.equals("Ocean"))
					MetalLookAndFeel.setCurrentTheme(new OceanTheme());

				UIManager.setLookAndFeel(new MetalLookAndFeel());

			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static String replaceNullStringwithBlank(String value)
	{
		if (value == null)
			return "";
		return value;
	}

	public static String padString(String input, boolean leftAlign, int width, String padChar)
	{
		if (input == null)
			input = "";
		if (input.length() >= width)
			return input.substring(0, width);
		StringBuilder sb = new StringBuilder(width);
		if (leftAlign)
		{
			sb.append(input);
			for (int i = input.length(); i < width; i++)
				sb.append(padChar);
		}
		else
		{
			for (int i = input.length(); i < width; i++)
				sb.append(padChar);
			sb.append(input);
		}
		return sb.toString();
	}

	public static Vector<String> getHostIPAddresses()
	{
		Vector<String> ips = new Vector<>();
		try
		{
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			for (NetworkInterface netint : Collections.list(nets))
			{
				if (netint.isUp())
				{
					Enumeration<InetAddress> addresses = netint.getInetAddresses();
					for (InetAddress addr : Collections.list(addresses))
					{
						String ip = addr.getHostAddress();
						if (ip.contains("."))
						{
							ips.add(ip);
						}
					}
				}
			}
		}
		catch (Exception ignored)
		{
		}
		return ips;
	}

	public static void setLookAndFeel(String LAF)
	{
		try
		{
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
			{
				if (LAF.equals(info.getName()))
				{
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		}
		catch (Exception e)
		{

		}
	}
}
