package com.net.train.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class AnalysisIpa {

	public static final String METADATA_FILE_NAME = "Payload/PatientEnterprise.app/Info.plist";

	@SuppressWarnings("resource")
	public static String getMetadata(File ipa) throws IOException {
		String bundleVersion = "";
		ZipFile zip = new ZipFile(ipa);
		Enumeration<? extends ZipEntry> entries = zip.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			String entryName = entry.getName();
			System.out.println(entryName);
			if (entryName.equals(METADATA_FILE_NAME)) {
				bundleVersion = AnalysisIpa.getMetadata(zip.getInputStream(entry));
				break;
			}
		}

		return bundleVersion;
	}

	public static String getMetadata(InputStream plist) {
		String bundleVersion = "";
		Document document = getDoc(plist);
		// 为了不遭遇空字符串Node, 不得不取出所有element
		// 如果使用nextSibling, 会取出#Text节点, 是一个空字符串...
		NodeList nodeList = document.getElementsByTagName("*");
		for (int i = 0, length = nodeList.getLength(); i < length; i += 2) {
			Node node = nodeList.item(i);
			if (!node.getNodeName().equals("key")) {
				continue;
			}
			String key = node.getTextContent();
			String value = nodeList.item(i + 1).getTextContent();

			if (key.equals("CFBundleShortVersionString")) {
				bundleVersion = value;
			}

		}

		return bundleVersion;
	}

	public static void main(String[] args) throws Exception {
		File file = new File("E://manifest.plist");
		InputStream infoIs = new FileInputStream(file);
		Document doc = getDoc(infoIs);
		NodeList nodeList = doc.getElementsByTagName("*");
		for (int i = 0, length = nodeList.getLength(); i < length; i += 2) {
			Node node = nodeList.item(i);
			if (!node.getNodeName().equals("key")) {
				continue;
			}
			String key = node.getTextContent();
			String value = nodeList.item(i + 1).getTextContent();
			if (StringUtils.isNotBlank(value) && value.length() > 3) {
				if (value.substring(value.length() - 3).toLowerCase().equals("ipa")) {
					nodeList.item(i + 1).setTextContent("");
				}
			}
			System.out.println("key:" + key + "\n" + "value:" + value);
		}
	}

	public static Document getDoc(InputStream plist) {
		Document document = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			// 关闭解析XML文件时, 通过DTD验证XML的功能
			// 因为如果网络不通会导致解析失败
			db.setEntityResolver(new EntityResolver() {
				public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
					return new InputSource(new StringReader(""));
					// 如果直接return null仍然需要联网获取DTD来验证
					// return null;
				}
			});
			document = db.parse(plist);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return document;
	}
}