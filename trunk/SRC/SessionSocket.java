package org.sufrin.dred;

import java.io.File;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.prefs.*;

import org.sufrin.nanohttp.NanoHTTPD;
import org.sufrin.nanohttp.Response;
import org.sufrin.logging.*;

/**
 * A SessionSocket listens for (HTTP) connections
 * requesting editing sessions. When it receives a request
 * it spawns an editing session, then waits for the
 * session to conclude before sending its response back to
 * the requester.
 * 
 * Other HTTP requests are for
 * 
 * /sessions: a table of the currently active editor
 * sessions
 * 
 * 
 */

public class SessionSocket extends NanoHTTPD
{ /** Make a session socket and publish (via preferences) its port. */
  
  public SessionSocket(int port, Preferences prefs) throws IOException
  { 
    super(port);
    this.prefs=prefs;
  }
  
  protected Preferences prefs = null;
  public final static String IPV6LOOP = "/0:0:0:0:0:0:0:1",
                             IPV4LOOP = "/127.0.0.1";
                             
  public final static Logging log   = Logging.getLog("SessionSocket");
  public final static boolean debug = log.isLoggable("FINE");

  public Response    serve
         (String     uri, 
          String     method, 
          Properties header,
          Properties params, 
          LineReader reader
         ) 
  throws IOException, UnsupportedEncodingException
  { String addr = header.get("REMOTE_ADDR");
    if (debug) log.fine("%s %s %s %s", method, uri, params, header);
    if (!(addr.equals(IPV6LOOP) || addr.equals(IPV4LOOP)))
    { 
       return new Response(HTTP_FORBIDDEN, MIME_PLAINTEXT, 
                           String.format("%s %s %s", HTTP_FORBIDDEN, method, uri)); 
    }
    
    if (method.equalsIgnoreCase("GET") && uri.equalsIgnoreCase("/serving"))
    {
      return new Response(HTTP_OK, MIME_PLAINTEXT, "OK");
    }
    else
    if (method.equalsIgnoreCase("FORM") && uri.equalsIgnoreCase("/edit"))
    { 
      final File startCWD       = new File(params.get("CWD"));
      final String fileName     = params.get("FILE");
      final EditorFrame session = Dred.startLocalSession(fileName);
      final String waitParam    = params.get("WAIT");
      final boolean wait        = waitParam!=null && waitParam.equals("true");
      session.setCWD(startCWD);
      if (wait) session.await();
      return new Response(HTTP_OK, MIME_PLAINTEXT,
                          String.format("Dred %s in %s ", fileName,startCWD));
    }
    else
    if (method.equalsIgnoreCase("GET") && uri.toUpperCase().startsWith("/HELP"))
    { URL url = Dred.class.getResource("index.html");
      return new Response(HTTP_OK, MIME_HTML, url.openStream());
    }
    else
    if (method.equalsIgnoreCase("GET") && uri.equals("/favicon.ico"))
    { return serveURL("class://org.sufrin.dred.Dred/favicon.png", "image/png");
    }
    else
    if (method.equalsIgnoreCase("GET") && uri.endsWith(".png"))
    { return serveURL("class://org.sufrin.dred.Dred"+uri, "image/png");
    }
    else
    if (method.equalsIgnoreCase("GET") && uri.endsWith(".html"))
    { 
      return serveURL("class://org.sufrin.dred.Dred"+uri, "text/html");
    }
    else
      return new Response(HTTP_FORBIDDEN,
                          MIME_PLAINTEXT,
                          String.format("%s %s %s", HTTP_FORBIDDEN, method, uri));
  }
  
  public void close() 
  { prefs.putInt("port", 0);
    try { prefs.sync(); } catch (BackingStoreException ex) { ex.printStackTrace(); }
    super.close();
    
  }
  
  public Response serveURL(String uri, String kind)
  { try
    { URL         url    = new URL(uri);
      InputStream is     = new BufferedInputStream(url.openStream());
      int         length = is.available();
      Response r = new Response(HTTP_OK, kind, is);
      r.addHeader("Content-length", "" + length);
      // System.err.printf("[%s %d]%n", uri, length);
      return r;
    }
    catch (Exception ex)
    {  ex.printStackTrace();
       return new Response(HTTP_FORBIDDEN,
                           MIME_PLAINTEXT,
                           String.format("%s %s %s", HTTP_FORBIDDEN, "GET", uri));
    }
  }

  
}















