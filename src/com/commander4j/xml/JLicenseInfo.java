package com.commander4j.xml;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import com.commander4j.sys.Common;
import com.commander4j.util.JUtility;

public class JLicenseInfo implements Comparable<JLicenseInfo>
{
	public static int width_description = 30;
	public static int width_version     = 12;
	public static int width_type        = 15;
	public String description;
	public String licenceFilename;
	public String version;
	public String type;
	private static JXMLDocument xmlMessage;

	public JLicenseInfo()
	{
	}

	public JLicenseInfo(String desc, String filename, String version, String type)
	{
		this.description    = desc;
		this.licenceFilename = filename;
		this.version        = version;
		this.type           = type;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getLicenceFilename()
	{
		return licenceFilename;
	}

	public void setLicenceFilename(String licenceFilename)
	{
		this.licenceFilename = licenceFilename;
	}

	public String toString()
	{
		return JUtility.padString(description, true, width_description, " ")
		     + JUtility.padString(version,     true, width_version,     " ")
		     + type;
	}

	public LinkedList<JLicenseInfo> getLicences()
	{
		HashMap<String, String> licenceTypes = new HashMap<>();
		licenceTypes.clear();

		String filename = "." + File.separator + "lib" + File.separator + "license" + File.separator + "LicenseInfo.xml";
		xmlMessage = new JXMLDocument(filename);

		LinkedList<JLicenseInfo> typeList = new LinkedList<>();

		int position = 1;
		while (!xmlMessage.findXPath("//info/licenses/license[" + position + "]/type").trim().isEmpty())
		{
			String t    = xmlMessage.findXPath("//info/licenses/license[" + position + "]/type").trim();
			String file = xmlMessage.findXPath("//info/licenses/license[" + position + "]/filename").trim();
			licenceTypes.put(t, file);
			position++;
		}

		if (!licenceTypes.containsKey("GNU General Public License V2"))
			licenceTypes.put("GNU General Public License V2", "GNU General Public License V2.txt");

		position = 1;
		while (!xmlMessage.findXPath("//info/libraries/library[" + position + "]/description").trim().isEmpty())
		{
			String desc      = xmlMessage.findXPath("//info/libraries/library[" + position + "]/description").trim();
			String t         = xmlMessage.findXPath("//info/libraries/library[" + position + "]/type").trim();
			String ver       = xmlMessage.findXPath("//info/libraries/library[" + position + "]/version").trim();
			String lfilename = JUtility.replaceNullStringwithBlank(licenceTypes.get(t));
			typeList.add(new JLicenseInfo(desc, lfilename, ver, t));
			position++;
		}

		typeList.add(new JLicenseInfo(Common.programName, "GNU General Public License V2.txt", Common.version, "GNU General Public License V2"));

		Collections.sort(typeList);
		return typeList;
	}

	@Override
	public int compareTo(JLicenseInfo o)
	{
		return this.description.toUpperCase().compareTo(o.description.toUpperCase());
	}
}
