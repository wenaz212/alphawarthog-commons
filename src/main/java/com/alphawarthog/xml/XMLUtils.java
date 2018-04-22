package com.alphawarthog.xml;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class XMLUtils {
	
	private static class XPathExp {
		private XPathExpression exp;
		private Object lock = new Object();
	}
	
	private static final Logger logger = LogManager.getLogger(XMLUtils.class);
	
	private static final ConcurrentMap<String, XPathExp> xpathExpressionMap = new ConcurrentHashMap<String, XPathExp>();
	
	private static final AtomicReference<DocumentBuilder> docBuilderRef = new AtomicReference<DocumentBuilder>();
	private static final Object docBuilderLock = new Object();
	
	private static final AtomicReference<XPath> xpathRef = new AtomicReference<XPath>();
	private static final Object xpathLock = new Object();
	
	private static final AtomicReference<Transformer> transformerRef = new AtomicReference<Transformer>();
	private static final Object transformerLock = new Object();
	
	private static Transformer getTransformer() throws XMLException {
		Transformer transformer = transformerRef.get();
		if (transformer == null) {
			try {
				transformer = TransformerFactory.newInstance().newTransformer();
			} catch (TransformerConfigurationException | TransformerFactoryConfigurationError e) {
				throw new XMLException(XMLException.CANNOT_CREATE_TRANSFORMER, e.getMessage());
			}
			
			transformerRef.set(transformer);
			logger.debug("XML Transformer created");
		}
		
		return transformer;
	}
	 
	private static XPath getXPath() {
		XPath xpath = xpathRef.get();
		if (xpath == null) {
			xpath = XPathFactory.newInstance().newXPath();
			xpathRef.set(xpath);
			logger.debug("XPath instance created");
		}
		
		return xpath;
	}
	
	private static DocumentBuilder getDocumentBuilder() throws XMLException {
		DocumentBuilder docBuilder = docBuilderRef.get();
		if (docBuilder == null) {
			try {
				docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				throw new XMLException(XMLException.CANNOT_CREATE_DOC_BUILDER, e.getMessage());
			}
			
			docBuilderRef.set(docBuilder);
			logger.debug("Document builder created");
		}
		
		return docBuilder;
	}
	
	public static Document newDocument() throws XMLException {
		Document doc;
		DocumentBuilder docBuilder = getDocumentBuilder();
		synchronized(docBuilderLock) {
			doc = docBuilder.newDocument();
		}
		
		logger.debug("New document created");
		return doc;
	}
	
	public static Document parseDocument(String xml) throws XMLException {
		Document doc;
		
		DocumentBuilder docBuilder = getDocumentBuilder();
		try {
			synchronized(docBuilderLock) {
				doc = docBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
			}
		} catch (SAXException | IOException e) {
			throw new XMLException(XMLException.CANNOT_PARSE_XML_STRING, xml, e.getMessage());
		} 
		
		return doc;
	}
	
	public static Document parseDocument(File file) throws XMLException {
		Document doc;
		
		DocumentBuilder docBuilder = getDocumentBuilder();
		try {
			synchronized(docBuilderLock) {
				doc = docBuilder.parse(file);
			}
		} catch (SAXException | IOException e) {
			throw new XMLException(XMLException.CANNOT_PARSE_XML_FILE, file.getPath(), e.getMessage());
		}
		
		return doc;
	}
	
	public static String evaluateXPath(String expression, Node node) throws XMLException {
		XPathExp exp;
		if ((exp = xpathExpressionMap.get(expression)) == null) {
			exp = new XPathExp();
			XPath xpath = getXPath();
			try {
				synchronized(xpathLock) {
					exp.exp = xpath.compile(expression);
				}
			} catch (XPathExpressionException e) {
				throw new XMLException(XMLException.CANNOT_COMPILE_XPATH, expression, e.getMessage());
			}
			
			logger.debug("XPath expression {0} compiled successfully", expression);
			xpathExpressionMap.putIfAbsent(expression, exp);
		}
		
		String result;
		try {
			synchronized(exp.lock) {
				result = exp.exp.evaluate(node);
			}
		} catch (XPathExpressionException e) {
			throw new XMLException(XMLException.CANNOT_EVALUATE_XPATH, expression, node.getNodeName(), e.getMessage());
		}
		
		logger.debug("XPath expression {0} evaluated to {1} against node {2}", expression, result, node.getNodeName());
		return result;
	}
	
	public static void saveToFile(Document doc, File targetFile) throws XMLException, com.alphawarthog.io.IOException {
		Source domSource = new DOMSource(doc);
		Result fileResult;
		try {
			fileResult = new StreamResult(new BufferedWriter(new FileWriter(targetFile)));
		} catch (IOException e) {
			throw new com.alphawarthog.io.IOException(com.alphawarthog.io.IOException.CANNOT_OPEN_FILE, targetFile.getPath(), e.getMessage());
		}
		
		Transformer transformer = getTransformer();
		try {
			synchronized(transformerLock) {
				transformer.transform(domSource, fileResult);
			}
		} catch (TransformerException e) {
			throw new XMLException(XMLException.CANNOT_SAVE_DOCUMENT_TO_FILE, doc.getNodeName(), targetFile.getPath(), e.getMessage());
		}
		
		logger.debug("Document {0} saved to file {1} successfully", doc.getNodeName(), targetFile.getPath());
	}
}
