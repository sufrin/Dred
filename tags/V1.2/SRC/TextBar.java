package org.sufrin.dred;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

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
  public TextLine argument, find, repl;

  public TextBar()
  {
    setLayout(new RowLayout(-1, true));
    setBorder(BorderFactory.createEtchedBorder());
    argument = new TextLine(24, " .... ");
    argument.setToolTipText("The argument text for various commands with arguments (....)");
    find     = new TextLine(24, " Find ");
    find.setToolTipText("The find text or pattern");
    repl     = new TextLine(24, " Repl ");
    repl.setToolTipText("The replacement text or template");
    add(argument);
    add(find);
    add(repl);
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



