package org.sufrin.dred;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;

/**
        A tool that inserts keystroke names into the document
*/
public class KeystrokeTool extends ToolExtension
{ public KeystrokeTool() { super("Keystroke Tool"); }
  
  public JComponent makeTool(final EditorFrame session)
  { //final JButton l = new JButton("Type Here");
    //l.setMinimumSize(new Dimension(150, 20));
    //l.setPreferredSize(new Dimension(150, 20));
    final JLabel l = new JLabel(new ImageIcon(this.getClass().getResource("keyboard.jpg")));
    l.setToolTipText("<html>Typing here enters keystroke names into the document.<br></br>This tool is used for composing binding files.</html>");
    l.addKeyListener
    ( new KeyAdapter()
      {
        public void keyPressed(KeyEvent e)
        {
          KeyStroke k = KeyStroke.getKeyStrokeForEvent(e);
          int  ch   = e.getKeyChar();
          int  mods = e.getModifiersEx();
          String comment = false ? "" : String.format(" #  %d %o[%s]", e.getKeyCode(), mods, KeyEvent.getModifiersExText(mods));
          if ((mods & KeyEvent.ALT_GRAPH_MASK) != 0) comment+=".AltGr";
          session.doc.insert(k.toString().replace("pressed", "")+comment+"\n");
        }
      }
    );
    l.addMouseListener
    (new MouseAdapter()
     { Color oldColor = Color.GREEN;
       public void mouseEntered(MouseEvent e) 
       {  oldColor = l.getForeground(); l.requestFocus(); l.setForeground(Color.GREEN); }       
       public void mouseExited(MouseEvent e)  
       {  l.setForeground(oldColor); }
     }
    );
    return l;
  }
}





