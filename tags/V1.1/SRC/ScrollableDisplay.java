package org.sufrin.dred;
import javax.swing.*;
import javax.swing.event.*;

import org.sufrin.logging.Logging;

import java.awt.event.*;

/**
        A Scrollable Display is a Display which can be linked to
        scrollbars. The bars are constructed and linked lazily, ie. only
        when a client component requests it.

        <PRE>
        $Id: ScrollableDisplay.java,v 1.7 2005/03/30 23:06:25 sufrin Exp $
        </PRE>
*/

public class ScrollableDisplay extends Display implements DisplayComponent
{  
   public ScrollableDisplay(int cols, int rows)
   { super(cols, rows);
     addMouseWheelListener
     ( new MouseWheelListener()
       {
         public void mouseWheelMoved(MouseWheelEvent e) 
         { int rot = e.getWheelRotation();
           int amt = e.getScrollType()==MouseWheelEvent.WHEEL_UNIT_SCROLL ? e.getScrollAmount() : 1;
           if (bar!=null) 
           { bar.setValue(bar.getValue()+rot*amt);
             forceOrigin(bar.getValue());
           }
         }
       }
     );
   }
   
   public void dragBy(int dx, int dy)
   { if (bar!=null && dy!=0)
     {  bar.setValue(bar.getValue()+(dy>0?-1:1));
        forceOrigin(bar.getValue());
     }
   }
   
   protected static Logging log   = Logging.getLog("ScrollableDisplay");
   public    static boolean debug = log.isLoggable("FINE");
   
   
   public void docChanged(int first, int last)    
   { 
     if (debug) log.finest("");
     super.docChanged(first, last);
     syncScrollBar();
   }
   
   public void selectionChanged(int first, int last) 
   { 
     if (debug) log.finest("");
     super.selectionChanged(first, last);
     syncScrollBar();
   }
   
   public void cursorChanged(int first, int last) 
   { 
     if (debug) log.finest("");
     super.cursorChanged(first, last);
     syncScrollBar();
   }
   
   public void setDoc(Document doc)
   { 
     if (debug) log.fine("");
     super.setDoc(doc);
     syncScrollBar();
   }
   
   
   protected JScrollBar bar = null, xbar=null;

   protected boolean syncing = false;
   protected boolean syncing() { return syncing; }

   protected void syncScrollBar()
   {  if (bar==null) return;
      syncing = true;
      if (debug) log.finer("oy=%d, adj=%s, rows=%d", originy, bar.getValueIsAdjusting(), rows);
      bar.setVisibleAmount(rows); 
      bar.setBlockIncrement(rows); 
      bar.setUnitIncrement(1); 
      bar.setVisibleAmount(rows); 
      bar.setBlockIncrement(rows-1);  // Keep context on screen
      bar.setValue(originy); 
      xbar.setValue(originx); 
      if (doc!=null) bar.setMaximum(doc.length()+1); 
      syncing = false;
   }
   
   public JScrollBar getYScrollBar()
   { if (bar!=null) return bar;
   
     bar = new JScrollBar();
     BoundedRangeModel model = bar.getModel();

     model.addChangeListener
     ( new ChangeListener()
       {
         public void stateChanged(ChangeEvent e) 
         { if (bar.getValueIsAdjusting())
           { int newY = bar.getValue();
             if (debug) log.finer("adjusting=%s", bar.getValueIsAdjusting());
             forceOrigin(newY);
           }
         }
       }
     );
     
     bar.addAdjustmentListener
     ( new AdjustmentListener()
       { 
         public void adjustmentValueChanged(AdjustmentEvent e)       
         {  int newY = bar.getValue();
            if (debug) log.finer("adjusting=%s, syncing=%s", bar.getValueIsAdjusting(), syncing);
            // BUG: pressing on scrollbar arrows doesn't set ...IsAdjusting
            // if (bar.getValueIsAdjusting())
            // so we only force the origin if we are not syncing
            if (!syncing)
                forceOrigin(newY);
         }
       }
     );
     
     return bar;
   }
   
   public JScrollBar getXScrollBar()
   { if (xbar!=null) return xbar;
   
     xbar = new JScrollBar(JScrollBar.HORIZONTAL);
     BoundedRangeModel model = xbar.getModel();

     model.addChangeListener
     ( new ChangeListener()
       {
         public void stateChanged(ChangeEvent e) 
         { if (xbar.getValueIsAdjusting())
           { int newX = xbar.getValue();
             if (debug) log.finer("adjusting=%s", xbar.getValueIsAdjusting());
             forceXOrigin(newX);
           }
         }
       }
     );
     
     xbar.addAdjustmentListener
     ( new AdjustmentListener()
       { 
         public void adjustmentValueChanged(AdjustmentEvent e)       
         {  int newX = xbar.getValue();
            if (debug) log.finer("adjusting=%s, syncing=%s", xbar.getValueIsAdjusting(), syncing);
            // BUG: pressing on scrollbar arrows doesn't set ...IsAdjusting
            // if (xbar.getValueIsAdjusting())
            // so we only force the origin if we are not syncing
            if (!syncing)
                forceXOrigin(newX);
         }
       }
     );
     
     return xbar;
   }
}









