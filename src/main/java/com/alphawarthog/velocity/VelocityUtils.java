package com.alphawarthog.velocity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.StringWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;

import com.alphawarthog.io.IOException;

public class VelocityUtils {
	
	private static final Logger logger = LogManager.getLogger(VelocityUtils.class);
	
	private static final VelocityEngine ve = new VelocityEngine();
	private static final Object veLock = new Object();
		
	static {
		ve.init();
	}
	
	public static String evaluate(String template, Context ctx) {
		return evaluate(template, ctx, "");
	}
	
	public static String evaluate(String template, Context ctx, String templateName) {
		StringWriter writer = new StringWriter();
		synchronized(veLock) {
			ve.evaluate(ctx, writer, templateName, template);
		}
		
		String result = writer.toString();
		logger.debug("{0} evaluated to {1}", template, result);
		return result;
	}
	
	public static void evaluate(File templateFile, File outputFile, Context ctx) throws IOException {
		boolean closingWriter = false;
		try (FileWriter out = new FileWriter(outputFile)) {
			
			boolean closingReader = false;
			try (FileReader in = new FileReader(templateFile)) {
				synchronized(veLock) {
					ve.evaluate(ctx, out, templateFile.getPath(), in);
				}
				
				logger.debug("{0} evaluated into {1}", templateFile.getPath(), outputFile.getPath());
				closingReader = true;
			} catch (FileNotFoundException e) {
				if (closingReader) {
					logger.warn("Unable to close reader of file {0}: {1}", templateFile.getPath(), e.getMessage());
				} else {
					throw new IOException(IOException.FILE_NOT_FOUND, templateFile.getPath(), e.getMessage());
				}
			}
			
			closingWriter = true;
		} catch (java.io.IOException e) {
			if (closingWriter) {
				logger.warn("Unable to close writer to file {0}: {1}", outputFile, e.getMessage());
			} else {
				throw new IOException(IOException.CANNOT_OPEN_FILE, outputFile.getPath(), e.getMessage());
			}
		}
	}
}
