package org.sufrin.dred;
import com.apple.eawt.*;
import javax.swing.*;

/**
        Apple Application root for Dred.
*/
public class AppleDred
{ final Application app;

  public AppleDred()
  { 
    
    app = Application.getApplication();
    
    app.addApplicationListener
    ( new ApplicationAdapter()
      { public void handleQuit(ApplicationEvent e) 
        {
           System.err.println("APPLEDRED SERVER QUIT REQUESTED");
           System.err.flush();           
           e.setHandled(Dred.closeServer());
           System.err.println("APPLEDRED SERVER QUIT DENIED");
           System.err.flush();           
        }
        
        public void handleOpenFile(ApplicationEvent e) 
        {  
           String fileName = e.getFilename();
           System.err.println("APPLEDRED OPEN FILE (FINDER)"+fileName);
           System.err.flush();
           try 
           {
             if (fileName!=null) 
                Dred.startRemoteSession(fileName, "UTF8");
             else
                Dred.startRemoteSession("Untitled", "UTF8");
           }
           catch (Exception ex)
           {
             ex.printStackTrace(System.err);
             System.exit(1);
           }
        }
        
        public void handleOpenApplication(ApplicationEvent e)
        {  if (!Dred.serverRunning()) 
           { Dred.startServer(0); 
             java.awt.PopupMenu menu = Dred.getDockMenu();
             app.setDockMenu(menu);
           }
        }
      }
    );

    
  }
  
  /*
  public static void main(String[] args)
  { 
    AppleDred dred = new AppleDred();
  }
  */
}















