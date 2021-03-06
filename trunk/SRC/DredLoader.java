package org.sufrin.dred;

import java.util.Hashtable;
import java.io.*;
import java.net.*;
import java.util.*;
import org.sufrin.logging.*;

public class DredLoader extends ClassLoader
{ public static Logging log = Logging.getLog("DredLoader");
  public static boolean debug = log.isLoggable("FINE");
  
  @SuppressWarnings("unchecked")
protected Hashtable<String,Class> classes = new Hashtable<String,Class>();
  
  protected Vector<String> roots = new Vector<String>();
  
  protected File root = new File(".");

  public DredLoader() { }
  
  public DredLoader(File root)   { super(); setRoot(root); }
  
  public void setRoot(File root) { this.root=root; addRoot(root.getAbsolutePath()); }
  
  public void addRoot(String url)
  { roots.add(url);
  }

  /**
   * Read class from the local filestore.
   */
  protected byte[] getLocalClass(String className)
  { if (!className.endsWith(".class")) className += ".class";
    log.fine("Fetching class implementation: %s", className);
    URL    url    = null;
    byte[] result = null;
    for (String prefix: roots)
    try
    {
       { 
         if (prefix.startsWith("jar:file:")) 
         {  result = getLocalClass(url=new URL(prefix+"!/"+className));
         }
         else
         if (prefix.startsWith("file:")) 
         {  result = getLocalClass(url=new URL(prefix+File.separator+className));
         }
         else
         if (prefix.endsWith(".jar")) 
         {  result = getLocalClass(url=new URL("jar:file://"+prefix+"!/"+className));
         }
         else
         {  result = getLocalClass(url=new URL("file://"+prefix+File.separator+className));
         }
         if (result!=null) break;
       }
    }
    catch (MalformedURLException ex)
    { ex.printStackTrace();
    }
    if (result!=null) log.fine("Returning: %d bytes from %s%n", result.length, url);
    return result;
  }
  
  protected byte[] getLocalClass(URL url)
  { try
    {
      InputStream fi = url.openStream();
      byte[] result = new byte[fi.available()];
      fi.read(result);
      return result;
    }
    catch (Exception e)
    { 
      return null;
    }
  }
  
  // Override
  @SuppressWarnings("unchecked")
public Class<?> findClass(String className) throws ClassNotFoundException
  { Class klass;
    byte  byteCode[];

    byteCode = getLocalClass(className);
    if (byteCode == null) 
    {  log.warning("Class %s wasn't found in %s", className, roots);
       throw new ClassNotFoundException(); 
    }

    // Check and load the bytecode
    klass = defineClass(className, byteCode, 0, byteCode.length);
    if (klass == null) 
    { log.severe("Class %s from %s could not be loaded into this JVM", className, root);
      throw new ClassFormatError(); 
    }
    
    return klass;
  }

}










