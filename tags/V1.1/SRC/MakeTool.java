package org.sufrin.dred;

import javax.swing.JComponent;

public class MakeTool extends RunTool
{ public MakeTool() { super("Make Bar"); }

  @Override
  public JComponent makeTool(EditorFrame session)
  {
    return new Tool("make", "make", true, session, "Run make", "make arguments");
  }

}

