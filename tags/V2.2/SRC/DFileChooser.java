package org.sufrin.dred;

import java.awt.Dimension;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;

public class DFileChooser extends JFileChooser
{
  public JComboBox theSelector = null; 
  
  public DFileChooser()
  { 
    super();
    Map<String, Charset> sets = Charset.availableCharsets();
    Set<String> names = sets.keySet();
    Vector<String> codings = new Vector<String>();
    // Put the ones in that we care about
    codings.add("UTF8");    
    for (String n:names) if (n.matches("ISO-8859-[0-9]|UTF-16.*|US.*")) codings.add(n);
    for (String n:names) if (!codings.contains(n)) codings.add(n);
    codings.remove("UTF-8");
    theSelector = new JComboBox(codings);
    theSelector.setMaximumSize(new Dimension((int)theSelector.getPreferredSize().getWidth(), 40));
    Box b = Box.createVerticalBox();
    b.add(Box.createVerticalGlue());
    b.add(theSelector);
    super.setAccessory(b);
  } 
  
  public String getCoding()              { return (String) theSelector.getSelectedItem(); }
  public void   setCoding(String coding) { theSelector.setSelectedItem(coding); }
}
