
package org.sufrin.nanohttp;

import java.io.*;
import java.util.*;
import java.net.*;

/**
 * A simple, tiny, nicely embeddable HTTP 1.0 server in Java
 * 
 * <p>
 * NanoHTTPD version 1.01, Copyright &copy; 2001 Jarno
 * Elonen (elonen@iki.fi, http://iki.fi/elonen/) Updated for
 * consistency with Java 1.5 (2005) Bernard Sufrin
 * (Bernard.Sufrin@comlab.ox.ac.uk) Dismembered for
 * embeddability (2005) Bernard Sufrin
 * (Bernard.Sufrin@comlab.ox.ac.uk)
 * 
 * <p>
 * <b>Features & limitations: </b>
 * <ul>
 * 
 * <li> Only one Java file </li>
 * <li> Java 1.1 compatible </li>
 * <li> Released as open source, Modified BSD licence </li>
 * <li> No fixed config files, logging, authorization etc.
 * (Implement yourself if you need them.) </li>
 * <li> Supports parameter parsing of GET and POST methods
 * </li>
 * <li> Supports both dynamic content and file serving </li>
 * <li> Never caches anything </li>
 * <li> Doesn't limit bandwidth, request time or
 * simultaneous connections </li>
 * <li> Default code serves files and shows all HTTP
 * parameters and headers</li>
 * <li> File server supports directory listing, index.html
 * and index.htm </li>
 * <li> File server does the 301 redirection trick for
 * directories without '/'</li>
 * <li> File server supports simple skipping for files
 * (continue download) </li>
 * <li> File server uses current directory as a web root
 * </li>
 * <li> File server serves also very long files without
 * memory overhead </li>
 * <li> Contains a built-in list of most common mime types
 * </li>
 * 
 * </ul>
 * 
 * <p>
 * <b>Ways to use: </b>
 * <ul>
 * 
 * <li> Run as a standalone app, serves files from current
 * directory and shows requests</li>
 * <li> Subclass serve() and embed to your own program </li>
 * <li> Call serveFile() from serve() with your own base
 * directory </li>
 * 
 * </ul>
 * 
 * See the end of the source file for distribution license
 * (Modified BSD licence)
 */

/*
 * Copyright (c) 2001 Jarno Elonen <elonen@iki.fi>
 * 
 * Redistribution and use in source and binary forms, with
 * or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the
 * following disclaimer. Redistributions in binary form must
 * reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the
 * documentation and/or other materials provided with the
 * distribution. The name of the author may not be used to
 * endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.;
 */

public class NanoHTTPD implements HTTP
{ // ==================================================
  // API parts
  // ==================================================

  /**
   * Override this to customize the server.
   * <p>
   * 
   * @parm uri Percent-decoded URI without parameters, for
   *       example "/index.cgi"
   * @parm method "GET", "POST" etc.
   * @parm parms Parsed, percent decoded parameters from URI
   *       and, in case of POST, data.
   * @parm header Header entries, percent decoded
   * @return HTTP response, see class Response for details
   */
  public Response serve(String uri, String method, Properties header,
                        Properties parms, LineReader reader) throws IOException,
                                                                    UnsupportedEncodingException
  {

    String reflect = method + " '" + uri + "' \n"
                     + (flattenProps("HEADER", header)) + "\n"
                     + (flattenProps("PARAMS", parms));

    /* ************************************************************************************ */
    /* CHARSET DEPENDENCY                                                                   */
    /* ************************************************************************************ */

    String length = header.get("CONTENT-LENGTH");
    if (length != null)
    {
      int bytes = Integer.parseInt(length);
      byte[] buf = new byte[bytes];
      reader.read(buf, 0, bytes);
      reflect += "CONTENT\n" + new String(buf, "UTF-8");
    }

    System.out.println(reflect);

    return new Response(HTTP_OK, "text/plain", reflect);

  }
  
  /** Find a socket on a free port */
  
  public static ServerSocket findSocket() throws IOException
  { ServerSocket socket = new ServerSocket();
    socket.bind(null); // find an ephemeral port
    return socket;
  }

  public static String flattenProps(String caption, Properties params)
  {
    StringBuilder out = new StringBuilder();
    if (params.size() > 0)
    {
      out.append(caption);
      out.append("\n");
    }
    for (String prop : params.keySet())
    {
      out.append("  ");
      out.append(prop);
      out.append("='");
      out.append(params.get(prop));
      out.append("'\n");
    }
    return out.toString();
  }

  // ==================================================
  // Socket & server code
  // ==================================================

  /**
   * Starts a HTTP server on the given port.
   * <p>
   * Throws an IOException if the socket is already in use
   */
  public NanoHTTPD(int port) throws IOException
  { 
    serverSocket = port==0 ? findSocket() : new ServerSocket(port);
    
    this.port = serverSocket.getLocalPort();   

    Thread t = new Thread()
    {
      public void run()
      {
        try
        {
          while (true)
            new HTTPSession(serverSocket.accept());
        }
        catch (IOException ioe) // Thrown when socket is closed
        {
        }
      }
    };
    t.setDaemon(true);
    t.start();
  }

