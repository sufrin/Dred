package org.sufrin.dred;
import java.io.*;
import java.nio.*;
import java.util.*;

import org.sufrin.logging.Logging;

/**
        Model of a plain text document with a current position (cursor) and
        an optional selected region.

        The selected region is between the ``mark'' and the ``cursor''.

        <PRE>$Id: Document.java,v 1.19 2005/03/30 23:06:25 sufrin Exp $</PRE>
*/

public class Document
{  /** Lines (newline-free) strictly above the cursor.*/
   protected LinkedList<String>  above;
   /** Lines (newline-free) strictly below the cursor.*/
   protected LinkedList<String>  below;
   /** The cursor is on this line. */
   protected LineBuffer          current;

   /** The logical position of the cursor after the most recent change
       that was reported to listeners.
   */
   protected int                 lastx, lasty;

   /** The current logical position of the mark: NOSELECTION if none.
   */
   protected int                 markx, marky;

   /** The logical position of the mark after the most recent change
       that was reported to listeners: NOSELECTION if none.
   */
   protected int                 lastmarkx, lastmarky;

   /** Set when the document content is changed. */
   protected boolean             changed;

   /** When markx==marky==NOSELECTION there is no mark/selection. */
   private final int NOSELECTION = -1;

   public static Logging  log   = Logging.getLog("Document");
   public static boolean debug  = log.isLoggable("FINE");

   /** Document listeners. */
   Vector<DocListener> docListeners = new Vector<DocListener>();

   /** Add a listener (usually a Display) */
   public void addDocListener(DocListener l)    
   { docListeners.add(l); 
     if (debug) log.fine(String.format("addListener(%s)", l));
   }
   
   /** Remove a listener (usually a Display) */
   public void removeDocListener(DocListener l) 
   { docListeners.remove(l); }
   
   /** The number of listeners */
   public int countListeners() { return docListeners.size(); }

   /** Topmost possible line of the most recent change to the selection. */
   protected int firstChanged()
   { int mark   = lastmarky==NOSELECTION ? marky : Math.min(lastmarky, marky);
     int cursor = Math.min(lasty, getY());
     return Math.min(cursor, mark); 
   }

   /** Bottommost possible line of the most recent change to the selection. */
   protected int lastChanged()
   { return Math.max(Math.max(lasty, getY()), Math.max(lastmarky, marky)); }

   /** Called when the document content has changed. Calculates the scope
       of the change and informs all listeners.
   */
   protected void docChanged()
   { if (hasSelection()) 
     { log.warning("BUG: docChanged called with selection."); 
       clearSelection();
     }
     int first = Math.min(lasty, getY());
     int last  = Math.max(lasty, getY());
     changed = true;
     if (debug) log.finest(String.format("Document.docChanged(%d..%d)", first, last));
     selectedRegion.set(getX(), getY(), markx, marky);
     for (DocListener listener: docListeners) listener.docChanged(first, last);
     lastx     = getX();
     lasty     = getY();
   }
   
   /** Called when the document content has changed a lot. 
   */
   protected void docChangedALot()
   { int last  = length();
     int first = 0;
     changed = true;
     if (debug) log.finest(String.format("Document.docChangedALot(%d..%d)", first, last));
     selectedRegion.set(getX(), getY(), markx, marky);
     for (DocListener listener: docListeners) listener.docChanged(first, last);
     lastx     = getX();
     lasty     = getY();
   }
   
   /** Called when the cursor has changed. Calculates the scope
       of the change and informs all listeners.
   */
   protected void cursorChanged()
   { int last  = lastChanged();
     int first = firstChanged();
     if (debug) log.finest(String.format("Document.cursorChanged(%d..%d)", first, last));
     selectedRegion.set(getX(), getY(), markx, marky);
     for (DocListener listener: docListeners) listener.cursorChanged(first, last);
     lastx = getX();
     lasty = getY();
   }
   
