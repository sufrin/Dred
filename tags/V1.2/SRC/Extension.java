package org.sufrin.dred;
import org.sufrin.logging.*;
import java.util.*;
import java.io.*;

/**
      An Extension class provides resources for Dred sessions that are
      not provided by the existing editor.
*/
public class Extension
{  
   String name;
   public Extension()            { this.name=this.getClass().getName(); }
   public Extension(String name) { this.name = name; }
  
   /** Invoked whenever a session has just started */
   public void openSession(EditorFrame session)
   { log.fine("Extension %s session started: %s", name, session);
   }
   
   /** Invoked whenever a session has just quit */
   public void quitSession(EditorFrame session)
   { log.fine("Extension %s session quit: %s", name, session);
   }
   
   /** Invoked whenever a session is about to be saved */
   public void saveSession(EditorFrame session)
   { log.fine("Extension %s session saved: %s", name, session);
   }
   
   public static void sessionOpened(EditorFrame session)
   { for (String name: extension.keySet())
         extension.get(name).openSession(session);
   }
   
   public static void sessionSaved(EditorFrame session)
   { for (String name: extension.keySet())
         extension.get(name).saveSession(session);
   }
   
   public static void sessionQuit(EditorFrame session)
   { for (String name: extension.keySet())
         extension.get(name).quitSession(session);
   }
   
   /////////////////////// Extension Factory //////////////////       
                                                                        
   /** Logging for the current class */
   public static Logging log = Logging.getLog("Extension");
                                                                                
   /** Debugging is enabled for this class: true if the
       log named Display has level FINER or above.
   */
   public static boolean debug  = log.isLoggable("FINE");
                                                                                
  static DredLoader loader = null;
  static
  {  
    loader=new DredLoader(new File(new File(System.getProperty("user.home")), ".dred"));
  }
  
  static Map<String, Extension> extension = new LinkedHashMap<String, Extension>();
  
  public static void register(Extension ext)
  {
    extension.put(ext.getName(), ext);     
  }
  
  public static void addRoot(String prefix) { loader.addRoot(prefix); }
  
  public String getName()
  {
    return name;
  }

  public static Extension load(String name)
  { Extension ext = extension.get(name);
    if (ext==null)    
       try
       { 
         Class<?> klass =  loader.loadClass(name);
         ext = (Extension) klass.newInstance();
         extension.put(name, ext);
       }
       catch (Exception e)
       {
         log.warning("Cannot load extension: %s", name);
       }
    return ext;    
  }
  
  protected static Bindings protoBindings = null;
  public    static void  setBindings(Bindings thePrototype) 
  { protoBindings = thePrototype; 
    for (Bindings.Binding binding: protoBindings)
        if (binding.matches("extension", "path") && binding.length()==3)
        { Extension.addRoot(binding.getField(2)); }
        else
        if (binding.matches("extension", "load") && binding.length()==3)
        { Extension.load(binding.getField(2)); }
  }
  


}







