package GUIBuilder;
import  javax.swing.*;


/**
        A panel whose width is the max of those of its components
        and whose preferred height is the sum of its components'
        preferred heights.  The <code>just</code> parameter specifies
        the justification within the bounding box of components
        narrower than the box. (0.0 means left justify; 0.5 means
        center justify; 1.0 means right justify).

        Extra height is distributed between the glue components.
        
        @version $Id$
*/
@SuppressWarnings("serial")
public class Col extends JPanel
{ public Col() { setLayout(new ColLayout()); }
  public Col(double just) { setLayout(new ColLayout(just)); }
}












