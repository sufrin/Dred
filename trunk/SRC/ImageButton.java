package GUIBuilder;
import java.awt.*;
import java.awt.event.*;
import javax.swing.ImageIcon;

/** Quick and dirty flat ImageButton implementation.
    Used for the stop button.
 */

public class ImageButton extends LightweightButton
{ 
  public ImageButton(ImageIcon arg) 
  { image = arg; 
    imageDimension = new Dimension(4+image.getIconWidth(), 4+image.getIconHeight()); 
  }
  
  ImageIcon image;
  Dimension imageDimension;
  
  public void pressed(boolean pressed)
  { this.pressed=pressed;
    if (!pressed && entered) act();
    repaint();
  }
 
  public void entered(boolean entered)
  { this.entered=entered; 
    repaint(); 
  }
  
  public void setEnabled(boolean state)
  { enabled = state;
    repaint();
  }
 
  public void paint(Graphics g)
  { Dimension d = getSize();
    Dimension m = imageDimension;
    g.setColor(enabled ? Color.RED : getBackground());
    int delta = !pressed||!entered ? 0 : 1;
    g.fill3DRect(0,0, d.width-1, d.height-1, !pressed||!entered);
    g.setColor(getForeground());
    g.drawImage(image.getImage(), delta+2+(d.width - m.width)/2, delta+2+(d.height - m.height)/2, null);
  }
  
  public Dimension getMinimumSize() { return imageDimension; }  
}