   /** Called when the mark has changed. Calculates the scope
       of the change and informs all listeners.
   */
   protected void selectionChanged()
   { int last  = lastChanged();
     int first = firstChanged();
     if (debug) log.finest(String.format("Document.selChanged(%d..%d)", first, last));
     selectedRegion.set(getX(), getY(), markx, marky);
     for (DocListener listener: docListeners) listener.selectionChanged(first, last);
     lastmarkx = markx;
     lastmarky = marky;
     lastx = getX();
     lasty = getY();
   }

   /** Construct a new, empty, document */
   public Document()
   { above   = new LinkedList<String>();
     below   = new LinkedList<String>();
     current = new LineBuffer();
     current.setLine("");
     current.moveTo(0);
     lastmarkx=lastmarky=markx=marky=NOSELECTION;
     changed=false;
   }

   /** Insert any character at the current position and notify
       listeners. Current position is moved to the right of the 
       inserted character. 
   */
   public void insert(char ch) 
   { 
     clearSelection();
     insertChar(ch);
     docChanged();
   }
     
   /** Insert any string at the current position and notify
       listeners. Current position is moved to the right of the 
       inserted string.
   */
   public void insert(String s) 
   { 
     clearSelection();
     for (int i=0; i<s.length(); i++) insertChar(s.charAt(i));
     docChanged();
   }

   /** Insert any character at the current position; listeners
       are not notified; newlines are treated appropriately.
   */
   protected void insertChar(char ch) 
   { if (ch=='\n') insertNewline(); else current.insert(ch);
   }

   /** Remove the selection if there is one, and notify listeners. */
   public void clearSelection()
   { if (hasSelection()) 
     { markx=marky=NOSELECTION;
       selectionChanged();
     }
   }

   /** True if the document has changed. */
   public boolean hasChanged()                { return changed; }

   /** Set the document's change flag. */
   public void    setChanged(boolean changed) { this.changed=changed; }
   
   /** True if the document has a selection. */
   public boolean hasSelection()              { return marky!=NOSELECTION; }
   
   /** True if the document has a nonempty selection. */
   public boolean hasNonemptySelection()      
   { return marky!=NOSELECTION&&(markx!=getX()||marky!=getY()); }

   /** Split the current line at the cursor; listeners are not notified.
   */
   protected void insertNewline()
   { above.addLast(current.leftString());
     current.setLine(current.rightString());
     current.moveTo(0);
   }
   
   /** Insert a newline character and indent to the current level of
       indentation.
   */
   protected void indentNewline()
   { int n=indentation();
     clearSelection();
     insertNewline();
     for (int i=0; i<n; i++) current.insert(' ');
     docChanged();
   }

   /** Return the current indentation: this is the number of spaces
       to the left of the first non-space character on the current line.
   */
   public int indentation()
   { CharSequence left = current.leftSeq();
     for (int i=0; i<left.length(); i++)
         if (left.charAt(i)!=' ') return i;
     return left.length();
   }

   /** Move the cursor left; notify listeners. */
   public void leftMove()
   { if (!current.atLeft())  current.leftMove(); else upMove();
     cursorChanged();
   }
   
   /** Move the cursor right; notify listeners. */
   public void rightMove()
   { if (!current.atRight()) current.rightMove(); else downMove(); 
     cursorChanged();
   }

   /** Delete the character to the left of the cursor; notify listeners. */
   public void leftDel()
   { clearSelection();
     if (!current.atLeft()) 
       current.leftDel(); 
     else
     { if (!atTop()) 
       { String line = above.removeLast();
         current.setLine(line+current.toString());
         current.moveTo(line.length());
       }
     }
     docChanged();
   }
   
   /** Delete the character to the right of the cursor; notify listeners. */
   public void rightDel()
   { clearSelection();
     if (!current.atRight()) 
       current.rightDel(); 
     else
     { if (!atBottom()) 
       { String nline = below.removeFirst();
         String cline = current.toString();
         current.setLine(cline+nline);
         current.moveTo(cline.length());
       }
     }
     docChanged();
   }

   /////////////////////////////////////////////////////////////////////

