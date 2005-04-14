package org.sufrin.dred;

import GUIBuilder.Row;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.*;

import GUIBuilder.RowLayout;

/**
 * A row with three adjacent textlines on it: the
 * ``argument'' line is labelled "...."; the ``find'' line
 * is labelled "Find"; the ``repl'' line is labelled
 * "Repl". These lines provide the arguments for various
 * editor commands.
 */
public class TextBar extends JPanel
{
  public    TextLine argument, find, repl;
  protected JCheckBox findLit = new JCheckBox(), 
                      replLit = new JCheckBox();
  protected Row       findRow = new Row(), 
                      replRow = new Row();
  
  { findRow.add(new JLabel(" Find")); findRow.add(findLit); 
    replRow.add(new JLabel(" Repl")); replRow.add(replLit); 
    findLit.setToolTipText("Check this to intepret the Find minitext as a regular expression");
    replLit.setToolTipText("Check this to intepret the Repl minitext as a regular expression substitution template");
  }
  
  public boolean isFindRegEx()             { return findLit.isSelected(); }
  public boolean isReplRegEx()             { return replLit.isSelected(); }
  public void setFindRegEx(boolean state)  { findLit.setSelected(state); }
  public void setReplRegEx(boolean state)  { replLit.setSelected(state); }

  public TextBar()
  { 
    setLayout(new RowLayout(-1, true));
    setBorder(BorderFactory.createEtchedBorder());
    argument = new TextLine(24, " .... ");
    argument.setToolTipText("The argument text for various commands with arguments (....)");
    find     = new TextLine(22, findRow, true, " Find ");
    findRow.setToolTipText("The find text or pattern (regular expression when checked)");
    repl     = new TextLine(22, replRow, true, " Repl ");
    replRow.setToolTipText("The replacement text or template (regular expression substitution when checked)");
    add(argument);
    add(find);
    add(repl);
    findLit.setBorderPaintedFlat(true);
    replLit.setBorderPaintedFlat(true);
  }
  
  /** Bind keys in all three textlines to the same action in each */
  public void bind(String key, Action action)
  {
    argument.bind(key, action);
    find.bind(key, action);
    repl.bind(key, action);
  }
  
  /** Add a focus eavesdropper to all the underlying documents */
  public void addFocusEavesdropper(FocusEavesdropper d) 
  {
    argument.addFocusEavesdropper(d);
    find.addFocusEavesdropper(d);
    repl.addFocusEavesdropper(d);
  }

}






