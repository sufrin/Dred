
package org.sufrin.nanohttp;

import java.io.*;
import java.util.*;

public class NanoHTTPDServer extends NanoHTTPD
{
  public NanoHTTPDServer(int port) throws IOException
  {
    super(port);
  }

  protected boolean authenticate = false;

  public File rootDir = new File(".");

  public Response serve(String uri, String method, Properties header,
                        Properties parms, LineReader reader) throws IOException,
                                                            UnsupportedEncodingException
  {
    Response challenge = needsAuthentication(uri, method, header, parms, reader);
    if (challenge != null)
      return challenge;

    if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("FORM"))
      if (reflectOK)
      {
        String reflect = method + " '" + uri + "' \n"
                         + (flattenProps("HEADER", header)) + "\n"
                         + (flattenProps("PARAMS", parms));

        /* ************************************************************************************ */
        /* CHARSET DEPENDENCY */
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
        return new Response(HTTP_OK, MIME_PLAINTEXT, reflect);
      }
      else return new Response(HTTP_FORBIDDEN, MIME_PLAINTEXT,
                               String.format("%s %s", HTTP_FORBIDDEN, method));
    else return serveFile(uri, header, rootDir, true);

  }

  // ==================================================
  // Challenge-response code [INCOMPLETE]
  // ==================================================
  public Response needsAuthentication(String uri, 
                                      String method,
                                      Properties header, 
                                      Properties parms,
                                      LineReader reader)
  {
    if (!authenticate)
      return null;
    String auth = header.get("AUTHORIZATION");
    if (auth != null)
    { // Authentication was provided: we should check it
      System.out.println(auth);
      // If it checks out there's no further challenge; otherwise we should challenge again
      return null;
    }
    Response r = null;
    r = Response.NEW(HTTP_UNAUTHORIZED, "WWW-Authenticate",
                     "Basic realm=\"Footling\"");
    r = Response.NEW(HTTP_UNAUTHORIZED, "WWW-Authenticate",
                     "Digest realm=\"Footling\" nonce=\"3aaaxzy47\" ");
    return r;
  }

  // ==================================================
  // File server code
  // ==================================================

  /**
   * Serves file from homeDir and its subdirectories (only).
   * Uses only URI, ignores all headers and HTTP parameters.
   */
  public Response serveFile(String uri, Properties header, File homeDir,
                            boolean allowDirectoryListing)
  {
    // Make sure we won't die of an exception later
    if (!homeDir.isDirectory())
      return new Response(HTTP_INTERNALERROR, MIME_PLAINTEXT,
                          "INTERNAL ERRROR: serveFile(): given homeDir is not a directory.");

    // Remove URL arguments
    uri = uri.trim().replace(File.separatorChar, '/');
    if (uri.indexOf('?') >= 0)
      uri = uri.substring(0, uri.indexOf('?'));

    // Prohibit getting out of current directory
    if (uri.startsWith("..") || uri.endsWith("..") || uri.indexOf("../") >= 0)
      return new Response(HTTP_FORBIDDEN, MIME_PLAINTEXT,
                          "FORBIDDEN: Won't serve ../ for security reasons.");

    File f = new File(homeDir, uri);
    if (!f.exists())
      return new Response(HTTP_NOTFOUND, MIME_PLAINTEXT,
                          "Error 404, file not found.");

    // List the directory, if necessary
    if (f.isDirectory())
    {
      // Browsers get confused without '/' after the
      // directory, send a redirect.
      if (!uri.endsWith("/"))
      {
        uri += "/";
        Response r = new Response(
                                  HTTP_REDIRECT,
                                  MIME_HTML,
                                  "<html><body>Redirected: <a href=\""
                                                                    + uri
                                                                    + "\">"
                                                                    + uri
                                                                    + "</a></body></html>");
        r.addHeader("Location", uri);
        return r;
      }

      // First try index.html and index.htm
      if (new File(f, "index.html").exists())
      {
        f = new File(homeDir, uri + "/index.html");
        uri = f.toString();
      }
      else if (new File(f, "index.htm").exists())
      {
        f = new File(homeDir, uri + "/index.htm");
        uri = f.toString();
      }

      // No index file, list the directory
      else if (allowDirectoryListing)
      {
        String[] files = f.list();
        String msg = "<html><body><h1>Directory " + uri + "</h1><br/>";

        if (uri.length() > 1)
        {
          String u = uri.substring(0, uri.length() - 1);
          int slash = u.lastIndexOf('/');
          if (slash >= 0 && slash < u.length())
            msg += "<b><a href=\"" + uri.substring(0, slash + 1)
                   + "\">..</a></b><br/>";
        }

        msg += "<table border='0'>\r\n";
        for (int i = 0; i < files.length; ++i)
        {
          msg += "<tr>";
          File curFile = new File(f, files[i]);
          boolean dir = curFile.isDirectory();
          msg += "<td><tt>";
          if (dir)
          {
            msg += "<b>";
            files[i] += "/";
          }

          msg += "<a href=\"" + encodeUri(uri + files[i]) + "\">" + files[i]
                 + "</a>";
          msg += "</tt></td><td><tt>";
          // Show file size
          if (curFile.isFile())
          {
            long len = curFile.length();
            if (len < 1024)
              msg += curFile.length() + "</tt></td><td><tt>&nbsp;b";
            else if (len < 1024 * 1024)
              msg += curFile.length() / 1024 + "."
                     + (curFile.length() % 1024 / 10 % 100)
                     + "</tt></td><td><tt>kb";
            else msg += curFile.length() / (1024 * 1024) + "."
                        + curFile.length() % (1024 * 1024) / 10 % 100
                        + "</tt>></td><td><tt>mb";

          }
          msg += "</tt></td>";
          if (dir)
            msg += "</b>";
          msg += "</tr>";
        }
        msg += "</table>\r\n";
        return new Response(HTTP_OK, MIME_HTML, msg);
      }
      else
      {
        return new Response(HTTP_FORBIDDEN, MIME_PLAINTEXT,
                            "FORBIDDEN: No directory listing.");
      }
    }

    // Get MIME type from file name extension, if possible
    String mime = null;
    int dot = uri.lastIndexOf('.');
    if (dot >= 0)
      mime = theMimeTypes.get(uri.substring(dot + 1).toLowerCase());
    if (mime == null)
      mime = MIME_DEFAULT_BINARY;

    try
    {
      // Support (simple) skipping:
      long startFrom = 0;
      String range = header.get("RANGE");
      if (range != null)
      {
        if (range.startsWith("bytes="))
        {
          range = range.substring("bytes=".length());
          int minus = range.indexOf('-');
          if (minus > 0)
            range = range.substring(0, minus);
          try
          {
            startFrom = Long.parseLong(range);
          }
          catch (NumberFormatException nfe)
          {
          }
        }
      }

      FileInputStream fis = new FileInputStream(f);
      fis.skip(startFrom);
      Response r = new Response(HTTP_OK, mime, fis);
      r.addHeader("Content-length", "" + (f.length() - startFrom));
      r.addHeader("Content-range", "" + startFrom + "-" + (f.length() - 1)
                                   + "/" + f.length());
      return r;
    }
    catch (IOException ioe)
    {
      return new Response(HTTP_FORBIDDEN, MIME_PLAINTEXT,
                          "FORBIDDEN: Reading file failed.");
    }
  }

  /**
   * Hashtable mapping FILENAME_EXTENSION -> MIME_TYPE
   */
  private static Hashtable<String, String> theMimeTypes = new Hashtable<String, String>();
  static
  {
    StringTokenizer st = new StringTokenizer
    (   "htm            text/html "
      + "html           text/html "
      + "java           text/plain "
      + "c              text/plain "
      + "h              text/plain "
      + "hs             text/plain "
      + "txt            text/plain "
      + "asc            text/plain "
      + "gif            image/gif "
      + "jpg            image/jpeg "
      + "jpeg           image/jpeg "
      + "png            image/png "
      + "mp3            audio/mpeg "
      + "m3u            audio/mpeg-url "
      + "pdf            application/pdf "
      + "doc            application/msword "
      + "ogg            application/x-ogg "
      + "zip            application/octet-stream "
      + "exe            application/octet-stream "
      + "class          application/octet-stream "
    );
    while (st.hasMoreTokens())
      theMimeTypes.put(st.nextToken(), st.nextToken());
  }

  static boolean reflectOK = true;

  /**
   * Starts as a standalone file server and waits for Enter.
   */
  public static void main(String[] args)
  {
    int port = 9090;
    boolean authenticate = false;
    for (String arg : args)
    {
      if ("-reflect".equals(arg))
        reflectOK = false;
      else if ("-auth".equals(arg))
        authenticate = true;
      else if (arg.matches("[0-9]+"))
        port = Integer.parseInt(arg);
      else
      {
        System.err.println("Usage: server [-reflect] [-auth] [port#]");
        System.exit(1);
      }
    }

    try
    {
      NanoHTTPDServer server = new NanoHTTPDServer(port);
      server.authenticate = authenticate;
    }
    catch (IOException ioe)
    {
      System.err.println("Couldn't start server:\n" + ioe);
      System.exit(-1);
    }
    System.out.println("Now serving port " + port + (authenticate ? " authenticated ":"")+" file requests");
    if (reflectOK)
      System.out.println("and reflecting port " + port + " other requests.");
    else System.out.println("and rejecting port " + port + " other requests.");

    try
    {
      System.in.read();
    }
    catch (Throwable t)
    {
    }
  }

}
