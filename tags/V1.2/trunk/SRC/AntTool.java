package org.sufrin.dred;

import javax.swing.JComponent;

public class AntTool extends RunTool
{ public AntTool() { super("Ant Bar"); }

  @Override
  public JComponent makeTool(EditorFrame session)
  {
    return new Tool("ant", "ant", true, session, "Run ant", "ant arguments");
  }

}

