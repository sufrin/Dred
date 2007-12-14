package org.sufrin.logging;

import java.io.*;
import java.util.HashMap;
import java.util.Formatter;
import java.lang.reflect.*;
import java.util.logging.*;

/**
        
<p>
        A <tt>Logging</tt> is a facade for a <tt>Logger</tt>: it offers formatted
        output as a convenience. The static methods of this class
        offer simplified access to the <tt>java.util.logging</tt> logging
        system.
</p>

<p>
        Loggers obtained  by using this class's <tt>getLogger</tt> method
        can have their level set from the java system properties,
        by the usual logging configuration file, or by a nominated
        configuration file.
</p>

<p>
        Logging configuration is compatible with the Java standard
        if either the environment variable <tt>"Logging.standard"</tt>
        (or a system resource with the same name) has value "1".
</p>

<p>
        Otherwise the logging levels for named logs can be specified
        by one or more java command-line <tt>-D</tt> parameters or system
        resources.
</p>
        
        <PRE>
        java -D<i>logname</i>.log=<i>level</i> ... 
        </PRE>

where <i>level</i> is one of the following
<pre>
        OFF            <i>(highest value)</i>
        SEVERE 
        WARNING
        INFO
        CONFIG
        FINE
        FINER
        FINEST  
        ALL             <i>(lowest value)</i>
</pre>

<p>
        Logs configured this way log at the given level  via the
        <tt>defaultLogger</tt>, an anonymous logger which uses a
        formatter that generates single-line records whose timestamps
        record milliseconds (but not the date). 
</p>

<p>
        If there is a system resource (or environment variable) 
        <tt>"Logging.configuration"</tt> then
        its value is taken to be the name of a properties-style
        file from which the log manager reads its configuration.
</p>

<p>
        Unless the system resource (or environment variable)
        <tt>"Logging.standard"</tt> has value "1" then the standard
        <tt>root</tt> Logger (ie the logger with the empty name) is
        disabled, and messages are directed to the a console handler
        which is given the simplified one line format.
</p>

        <PRE>
        $Id$
        </PRE>

*/
public class Logging 
{ 
  public static boolean standardLogging = 
         "1".equals(System.getProperty("Logging.standard")) ||
         "1".equals(System.getenv("Logging.standard"));

  static java.util.logging.Formatter formatter = new LineFormatter();

  static Logger rootLogger    = Logger.getLogger("");
  static Logger defaultLogger = Logger.getAnonymousLogger();

  static
  { String configFile = System.getProperty("Logging.configuration");
    if (configFile!=null)
    { try
      {  LogManager.getLogManager().reset();
         LogManager.getLogManager().readConfiguration(new FileInputStream(configFile));
      }
      catch (Exception ex)
      {
        throw new RuntimeException("Error initialising Logging configuration", ex);
      }
    }
    if (!standardLogging) 
    { // Disable the root Logger
      rootLogger.setLevel(Level.OFF);
      for (Handler h: rootLogger.getHandlers())
      {   h.setLevel(Level.OFF);
          h.setFormatter(formatter);
      }
      // Make the default Logger handler show everything
      defaultLogger.setLevel(Level.ALL);
      { Handler h = new ConsoleHandler();
        h.setFormatter(formatter);
        h.setLevel(Level.ALL);
        defaultLogger.addHandler(h);
      }
    }
    // Configure loggers from the command-line

    for (Object p: System.getProperties().keySet())
    { String prop = (String) p;
      if (prop.endsWith(".log"))
      { try
        {  
           Logger.getLogger(prop.substring(0, prop.length()-4)).setLevel(Level.parse(System.getProperty(prop).toUpperCase()));
        }
        catch (IllegalArgumentException ex)
        {
           System.err.println("[Log property "+p+"="+System.getProperty(prop)+"]");
        }
      }
    }       
  }
  
  
  static Logger logLogging = getLogger("Logging");
  
