package com.alphawarthog.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Locale;

import junit.framework.TestCase;

public class IOExceptionTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testIOException() throws java.io.IOException {
		try {
			testIOException_Locale(null, false); 
		} catch (IOException e1) {
			assertEquals("File Non existent file.txt cannot be found: Non existent file.txt (No such file or directory)", e1.getMessage());
		}
	}
	
	public void testIOException_WithCause() throws java.io.IOException {
		try {
			testIOException_Locale(null, true); 
		} catch (IOException e1) {
			assertEquals("File Non existent file.txt cannot be found: Non existent file.txt (No such file or directory)", e1.getMessage());
		}
	}
	
	public void testIOException_WithLocale() throws java.io.IOException {
		try {
			testIOException_Locale(new Locale("in", "ID"), false); 
		} catch (IOException e1) {
			assertEquals("File Non existent file.txt nggak ketemu!: Non existent file.txt (No such file or directory)", e1.getMessage());
		}
	}
	
	public void testIOException_WithLocale_WithCause() throws java.io.IOException {
		try {
			testIOException_Locale(new Locale("in", "ID"), true); 
		} catch (IOException e1) {
			assertEquals("File Non existent file.txt nggak ketemu!: Non existent file.txt (No such file or directory)", e1.getMessage());
		}
	}
	
	private void testIOException_Locale(Locale locale, boolean useThrowable) throws java.io.IOException, IOException {
		File bogusFile = new File("Non existent file.txt");
		try {
			try (FileReader reader = new FileReader(bogusFile)) {
				// do nothing
			}
		} catch (FileNotFoundException e) {
			if (locale == null) {
				if (!useThrowable) {
					throw new IOException("file.not.found", bogusFile.getPath(), e.getMessage());
				} else {
					throw new IOException("file.not.found", e, bogusFile.getPath(), e.getMessage());
				}
			} else {
				if (!useThrowable) {
					throw new IOException("file.not.found", locale, bogusFile.getPath(), e.getMessage());
				} else {
					throw new IOException("file.not.found", locale, e, bogusFile.getPath(), e.getMessage());
				}
			}
		} 
	}
}