   /** 
       Represents a region of the document in normal form (start
       lexicographically less than end). Reversed is true if the
       original coordinates were not in normal form.
   */
   public static class Region
   { /** Start and end coordinates of this region. 
         Invariant:
         <PRE>
          starty&lt;endy or (starty==endy and startx&le;endx)
         </PRE>
     */
     public int startx, starty, endx, endy;

     /**
         True if the original start and end positions of the region
         were reversed (ie. end was before start).
     */
     public boolean reversed;
     
     public String toString()
     { return String.format("(%d,%d)..(%d,%d)", startx, starty, endx, endy);
     }
     
     /** Set the start and end positions of this region. */
     public void set(int sx, int sy, int ex, int ey)
     { if (sy<ey) 
       { starty=sy; startx=sx;
         endy=ey;   endx=ex;
         reversed=false;
       }
       else
       if (sy>ey)
       { starty=ey; startx=ex;
         endy=sy;   endx=sx;
         reversed=true;
       }
       else
       { starty=endy=sy;
         if (sx<=ex) 
         { startx=sx; endx=ex; reversed=false; } 
         else
         { startx=ex; endx=sx; reversed=true; }
       }
     }

     /** Return a copy of this region. */
     public Region copy()
     { Region result = new Region();
       result.set(startx, starty, endx, endy);
       result.reversed=reversed;
       return result;
     }
     
     /** Return a copy of this region with a negated reversed flag. */
     public Region reverse()
     { Region result = copy();
       result.reversed=!reversed;
       return result;
     }
   }

   /** Invariant: selectedRegion represents (cursorx, cursory)(markx, marky) */
   protected Region selectedRegion = new Region();
   { selectedRegion.set(0, 0, markx, marky); }

   public    Region getSelectedRegion()
   { return selectedRegion; }

   /** Return the text in the given region. */
   protected String getTextOfRegion(Region r)
   { if (r.starty==r.endy) return lineAt(r.starty).toString().substring(r.startx, r.endx);
     StringBuilder b = new StringBuilder();
     String top    = lineAt(r.starty).toString().substring(r.startx);
     String bottom = lineAt(r.endy).toString().substring(0, r.endx);
     b.append(top);
     b.append('\n');
     for (int lineNo=r.starty+1; lineNo<r.endy; lineNo++)
     {   b.append(lineAt(lineNo).toString());
         b.append('\n');
     }
     b.append(bottom);
     return b.toString();
   }
   
   ////////////////////////////// protected ///////////////////////////

   /** Move the cursor up to the end of the previous line. */
   protected void upMove() 
   { if (!atTop()) 
     { below.addFirst(current.toString());
       String line = above.removeLast();
       current.setLine(line);
       current.moveTo(line.length());
     }
   }

   /** Move the cursor down to the start of the next line. */
   protected void downMove()
   { if (!atBottom())
     { above.addLast(current.toString());
       String line = below.removeFirst();
       current.setLine(line);
       current.moveTo(0);
     }
   }

   /** Row number of the last-reported cursor position. */
   public int getY()     { return above.size(); }

   /** Column number of the last-reported cursor poosition. */
   public int getX()     { return current.getPosition(); }

   /** Row number of the previous cursor. */
   public int getLastY() { return lasty; }

   /** Column number of the previous cursor. */
   public int getLastX() { return lastx; }

   /** Move the cursor to the given row (if possible);
       otherwise as far as possible in the appropriate direction.
   */
   public void setY(int y)
   { while (!atTop()    && y<getY()) upMove();
     while (!atBottom() && y>getY()) downMove();
   }

   /** Move the cursor to the given location and notify listeners. */
   public void setCursor(int x, int y)
   { if (x==getX() && y==getY()) return;
     setCursorXY(x, y);
     cursorChanged(); 
   }
   
   /** Move the cursor to the (nearest location to the) given
       location; don't notify listeners. 
   */
   public void setCursorXY(int x, int y)
   { if (x==getX() && y==getY()) return;
     if (debug) log.finer(String.format("X,Y=%d,%d", x, y));
     setY(y);
     current.moveTo(Math.max(0, Math.min(x, current.length())));
   }
   
