package org.sufrin.dred;
import com.apple.eawt.*;
import com.apple.eawt.AppEvent.*;
import javax.swing.*;
import java.io.File;

/**
        Apple Application root for Dred.
*/
public class AppleDred
{ final Application app;

  public AppleDred()
  { String dredapp = System.getProperty("org.sufrin.dred.app");    
    app = Application.getApplication();
    
    app.setQuitHandler(
       new QuitHandler() 
       {  @Override
          public void handleQuitRequestWith(QuitEvent qe, QuitResponse qr) 
          {
             if (Dred.closeServer())
                 qr.performQuit();
             else
                 qr.cancelQuit();  
          }
       });
        
    app.setOpenFileHandler(
        new OpenFilesHandler() 
        {
          @Override
          public void openFiles(OpenFilesEvent e) 
          {
             for (File file: e.getFiles())
             try 
             { String fileName = file.getAbsolutePath();
               if (fileName!=null) 
                  Dred.startRemoteSession(fileName, "UTF8");
               else
                  Dred.startRemoteSession("Untitled", "UTF8");
             }
             catch (Exception ex)
             {
               ex.printStackTrace(System.err);
             }
          }                       
       });
 

        
        { Dred.startServer(0); 
          java.awt.PopupMenu menu = Dred.getDockMenu();
          app.setDockMenu(menu);
        }


    /*
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
    */

    
  }
  
  /*
  public static void main(String[] args)
  { 
    AppleDred dred = new AppleDred();
  }
  */
}




















