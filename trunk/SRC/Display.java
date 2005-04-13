package org.sufrin.dred;
import org.sufrin.font.*;
import org.sufrin.logging.Logging;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
/**
        A Display implements a pan-able / tilt-able window onto a document.
        This implementation handles variable-width fonts correctly and
        (for simplicity) shows TAB-characters as if they were of fixed width.
        
        <PRE>
        $Id$
        </PRE>
*/

public     class Display 
extends    JComponent  
implements DocListener, 
           FocusListener, 
           DisplayComponent
           
{  /** The associated document. */
   protected Document doc;

   /** Current logical dimensions of the screen. */
   protected int   rows=24,   cols=80;

   /** Logical position (relative to document origin) of
       the origin of the display window.

       Invariant:
       <PRE>
          0&lt;=originy&lt;doc.length() and 0&lt;=originx&lt;doc.lineAt(doc.getY()).length()
       </PRE>
   */
   protected int   originx=0, originy=0;

   /** Physical border dimensions. 0..xmargin-1 is space for (line) annotations. */
   protected int xmargin=2, xborder=6, yborder=4;

   /** Logging for the current class */
   static Logging log = Logging.getLog("Display");

   /** Debugging is enabled for this class: true if the 
       log named Display has level FINER or above.
   */  
   public static boolean debug  = log.isLoggable("FINE");

   /** Name of the default font: set from property DREDFONT
       or environment variable DREDFONT, otherwise MONOSPACED 14.
   */
   public static String  defaultFontName = System.getProperty("DREDFONT"); 
   
   public static void setDefaultFontName(String newName)
   {
     defaultFontName = newName;
   }
   
   static 
   { if (defaultFontName==null) defaultFontName = System.getenv("DREDFONT");
     if (defaultFontName==null) defaultFontName = "MONOSPACED 14"; 
   }

   /** The current font. */
   protected Font  font = null;
   
   /** The current font's name. */
   protected String  fontName = null;

   /** The current font's url (may start with type1: or truetype:)*/
   protected String  fontURL = null;

   /** Metrics for the current font. */
   protected FontMetrics metrics = null;

   /** Characteristic measurements (in pixels) derived from the current font. */
   protected int   fontAscent, fontHeight, fontDescent, fontEmWidth, fontHalfEm;
   
   /** The font's model character if it's fixed-width */
   protected char pitchModelChar = 0;
   
   /** The default monospace character model */
   final public static char defaultPitchModelChar = 'M';

   /** Width (in pixels) for text on the display. */
   protected int textWidth;

   /** Current physical dimensions. */
   protected Dimension dim = new Dimension(0,0), preferred = new Dimension(0,0);  
   
   public Dimension getMinimumSize()   { return new Dimension(50, 50); }
   public Dimension getMaximumSize()   { return new Dimension(2048, 2048); }
   public Dimension getPreferredSize() { return preferred; }
   
   /** Is the editor in TypeOver mode */
   protected static boolean typeOver = false;

   /** Set physical dimensions. */
   public void setSize(Dimension d)   
   {
     setSize(d.width, d.height);
   }

   public boolean isDoubleBuffered() { return false; }

   /** Set the physical dimensions of the display to w pixels wide and h pixels deep
       using the dimensions of the current font to calculate the
       logical dimensions in characters. Character widths are estimated (a
       unit of one 'm'-width is used) so may be misleading for a
       non-monospaced font.
   */
   public void setSize(int w, int h)   
   { dim = new Dimension(w, h); 
     w -= 2*xborder + xmargin;
     h -= 2*yborder;
     rows = h / fontHeight;
     cols = w / fontEmWidth;
     textWidth = w+xborder/2;
     if (debug) log.finer(String.format("setSize(%d:%d,%d:%d)", w, cols, h, rows));  
     if (doc!=null && chooseOrigin()) repaint();
   }
   
   public void setBounds(int x, int y, int w, int h)
   { super.setBounds(x, y, w, h);
     setSize(w, h);
   }
   
   public void setFont(String fontURL)
   { Font theFont = null;
     this.fontURL = fontURL;
     if (fontURL.startsWith("truetype:")) 
     { fontName  = fontURL.substring(9);
       theFont      = FontMaker.decode(fontName, true);
       pitchModelChar = FontMaker.fixedChar(fontName, true);
     }
     else
     if (fontURL.endsWith(".ttf") || fontURL.endsWith(".ttf")) 
     { fontName  = fontURL;
       theFont      = FontMaker.decode(fontName, true);
       pitchModelChar = FontMaker.fixedChar(fontName, true);
     }
     else
     if (fontURL.startsWith("type1:")) 
     { fontName  = fontURL.substring(6);
       theFont      = FontMaker.decode(fontName, false);
       pitchModelChar = FontMaker.fixedChar(fontName, false);
     }
     else
     {  fontName = fontURL;
        theFont = Font.decode(fontName);
     }
     monoSpaced = pitchModelChar != 0;
     setFont(theFont); 
     if (doc!=null) calibrate();
   }

   /** Set the font; resizing the display as necessary to accomodate
       the current width and height measured in characters.
   */
   public void setFont(Font font)
   { metrics     = getFontMetrics(font);
     this.font   = font;
     fontAscent  = metrics.getAscent();
     fontHeight  = metrics.getHeight();
     fontDescent = metrics.getDescent();
     fontEmWidth = monoSpaced ? metrics.charWidth(pitchModelChar) : metrics.charWidth(defaultPitchModelChar);
     fontHalfEm  = monoSpaced ? fontEmWidth / 2 : 0;
     setSize(xmargin+2*xborder+fontEmWidth*cols, 2*yborder+fontHeight*rows);
   }
   
   /** True if simulating a monospaced font -- we usually use font data for positioning calculations. */
   protected boolean monoSpaced = false;
   
   /** Switches mode between pseudofixed and natural widths */
   public void setMonoSpace(boolean on, char pitchModel)
   { monoSpaced = on;
     if (monoSpaced && pitchModel!='\000') pitchModelChar = pitchModel;
     setFont(this.font);
     repaint();
   }
   
   /** Are we pseudofixed or natural width? */
   public boolean isMonoSpaced() { return monoSpaced; }
   
   /** Local calculation of character width: implements pseudofixed */
   protected int charWidth(char c)
   { 
     return monoSpaced ? fontEmWidth : metrics.charWidth(c);
   }
   
   /** Translate character coordinates to a pixel offset from the left margin. */
   protected int charToPixelX(int ox, int x, int y)
   { CharSequence line = doc.lineAt(y);
     int pixel = 0;
     for (int i=x-1; i>=ox; i--) pixel+=charWidth(Document.transChar(line.charAt(i)));
     return pixel;
   }

   /** Translate pixel offset from the left margin to a character index */
   protected int pixelToX(int x, int y)
   { if (y>=doc.length()) return 0;
     if (monoSpaced) return originx + ((x+fontHalfEm)/fontEmWidth);
     CharSequence line   = doc.lineAt(y);
     int          length = line.length();
     int          lx     = 0;
     int          px     = 0;
     int          i      = originx;
     while (i<length)
     { px += charWidth(Document.transChar(line.charAt(i)));
       if (x<px) { return (px-x)<=(x-lx) ? i+1 : i; }
       i++;
       lx=px;
     }
     return length;
   }

   /**
      Set the current document associated with the display.
   */
   public void setDoc(Document doc)
   { if (this.doc!=null) 
        log.severe("BUG: this display is already associated with a document");
     this.doc=doc;
     doc.addDocListener(this);
   }
   
   /**
    * Set the number of lines shown on the display (largely for minitexts)
    */
   public void setLines(int n)
   {
     this.rows=n;
     setFont(font);
     preferred = dim;
   }
   
   /**
      Decouple the given document from this Display.
   */
   public void removeDoc()
   { if (doc!=null) doc.removeDocListener(this);   
     doc=null;
   }
   
   /**
      Return the JComponent view of this.
   */
   public JComponent getComponent()
   { return this;   
   }

   /** Set the character dimensions of the display from its
       physical (pixel) dimensions.
   */
   protected void calibrate()
   { 
     setSize(getSize());
     repaint();
   }

   /** Construct a new display with the given logical (character)
       dimensions. 
   */
   public Display(int cols, int rows)
   { originx=0;
     originy=0;
     this.cols=cols;
     this.rows=rows;
     setFont(defaultFontName);
     preferred = dim;
     
     addComponentListener
     ( new ComponentAdapter()
       { public void componentResized(ComponentEvent e) 
         { calibrate(); }
       }
     );

   }

   /** Paint the cursor and mark. */
   protected void paintCursor(Graphics g)
   { // Draw the mark
     if (doc.hasSelection())
     {  int my = doc.getMarkY()-originy;
        int mx = doc.getMarkX()-originx;
        int mp = charToPixelX(originx, originx+mx, my+originy);
        g.setColor(Color.GREEN);
        g.fillRect(xmargin+xborder+mp-1, yborder+my*fontHeight, 2, fontHeight);
     }
     // Draw the cursor
     int cy = doc.getY()-originy;
     int cx = doc.getX()-originx;
     int mp = charToPixelX(originx, originx+cx, cy+originy);
     g.setColor(focussed ? Color.RED : Color.GRAY);
     g.fillRect(xmargin+xborder+mp-1, yborder+cy*fontHeight, 2, fontHeight);
     g.setColor(getForeground());
   }

   /** Request a repaint of the region of the display that gives
       feedback indicating whether the display has the focus.
   */
   public void repaintFocus()
   { if (doc==null) return;
     int cy = doc.getY()-originy;
     int cx = doc.getX()-originx;
     int mp = charToPixelX(originx, originx+cx, cy+originy);
     repaint(xmargin+xborder+mp-1, yborder+cy*fontHeight, 2, fontHeight);
     //repaint(0, 0, dim.width, yborder);
   }
      
   /** No-op: repainting (including background) is done by paint. */
   public void update(Graphics g) {}

   /** Paint the lines visible in the clip rectangle. */
   public void paint(Graphics g)
   { if (doc==null) return;
     if (debug) log.finer(String.format("Paint origin=%d,%d", originx,originy));
     // Calculate the bounding rectangle(s) of the selection
     Document.Region region = doc.getSelectedRegion();
     
     int selStartY = region.starty;
     int selStartX = region.startx;
     int selStopY  = region.endy;
     int selStopX  = region.endx;
     int bottom    = Math.min(originy+rows, doc.length());
     
     // Draw the visible lines
     g.setColor(getForeground());
     g.setFont(font);

     int drawn=0; 
     Color selColour   = getBackground().darker();
     if (typeOver && doc.wasDeliberateSelection()) selColour = selColour.darker();
     
     Color panColour   = Color.YELLOW;
     Color plainColour = Color.WHITE;
     
     for (int lineNo=originy, y=yborder; lineNo<bottom; lineNo++, y+=fontHeight)
     { 
       if (g.hitClip(0, y, dim.width, fontHeight))
       { CharSequence line = doc.lineAt(lineNo);
         int baseLine = y+fontAscent;
         drawn+=1;

         // Marginal fixedCharrmation
         g.setColor(originx>0 ? panColour : plainColour);
         g.fill3DRect(xmargin, y, xborder/2, fontHeight+1, true);
         g.setColor(getForeground());
         
         // Draw the selection background, if necessary
         if (0<=selStartY && selStartY<=lineNo && lineNo<=selStopY)
         {  int l=0, w=0;

            // Calculate start and width of selection rectangle (doc coordinates)
            if (lineNo==selStartY)                        
            { l=charToPixelX(originx, selStartX, lineNo); 
              if (selStartY==selStopY)
                 w=charToPixelX(originx, selStopX, lineNo)-l; 
              else
                 w=textWidth-l; 
            }
            else
            if (lineNo==selStopY)                         
              w=charToPixelX(originx, selStopX, lineNo); 
            else
              w=textWidth; 

            // Draw the background rectangle
            g.setColor(selColour);
            g.fillRect(xmargin+xborder+l, y, w, fontHeight);
            g.setColor(getForeground());
         }
         

         // Draw the line: character-by-character 
         int length = line.length();
         char [] ch = new char[1];
         for (int c=originx, x=xmargin+xborder; c<length; c++, x+=charWidth(ch[0]))
         {  ch[0]=line.charAt(c);
            if (ch[0]=='\t')
            {
               g.drawRect(x, baseLine-4, 6, 6); x+=6;
            }
            else 
            if (Document.isMark(ch[0]))
            {  g.setColor(Color.RED);
               ch[0]=Document.transChar(ch[0]);
               g.drawString(new String(ch), x, baseLine);
               g.setColor(getForeground());
            }
            else
               g.drawString(new String(ch), x, baseLine);
         }
       }
     }

     // Draw the cursor
     paintCursor(g);
     if (debug) log.finer("drawn: "+drawn);
   }

   
   /** Choose a new origin if that is necessary for the cursor to
       be visible on the display; return true if changed.
   */
   protected boolean chooseOrigin()
   { 
     // Tilt, if necessary
     int cy    = doc.getY();
     int oy    = originy;
     int lasty = originy+rows-1;
     if (debug) log.fine("oy, cy, lasty = %d, %d, %d", oy, cy, lasty);
     if (originy <= cy && cy <= lasty)
        // On the screen
        {  }
     else
     if (cy<originy && cy > originy-3)  
        // Upper margin
        { originy=Math.max(0, cy-4); }   
     else
     if (cy==originy && doc.hasSelection() && doc.getMarkY()<originy) 
        // Top line: scroll up if there's a selection that starts above
        { originy=Math.max(0, cy-4); }   
     else
     if (cy<originy)    
        // Well above the upper margin               
        { originy=Math.max(0, cy-rows/2); }
     else
     if (cy>lasty && cy<lasty+rows)    
        // No more than a screen-depth below
        { originy+=cy-lasty; }     
     else
        // More than a screen-depth below
        { while (cy>lasty) { originy+=rows; lasty+=rows; }
        }
     
     if (debug) log.fine(String.format("oy, cy, ly = %d, %d, %d => %d", oy, cy, oy+rows-1, originy));

     // Pan if necessary
     int cx    = doc.getX();
     int ox    = originx;
     if (charToPixelX(originx, cx, cy)>textWidth)
     { // Shift right to accomodate the cursor
       while (charToPixelX(originx, cx, cy)>textWidth) originx += 4;
     }
     else
     if (cx<ox)
     { // Shift left to accomodate the cursor
       if (charToPixelX(0, cx, cy)<=textWidth) 
          originx=0;
       else
          while (cx<originx) originx -= 4;
     }
     if (debug) log.finer(String.format("origin=%d,%d", originx, originy));
     return originy!=oy || originx!=ox;
   }

   /** Force the new origin of the display to be at line y of the document, 
      if possible.  Intended for use by scrollbars and mouse wheels.
   */
   public void forceOrigin(int y)
   { int neworiginy=Math.max(0, Math.min(y, doc.length()-rows));
     if (neworiginy!=originy)
     { 
       if (debug) log.finer(String.format("%d => %d", originy, neworiginy));
       originy=neworiginy;
       repaint();
     }
   }
   
   /** Force the new X origin of the display to be at column X of the document, 
      if possible.  Intended for use by scrollbars.
   */
   public void forceXOrigin(int x)
   { int neworiginx=Math.max(0, Math.min(x, cols));
     if (neworiginx!=originx)
     { 
       if (debug) log.finer(String.format("%d => %d", originx, neworiginx));
       originx=neworiginx;
       repaint();
     }
   }

   /**
     True if a notified change in the document can be ignored by this
     display because it's not the active display and the scope of the
     change is outside the region of the document shown by the screen.
   */
   protected boolean ignoreableChange(int first, int last)
   { return (!isActive()) && (last < originy || first > originy+rows);  }

   /** Notification by the document that its content changed between the given lines.
   */
   public void docChanged(int first, int last)    
   { if (debug) log.finer(String.format("docChanged(%d..%d) (%s)", first, last, isActive()));
     if (ignoreableChange(first, last)) 
        return;
     else
     if (chooseOrigin()) 
        repaint(); 
     else
     if (last==first)    
        repaintCursorLines(); 
     else 
        repaintBelow(first);
   }
   
   /** Request repainting of the visible lines starting with first. */
   protected void repaintBelow(int first)
   {  int vtop = first-originy;
      repaint(0, yborder+vtop*fontHeight, getWidth(), fontHeight*(rows-vtop));
   }   
   
   /** Notification by the document that the selection scope changed -- 
       between the given lines.
   */
   public void selectionChanged(int first, int last) 
   { if (debug) log.finer(String.format("selChanged(%d..%d)", first, last));
     if (ignoreableChange(first, last)) 
        return;
     else
     if (last>first) 
     { if (isActive() && chooseOrigin()) 
          repaint(); 
       else
       { int vtop = first-originy;
         repaint(0, yborder+vtop*fontHeight, getWidth(), fontHeight*(last-first+1));
       }
     }
     else
       repaintCursorLines();
   }
   
   /** Notification by the document that the cursor position changed -- 
       on no more than the given number of lines. 
   */
   public void cursorChanged(int first, int last) 
   { if (debug) log.finer(String.format("cursorChanged(%d..%d)", first, last));
     if (ignoreableChange(first, last)) 
        return;
     else
     if (chooseOrigin())
        repaint();
     else
        repaintCursorLines();
   }

   /** Repaint the last and the current line occupied by the cursor. 
       Invisible unless one or the other is on the current screen.
   */
   protected void repaintCursorLines()
   {  int ly = doc.getLastY()-originy;     
      int cy = doc.getY()-originy;
      repaint(0, yborder+cy*fontHeight, getWidth(), fontHeight);
      if (ly!=cy)
      { repaint(0, yborder+ly*fontHeight, getWidth(), fontHeight);
      }
      if (debug) log.finer(("repaintcursorlines ")+(isActive() ? "focussed" : "unfocussed"));
   }

   /** Translate screen coordinates of a mouse event to document coordinates. */
   public final Point documentCoords(MouseEvent e) 
   { return documentCoords(e.getX(), e.getY()); }
   
   /** Translate screen coordinates to document coordinates. */
   public final Point documentCoords(int ex, int ey)
   { int y = Math.max(0, (ey-yborder) / fontHeight + originy);
     int x = pixelToX(ex-xborder-xmargin, y);
     return new Point(x, y);
   }

   /** Add the given interaction listener by adding its
       component Mouse, MouseMotion, and Keyboard listeners.
   */
   public void addInteractionListener(InteractionListener listener)
   { addMouseListener(listener);
     addMouseMotionListener(listener);
     addKeyListener(listener);
     if (debug) log.finer("InteractionListener: "+listener);
   }   
   
   /*  Swing KeyboardFocus should let this component request focus,
       but should otherwise ignore it.
   */
   private static Set<KeyStroke> empty = new HashSet<KeyStroke>();
   { setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,  empty);
     setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, empty);
     setFocusTraversalKeys(KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, empty);
     setFocusable(true);
     setRequestFocusEnabled(true);
     addFocusListener(this);
   }

   /** True iff this Display has the keyboard focus. */
   protected boolean focussed;

   /** Called when this component gets the focus. 
       Sets the mouse cursor shape, and shows a visual indication
       that the component has the focus.
   */
   public void focusGained(FocusEvent e) 
   {
     log.fine("Focus gained from: %s", e.getOppositeComponent());
     focussed = true;
     setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
     repaintFocus();
     if (doc!=null) doc.focusGained(); // inform the document
   }
   
   /** The display cursor shown when this Display doesn't have the focus */
   protected static Cursor unfocussed = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
   
   /** Called when this component loses the focus. 
       Sets the mouse cursor shape, and shows a visual indication
       that the component has not got the focus.
   */
   public void focusLost(FocusEvent e) 
   { 
     log.fine("Focus lost to: %s", e.getOppositeComponent());
     focussed = false;
     setCursor(unfocussed);
     repaintFocus();
   }

   /** Request the focus. */
   public void requestFocus()
   { if (debug) log.fine("");
     super.requestFocus();
   }

   /** There is only one ACTIVE Display, and that is the one on which
       the most recent MousePressed or KeyPressed event took place.

       A Display at rest always shows a view of the current content of
       its document.

       An ACTIVE display always shows a region around its document's
       cursor when it is at rest, and will Pan and/or Tilt in order to
       do so when the state of the document changes.

       After a change in document state a non-ACTIVE display will not
       pan or tilt in order to show its document's cursor unless that
       cursor was already visible before the change.

       This makes it possible to have more than one display of a given
       document on the screen at once.       
   */
   protected static Display active = null;

   /** Make this display the active one. */
   public void makeActive()
   { active = this; }

   /** Is this display the active one? */
   public boolean isActive() { return active==this; }
   
   /** Some UIs let the canvas be dragged by the mouse */
   public void dragBy(int dx, int dy) {}

}








































