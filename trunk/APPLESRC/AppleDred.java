package org.sufrin.dred;
import java.awt.Desktop;
import java.awt.desktop.*;
import javax.swing.*;
import java.io.File;

/**
        Apple Application root for Dred.
*/
public class AppleDred
{ 
  final Desktop desk;

  public AppleDred()
  { String dreddesk = System.getProperty("org.sufrin.dred.app");    
    desk = Desktop.getDesktop();
    desk.disableSuddenTermination();
    
    
    desk.setQuitHandler(
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
     //System.err.println("Quit handler set");
    
    //*     
    desk.setOpenFileHandler(
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
     //System.err.println("OpenFile handler set");
 

        
        { Dred.startServer(0); 
          System.err.println("Server Started");

          JMenuBar menu = Dred.getDockMenu();
          desk.setDefaultMenuBar(menu);
        }
    
  }
  
  /*
  public static void main(String[] args)
  { 
    AppleDred dred = new AppleDred();
  }
  */
}























