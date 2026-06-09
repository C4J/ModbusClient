package com.commander4j.modbus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Reads and writes the three Modbus client settings (ip, port, id) to a simple XML file:
 * <pre>{@code
 * <config>
 *   <ip>127.0.0.1</ip>
 *   <port>502</port>
 *   <id>1</id>
 * </config>
 * }</pre>
 * The conventional location is {@link #DEFAULT_FILE} ({@code xml/config/config.xml}), but the
 * open/save chooser allows any path. The parent directory is created on save if missing.
 */
public final class ConfigStore
{
	public static final File DEFAULT_FILE = new File("xml" + File.separator + "config" + File.separator + "config.xml");

	private ConfigStore()
	{
	}

	public static ClientConfig load(File file) throws Exception
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = dbf.newDocumentBuilder();
		Document doc = builder.parse(file);
		doc.getDocumentElement().normalize();

		String ip = readText(doc, "ip");
		int port = Integer.parseInt(readText(doc, "port"));
		int unitId = Integer.parseInt(readText(doc, "id"));
		if (port < 1 || port > 65535)
		{
			throw new IllegalArgumentException("port out of range (1..65535): " + port);
		}
		if (unitId < 0 || unitId > 247)
		{
			throw new IllegalArgumentException("id out of range (0..247): " + unitId);
		}
		return new ClientConfig(ip, port, unitId);
	}

	public static void save(File file, ClientConfig cfg) throws IOException, Exception
	{
		File parent = file.getParentFile();
		if (parent != null)
		{
			Files.createDirectories(parent.toPath());
		}

		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = builder.newDocument();
		Element root = doc.createElement("config");
		doc.appendChild(root);
		appendTextElement(doc, root, "ip", cfg.host());
		appendTextElement(doc, root, "port", Integer.toString(cfg.port()));
		appendTextElement(doc, root, "id", Integer.toString(cfg.unitId()));

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		transformer.transform(new DOMSource(doc), new StreamResult(file));
	}

	private static String readText(Document doc, String tag)
	{
		NodeList nodes = doc.getElementsByTagName(tag);
		if (nodes.getLength() == 0)
		{
			throw new IllegalArgumentException("Missing <" + tag + "> element");
		}
		String text = nodes.item(0).getTextContent();
		return text == null ? "" : text.trim();
	}

	private static void appendTextElement(Document doc, Element parent, String tag, String value)
	{
		Element e = doc.createElement(tag);
		e.setTextContent(value);
		parent.appendChild(e);
	}
}