   /** Move the mark to the (nearest location to the) given location
       and notify listeners. 
   */
   protected void setMark(int x, int y) 
   { if (y==marky && x==markx) return;
     lastmarkx=markx;
     lastmarky=marky;
     setMarkXY(x, y);
     selectionChanged(); 
   }
   
   /** Move the mark to the (nearest location to the) given location;
       don't notify listeners. 
   */
   protected void    setMarkXY(int x, int y) 
   { if (y==marky && x==markx) return;
     marky=Math.max(0, Math.min(y, length()-1)); 
     markx=Math.max(0, Math.min(x, lineAt(marky).length())); 
     if (debug) log.finer(String.format("x,y=%d,%d => markx, marky=%d,%d", x, y, markx, marky));
   }

   /** Return the line with the given line number. 
       Requires: <PRE>0&le;y&ls;length()</PRE>
   */
   public CharSequence lineAt(int y)
   { int s=above.size();
     CharSequence line =  (y<s)  ? above.get(y) :
                          (y==s) ? current :
                          below.get(y-s-1);
     return line;
   }

   /** Return the text of the current selection */
   public String  getSelection()
   { if (!hasSelection()) return null;
     return getTextOfRegion(selectedRegion);
   }

   /** Swap cursor and mark positions and notify listeners. */
   public void swapSelectionEnds()
   { if (hasSelection())
     {  int x=getX();
        int y=getY();
        setCursorAndMark(markx, marky, x, y);
        cursorChanged();
     }
   }

   /** Paste the given string and select what was pasted and notify
       listeners. If "reversed" is true then set the mark to the
       left/above the cursor; otherwise to the right/below.
   */
   public void  pasteAndSelect(String s, boolean reversed)
   { quietPasteAndSelect(s, reversed);
     docChangedALot();
   }

   /** Paste the given string at the current position without notifying
       listeners; select the pasted material.
   */
   protected void  quietPasteAndSelect(String s, boolean reversed)
   { int sx = getX();
     int sy = getY();
     insert(s);
     if (reversed)
        setMarkXY(sx, sy);
     else 
     {  setMark(getX(), getY());
        setCursorXY(sx, sy);
     }
   }

   /** Remove the current selection from the document and notify listeners. */
   public void  cutSelection()
   { quietCutSelection();
     docChangedALot();
   }
   
   /** Remove the current selection from the document. */
   protected void  quietCutSelection()
   { if (selectedRegion.reversed) 
     { // Mark < Cursor
       setMarkXY(selectedRegion.endx, selectedRegion.endy); 
       setCursorXY(selectedRegion.startx, selectedRegion.starty);
     }
     // Cursor < Mark
     String topLeft  = current.leftString();
     String botRight = lineAt(selectedRegion.endy).toString().substring(selectedRegion.endx);
     rightDelLines(selectedRegion.endy-selectedRegion.starty);
     clearMark();
     current.setLine(topLeft+botRight);
     current.moveTo(topLeft.length());
   }
   
   /** Delete the entire document. */
   protected void deleteAll()
   { clearMark();
     setCursor(0, 0);
     current.setLine("");
     above.clear();
     below.clear();
     docChangedALot();
   }

   /** Delete n lines below the current line. */
   protected void rightDelLines(int n)
   { while (n-- > 0 && !atBottom()) below.removeFirst(); }

   /** Remove the mark. */
   protected void    clearMark() { markx=marky=NOSELECTION; }

   /** Return the mark column number. */
   public int        getMarkX()  { return markx; }

   /** Return the mark row number. */
   public int        getMarkY()  { return marky; }

   /** Return the current length (in lines/rows) of the document. */
   public int     length()    { return above.size() + below.size() + 1; }

   /** Is the current line the bottom line? */
   public boolean atBottom()  { return below.size()==0; }
   
   /** Is the current line the top line? */
   public boolean atTop()     { return above.size()==0; }
   
   /** Is the cursor at the very start of the document? */
   public boolean atStart()   { return atTop() && current.atLeft(); }
   
   /** Is the cursor at the very end of the document? */
   public boolean atEnd()     { return atBottom() && current.atRight(); }

