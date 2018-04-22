package com.alphawarthog.xml;

import java.util.Locale;

import com.alphawarthog.exception.LocalizedLoggedException;

public class XMLException extends LocalizedLoggedException {
	
	public static final String CANNOT_CREATE_DOC_BUILDER = "cannot.create.doc.builder";
	public static final String CANNOT_EVALUATE_XPATH = "cannot.evaluate.xpath";
	public static final String CANNOT_COMPILE_XPATH = "cannot.compile.xpath";
	public static final String CANNOT_PARSE_XML_STRING = "cannot.parse.xml.string";
	public static final String CANNOT_PARSE_XML_FILE = "cannot.parse.xml.file";
	public static final String CANNOT_CREATE_TRANSFORMER = "cannot.create.transformer";
	public static final String CANNOT_SAVE_DOCUMENT_TO_FILE = "cannot.save.document.to.file";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2364040733642327960L;

	public XMLException(String key, Object... params) {
		super(XMLException.class.getName(), key, params);
	}

	public XMLException(String key, Locale locale, Object... params) {
		super(XMLException.class.getName(), key, locale, params);
	}

	public XMLException(String key, Throwable cause, Object... params) {
		super(XMLException.class.getName(), key, cause, params);
	}

	public XMLException(String key, Locale locale, Throwable cause, Object... params) {
		super(XMLException.class.getName(), key, locale, cause, params);
	}
}
