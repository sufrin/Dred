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
public class RadioItem<VAL> extends    JRadioButtonMenuItem 
                            
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
    this.value    = value;
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
  public static abstract class Group<VAL> extends ButtonGroup implements PreferenceChangeListener
  { 
    /** The items in the group */
    HashSet<RadioItem<VAL>> items = new HashSet<RadioItem<VAL>>();
    /** The current value of the group */
    VAL    value   = null;
    /** The tooltip shared by members of the group */
    String tooltip = null;
    /** The name of the group */
    String name    = null;
    /** A parser */
    Parser parser  = null;
    /** The preferences that this group is associated with */
    Preferences prefs = null;
    
    /** Construct a group with the given name, whose initial value is
        the given value.         
    */
    public Group(String name, VAL value, String tooltip)
    { this(name, value, tooltip, null, null);
    }
    
    public Group(String name, VAL value, String tooltip, Preferences prefs, Parser<VAL> parser)
    { 
      this.value   = value;
      this.tooltip = tooltip;
      this.name    = name;
      this.prefs   = prefs;
      this.parser  = parser;
      if (prefs!=null) prefs.addPreferenceChangeListener(this);
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
    
    public void preferenceChange(PreferenceChangeEvent event)
    { if (parser!=null && event.getKey().equals(name))
      { VAL newVal = (VAL) parser.parse(event.getNewValue());
        for (RadioItem item: items)
            item.setState(item.value!=null && item.value.equals(newVal));
      }
    }
    
  }
  
  public static interface Parser<VAL> { VAL parse(String source); }
  
  public static Parser<String> toString = new Parser<String>()
  {
    public String parse(String source) { return source; }
  };
  
  public static Parser<Integer> toInt = new Parser<Integer>()
  {
    public Integer parse(String source) { return Integer.getInteger(source); }
  };
  
}








