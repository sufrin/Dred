package org.sufrin.dred;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * An AbstractAction whose run() method is invoked when
 * its actionPerformed mathod is called.
 */
public abstract class Act extends AbstractAction
{
  public Act(String s)
  {
    super(s);
  }
  String s;
  
  public String toString() { return s; }
  
  public Act(String s, String tip)
  {
    super(s);
    putValue(SHORT_DESCRIPTION, tip);
    this.s=s;
  }

  public void actionPerformed(ActionEvent ev)
  { 
    run();
  }

  public abstract void run();
}

