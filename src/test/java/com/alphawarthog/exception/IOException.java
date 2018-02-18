package com.alphawarthog.exception;

import java.util.Locale;

public class IOException extends LocalizedLoggedException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8067052944648856848L;

	public IOException(String key, Object... params) {
		super(IOException.class.getName(), key, params);
	}

	public IOException(String key, Locale locale, Object... params) {
		super(IOException.class.getName(), key, locale, params);
	}

	public IOException(String key, Throwable cause, Object... params) {
		super(IOException.class.getName(), key, cause, params);
	}

	public IOException(String key, Locale locale, Throwable cause, Object... params) {
		super(IOException.class.getName(), key, locale, cause, params);
	}
}
