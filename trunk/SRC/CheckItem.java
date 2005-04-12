package org.sufrin.dred;

import java.awt.event.*;
import java.util.prefs.Preferences;
import org.sufrin.logging.*;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;

/**
 * A JCheckBoxMenuItem with a run method that is called
 * whenever the state of the associated button is changed.
 * The boolean variable state, that represents the state
 * of the associated button, may be used in the run
 * method. CheckItems may have persistent state that is associated with
 * a java.util.prefs.Preferences object.
 */
public abstract class CheckItem extends JCheckBoxMenuItem
{ /** Current state */
  protected boolean      state    = false;
  /** Key used in prefs lookup: the menu name without spaces. */
  protected String       itemName = null;
  /** Preferences passed in from the client if the item is to be persistent */
  protected Preferences  prefs    = null;
  
  public static Logging log = Logging.getLog("CheckItem");
  public static boolean debug = log.isLoggable("FINE");

  /** Make a non-persistent CheckItem initialised to the given state */
  public CheckItem(String s, boolean istate) { this(s, istate, null, null); }
  
  /** Make a checkitem with the given tooltip that is initialised
      to the given state. 
  */
  public CheckItem(String s, boolean istate, String tooltip) 
  { this(s, istate, tooltip, null); }
  
  /** Make a CheckItem with the given tooltip. If pref is non-null then the
      CheckItem is persistent and is associated with a boolean preference
      in pref with a name derived from s by removing spaces from it.
  */

  public CheckItem(String s, boolean istate, String tooltip, Preferences pref)
  {
    super();
    this.itemName = s.replace(" ", "");
    this.prefs=pref;
    this.setState(prefs==null?istate:prefs.getBoolean(itemName, istate));
    state = getState();
       
    setAction(new AbstractAction(s)
    {
      public void actionPerformed(ActionEvent ev)
      {
        state = getState();
        if (debug) log.fine("CheckItem(%s) is %s", itemName, state);
        run();
        if (prefs!=null)
        { prefs.putBoolean(itemName, state);
        }
      }
    });
    
    if (tooltip!=null) setToolTipText(tooltip); 
   
  }
  
  /**
   * Invoked when the state of the switch changes; should
   * maintain any invariant relationships that are
   * intended to hold between the state of the switch and
   * the editor session.
   */
  public abstract void run();
}




