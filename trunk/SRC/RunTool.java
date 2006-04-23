package org.sufrin.dred;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.border.BevelBorder;

/**
        Precursor of the TextLine-based ToolExtensions that run specific programs.
*/
public abstract class RunTool extends ToolExtension
{ public RunTool(String name) { super(name); }

  public abstract JComponent makeTool(EditorFrame session);
  
@SuppressWarnings("serial")
public static class Tool extends TextLine
  {
    public Tool(String label, final String cmd, final boolean noSelection, 
                final EditorFrame session)
    {
       this(label, cmd, noSelection, session, null, null);
    }
    
    public Tool(String label, 
                final  String cmd, 
                final  boolean noSelection, 
                final  EditorFrame session, 
                String buttonTip, 
                String argTip)
    {
      super(20, new JButton(), true, label);
      JButton but = (JButton) getLabel();
      but.setAction(new Act(label)
      {
        public void run()
        {
          if (noSelection)
            session.doSave();
          session.startProcess(cmd + " " + getText(), 
                               noSelection ? "" : session.doc.getSelection());
        }
      });
      if (buttonTip!=null) but.setToolTipText(buttonTip);
      if (argTip!=null) setToolTipText(argTip);
      setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
    }
  }
}




