package org.sufrin.dred;

import java.awt.event.*;
import javax.swing.event.*;

import java.util.prefs.*;
import java.util.HashSet;
import org.sufrin.logging.*;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;

/**
 * A JRadioButtonMenuItem with a run method that is called
 * whenever the state of the associated button is changed.
 * The boolean variable state, that represents the state
 * of the associated button, may be used in the run
 * method. 
 */
public class RadioItem<VAL> extends JRadioButtonMenuItem 
{ /** Current state */
  protected boolean      state    = false;

  /** The value associated with this RadioButton */
  protected VAL value    = null;
  
  public static Logging log   = Logging.getLog("RadioItem");
  public static boolean debug = log.isLoggable("FINE");

  /** Simulates CheckBox..Item getState() behaviour */
  public boolean getState() { return isSelected(); }
  
  /** Simulates CheckBox..Item setState() behaviour */
  public void    setState(boolean newState) 
  { setSelected(newState); }
  
  public RadioItem(final Group<VAL> group, VAL value)
  {
    this(group, value.toString(), value);
  }
  
  public RadioItem(final Group<VAL> group, String name, VAL value)
  {
    super();
    this.value = value;
    group.add(this);
    setAction
    (new Act(name, group.tooltip)
    { 
      public void run()
      {
        state = getState();
        if (debug) log.fine("RadioItem(%s) is %s", RadioItem.this.value==null?"?":RadioItem.this.value, state);  
        if (state) group.setValue(RadioItem.this.value);      
      }
    });
          
       
  }
  
  /** A RadioItem.Group is custodian of a value controlled by a collection
     of RadioItems.
  */
  public static abstract class Group<VAL> extends ButtonGroup
  { /** The items in the group */
    HashSet<RadioItem<VAL>> items = new HashSet<RadioItem<VAL>>();
    /** The current value of the group */
    VAL    value   = null;
    /** The tooltip shared by members of the group */
    String tooltip = null;
    /** The name of the group */
    String name    = null;
    /** The preferences that this group is associated with */
    Preferences prefs = null;
    
    /** Construct a group with the given name, whose initial value is
        the given value.         
    */
    public Group(String name, VAL value, String tooltip, Preferences prefs)
    { 
      this.value   = value;
      this.tooltip = tooltip;
      this.name    = name;
      this.prefs   = prefs;
    }
    
    public void add(RadioItem<VAL> item) 
    { super.add(item);
      items.add(item); 
      item.setState(item.value!=null && item.value.equals(this.value));
    }
    
    /** Set the value associated with the group */
    public void setValue(VAL value)
    { this.value=value;
      run();
      if (prefs!=null) prefs.put(name, value.toString());
    }  
    
    /** Invoked when the value associated with  the group changes */
    abstract public void run();
  }
  
}







