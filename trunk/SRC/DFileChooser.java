package org.sufrin.dred;

import java.awt.Dimension;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;

/** A FileChooser component with a combobox for selecting character set encodings */

@SuppressWarnings("serial")
public class DFileChooser extends JFileChooser
{
  public JComboBox<String> theSelector = null; 
  
  /** The available codings : UTF8 and the various ISO-8859 codings come first in the list */
  protected Vector<String> codings = new Vector<String>();
  
  /** Return the available codings : UTF8 and the various ISO-8859 codings come first in the list */  
  public Vector<String> getCodings() { return codings; }
  
  /** Build the chooser with whatever encodings are implemented in the current java VM */
  public DFileChooser()
  { 
    super();
    Map<String, Charset> sets = Charset.availableCharsets();
    Set<String> names = sets.keySet();
    // Put the ones in that we care about
    codings.add("UTF8");    
    for (String n:names) if (n.matches("ISO-8859-[0-9]|UTF-16.*|US.*")) codings.add(n);
    for (String n:names) if (!codings.contains(n)) codings.add(n);
    codings.remove("UTF-8");
    theSelector = new JComboBox<String>(codings);
    theSelector.setMaximumSize(new Dimension((int)theSelector.getPreferredSize().getWidth(), 40));
    Box b = Box.createVerticalBox();
    b.add(Box.createVerticalGlue());
    b.add(theSelector);
    super.setAccessory(b);
  } 
  
  /** Get the currently-selected encoding */
  public String getCoding()              { return (String) theSelector.getSelectedItem(); }
  /** Set the currently-selected coding */
  public void   setCoding(String coding) { theSelector.setSelectedItem(coding); }
}

