package org.sufrin.dred;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

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
{
  public SessionSocket(int port) throws IOException
  {
    super(port);
  }

  public Response    serve
         (String     uri, 
          String     method, 
          Properties header,
          Properties params, 
          LineReader reader
         ) 
  throws IOException, UnsupportedEncodingException
  {
    if (!("/127.0.0.1".equals(header.get("REMOTE_ADDR")))) 
    { 
       return new Response(HTTP_FORBIDDEN, MIME_PLAINTEXT, 
                           String.format("%s %s %s", HTTP_FORBIDDEN, method, uri)); 
    }
    if (method.equalsIgnoreCase("FORM") && uri.equalsIgnoreCase("/edit"))
    {
      final File startCWD = new File(params.get("CWD"));
      final String fileName = params.get("FILE");
      final EditorFrame session = Dred.session(fileName);
      session.setCWD(startCWD);
      session.await();
      return new Response(HTTP_OK, MIME_PLAINTEXT,
                          String.format("Edited %s in %s ", fileName,startCWD));
    }
    return new Response(HTTP_FORBIDDEN,
                        MIME_PLAINTEXT,
                        String.format("%s %s %s", HTTP_FORBIDDEN, method, uri));
  }
}