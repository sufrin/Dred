package org.sufrin.dred;
import  java.awt.event.*;
import  java.io.*;
import  java.util.prefs.Preferences;
import  java.util.LinkedList;
import  javax.swing.*;
import  org.sufrin.logging.Dialog;

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
    ringSize=prefs.getInt("cutringsize", 16);
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
    showEditor();
  }
  
  /**  Make the editor visible */ 
  protected static void showEditor()
  {
    if (theEditor!=null)
    { theEditor.setVisible(true);
      theEditor.setExtendedState(EditorFrame.NORMAL);
      theEditor.refresh();  
    }
  }
  
  /** The singleton Editor */
  static protected CutRingEditor      theEditor = null;
  /** The size of the ring */
  static protected int                ringSize  = 0;
  /** The most-recently selected Cut */
  static protected int                lastSel   = 0;
  
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
    showEditor();  
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
      sizeTool.setSelectedItem(""+ringSize); 
      
      setDoc(new FileDocument("UTF8", new File("Dred Cut Ring"), true));
      menuBar.add(sizeLabel);
      menuBar.add(sizeTool);
      menuBar.add(Box.createHorizontalGlue());
      menuBar.add(prevTool);
      menuBar.add(nextTool);
      menuBar.add(refreshButton);
      menuBar.add(clearButton);
      ed.stopRecordingCuts();   // Don't record cuts from the cut ring!
      ed.allowTypeOver(false); // Don't pay attention to typeover
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
    
    /** No-op: we don't report changes to the Cut Ring */
    public void fileChanged(File file, String fileTitle)
    {
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
      lastSel = cutRing.size(); 
    }
    
    /** Insert and select the previous / next cut */
    public void selectCut(boolean up)
    { int    n = cutRing.size();
      String s = null;
      if (n>0)
      { 
        do
        {
           if (up)
           { if (lastSel==0) lastSel=cutRing.size();
             lastSel--;
           }
           else
           { lastSel = (lastSel+1) % cutRing.size();
           }
           s = cutRing.get(lastSel);
        }  while (n-->0 && s==null);
        doc.deleteAll();
        if (s!=null) 
        { doc.pasteAndSelect(cutRing.get(lastSel), true);
          ed.doCopy();
        }
      }
    }
    
    /** Clears the ring */
    JButton clearButton = new JButton
    (new Act("Clear", "Clear the cut ring")
     {
       public void run() 
       { 
         cutRing = new LinkedList<String>();
         for (int i=0; i<ringSize; i++) cutRing.add(null);
         refresh();
       }
     }
    );
    
    /** Clears the ring */
    JButton refreshButton = new JButton
    (new Act("All", "Show the whole cut ring")
     {
       public void run() 
       { 
         refresh();
       }
     }
    );
    
    JLabel    sizeLabel = new JLabel("Ring Size: ");
    /** Sets the size of the ring */
    JComboBox sizeTool = new JComboBox
    (new String[]{"2", "4","8","16","32","64","128","256","512","1024"});
    { sizeTool.addActionListener
      ( new Act("Set Size")
        {   public void run()
            { 
              try
              { CutRingTool.setRingSize(Integer.parseInt((String) sizeTool.getSelectedItem())); }
              catch (NumberFormatException ex) 
              { 
                Dred.showWarning("Cut ring size should be a number"); 
              }
            }
        });
        sizeTool.setMaximumSize(sizeLabel.getMaximumSize());
    }
        
    /** Select the previous Cut */
    JButton prevTool = new JButton(new Act("Prev", "Select the previous cut in the ring")
    {
        public void run()
        {
          selectCut(true);
        }
    });
        
    /** Select the next Cut */
    JButton nextTool = new JButton(new Act("Next", "Select the next cut in the ring")
    {
        public void run()
        {
          selectCut(false);
        }
    });
        
  }
}





