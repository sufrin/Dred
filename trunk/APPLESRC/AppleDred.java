import com.apple.eawt.*;
import javax.swing.*;

/**
        Apple Application trial
*/
public class AppleDred
{ JLabel       b    = new JLabel("AppleDred");
  JFrame       f    = new JFrame("AppleDred");
  Application app;

  public AppleDred()
  { 
    System.err.println("AppleDred2");
    System.err.flush();
    f.add(b);
    f.pack();
    f.setVisible(true);      
    
    app = Application.getApplication();
    app.addApplicationListener
    ( new ApplicationAdapter()
      { public void handleQuit(ApplicationEvent e) 
        {
           System.err.println("QUIT");
           System.err.flush();
           e.setHandled(true);
        }
        
        public void handleOpenFile(ApplicationEvent e) 
        {  System.err.println("OPEN FILE "+e.getFilename()+e);
           System.err.flush();
           
        }
        
        public void handleOpenApplication(ApplicationEvent e)
        {  System.err.println("OPEN APPLICATION "+e.getFilename());
           System.err.flush();
        }
      }
    );

    
  }
  
  public static void main(String[] args)
  { 
    System.err.println("AppleDred");
    System.err.flush();
   
    AppleDred dred = new AppleDred();

  }
}