   /** Output the document to the given PrintWriter. */
   public void output(PrintWriter out)
   { for (String line: above) out.println(line);
     if (below.size()>0) 
        out.println(current.toString());
     else 
        out.print(current.toString());
     for (String line: below) out.println(line);
   }

   /** Get the text of the document */
   public String getText()
   { StringWriter s = new StringWriter();
     PrintWriter  p = new PrintWriter(s);
     output(p);
     p.close();
     return s.toString();
   }

   /** Set the text of the document */
   public void setText(String text)
   { above.clear();
     below.clear();
     current.clear();
     setMarkXY(0, 0);
     insert(text);
     docChangedALot();
   }
   
   /** Select the text from the nearest occurence of the given character to the
       left of the cursor on the current line; return true if successful.
   */
   public boolean selectFrom(char c, boolean noBlanks)
   { CharSequence left  = current.leftSeq();
     int lc=left.length()-1;
     while (lc>=0 && left.charAt(lc)!=c)
       if  (noBlanks && left.charAt(lc)==' ') 
           return false;
       else 
           lc--;
     if (lc<0) return false;
     setMarkXY(lc, getY());
     selectionChanged();
     return true;
   }
   
   /** Select the word, if any, at the indicated position; the cursor
       is placed at the end of the word nearest to where it started.
   */
   public void selectWord(int x, int y)
   { setCursorXY(x, y);
     CharSequence left  = current.leftSeq();
     CharSequence right = current.rightSeq();
     int rc=0;
     while (rc<right.length() && Character.isLetter(right.charAt(rc))) rc++;
     // 0<=rc<right.length
     int lc=left.length()-1;
     while (lc>=0 && Character.isLetter(left.charAt(lc))) lc--;
     lc = left.length()-lc;
     if (rc>0)
     { setCursorXY(x+rc, y);
       setMarkXY(x-lc+1, y);
       if (lc<rc) swapSelectionEnds(); else selectionChanged();
     }
   }
   
   /** Select the line on which the cursor is sitting.
   */
   public void selectLine(int x, int y)
   { setCursor(0, y);
     int rc = current.rightSeq().length();
     if (!atBottom()) setMarkXY(0, y+1); else setMarkXY(rc, y);
     selectionChanged();
   }

   protected boolean isEmpty(int y)
   { 
     CharSequence line = lineAt(y);
     for (int i=0; i<line.length(); i++)
         if (line.charAt(i)!=' ') return false;
     return true;
   }
   
   /** Select the paragraph(s) that contain the selection.
   */
   public void selectParagraph()
   { int y = getY();
     int top=y, bot=hasSelection()?marky:y, len=length();
     if (top>bot) { int t=top; top=bot; bot=t; }
     while (top!=0 && !isEmpty(top)) top--;
     while (bot<len && !isEmpty(bot)) bot++;
     if (y-top > bot-y)  { int t=top; top=bot; bot=t; }
     setCursorAndMark(0, top, 0, bot);
     selectionChanged();
   }

   /** Set the cursor and mark to the given locations. */
   public void setCursorAndMark(int cx, int cy, int mx, int my)
   { // Order is important to avoid unnecessary display-origin changes
     setCursor(cx, cy); setMark(mx, my); 
   }
   
   /** Set both the cursor and mark to the given location. */
   public void setCursorAndMark(int x, int y)
   { // Order is important to avoid unnecessary display-origin changes
     setCursor(x, y); setMark(x, y);
   }

   // Utilities for external line-scoped editing tasks
   /** Returns the characters to the left of the cursor. */
   public String stringLeft()  { return current.leftSeq().toString(); }

   /** Returns the characters to the right of the cursor. */
   public String stringRight() { return current.rightSeq().toString(); }

   /** Replace the characters to the left of the cursor with the given string. */
   public void replaceLeft(String s)  
   { current.setLine(s+stringRight()); 
     current.moveTo(s.length());
     docChanged();
   }
   
   /** Replace the characters to the right of the cursor with the given string. */
   public void replaceRight(String s) 
   { current.setLine(stringLeft()+s); 
     docChanged();
   }

