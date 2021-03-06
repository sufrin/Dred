package org.sufrin.dred;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.sufrin.logging.Logging;

/** 
        Convenience class -- a display with possibly-attached scrollbars.

        <PRE>$Id$</PRE>
*/
@SuppressWarnings("serial")
public class ScrolledDisplay extends JPanel implements DisplayComponent
{ 
  /** Construct a ScrolledDisplay from a scrollable display; add 
      a Y-axis scrollbar if showBar is true.
  */
  protected static boolean noBars = Dred.onMac() && !"mac".equals(System.getProperty("scroll"));
  static
  { 
     if (noBars) 
        System.err.println("[Buggy Mac OS X Scrollbars suppressed. (Reinstate with java -Dscroll=mac ... )]\n");
  }
  
  public ScrolledDisplay(ScrollableDisplay display, boolean showBar)
  { super(true);
    { // Cope with dreadful mac scrollbar bug (pro-tem)
      showBar = showBar && !noBars;
    }
    this.display = display;
    setLayout(new BorderLayout());
    add(display, "Center");
    if (showBar) 
    { 
      bar  = display.getYScrollBar();
      xbar = display.getXScrollBar();
      add(bar,   "East");
      add(xbar,  "South");
    }
  }

  /** Construct a ScrolledDisplay from a scrollable display; add a 
      Y-axis scrollbar.
  */
  public ScrolledDisplay(ScrollableDisplay display) { this(display, true); }

  /** Logger for the current class */
  static Logging log = Logging.getLog("Display");

  /** Debugging is enabled for this class: true if the 
      log named Display has level FINE or above.
  */
  public static boolean debug  = log.isLoggable("FINE");
  
  /** Set the pseudofixed mode of the display */
  public void setMonoSpaced(boolean on, char pitchModel)
  { display.setMonoSpaced(on, pitchModel);
  }

   /** Ask the pseudofixed mode of thedisplay */
   public boolean isMonoSpaced()
   { return display.isMonoSpaced();
   }

  /** Construct a ScrolledDisplay of given dimensions; add a 
      Y-axis scrollbar if showBar is true.
  */
  public ScrolledDisplay(int cols, int rows, boolean showBar)
  { 
    this(new ScrollableDisplay(cols, rows), showBar);
  }
  
  /** Construct a ScrolledDisplay of given dimensions; add a 
      Y-axis scrollbar.
  */
  public ScrolledDisplay(int cols, int rows)
  { 
    this(cols, rows, true);
  }
  
  /** Set the number of lines on the display */
  public void setLines(int n)
  { display.setLines(n);
  }

  public void addInteractionListener(InteractionListener listener)
  { 
    if (debug) log.finer("addInteractionListener: "+listener);
    display.addInteractionListener(listener);
  }

  public void requestFocus() 
  { if (debug) log.fine(""); 
    display.requestFocus(); 
  }

  public void makeActive()
  { display.makeActive(); }

  protected ScrollableDisplay display;
  protected JScrollBar        bar, xbar;

  /** Set the document associated with the display. */
  public void setDoc(Document doc)
  { display.setDoc(doc); }
  
  /** Decouple the display from the document. */
  public void removeDoc()
  { display.removeDoc(); }
  
  /** Return the JComponent view of this */
  public JComponent getComponent()
  { return this;
  }

  /** Return the document coordinates corresponding to a MouseEvent. */
  public final Point documentCoords(MouseEvent e) 
  { return display.documentCoords(e.getX(), e.getY()); }
  
  public void dragBy(int dx, int dy) { display.dragBy(dx, dy); }

  public void setFont(String font)
  {
    display.setFont(font);   
  }
}

















