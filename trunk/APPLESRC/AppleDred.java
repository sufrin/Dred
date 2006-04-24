import com.apple.eawt.*;
import java.awt.*;

/**
        Apple Application to drive the Dred server.
*/
public class AppleDred
{ 
  Application app = Application.getApplication();
  { app.addApplicationListener
    ( new ApplicationAdapter()
      { public void handleQuit(ApplicationEvent e) 
        {
           System.err.println("QUIT");
           e.setHandled(true);
        }
        
        public void handleOpenFile(ApplicationEvent e) 
        {  System.err.println(e.getFilename());
           e.setHandled(true);
        }
      }
    );
  }
  
  public static void main(String[] args)
  { AppleDred dred = new AppleDred();
    Frame f = new Frame("AppleDred");
    Button b = new Button("AppleDred");
    f.add(b);
    f.pack();
    f.setVisible(true);
  }
}
