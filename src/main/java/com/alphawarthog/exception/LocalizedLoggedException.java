package com.alphawarthog.exception;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class LocalizedLoggedException extends LoggedException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5448409421994444582L;

	protected LocalizedLoggedException(String bundleName, String key, Object... params) {
		super(formatMessage(bundleName, key, params));
	}
	
	protected LocalizedLoggedException(String bundleName, String key, Locale locale, Object... params) {
		super(formatMessage(bundleName, locale, key, params));
	}

	private static String formatMessage(String bundleName, String key, Object... params) {
		try {
			return formatMessage(ResourceBundle.getBundle(bundleName), key, params);
		} catch (MissingResourceException e) {
			return "Resource bundle " + bundleName + " cannot be found: " + e.getMessage();
		}
	}
	
	private static String formatMessage(String bundleName, Locale locale, String key, Object... params) {
		try {
			return formatMessage(ResourceBundle.getBundle(bundleName, locale), key, params);
		} catch (MissingResourceException e) {
			return "Resource bundle " + bundleName + " for locale " + locale + " cannot be found: " + e.getMessage();
		}
	}

	private static String formatMessage(ResourceBundle bundle, String key, Object... params) {
		try {
			return MessageFormat.format(bundle.getString(key), params);
		} catch (MissingResourceException e) {
			return "Key " + key + " cannot be found in resource bundle " + bundle.getBaseBundleName() + " for locale " + bundle.getLocale() + ": " + e.getMessage();
		}
	}

	protected LocalizedLoggedException(String bundleName, String key, Throwable cause, Object... params) {
		super(formatMessage(bundleName, key, params), cause);
	}
	
	protected LocalizedLoggedException(String bundleName, String key, Locale locale, Throwable cause, Object... params) {
		super(formatMessage(bundleName, locale, key, params), cause);
	}
}
