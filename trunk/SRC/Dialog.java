package org.sufrin.logging;
import javax.swing.JOptionPane;

/** A static module with helpers for putting up messages in dialogues */
public class Dialog
{
   /**
   * Show a warning message in a dialogue window with
   * various option buttons. When a button is pressed return
   * its number. The default button (the one fired by
   * pressing ENTER) is indicated by the given integer
   * parameter. The dialogue's parent is the given
   * component.
   */
  public static int showWarning
                    (java.awt.Component parent, String msg, int dflt,
                     Object[] options)
  {
    int option = JOptionPane.showOptionDialog
                 (parent, msg, "Warning",
                  JOptionPane.DEFAULT_OPTION,
                  JOptionPane.WARNING_MESSAGE,
                  null, options, options[dflt]);
    return option;
  }
  

   /**
   * Show an error message in a dialogue window with
   * various option buttons. When a button is pressed return
   * its number. The default button (the one fired by
   * pressing ENTER) is indicated by the given integer
   * parameter. The dialogue's parent is the given
   * component.
   */  
  public static int showError
                    (java.awt.Component parent, String msg, int dflt,
                     Object[] options)
  {
    int option = JOptionPane.showOptionDialog 
                 (parent, msg, "Error",
                  JOptionPane.DEFAULT_OPTION,
                  JOptionPane.ERROR_MESSAGE,
                  null, options, options[dflt]);
    return option;
  }
  
  protected static Object[] OK = { "OK" };
  
  /** Format and show a warning */
  public static int showWarning(java.awt.Component parent, String msg, Object... params)
  {
    return showWarning(parent, String.format(msg, params), 0, OK);
  }
  
  /** Format and show a warning */
  public static int showWarning(String msg, Object... params)
  {
    return showWarning(null, String.format(msg, params), 0, OK);
  }

  /**
   * Show a warning message in a dialogue window with
   * various option buttons. When a button is pressed return
   * its number. The default button (the one fired by
   * pressing ENTER) is indicated by the given integer
   * parameter.
   */
  public static int showWarning(String msg, int dflt, Object[] options)
  {
    return showWarning(null, msg, dflt, options);
  }

  /** Format and show an error */
  public static int showError(java.awt.Component parent, String msg, Object... params)
  {
    return showError(parent, String.format(msg, params), 0, OK);
  }
  
  /** Format and show an error */
  public static int showError(String msg, Object... params)
  {
    return showError(null, String.format(msg, params), 0, OK);
  }

  /**
   * Show an error message in a dialogue window with
   * various option buttons. When a button is pressed return
   * its number. The default button (the one fired by
   * pressing ENTER) is indicated by the given integer
   * parameter.
   */
  public static int showError(String msg, int dflt, Object[] options)
  {
    return showError(null, msg, dflt, options);
  }

}


