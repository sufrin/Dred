package org.sufrin.dred;

import java.awt.event.*;
import java.util.prefs.*;
import org.sufrin.logging.*;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;

/**
 * A JRadioButtonMenuItem with a run method that is called
 * whenever the state of the associated button is changed.
 * The boolean variable state, that represents the state
 * of the associated button, may be used in the run
 * method. RadioItems may have persistent state that is associated with
 * a java.util.prefs.Preferences object.
 */
public abstract class RadioItem extends JRadioButtonMenuItem implements PreferenceChangeListener
{ /** Current state */
  protected boolean      state    = false;
  /** Key used in prefs lookup: the menu name without spaces. */
  protected String       itemName = null;
  /** Preferences passed in from the client if the item is to be persistent */
  protected Preferences  prefs    = null;
  
  public static Logging log = Logging.getLog("RadioItem");
  public static boolean debug = log.isLoggable("FINE");

  /** Simulates CheckBox..Item getState() behaviour */
  public boolean getState() { return isSelected(); }
  
  /** Simulates CheckBox..Item setState() behaviour */
  public void    setState(boolean newState) { setSelected(newState); }
  
  /** Make a non-persistent RadioItem initialised to the given state */
  public RadioItem(ButtonGroup g, String s, boolean istate)
  { this(g, s, istate, null, null); }

  /** Make a checkitem with the given tooltip that is initialised
      to the given state. 
  */
  public RadioItem(ButtonGroup g, String s, boolean istate, String tooltip) 
  { this(g, s, istate, tooltip, null); }
  
  /** Make a RadioItem with the given tooltip. If pref is non-null then the
      RadioItem is persistent and is associated with a boolean preference
      in pref with a name derived from s by removing spaces from it.
  */

  public RadioItem(ButtonGroup g, String s, boolean istate, String tooltip, Preferences pref)
  {
    super();
    g.add(this);
    this.itemName = s.replace(" ", "");
    this.prefs=pref;
    this.setState(prefs==null?istate:prefs.getBoolean(itemName, istate));
    state = getState();
       
    setAction(new AbstractAction(s)
    {
      public void actionPerformed(ActionEvent ev)
      {
        state = getState();
        if (debug) log.fine("RadioItem(%s) is %s", itemName, state);
        run();
        if (prefs!=null)
        { prefs.putBoolean(itemName, state);
        }
      }
    });
    
    if (tooltip!=null) setToolTipText(tooltip); 
    
    if (prefs!=null) prefs.addPreferenceChangeListener(this);
   
  }
  
  public void preferenceChange(PreferenceChangeEvent event)
  { // log.info("%s notices %s changed.", itemName, event.getKey());
    if (event.getKey().equals(itemName))
    { if (debug) log.finer("%s changed.", itemName);
      boolean newState = Boolean.parseBoolean(event.getNewValue());
      if (newState!=getState())
      { if (debug) log.fine("%s changed to %s", itemName, newState);
        this.setState(newState);
        // run();
      }
    }
  }
  
  /**
   * Invoked when the state of the switch changes; should
   * maintain any invariant relationships that are
   * intended to hold between the state of the switch and
   * the editor session.
   */
  public abstract void run();
}






