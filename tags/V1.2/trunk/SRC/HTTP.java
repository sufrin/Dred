package org.sufrin.nanohttp;
public interface HTTP
{
 public static class Properties extends java.util.TreeMap<String, String> {};
  /**
   * Some HTTP response status codes
   */
 final String
          HTTP_OK                  = "200 OK",
          HTTP_REDIRECT            = "301 Moved Permanently",
          HTTP_UNAUTHORIZED        = "401 Unauthorized",
          HTTP_FORBIDDEN           = "403 Forbidden",
          HTTP_NOTFOUND            = "404 Not Found",
          HTTP_BADREQUEST          = "400 Bad Request",
          HTTP_INTERNALERROR       = "500 Internal Server Error",
          HTTP_NOTIMPLEMENTED      = "501 Not Implemented";
  
  /**
   * Common mime types for dynamic content
   */
  final String 
          MIME_PLAINTEXT           = "text/plain",
          MIME_HTML                = "text/html",
          MIME_DEFAULT_BINARY      = "application/octet-stream";
 
}

