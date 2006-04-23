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
        $Id$
        </PRE>
*/
@SuppressWarnings("serial") 
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
           if (!passiveBar && bar!=null) 
           { bar.setValue(bar.getValue()+rot*amt);
             forceOrigin(bar.getValue());
           }
           else
           {  forceOrigin(originy+rot*amt);
           }
         }
       }
     );
   }
   
   public void dragBy(int dx, int dy)
   { if (dy!=0)
     {
       if (!passiveBar && bar!=null)
       {  bar.setValue(bar.getValue()+(dy>0?-1:1));
          forceOrigin(bar.getValue());
       }
       else
          forceOrigin(originy+(dy>0?-1:1));
     };
     if (dx!=0)
     {
       if (!passiveBar && xbar!=null)
       {  xbar.setValue(xbar.getValue()+(dx>0?-1:1));
          forceXOrigin(xbar.getValue());
       }
       else
          forceXOrigin(originx+(dx>0?-1:1));
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
   protected boolean passiveBar = "passive".equals(System.getProperty("scroll"));

   protected void syncScrollBar()
   {  if (bar==null) return;
      syncing = true;
      if (debug) log.finer("oy=%d, adj=%s, rows=%d", originy, bar.getValueIsAdjusting(), rows);
      bar.setValueIsAdjusting(true);
      //bar.setVisibleAmount(rows); 
      //bar.setBlockIncrement(rows); 
      bar.setUnitIncrement(1); 
      bar.setVisibleAmount(rows); 
      bar.setBlockIncrement(rows-1);  // Keep context on screen
      bar.setValue(originy); 
      xbar.setValue(originx); 
      if (doc!=null) bar.setMaximum(doc.length()+1); 
      syncing = false;
      bar.setValueIsAdjusting(false);
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
     
     if (passiveBar) return bar;
     
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
     
     if (passiveBar) return xbar;
     
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


