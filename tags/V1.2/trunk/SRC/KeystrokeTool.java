package org.sufrin.dred;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import  javax.swing.*;

/**
        A tool that inserts keystroke names into the document
*/
public class KeystrokeTool extends ToolExtension
{ public KeystrokeTool() { super("Keystroke Tool"); }
  
  public JComponent makeTool(final EditorFrame session)
  { final JLabel l = new JLabel("Type Here");
    l.setToolTipText("<html>Typing here enters keystroke names into the document.<br></br>This tool is used for composing binding files.</html>");
    l.addKeyListener
    ( new KeyAdapter()
      {
        public void keyPressed(KeyEvent e)
        {
          KeyStroke k = KeyStroke.getKeyStrokeForEvent(e);
          session.doc.insert(k.toString().replace("pressed", "")+"\n");
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