package org.sufrin.dred;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.BevelBorder;
/**
 *  A TextLine labelled with a button. When the button is pressed  the run() method is called.
 */
public abstract class ArgTool extends TextLine implements Runnable
{
  public ArgTool(String label)
  {
    super(6, new JButton(), false, label);
    JButton but = (JButton) getLabel();
    but.setAction(new Act(label)
    {
      public void run()
      {
        ArgTool.this.run();
      }
    });
    setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
  }

  abstract public void run();
}