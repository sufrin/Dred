package org.sufrin.dred;
import  javax.swing.*;

/**
        TODO: Keep the ring buffer in the tool.
              Update it at the end online -- removing front each time
              We REALLY should use a different Document implementation, 
              but Document isn't an interface .... yet.
*/

public class CutRingTool extends Extension
{ public CutRingTool() { super("CutRing Tool"); }
  
  public void openSession(EditorFrame session)
  { super.openSession(session);
    JMenu menu = session.addMenu("Edit");
    menu.addSeparator();
    menu.add
    (new Act("Cut Ring Tool", "Open a window on the cut ring.")
     {
       public void run()
       {
           restartFrame(); 
       }
     }
    );
  }
  
  protected void restartFrame()
  { 
    if (cutRingFrame==null) cutRingFrame = new CutRingFrame();
    cutRingFrame.refresh();
  }
    
  protected static CutRingFrame cutRingFrame = null;
  
  protected static class CutRingFrame extends EditorFrame 
  { public CutRingFrame() 
    { super(80, 24, "Dred Cut Ring"); 
      setDoc(new FileDocument("UTF8"));
      menuBar.add(clearButton);
      ed.stopRecordingCuts(); // Don't record cuts from the cut ring!
    }
    
    /** Quit without the usual song and dance over saving */
    @ActionMethod(label="Quit", tip="Quit this cut-ring window without the usual ``do you want to save'' dialogue")
    public void doQuit()
    { cutRingFrame.dispose();
      cutRingFrame = null;
    }
    
    public void refresh()
    {
      doc.deleteAll();
      for (int i=0; i<SystemClipboard.getRingSize(); i++)
      { String s = SystemClipboard.get(i);
        if (s!=null) 
        { doc.insert(s);
          doc.insert("\n\n-----------\n\n");
        }
      }    
    }
    
    JButton clearButton = new JButton
    (new Act("Refresh", "Refresh the window")
     {
       public void run() { refresh(); }
     }
    );
        
  }
}

