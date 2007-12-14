
package org.sufrin.urlfactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Hashtable;

/**
 * Handles URLs of the form <tt>class://qualified.classname/path/to/resource</tt>. The
 * ``meaning'' of such a URI is defined by
 * 
 * <pre>
 * 
 *      qualified.classname.class.getResource(path) = new URL(&quot;class://qualified.classname/path&quot;);
 * 
 * </pre>
 * 
 * <b>Caution</b>: there is really no point in using such URLs in a
 * program whose resource names are known and fixed.
 * <p>
 * The handler needs to be registered by using:
 * 
 * <pre>
 * 
 *      URL.setURLStreamHandlerFactory(new ClassURLFactory());
 * 
 * </pre>
 */

public class ClassURLFactory implements URLStreamHandlerFactory
{
  public URLStreamHandler createURLStreamHandler(String protocol)
  {
    return protocol.equalsIgnoreCase("class") ? new ClassUrlStreamHandler() : null;
  }

  static class ClassUrlStreamHandler extends URLStreamHandler
  {
    protected URLConnection openConnection(URL url) throws IOException
    {
      return new ClassUrlConnection(url);
    }
  }

  static class ClassUrlConnection extends URLConnection
  {
    @SuppressWarnings("unchecked")
	private Class klass;

    public ClassUrlConnection(URL url)
    {
      super(url);
    }

    public String getContentType()
    {
      String path = this.url.getPath();
      return guessContentTypeFromName(path);
    }

    public synchronized InputStream getInputStream() throws IOException
    {
      if (!connected)
        connect();
      String path = this.url.getPath();
      if (path.startsWith("/"))
        path = path.substring(1);
      InputStream in = klass.getResourceAsStream(path);
      if (in==null) 
         throw new IOException("File not found: "+url.toString());
      return in;
    }

    public synchronized void connect() throws IOException
    {
      String className = this.url.getHost();
      try
      {
        klass = Class.forName(className);
        this.connected = true;
      }
      catch (ClassNotFoundException cex)
      {
        throw new IOException("Anchor class not found: " + url.toString());
      }
    }
    
    /** Maps filenames to content types. */
    
    protected static Hashtable<String, String> tab = new Hashtable<String, String>();
    static
    {
      tab.put(".html",          "text/html");
      tab.put(".htm",           "text/html");
      tab.put(".txt",           "text/plain");      
      tab.put(".bindings",      "text/plain");
      tab.put(".gif",           "image/gif");
      tab.put(".jpg",           "image/jpg");
      tab.put(".jpeg",          "image/jpg");
      tab.put(".png",           "image/png");
      tab.put(".jar",           "application/x-java-archive");
      
    }
    
    public static String guessContentTypeFromName(String name)
    {
      name = name.toLowerCase();
      for (String ext:tab.keySet()) 
          if (name.endsWith(ext)) return tab.get(ext);
      return "text/plain";
    }

  }

}

