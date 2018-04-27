package com.alphawarthog.xml;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
	
	private static class XPathProcessor {
		
		private XPath xpath = XPathFactory.newInstance().newXPath();
		private Map<String, XPathExpression> expressionMap = new HashMap<String, XPathExpression>();
		
		private String evaluateXPath(String expression, Node node) throws XMLException {
		  XPathExpression exp = expressionMap.get(expression);
		  if (exp == null) {
	  		try {
	  			exp = xpath.compile(expression);
	  		} catch (XPathExpressionException e) {
					throw new XMLException(XMLException.CANNOT_COMPILE_XPATH, expression, e.getMessage());
				}
		  	
		  	logger.debug("XPath expression {0} compiled successfully", expression);
		  	expressionMap.put(expression, exp);
		  }
			
			String result;
			try {
				result = exp.evaluate(node);
			} catch (XPathExpressionException e) {
				throw new XMLException(XMLException.CANNOT_EVALUATE_XPATH, expression, node.getNodeName(), e.getMessage());
			}
			
			logger.debug("XPath expression {0} evaluated to {1} against node {2}", expression, result, node.getNodeName());
			return result;
		}
	}
	
	private static final Logger logger = LogManager.getLogger(XMLUtils.class);
	
	private static final ThreadLocal<XPathProcessor> xpathProcessor = new ThreadLocal<XPathProcessor>() {
		
		protected XPathProcessor initialValue() {
			return new XPathProcessor();
		}
	};
	
	private static final ThreadLocal<Transformer> transformer = new ThreadLocal<Transformer>() {

		protected Transformer initialValue() {
			try {
				logger.debug("Creating new transformer");
				return TransformerFactory.newInstance().newTransformer();
			} catch (TransformerConfigurationException | TransformerFactoryConfigurationError e) {
				throw new RuntimeException(new XMLException(XMLException.CANNOT_CREATE_TRANSFORMER, e.getMessage()));
			}
		}
	};
	
	private static ThreadLocal<DocumentBuilder> documentBuilder = new ThreadLocal<DocumentBuilder>() {
		
		protected DocumentBuilder initialValue() {
			try {
				logger.debug("Creating new document builder");
				return DocumentBuilderFactory.newInstance().newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				throw new RuntimeException(new XMLException(XMLException.CANNOT_CREATE_DOC_BUILDER, e.getMessage()));
			}
		}
	};
	
	public static Document newDocument() {
		logger.debug("Creating new document");
		return documentBuilder.get().newDocument();
	}
	
	public static Document parseDocument(String xml) throws XMLException {
		try {
			logger.debug("Parsing " + xml);
			return documentBuilder.get().parse(new ByteArrayInputStream(xml.getBytes()));
		} catch (SAXException | IOException e) {
			throw new XMLException(XMLException.CANNOT_PARSE_XML_STRING, xml, e.getMessage());
		} 
	}
	
	public static Document parseDocument(File file) throws XMLException {
		try {
			logger.debug("Parsing " + file.getPath());
			return documentBuilder.get().parse(file);
		} catch (SAXException | IOException e) {
			throw new XMLException(XMLException.CANNOT_PARSE_XML_FILE, file.getPath(), e.getMessage());
		}
	}
	
	public static String evaluateXPath(String expression, Node node) throws XMLException {
		return xpathProcessor.get().evaluateXPath(expression, node);
	}
	
	public static void saveToFile(Document doc, File targetFile) throws XMLException, com.alphawarthog.io.IOException {
		Source domSource = new DOMSource(doc);
		Result fileResult;
		try {
			fileResult = new StreamResult(new BufferedWriter(new FileWriter(targetFile)));
		} catch (IOException e) {
			throw new com.alphawarthog.io.IOException(com.alphawarthog.io.IOException.CANNOT_OPEN_FILE, targetFile.getPath(), e.getMessage());
		}
		
		try {
			transformer.get().transform(domSource, fileResult);
			logger.debug("Document {0} saved to file {1} successfully", doc.getNodeName(), targetFile.getPath());
		} catch (TransformerException e) {
			throw new XMLException(XMLException.CANNOT_SAVE_DOCUMENT_TO_FILE, doc.getNodeName(), targetFile.getPath(), e.getMessage());
		}
	}
}