  /** Get and configure the named logger as specified by system properties
      or the standard logging configuration.
  */
  public static Logger getLogger(String name)
  { Logger  log   = Logger.getLogger(name);
    if ("Logging".equals(name)) logLogging=log;
    String  lev   = System.getProperty(name+".log");
    Level level   = lev==null ? Level.INFO : Level.parse(lev.toUpperCase());
    if (!standardLogging) 
    { log.setLevel(level); 
      log.setParent(defaultLogger); 
    }
    String parentName = log.getParent()==defaultLogger ? "Logging-default" : log.getParent().toString();
    logLogging.fine(String.format("Logging %s @ %s via %s @ %s", name, log.getLevel(), parentName, log.getParent().getLevel()));
    for (Handler h:log.getHandlers()) 
         logLogging.fine("Logging "+name+" by "+h+"@"+h.getLevel());
    return log;
  }

  /**
    Is the given logger going to log a message with the given level?
  */
  public static boolean isLoggable(Logger logger, String level)
  { 
    return isLoggable(logger, Level.parse(level.toUpperCase()));
  }
  
  /**
    Is the given logger going to log a message with the given level?
  */
  public static boolean isLoggable(Logger logger, Level level)
  { while (logger.getLevel()==null && logger.getParent()!=null)
          logger=logger.getParent();
    if (logger.getLevel()==null) return false;
    return logger.isLoggable(level);
  }

  /**
    A formatter that generates single lines of output.
  */
  public static class LineFormatter extends SimpleFormatter
  { public synchronized String format(LogRecord record)
    { StringBuilder sb = new StringBuilder();
      long t=record.getMillis();
      sb.append(String.format("%s: %tH%tM%tS.%tL ", record.getThreadID(), t,t,t,t));
      if (record.getSourceClassName() != null) 
         sb.append(record.getSourceClassName());
      else
         sb.append(record.getLoggerName());
      sb.append(" ");
      if (record.getSourceMethodName() != null) 
      {
         sb.append(record.getSourceMethodName());
         sb.append(" ");
      }
      sb.append(record.getLevel().getLocalizedName());
      sb.append(": ");
      sb.append(record.getMessage());
      sb.append('\n');
      return sb.toString();
    }
  }

  // Adapter class
  
  protected Logging(Logger log) 
  { this.log=log; 
    this.klass = discoverCallerClass(); 
    getLevel();
  }
  protected Logger log;
  @SuppressWarnings("unchecked")
  protected Class  klass; // For use by dynamic configurers that need to switch on a debug boolean
  protected Level  level; 
  
  /** Set the value of a static boolean field in the class that this logger is logging: return false
      if the field couldn't be set. 
  */  
  public boolean setField(String fieldname, boolean val)
  { Field field = null;
    if (klass==null) return false;
    try { field = klass.getField(fieldname); } 
    catch (Exception ex) { return false; }
    try { field.setAccessible(true); }       
    catch (Exception ex) { return false; }
    try { field.setBoolean(null, val); }       
    catch (Exception ex) { return false; }
    return true;
  }
  
  /** Will this Logging log messages of the given level? */
  public boolean isLoggable(String level) 
  { return isLoggable(this.log, Level.parse(level.toUpperCase())); }

  /** Will this Logging log messages of the given level? */
  public boolean isLoggable(Level level) 
  { return isLoggable(this.log, level); }

  /** Set the level of this Logging */
  public void setLevel(String level)
  { log.setLevel(this.level=Level.parse(level)); 
  }
  
  /** Get the level of this Logging */
  public String getLevel()
  { Logger logger = log;
    while (logger.getLevel()==null && logger.getParent()!=null)
           logger=logger.getParent();
    if (logger.getLevel()==null) { this.level = Level.OFF; return "OFF"; }
    this.level=logger.getLevel();
    return this.level.toString();
  }
  
  final static String loggingClassName = Logging.class.getName(); // So *this* class can migrate packages

