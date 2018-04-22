package com.alphawarthog.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggedException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4401937269339345731L;

	public LoggedException(String message) {
		super(message);
		log();
	}

	public LoggedException(Throwable cause) {
		super(cause);
		log();
	}

	public LoggedException(String message, Throwable cause) {
		super(message, cause);
		log();
	}

	protected void log() {
		StackTraceElement[] stackTraces = this.getStackTrace();
		Logger logger = stackTraces.length > 0 ? LogManager.getLogger(stackTraces[0].getClassName()) 
				                                   : LogManager.getLogger(this.getClass());
		logger.error(this.getLocalizedMessage());
	}
	
	public RuntimeException toRuntimeException() {
		return new RuntimeException(this);
	}
}
