package util;

import java.util.Iterator;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyLogger {
  static private ConsoleHandler consoleTxt;
  static private MyLogFormatter formatterTxt;

  static public void setup() {
    // Create Logger
    Logger logger = Logger.getLogger("");
    Handler[] handlers = logger.getHandlers();
    for(Handler h : handlers){
    	logger.removeHandler(h);
    }
    logger.setLevel(Level.FINEST);
    consoleTxt = new ConsoleHandler();
    consoleTxt.setLevel(Level.FINEST);

    // Create txt Formatter
    formatterTxt = new MyLogFormatter();
    consoleTxt.setFormatter(formatterTxt);
    logger.addHandler(consoleTxt);
  }
} 