  /** 
    Record the details of the innermost non-Logging caller in the given record.  
  */
  private static void discoverCaller(LogRecord record) 
  {
       StackTraceElement frame = discoverCaller();
       if (frame==null)
       { record.setSourceClassName("Unknown class");
         record.setSourceMethodName("Unknown method");        
       }
       else
       { record.setSourceClassName(frame.getClassName());
         record.setSourceMethodName(frame.getMethodName());
       }
  }
  
  /** 
    Returns the innermost current non-Logging frame  
  */
  private static StackTraceElement discoverCaller() 
  {
       // Get the stack trace.
       StackTraceElement stack[] = (new Throwable()).getStackTrace();
       // Find a method in the Logging class.
       int frameLevel = 0;
       while (frameLevel < stack.length && !stack[frameLevel].getClassName().equals(loggingClassName)) 
       {   frameLevel++;
       }
       // Find the first subsequent non-"Logging" class frame.
       while (frameLevel < stack.length) 
       {   StackTraceElement frame = stack[frameLevel];
           String            name  = frame.getClassName();
           if (!name.equals(loggingClassName)) 
           {
               return frame;
           }
           frameLevel++;
       }
       return null;        
  }
  
  /** Returns the Class object describing the class from which it was called */
  @SuppressWarnings("unchecked")
  private static Class discoverCallerClass()
  { StackTraceElement frame = discoverCaller();
    try
    { 
      if (frame!=null) return Class.forName(frame.getClassName()); else return null;
    }
    catch (Exception ex)
    { throw new RuntimeException(ex); }
  }  

  public void logMessage(Level level, String fmt, Object[] args)
  { if (level.intValue()<this.level.intValue()) return; // Save the formatter call if it won't be published
    LogRecord record = new LogRecord(level, new Formatter().format(fmt, args).toString());
    discoverCaller(record);
    log.log(record);
  }
  
  
  /** Return the Throwable wrapped for fuller printing */  
  public static Thrown thrown(Throwable ex) { return new Thrown(ex); }
  
  /** Return the Throwable wrapped for fuller printing */  
  public static Thrown stackTrace() { return new Thrown(new Throwable("STACK TRACE FROM LOGGER")); }
  
  /** A wrapper for throwables that shows their trace */
  protected static class Thrown
  { Throwable ex;
    public Thrown(Throwable ex) { this.ex = ex; }
    public String toString()    { return toString(ex); }
    public void throwAgain()    { throw new RuntimeException(ex); }
    public static String toString(Throwable ex)
    { ByteArrayOutputStream b = new ByteArrayOutputStream(100);
      PrintStream p = new PrintStream(b);
      ex.printStackTrace(p);
      p.flush();
      return b.toString();
    }
  }
    
  public void info(String fmt, Object ... args) 
  { logMessage(Level.INFO, fmt, args); }
  public void config(String fmt, Object ... args)
  { logMessage(Level.CONFIG, fmt, args); }  
  public void fine(String fmt, Object ... args)
  { logMessage(Level.FINE, fmt, args); }  
  public void finer(String fmt, Object ... args)
  { logMessage(Level.FINER, fmt, args); }  
  public void finest(String fmt, Object ... args)
  { logMessage(Level.FINEST, fmt, args); }  
  public void warning(String fmt, Object ... args)
  { logMessage(Level.WARNING, fmt, args); 
  }  
  public void severe(String fmt, Object ... args)
  { logMessage(Level.SEVERE, fmt, args); 
  }  


  protected static HashMap<String, Logging> loggerWithName = new HashMap<String, Logging>();
  
  /** Factory method  to make a named Logging */
  public static Logging getLog(String name)
  { Logging result = loggerWithName.get(name);
    if (result==null)
    { result = new Logging(getLogger(name));
      loggerWithName.put(name, result);
    }
    return result;
  }
  
  /** Factory method to make a Logging named with the calling class's name. */
  public static Logging getLog()
  { String name = discoverCallerClass().getName();
    return getLog(name);
  } 
  
  
}
















