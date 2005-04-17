package GUIBuilder;
import java.awt.*;
import java.awt.event.*;
import javax.swing.JComponent;

/** 
        Basis for a very light alternative to Swing buttons that can
        coexist with Swing, without the awful standard colour scheme.
        I really didn't want to find out about Swing's UI delegates, etc. 
*/
public abstract class LightweightButton extends JComponent 
{ ActionListener listener;
  String         command;
  boolean pressed = false, entered=false, enabled = true;                     

  abstract public void paint(Graphics g);                     
  abstract public void pressed(boolean pressed);              
  abstract public void entered(boolean entered);              

  public LightweightButton()
  { enableEvents(AWTEvent.MOUSE_EVENT_MASK); }                
 

  public void addActionListener(ActionListener newlistener) 
  { listener = AWTEventMulticaster.add(listener, newlistener); }
  
  public void processMouseEvent(MouseEvent e)                 
  {    switch(e.getID()) 
       {  case MouseEvent.MOUSE_PRESSED:  pressed(true);  break;
          case MouseEvent.MOUSE_RELEASED: pressed(false); break;
          case MouseEvent.MOUSE_ENTERED:  entered(true);  break;
          case MouseEvent.MOUSE_EXITED:   entered(false); break;
       }  
       super.processMouseEvent(e);
  }
  
  public void act()                                                  
  { if (listener != null) 
      listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command)); 
    run();
  }
  
  public void run() {}

  public void removeActionListener(ActionListener oldlistener) 
  { listener = AWTEventMulticaster.remove(listener, oldlistener);
  }
    
  public Dimension getMinimumSize()   { return new Dimension(20, 20); }

  public Dimension getPreferredSize() { return getMinimumSize(); }
  
  public Dimension getMaximumSize()   { return getMinimumSize(); }
}


