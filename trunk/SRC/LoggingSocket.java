package org.sufrin.logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.MemoryHandler;
import java.util.logging.StreamHandler;

import org.sufrin.nanohttp.HTTP;
import org.sufrin.nanohttp.NanoHTTPD;
import org.sufrin.nanohttp.Response;

/**
 * A LoggingSocket lets us inspect the logs and control the logging levels
 * from a web browser. 
 */

public class LoggingSocket extends NanoHTTPD
{
  public LoggingSocket(int port) throws IOException
  {
    super(port);
    startMemoryHandler();
  }

  static protected class LocalHandler extends StreamHandler
  {
    public LocalHandler()
    {
      setFormatter(Logging.formatter);
      setLevel(Level.ALL);
    }

    public void setOutputStream(OutputStream out)
    {
      super.setOutputStream(out);
    }
  }

  static protected MemoryHandler memoryHandler = null;

  static protected LocalHandler httpHandler = null;

  static protected void startMemoryHandler()
  {
    for (Handler h : Logging.defaultLogger.getHandlers())
      Logging.defaultLogger.removeHandler(h);
    httpHandler = new LocalHandler();
    MemoryHandler h = memoryHandler = new MemoryHandler(httpHandler, 1000,
                                                        Level.ALL);
    h.setFormatter(Logging.formatter);
    h.setLevel(Level.ALL);
    h.setPushLevel(Level.OFF);
    Logging.defaultLogger.addHandler(h);
  }

  static protected Response publishLog()
  { final ByteArrayOutputStream store = new ByteArrayOutputStream(500);
    final PrintStream out = new PrintStream(store);
    httpHandler.setOutputStream(out);
    memoryHandler.push();
    httpHandler.flush();
    out.print("\r\n");
    out.flush();
    Response response = new Response(HTTP_OK, MIME_PLAINTEXT, store.toString());
    return response;
  }

  protected static void setLoggerStates(HTTP.Properties params)
  {
    for (String loggerName : Logging.loggerWithName.keySet())
    {
      Logging logging = Logging.loggerWithName.get(loggerName);
      String level = params.get(loggerName);
      if (level != null)
      {
        String lev = level.toUpperCase();
        logging.setLevel(lev);
        if (!logging.setField("debug", !lev.equals("OFF")))
          System.err.println(loggerName + " has no debug");
      }
    }
  }

  protected static void getLoggerStates(HTTP.Properties params)
  {
    for (String loggerName : Logging.loggerWithName.keySet())
    {
      Logging logging = Logging.loggerWithName.get(loggerName);
      String level = logging.getLevel().toLowerCase();
      params.put(loggerName, level);
    }
  }

  protected static String[] levels =
  {
      "all", "finest", "finer", "fine", "config", "info", "warning", "severe", "off"
  };

  protected static String doctype = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">";

  protected static String debugForm(String host, HTTP.Properties params)
  {
    StringBuilder b = new StringBuilder();
    b.append(doctype + "\r\n");
    b.append("<html><head> <title> Debugger </title></head><body>\r\n");
    b.append(String.format("<form method='get' action='http://%s/debug'>\r\n", host));
    b.append("<INPUT type='submit' value='Send'  />\r\n");
    b.append("<INPUT type='reset'  value='Reset' />\r\n");
    b.append("<table border='1'>\r\n");
    b.append("<colgroup/><colgroup span='9' width='8%'/>\r\n");
    b.append("<thead><tr><th/><th align='center' colspan='9'>Logging Level</th></tr>\r\n");
    b.append("<tr>\r\n");
    b.append("<th>Class</th>\r\n");
    for (String level : levels)
      b.append(String.format("<td align='center'>%s</td>\r\n", level));
    b.append("</tr></thead>\r\n");
    for (String logger : Logging.loggerWithName.keySet())
    {
      String logval = params.get(logger);
      if (logval == null)
        logval = "''";
      b.append("<tr>\r\n");
      b.append(String.format("<td> %s </td>\r\n", logger));
      for (String level : levels)
      {
        String checked = level.equals(params.get(logger)) ? "checked='checked'" : "";
        b.append(String.format("<td align='center'><input type='radio' %s name='%s' value='%s'/></td>\r\n",
                               checked, logger, level));
      }
      b.append("</tr>\r\n");
    }
    b.append("</table>\r\n");
    b.append("</form>\r\n");
    b
     .append(String.format("<p><a href='http://%s/log'>Show the Log</a></p>\r\n", host));
    b.append("</body></html>\r\n");
    return b.toString();
  }

  public Response serve(String uri, String method, Properties header, Properties params, LineReader reader) 
  throws IOException, UnsupportedEncodingException
  {
    if (!("/127.0.0.1".equals(header.get("REMOTE_ADDR")))) 
    { 
      return new Response(HTTP_FORBIDDEN, MIME_PLAINTEXT, String.format("%s %s %s", HTTP_FORBIDDEN, method, uri)); 
    }
    if (method.equalsIgnoreCase("GET") && uri.equalsIgnoreCase("/log"))
      return publishLog();
    else if (method.equalsIgnoreCase("GET") && uri.equalsIgnoreCase("/"))
    {
      getLoggerStates(params);
      return new Response(HTTP_OK, MIME_HTML, debugForm(header.get("HOST"), params));
    }
    else if (method.equalsIgnoreCase("FORM") && uri.equalsIgnoreCase("/debug"))
    {
      setLoggerStates(params);
      return new Response(HTTP_OK, MIME_HTML, debugForm(header.get("HOST"), params));
    }
    return new Response(HTTP_FORBIDDEN, MIME_PLAINTEXT, String.format("%s %s %s", HTTP_FORBIDDEN, method, uri));
  }
}