  /** The port on which this server is listening */

  public int getPort()
  {
    return port;
  }

  protected ServerSocket serverSocket;

  /** Close the server socket */
  public void close()
  {
    try
    {
      serverSocket.close();
    }
    catch (IOException ex)
    {
    }
  }

  /**
   * Starts as a standalone ``reflect'' server and runs until 
   * Enter is typed on the controlling terminal
   */
  public static void main(String[] args)
  {
    int port = 8080;
    if (args.length > 0)
      port = Integer.parseInt(args[0]);
    try
    {
      new NanoHTTPD(port);
    }
    catch (IOException ioe)
    {
      System.err.println("Couldn't start server:\n" + ioe);
      System.exit(-1);
    }

    System.out.println("Now serving port " + port + " reflecting requests.");

    try
    {
      System.in.read();
    }
    catch (Throwable t)
    {
    }

  }

  /** Number of sessions made so far. */
  protected static long sessionsMade = 0;

  /**
   * Handles one session, i.e. parses the HTTP request and
   * returns the response.
   */
  private class HTTPSession implements Runnable
  {
    public HTTPSession(Socket socket)
    {
      this.socket = socket;
      Thread t = new Thread(this);
      t.setDaemon(true);
      t.start();
    }

    protected String     protocol = null;

    protected Socket     socket   = null;

    protected LineReader reader   = null;

    protected long sessionNumber = sessionsMade++;

