package GUIBuilder;
import  javax.swing.*;

/**
        A panel whose width is the max of those of its components and whose
        preferred height is the sum of its components' preferred heights.
        The <code>just</code> parameter specifies the
        justification within the bounding box of components
        shorter than the box. (0.0 means place them at the top; 0.5 means place
        them midway; 1.0 means place them at the bottom.)

        Extra width is distributed between the glue components.
        
        @version $Id: Row.java,v 1.2 2005/03/07 13:05:36 sufrin Exp $
*/
public class Row extends JPanel
{ public Row() { setLayout(new RowLayout()); }
  public Row(double just) { super(); setLayout(new RowLayout(just)); }
}














