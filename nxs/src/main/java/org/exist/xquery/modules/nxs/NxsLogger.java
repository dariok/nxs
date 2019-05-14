package org.exist.xquery.modules.nxs;

import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import net.sf.saxon.lib.Logger;

public class NxsLogger extends Logger {
	org.apache.logging.log4j.Logger logger = LogManager.getLogger();

	@Override
	public void println(String message, int severity) {
		Level level;
		switch (severity) {
		case Logger.DISASTER:
			level = Level.FATAL;
		case Logger.ERROR:
			level = Level.ERROR;
		case Logger.INFO:
			level = Level.INFO;
		case Logger.WARNING:
			level = Level.WARN;
		default:
			level = Level.DEBUG;
		}
		
		logger.log(level, message);
	}

	@Override
	public StreamResult asStreamResult() {
		return null;
	}

	@Override
	public void info(String message) {
		logger.info(message);
	}

	@Override
	public void warning(String message) {
		logger.warn(message);
	}

	@Override
	public void error(String message) {
		logger.error(message);
	}

	@Override
	public void disaster(String message) {
		logger.fatal(message);
	}

	@Override
	public boolean isUnicodeAware() {
		return true;
	}
	
	@Override
	public void close() {
		logger.debug("Saxon Logger closed");
	}
}