   // Handles for expansion

   /** NO-OP for overriding: to be invoked when the document is loaded. */
   public void doLoad(String s)
   {}

   /** Load the document from the given reader. */
   public void doLoad(BufferedReader reader)
   {  String line=null;
      try
      {
        while ((line=reader.readLine())!=null)
        { 
          insert(line);  
          insert("\n");
        }
      }
      catch (Exception ex)
      {
        throw new RuntimeException(ex);
      }
   }
   
   /** Append lines from the given reader to the document. */
   public void doAppend(BufferedReader reader)
   {  String line=null;
      try
      {
        while ((line=reader.readLine())!=null)
        { setCursorXY(0, length());
          insert(line);  
          insert("\n");
        }
      }
      catch (Exception ex)
      {
        throw new RuntimeException(ex);
      }
   }

   /** NO-OP for overriding: to be invoked when the document is saved. */
   public void doSave()
   {}


    /** 
            A LineBuffer represents a character sequence with a
            current position, optimised for read-only operations.
    */
    public class LineBuffer extends Buffer implements CharSequence
    { /** The original line */
      String  original;     
      /** True if the original has been changed since it was set. */
      boolean lineChanged;      

      /** Invoke when the buffer changes. */
      final private void setChanged()
      { if (!lineChanged)
        { int p = position;
          lineChanged = true;
          setBuffer(original);
          moveTo(p);
        }
      }
      
      public LineBuffer()         { super(20); }
      public LineBuffer(int size) { super(size); }

      /** Set the buffer to the given string; mark it unchanged. */
      public void setLine(String original)      
      { 
        clear();
        this.original = original; 
        this.lineChanged  = false;
        this.length   = original.length();
      }
      
      /** Return the current buffer content. */
      public String toString() { return lineChanged ? super.toString() : original; }

      /** Delete the character to the left of the current position. */
      public void leftDel()     
      { setChanged();
        super.leftDel();
      }
      
      /** Delete the character to the right of the current position. */
      public void  rightDel()    
      { setChanged();
        super.rightDel();
      }

      /** 
          Insert a character at the current position. Tab characters
          are replaced by enough spaces to get to the next tab position
          (tab positions are every 8 columns).
      */
      public void insert(char ch)           
      { setChanged();
        if (ch=='\t')
        { 
          do { super.insert(' '); } while (position%8!=0);
        }
        else
          super.insert(ch);
      }

      /** Return the character to the left of the cursor (or newline
          if the cursor is in column 0)
      */
      public char leftChar()
      { CharSequence left = leftSeq();
        return left.length()==0 ? '\n' : left.charAt(left.length()-1); 
      }
      
      /** Return the character to the right of the cursor (or newline
          if the cursor is at the end of the current line)
      */
      public char rightChar()
      { CharSequence right = rightSeq();
        return right.length()==0 ? '\n' : right.charAt(0); 
      }

      public CharSequence leftSeq()
      { return lineChanged ? super.leftSeq() : CharBuffer.wrap(original, 0, position); }
      
      public CharSequence rightSeq()
      { return lineChanged ? super.rightSeq() : CharBuffer.wrap(original, position, original.length()); }

      /** A string with the characters to the left of the current popsition. */
      public String leftString()  { return leftSeq().toString(); }
      
      /** A string with the characters to the right of the current popsition. */
      public String rightString() { return rightSeq().toString(); }
    
      public char charAt(int n)   
      { return lineChanged ? super.charAt(n) : original.charAt(n); }

      public CharSequence subSequence(int start, int end)
      { return lineChanged ? super.subSequence(start, end) : original.subSequence(start, end);
      }      
    }

    protected Vector<FocusEavesdropper> eavesdroppers = new Vector<FocusEavesdropper>();    
    
    public void addFocusEavesdropper(FocusEavesdropper d) 
    {
      eavesdroppers.add(d);
    }
    
    public void focusGained()
    {
      for (FocusEavesdropper d: eavesdroppers) d.focusGained(this);
    }
}
























