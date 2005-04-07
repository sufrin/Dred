package org.sufrin.dred;
import  java.awt.event.*;
import  java.io.*;
import  java.util.prefs.Preferences;
import  java.util.LinkedList;
import  javax.swing.*;

/**
        The CutRingTool module has static methods that save the last
        <tt>(N % size)</tt> pieces of material placed on the system
        clipboard by Dred. A CutRingTool is an extension that generates a
        menu button that opens a (singleton) editing window that provides
        access to the saved material.
*/

public class CutRingTool extends Extension
{ 
  /** Make a CutRingTool */
  public CutRingTool(Preferences prefs) 
  { super("CutRing Tool"); 
    ringSize=prefs.getInt("cutringsize", 10);
    cutRing = new LinkedList<String>();
    for (int i=0; i<ringSize; i++) cutRing.add(null);
    preferences = prefs;
  }
  
  /** (Invoked whenever an editing session starts) adds a button to the Edit menu
      which makes the cut ring editing session visible.    
  */
  public void openSession(EditorFrame session)
  { super.openSession(session);
    JMenu menu = session.addMenu("Edit");
    menu.addSeparator();
    menu.add
    (new Act("Cut Ring Editor", "Open a window on the cut ring.")
     {
       public void run() { startEditor(); }
     }
    );
  }
  
  /** The (global) preferences shared by all Dred components. */
  protected static Preferences preferences;
  
  /**  Start the editor if necessary, and make it visible */ 
  protected static void startEditor()
  { 
    if (theEditor==null) theEditor = new CutRingEditor();
    theEditor.setVisible(true);
    theEditor.setExtendedState(EditorFrame.NORMAL);
    theEditor.refresh();  
  }
  
  /** The singleton Editor */
  static protected CutRingEditor      theEditor = null;
  /** The size of the ring */
  static protected int                ringSize  = 0;
  /** The representation of the ring: invariant <tt>cutRing.size()==ringSize</tt> */
  static protected LinkedList<String> cutRing   = null;
  
  /** Change the ring size, inheriting the last <tt>(ringSize%_ringSize)</tt> elements */
  static public void setRingSize(int _ringSize) 
  { preferences.putInt("cutringsize", _ringSize);
    LinkedList<String> _cutRing  = new LinkedList<String>(cutRing);
    // Reestablish the invariant
    while (ringSize<_ringSize) { _cutRing.addFirst(null); ringSize++; }
    while (ringSize>_ringSize) { _cutRing.removeFirst(); ringSize--; }
    ringSize = _ringSize;
    cutRing  = _cutRing;
    // Show the ring
    startEditor();  
  }
  
  /** Add a selection to the ring: refresh the editor window if it's on view */
  static public void addToRing(String sel)
  { cutRing.remove(); // the first
    cutRing.addLast(sel);
    if (theEditor!=null) theEditor.refresh();
  }
  
  /** A CutRingEditor is an EditorFrame which:
  <ul>
    <li>
      has a button for setting its size
    </li>
    <li>
      always shows the latest cut ring
    </li>
    <li>
      closes without a ``has been modified'' fuss
    </li>
  </ul>
  */
  protected static class CutRingEditor extends EditorFrame 
  { public CutRingEditor() 
    { super(80, 24, "Dred Cut Ring"); 
      setDoc(new FileDocument("UTF8", new File("Dred Cut Ring")));
      menuBar.add(clearButton);
      menuBar.add(sizeTool);
      sizeTool.setText(""+ringSize);
      ed.stopRecordingCuts(); // Don't record cuts from the cut ring!
      addWindowListener(new WindowAdapter()
      {
        public void windowDeiconified(WindowEvent e)
        { 
          refresh();
        }
        
        public void windowIconified(WindowEvent e)
        {
        }
      });
    }
    
    /** Quit without the usual song and dance over saving */
    @ActionMethod(label="Quit", tip="Quit this cut-ring window without the usual ``do you want to save'' dialogue")
    public void doQuit()
    { theEditor.dispose();
      theEditor = null;
    }
    
    /** Refresh the editor if its window is showing */
    public void refresh()
    { if (!isShowing()) return;
      doc.deleteAll();
      for (String s: cutRing)
      { 
        if (s!=null) 
        { doc.insert(s);
          doc.insert("\n\n-----------\n\n");
        }
      } 
    }
    
    /** Clears the ring */
    JButton clearButton = new JButton
    (new Act("Clear Ring", "Clear the cut ring")
     {
       public void run() 
       { 
         cutRing = new LinkedList<String>();
         for (int i=0; i<ringSize; i++) cutRing.add(null);
         refresh();
       }
     }
    );
    
    /** Sets the size of the ring */
    ArgTool sizeTool = new ArgTool("Set size")
    {
        public void run()
        {
          try
          { CutRingTool.setRingSize(Integer.parseInt(getText())); }
          catch (NumberFormatException ex) 
          { 
            Dred.showWarning("Cut ring size should be a number: "+getText()); 
          }
        }
    };
        
  }
}


