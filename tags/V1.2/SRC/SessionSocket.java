package org.sufrin.dred;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.prefs.*;

import org.sufrin.nanohttp.NanoHTTPD;
import org.sufrin.nanohttp.Response;

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
    prefs.putInt("port", port);
    try { prefs.sync(); } catch (BackingStoreException ex) { ex.printStackTrace(); }
  }
  
  protected Preferences prefs = null;

  public Response    serve
         (String     uri, 
          String     method, 
          Properties header,
          Properties params, 
          LineReader reader
         ) 
  throws IOException, UnsupportedEncodingException
  { 
    if (!("localhost".equals(header.get("REMOTE_HOST")))) 
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
    return new Response(HTTP_FORBIDDEN,
                        MIME_PLAINTEXT,
                        String.format("%s %s %s", HTTP_FORBIDDEN, method, uri));
  }
  
  public void close() 
  { prefs.putInt("port", 0);
    try { prefs.sync(); } catch (BackingStoreException ex) { ex.printStackTrace(); }
    super.close();
    
  }
}




