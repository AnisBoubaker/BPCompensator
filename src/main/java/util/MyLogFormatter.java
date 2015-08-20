package util;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

//This custom formatter formats parts of a log record to a single line
class MyLogFormatter extends Formatter {
	// This method is called for every log records
	public String format(LogRecord rec) {
		StringBuffer buf = new StringBuffer(1000);

		buf.append(rec.getLoggerName()+": ");
		
		if (rec.getLevel().intValue() == Level.FINEST.intValue()) {
			buf.append("\t\t");
		} else if (rec.getLevel().intValue() == Level.FINER.intValue()) {
			buf.append("\t");
		}
		
		buf.append(formatMessage(rec));
		buf.append('\n');
		return buf.toString();
	}

	// This method is called just after the handler using this
	// formatter is created
	public String getHead(Handler h) {
		return "";
	}

	// This method is called just after the handler using this
	// formatter is closed
	public String getTail(Handler h) {
		return "";
	}
}