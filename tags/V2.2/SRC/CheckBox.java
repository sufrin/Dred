package org.sufrin.dred;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;

/**
 * A JCheckBox with a run method that is called whenever
 * the state of the associated button is changed. The
 * boolean variable state, that represents the state of
 * the associated button, may be used in the run method.
 */
public abstract class CheckBox extends JCheckBox
{
  protected boolean state = false;

  public CheckBox(String s, boolean istate)
  {
    super();
    this.setSelected(istate);
    state = istate;
    setAction(new AbstractAction(s)
    {
      public void actionPerformed(ActionEvent ev)
      {
        state = isSelected();
        run();
      }
    });
  }

  /**
   * Invoked when the state of the switch changes; should
   * maintain any invariant relationships that are
   * intended to hold between the state of the switch and
   * the editor session.
   */
  public abstract void run();
}