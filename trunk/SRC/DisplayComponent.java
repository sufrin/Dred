package org.sufrin.dred;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
        A DisplayComponent 
*/
public interface DisplayComponent
{ 
  /** Associate the given interaction listener with this component.
      Future keyboard, mouse, and mousemotion events are passed on to
      the given listener.
  */
  public void       addInteractionListener(InteractionListener listener);
  
  /** Remove this component's association with its document. */
  public void       removeDoc();
  
  /** Associate the given document with this component. */
  public void       setDoc(Document doc);
  
  /** Translate the mouse coordinates of an event into its document coordinates */
  public Point      documentCoords(MouseEvent e);
  
  /** Return the JComponent that presents the view */
  public JComponent getComponent(); 
  
  /** Insist on handling subsequent keystrokes */
  public void       requestFocus(); 
  
  /** Insist on being the *active* display */
  public void       makeActive();
  
  /** Let the canvas be dragged by the mouse */
  public void dragBy(int dx, int dy); 
  
  /** Set a tooltip */
  public void setToolTipText(String tip);
  
  /** Set the number of lines of the display */
  public void setLines(int n);

  /** Switches mode between varispaced and monospaced */
  public void setPseudoFixed(boolean on);
  
  /** Is the display pseudofixed or natural width? */
  public boolean isPseudoFixed();
}