    public void run()
    {
      try
      {
        InputStream is = socket.getInputStream();
        if (is == null)
          return;
        reader = new LineReader(is);

        //while (!socket.isClosed()) // KEEPOPEN FEATURE
        //{                          // KEEPOPEN FEATURE
          // System.err.println("READING NEW REQUEST");
          // Read the request line
          StringTokenizer st = new StringTokenizer(reader.readLine());
          if (!st.hasMoreTokens())
            sendError(HTTP_BADREQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html");

          String method = st.nextToken();

          if (!st.hasMoreTokens())
            sendError(HTTP_BADREQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html");

          String uri = st.nextToken(); 

          // Decode parameters from the URI
          Properties parameters = new Properties();
          int startQuery = uri.indexOf('?');
          if (startQuery >= 0)
          {
            decodeParameters(uri.substring(startQuery + 1), parameters);
            uri = decodePercent(uri.substring(0, startQuery));
          }

          Properties header = new Properties();
          if (st.hasMoreTokens())
          { // Protocol version and parameters
            header.put("PROTOCOL_VERSION", protocol = st.nextToken());
            // if (protocol.equalsIgnoreCase("HTTP/1.1")) socket.setKeepAlive(true);
            String line = reader.readLine();
            while (line.trim().length() > 0)
            {
              int colon = line.indexOf(':');
              if (colon<0) sendError(HTTP_BADREQUEST, "Header line '"+line+"'malformed.");
              header.put(line.substring(0, colon).trim().toUpperCase(),
                         line.substring(colon + 1).trim());
              line = reader.readLine();
            }
          }
          

          header.put("REMOTE_ADDR", socket.getInetAddress().toString());
          header.put("REMOTE_HOST", socket.getInetAddress().getHostName());
          header.put("SERVER_SESSION_NUMBER", "" + sessionNumber);

          // A GET with parameters is a FORM (eventually a POST should be treated likewise?)
          if (method.equalsIgnoreCase("GET") && parameters.size() > 0)
            method = "FORM";

          // Ok, now do the serve()
          Response r = serve(uri, method, header, parameters, reader);
          
          if (r == null)
            sendError(HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: Serve() returned a null response.");
          else 
            sendResponse(r.status, r.mimeType, r.header, r.data);

          if (false) // KEEPOPEN FEATURE
          {  String disposal = header.get("CONNECTION");
             if (disposal != null && disposal.equalsIgnoreCase("CLOSE"))
             { System.err.printf("Connection%d closed%n", sessionNumber);
               // break;
             }
          }
        //} // KEEPOPEN FEATURE
        reader.close();
      }
      catch (SocketException ex) 
      {
        
      }
      catch (IOException ioe)
      {
        ioe.printStackTrace();
        try
        {
          sendError(HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: IOException: "
                                        + ioe.getMessage());
        }
        catch (Throwable t)
        {
          t.printStackTrace();
        }
      }
      catch (InterruptedException ie)
      {
        // Thrown by sendError, ignore and exit the thread.
        ie.printStackTrace();
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
      finally
      {
        // System.err.printf("HTTPSession%d finished%n", sessionNumber);
      }
    }

    /**
     * Decodes the percent encoding scheme. <br/> For
     * example: "an+example%20string" -> "an example string"
     */
    private String decodePercent(String str) throws InterruptedException
    {
      try
      {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++)
        {
          char c = str.charAt(i);
          switch (c)
          {
            case '+':
              sb.append(' ');
            break;
            case '%':
              sb.append((char) Integer.parseInt(str.substring(i + 1, i + 3), 16));
              i += 2;
            break;
            default:
              sb.append(c);
            break;
          }
        }
        return new String(sb.toString().getBytes());
      }
      catch (Exception e)
      {
        sendError(HTTP_BADREQUEST, "BAD REQUEST: Bad percent-encoding.");
        return null;
      }
    }

    /**
     * Decodes parameters in percent-encoded URI-format (
     * e.g. "name=Jack%20Daniels&pass=Single%20Malt" ) and
     * adds them to given Properties.
     */
    private void decodeParameters(String uriParams, Properties properties) throws InterruptedException
    {
      if (uriParams == null)
        return;
      StringTokenizer st = new StringTokenizer(uriParams, "&");
      while (st.hasMoreTokens())
      {
        String e = st.nextToken();
        int sep = e.indexOf('=');
        if (sep >= 0)
          properties.put(decodePercent(e.substring(0, sep)).trim(),
                         decodePercent(e.substring(sep + 1)));
      }
    }

    /**
     * Returns an error message as a HTTP response and
     * throws InterruptedException to stop further request
     * processing.
     */
    private void sendError(String status, String msg) throws InterruptedException
    {
      sendResponse(status, MIME_PLAINTEXT, null,
                   new ByteArrayInputStream(msg.getBytes()));
      throw new InterruptedException();
    }

    /**
     * Sends given response to the socket.
     */
    private void sendResponse(String status, String mime, Properties header, InputStream data)
    {
      try
      {
        if (status == null)
          throw new Error("sendResponse(): Status can't be null.");

        OutputStream out = socket.getOutputStream();
        PrintWriter pw = new PrintWriter(out);
        pw.printf("HTTP/1.0 %s \r\n", status);

        if (mime != null)
          pw.printf("Content-Type: %s \r\n", mime);

        if (header == null || header.get("Date") == null)
          pw.printf("Date: %s \r\n", gmtFormat.format(new Date()));

        if (header != null)
        {
          for (String prop : header.keySet())
            pw.print(prop + ": " + header.get(prop) + "\r\n");
        }

        pw.print("\r\n");
        pw.flush();

        if (data != null)
        {
          byte[] buff = new byte[2048];
          int read = 2048;
          while (read == 2048)
          {
            read = data.read(buff, 0, 2048);
            out.write(buff, 0, read);
          }
        }
        out.flush();
        out.close();
        if (data != null)
          data.close();
      }
      catch (IOException ioe)
      { // Here if the client went away
        // ioe.printStackTrace();     
        try
        {
          socket.close();
        }
        catch (Throwable t)
        {
        }
      }
      finally
      {
        // System.err.printf("SendResponse c=%s, i=%s,
        // o=%s%n", socket.isClosed(),
        // socket.isInputShutdown(),
        // socket.isOutputShutdown());
      }
    }

  }

  /**
   * URL-encodes everything between "/"-characters. Encodes
   * spaces as '%20' instead of '+'.
   */
  public static String encodeUri(String uri)
  {
    String newUri = "";
    StringTokenizer st = new StringTokenizer(uri, "/ ", true);
    while (st.hasMoreTokens())
    {
      String tok = st.nextToken();
      if (tok.equals("/"))
        newUri += "/";
      else if (tok.equals(" "))
        newUri += "%20";
      else try
      {
        newUri += URLEncoder.encode(tok, "UTF-8");
      }
      catch (UnsupportedEncodingException ex)
      {
        throw new RuntimeException(ex);
      }
    }
    return newUri;
  }

  private int port;

  /**
   * GMT date formatter
   */
  private static java.text.SimpleDateFormat gmtFormat;
  static
  {
    gmtFormat = new java.text.SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.UK);
    gmtFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  /**
   * This class supports the reading of lines from an
   * octet-encoded input stream
   */

  public static class LineReader extends BufferedInputStream
  {
    public LineReader(InputStream base)
    {
      super(base);
      this.base = base;
    }

    int bytesRead = 0;

    /** In case we need a handle on the base stream */
    public InputStream base;

    /**
     * Reads a line up to an eol sequence consisting of: \r,
     * \r\n, or \n. Remarkable that in 2005 we have Unicode
     * but can't agree on what a line-ending is
     */
    public String readLine() throws IOException
    {
      StringBuilder b = new StringBuilder();
      int ch = read();
      while (ch != '\r' && ch != -1 && ch != '\n')
      {
        b.append((char) ch);
        ch = read();
      }
      if (ch == '\r')
      {
        mark(1);
        ch = super.read();
        if (ch != '\n')
        {
          reset();
        }
        else bytesRead++;
      }
      return b.toString();
    }

    public int read() throws java.io.IOException
    {
      int ch = super.read();
      if (ch > 0)
        bytesRead++;
      return ch;
    }

    final public int bytesRead()
    {
      return bytesRead;
    }
  }

}

