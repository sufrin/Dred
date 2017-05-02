package org.sufrin.dred;

import javax.swing.JComponent;

public class ShellTool extends RunTool
{ public ShellTool() { super("Shell Bar"); }

  @Override
  public JComponent makeTool(EditorFrame session)
  {
    return new Tool("sh", "", true, session, "Run a shell command (on the current selection, if any)", "the shell command to run");
  }

}

