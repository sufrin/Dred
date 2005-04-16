package org.sufrin.dred;
import  java.net.*;

/** This module is a URL factory that translates various locally-defined protocols */
public class DredURL 
{ static public URL newURL(URL relative, String path) throws MalformedURLException  
  { 
    return new URL(relative, fixURL(path)); 
  }
  
  static public URL newURL(String path) throws MalformedURLException                 
  { 
    return new URL(fixURL(path)); 
  }  
  
  static public String fixURL(String path)
  {
      if (path.startsWith("dred://")) path = "class://org.sufrin.dred.Dred"+path.substring(6);
      else
      if (path.startsWith("dred:/")) path = "class://org.sufrin.dred.Dred"+path.substring(5);
      else
      if (path.startsWith("dred:")) path = "class://org.sufrin.dred.Dred/"+path.substring(5);
      return path;
  }
}
