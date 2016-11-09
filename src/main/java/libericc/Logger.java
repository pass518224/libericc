package libericc;

import java.util.logging.LogManager;


// Wrapper class of java.util.logging.LogManager
public class Logger {
	static java.util.logging.Logger logger;
	
	static {
		try {
			LogManager.getLogManager().readConfiguration(Object.class.getResourceAsStream("/javalog.properties"));
			logger = java.util.logging.Logger.getLogger("HisDroid");
		}
		catch (Exception e) {
			System.err.println("Cannot Find Log Properties");
		}
	}
	
	public static java.util.logging.Logger getLogger() {
		return logger;
	}
}
