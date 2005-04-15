package org.sufrin.dred;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import org.sufrin.logging.Logging;

/**
        A simple editor.

        <PRE>$Id$</PRE>
*/

class SimpleEditor implements InteractionListener, Patient
{  /** The Display component */
   protected DisplayComponent  display;
   
   /** The associated document */
   protected Document          doc;
   
   /** The last selection */
   protected String            lastSel;
   
   /** True if the last selection had the mark before the cursor */
   protected boolean           reversed;
   
   /** True if all sessions are in "type-over" mode */
   static protected boolean   typeOver;
   
   /** True if THIS editor pays attention to the idea of typeover */
   protected boolean allowsTypeOver = true;
   
   /** Sets whether THIS session pays attention to the notion of typeover or not.
   */
   protected void allowTypeOver(boolean b) { allowsTypeOver = b; }
   
   /** Set "type-over" mode */
   static public void setTypeOver(boolean mode) 
   { typeOver = mode; 
     Display.typeOver = mode;
   }
   
   /** Indicate that the selection just made was deliberate (not automatic) */
   protected void deliberateSelection()
   { if (allowsTypeOver && typeOver) doc.deliberateSelection();
   }
   
   /** Remove a nonempty deliberate selection made in typeover mode.
       Return true iff the selection was removed.
   */
   protected boolean typeOver()
   { if (allowsTypeOver && typeOver && doc.wasDeliberateSelection() && doc.hasNonemptySelection())
     { lastSel = clippedSelection();
       return true;
     }
     return false;
   }
   
   /** Log for debugging output */
   protected static Logging log   = Logging.getLog("SimpleEditor");
   
   /** True if logging level is at least FINE */
   public    static boolean debug = log.isLoggable("FINE");

   /** Construct a simple editor using the given DisplayComponent */
   public SimpleEditor(DisplayComponent display)
   { this.display = display;
     display.addInteractionListener(this);
     buildActionMap();  //
     bindKeys();
   }

   /** Construct a simple editor from a ScrolledDisplay of the specified size;
       the bars are shown if showBars is true.
   */
   public SimpleEditor(int cols, int rows, boolean showBars)
   { this(new ScrolledDisplay(cols, rows, showBars)); }
   
   
   /** Construct a simple editor from a ScrolledDisplay of the specified size. */
   public SimpleEditor(int cols, int rows)
   { this(cols, rows, true); }

   /** Set the associated document. */
   public void setDoc(Document doc)
   { this.doc=doc;
     display.setDoc(doc);
   }

   public void setToolTipText(String tip)
   { display.setToolTipText(tip);
   }
   
   /** Add a focus eavesdropper to the underlying document */
   public void addFocusEavesdropper(FocusEavesdropper d) 
   {
     doc.addFocusEavesdropper(d);
   }
   
   /** Set the waiting state (from something long-running) [A no-op] */
   public void setWaiting(boolean state)
   { 
   }
   
   /** Ask that subsequent keyboard events get delivered here. */
   public void requestFocus() 
   { log.fine(""); 
     display.requestFocus(); 
   }

   /** Make this editor's display the active display.  */
   public void makeActive() 
   { log.fine(""); 
     display.makeActive(); 
   }
   
   /** Set the monospace features of this editor's display  */
   public void setMonoSpaced(boolean on, char pitchModel)
   { display.setMonoSpaced(on, pitchModel);
   }
   
   /** As if the monospace feature of this editor's display is set */
   public boolean isMonoSpaced()
   { return display.isMonoSpaced();
   }
   
   /** Get the display component (on which the document is being shown) */
   public JComponent getComponent()  
   { return display.getComponent(); }

   /** An Act associates a key description with the code that will be
       run when the key is pressed. When an Act is constructed 
       an entry is made in the actionMap that binds a (Java library)
       KeyStroke description to the Act itself. 
   */
 
  /** Keystroke named by pressed+key */
  public static KeyStroke keyPress(String key)
  {
    //  Force ``pressed'' interpretation
    key = key.replace("typed",    "")
             .replace("pressed",  "")
             .replace("released", "");
    KeyStroke k = KeyStroke.getKeyStroke(key);  
    if (k == null) throw new RuntimeException("Unknown key: " + key);
    return k;
  }
   
  /**
   * Bind the described keystroke to the named action.
   */
  public void bind(String key, String actionName)
  { 
    Action act = actions.get(actionName);
    if (act==null) throw new RuntimeException(String.format("Binding Key %s to unknown action: %s", key, actionName));
    bind(keyPress(key), act);
  }
  
  /** Set the number of lines of the display */
  public void setLines(int n)
  {
     display.setLines(n);  
  }
  
  /*
   * Get the named action
   */
  public Action getAction(String actionName) { return actions.get(actionName); }
  
  /**
   * Bind the described keystroke to the given action.
   */
  protected void bind(String key, Action action)
  { 
    bind(keyPress(key), action);
  }
  
  /** Bind the given KeyStroke to the given action */
  protected void bind(KeyStroke key, Action action) 
  { keyBindings.put(key, action); 
    if (action instanceof ActionMethod.Action) ((ActionMethod.Action) action).activatedBy(key);
  }  
  
  /** Maps a KeyStroke to the corresponding Action. */
  protected HashMap<KeyStroke, Action> keyBindings = new HashMap<KeyStroke, Action>();
   
  /** Maps names of actions to actions */
  protected ActionMethod.Map actions = new ActionMethod.Map();
  
  public String getBindingsText(String prefix, boolean local) 
  { return actions.getBindingsText(prefix, this, local); }
  
  public String getBindingsHTML(String prefix, boolean local) 
  { return actions.getBindingsHTML(prefix, this, local); }
  
  protected String hQuote(String s)
  { return s.replace("&", "&amp;").replace("<", "&lt;");
  }
  
  /** Return table describing current abbreviations */
  public String getAbbreviationsHTML() 
  { StringBuilder b = new StringBuilder();
    int n = 0;
    b.append("<table border=1><caption><b>Short Abbreviations</b></caption>\n<tr>");
    for (String abbr: abbrs.keySet())
    {  if (abbrs.get(abbr).length()>=5) continue;
       b.append(String.format("<td><tt>%s</tt></td><td><b>%s</b></td>\n", hQuote(abbr), abbrs.get(abbr)));
       n = (n+1) % 4;
       if (n==0) b.append("</tr>\n<tr>");
    }
    b.append("</tr></table>\n<br></br>\n");
    b.append("<table border=1><caption><b>Long Abbreviations</b></caption>\n<tr>");
    n = 0;
    for (String abbr: abbrs.keySet())
    {  if (abbrs.get(abbr).length()<5) continue;
       b.append(String.format("<td><tt>%s</tt></td><td><b>%s</b></td>\n", hQuote(abbr), abbrs.get(abbr)));
       n = (n+1) % 2;
       if (n==0) b.append("</tr>\n<tr>");
    }
    b.append("</tr></table>\n");
    return b.toString(); 
  }
   
  /** Bind action names to the corresponding actions */
  protected void buildActionMap()
  { actions.register(this); }
  
  public void doMarkPosition()
  { 
     doc.markPosition();
  }
  
  @ActionMethod(label="DeleteLineLeft", tip="Delete the line to the left of the cursor")
  public void doDeleteLineLeft()
  { doc.setMark(0, doc.getY()); doCut(); }
  
  @ActionMethod(label="InsertTab", tip="Simulate the insertion of a tab character\n(Tabs never appear in Dred documents)")
  public void doInsertTab()
  { typeOver(); doc.insert('\t'); }
  
  @ActionMethod(label="InsertNewline", tip="Insert a newline without copying current indentation")
  public void doInsertNewline()
  { typeOver(); doc.insert('\n'); }
  
  @ActionMethod(label="IndentNewline", tip="Insert a newline and copy current indentation")
  public void doIndentNewline()
  { typeOver(); doc.indentNewline(); }
  
  @ActionMethod(label="Left", tip="Move cursor to the previous location in the document")
  public void doLeftMove()
  { doc.leftMove(); doc.deliberateSelection(); }
  
  @ActionMethod(label="Right", tip="Move cursor to the next location in the document")
  public void doRightMove()
  { doc.rightMove(); deliberateSelection(); }
  
  @ActionMethod(label="Up", tip="Move cursor up (in the same column if possible)")
  public void doUpMove()
  { doMarkPosition(); doc.setCursor(doc.getX(), doc.getY()-1); deliberateSelection(); }
  
  @ActionMethod(label="Down", tip="Move cursor down (in the same column if possible)")
  public void doDownMove()
  { doMarkPosition(); doc.setCursor(doc.getX(), doc.getY()+1); deliberateSelection(); }
  
  @ActionMethod(label="Home", tip="Move cursor to the start of the document")
  public void doHomeMove()
  { doMarkPosition(); doc.setCursor(0, 0); }
  
  @ActionMethod(label="End", tip="Move cursor to the start of the last line of the document")
  public void doEndMove()
  { doMarkPosition(); doc.setCursor(0, doc.length()); }
  
  @ActionMethod(label="Swap", tip="Swap the cursor and the mark")
  public void doSwapCursorAndMark()
  { doMarkPosition(); doc.swapSelectionEnds(); }
  
  @ActionMethod(label="Delete", tip="Delete the character to the left of the cursor")
  public void doLeftDelete()
  { if (!typeOver()) doc.leftDel(); } 

  @ActionMethod(label="SelectWord", tip="Select the word under the cursor (also double-click)")
  public void doSelectWord()
  { doc.selectWord(doc.getX(), doc.getY()); deliberateSelection(); }  
  
  @ActionMethod(label="SelectLine", tip="Select the line under the cursor (also triple-click)")
  public void doSelectLine()
  { doc.selectLine(doc.getX(), doc.getY()); deliberateSelection(); }  
  
  @ActionMethod(label="SelectParagraph", tip="Select the paragraph(s) that contain the selection or cursor")
  public void doSelectParagraph()
  { doc.selectParagraph(); deliberateSelection(); }  

  /**
   * Returns the current selection after placing it on the
   * system clipboard and cutting it from the document.
   */ 
  public String clippedSelection()
  {
    String sel = doc.getSelection();
    SystemClipboard.set(sel);
    if (recordCuts) CutRingTool.addToRing(sel);
    doc.cutSelection();
    return sel;
  }
  
  /** Do we record cuts */
  protected boolean recordCuts = true;
  
  /** Stop recording cuts */
  public void stopRecordingCuts() { recordCuts = false; }
  
  
  /** Bind keystrokes to the corresponding actions */
  protected void bindKeys()
  { if (protoBindings==null || protoBindings.isEmpty())
    {  abbrs.clear();
       keys.clear();
       bind("ESCAPE",              "doAbbrev");
       bind("TAB",                 "doInsertTab");
       bind("ENTER",               "doIndentNewline");
       bind("control ENTER",       "doInsertNewline");
       bind("LEFT",                "doLeftMove");
       bind("RIGHT",               "doRightMove");
       bind("UP",                  "doUpMove");
       bind("DOWN",                "doDownMove");
       bind("HOME",                "doHomeMove");
       bind("END",                 "doEndMove");
       bind("ctrl S",              "doSwapCursorAndMark");
       bind("ctrl X",              "doCut");
       bind("ctrl V",              "doPaste");
       bind("ctrl C",              "doCopy");
       bind("ctrl shift V",        "doSwapSel");
       bind("DELETE",              "doLeftDelete");
       bind("BACK_SPACE",          "doLeftDelete");
       bind("ctrl DELETE",         "doSwap2");
       bind("ctrl BACK_SPACE",     "doSwap2");
       bind("control W",           "doSelectWord");
       bind("control L",           "doSelectLine");
       bind("control P",           "doSelectParagraph");
       bind("control U",           "doDeleteLineLeft");
    }
    else
    { for (Bindings.Binding binding: protoBindings) 
       try
       {
          if (binding.length()>3 && binding.matches("text", "action") )
             bind(binding.toKey(3), binding.getField(2));
          else
          if (binding.length()>3 && binding.matches("text", "key"))
             bind(binding.toKey(3), insertString(binding.getField(2)));
          else 
          if (binding.length()>4 && binding.matches("text", "abbrev"))
             bind(binding.toKey(4), insertString(binding.getField(3)));
       }
       catch (Exception ex)
       {
          log.warning("%s for binding %s (%d)", ex, binding, binding.length());
       }
    }
  }
  
  protected static Bindings protoBindings = null;
  
  public    static void  setBindings(Bindings thePrototype) 
  { protoBindings = thePrototype; 
    for (Bindings.Binding binding: protoBindings)
          if (binding.length()>3 && binding.matches("text", "abbrev"))
          { addAbbrev(binding.getField(2), binding.getField(3));
          }
  }
  
  
  /** Returns an action that inserts the given string */
  protected AbstractAction insertString(final String string)
  { 
    return new Act("Insert "+string, "Insert a string")
    {
      public void run() { typeOver(); doc.insert(string); }
    };
  }
   
  /** NO-OP */
  public void keyReleased(KeyEvent e) {}
   
  /**
   * If the key press that caused the given event is bound
   * to an Action, then the actionPerformed method of that
   * Action is invoked; otherwise if the key that was
   * pressed corresponds to a (Unicode) character, then
   * insert that character into the document.
   */
  public void keyPressed(KeyEvent e)
  {
    display.makeActive();
    KeyStroke k = KeyStroke.getKeyStrokeForEvent(e);
    Action act = keyBindings.get(k);
    if (debug)
      log.finest("%s (%s)", e, k);
    if (act == null)
    {
      char c = e.getKeyChar();
      if (c != KeyEvent.CHAR_UNDEFINED && c >= ' ')
      { typeOver(); doc.insert(c); }
      else
      {
        if (debug)
          log.fine("%s (%s)", e, k);
      }
    }
    else act.actionPerformed(dummyEvent);
  }
  
   /**
     * A dummy event that is passed to the actionPerformed
     * method of an Action that's invoked by a keystroke.
     */
   protected ActionEvent dummyEvent = new ActionEvent(this, 0, "KeyStroke");

   /**
     * Make the display active, set the cursor and/or the
     * mark to the position of the mouse, and perhaps select
     * a word (second click) or line (third click).
     */
  public void mousePressed(MouseEvent e)
  {
    Component c = e.getComponent();
    c.requestFocus();
    display.makeActive();
    int b = e.getButton();
    // int m = e.getModifiersEx();
    Point p = display.documentCoords(e);
    doc.markPosition();
    switch (b)
    {
      case 1:
        if (e.isControlDown())
        { 
          doc.setCursor(p.x, p.y);
          deliberateSelection();
        }
        else switch (e.getClickCount())
        {
          case 1:
            deliberateSelection();
            doc.setCursorAndMark(p.x, p.y);
          break;
          case 2:
            doc.selectWord(p.x, p.y);
            deliberateSelection();
          break;
          case 3:
            doc.selectLine(p.x, p.y);
            deliberateSelection();
          break;
        }
      break;
      case 3:
        if (e.isControlDown())
        { 
          doc.clearSelection();
        }
        else
        { doc.setMark(p.x, p.y);
          deliberateSelection();
        }
      break;
      case 2:
        lastX = e.getX();
        lastY = e.getY();
      default:
      break;
    }
  }
   
   int lastX, lastY;
      
  public void mouseDragged(MouseEvent e)
  {
    int b = 0;
    int m = e.getModifiersEx();
    // Stupid AWT makes us do the work here, but not in mousePressed!
    if ((m & MouseEvent.BUTTON1_DOWN_MASK) != 0)
      b = 1;
    else if ((m & MouseEvent.BUTTON2_DOWN_MASK) != 0)
      b = 2;
    else if ((m & MouseEvent.BUTTON3_DOWN_MASK) != 0)
      b = 3;
    Point p = display.documentCoords(e);
    switch (b)
    {
      case 1:
        doc.setCursor(p.x, p.y);
        deliberateSelection();
      break;
      case 3:
        doc.setMark(p.x, p.y);
        deliberateSelection();
      break;
      case 2:
        int dx = e.getX() - lastX,
        dy = e.getY() - lastY;
        display.dragBy(dx, dy);
        lastX = e.getX();
        lastY = e.getY();
      break;
      default:
    }
  }
   
  public void mouseEntered(MouseEvent e)   { if (debug) log.finer("Entered"); requestFocus();  }
  public void mouseExited(MouseEvent e)    { if (debug) log.finer("Exited"); }
  public void mouseReleased(MouseEvent e)  { }
  public void keyTyped(KeyEvent e)         { }
  public void mouseMoved(MouseEvent e)     { }
  public void mouseClicked(MouseEvent e)   { }


  @ActionMethod(label="Swap 2 characters", tip="Swap the two characters preceding the cursor")
  public void doSwap2()
  {
    if (doc.getX() < 2)
      return;
    doc.clearSelection();
    String s = doc.stringLeft();
    String t = s.substring(s.length() - 2, s.length());
    doc.replaceLeft(s.substring(0, s.length() - 2) + t.charAt(1) + t.charAt(0));
  }

  @ActionMethod(label="Cut", tip="Set the system clipboard from the document selection, and remove the document selection")
  public void doCut()
  { 
    if (doc.hasNonemptySelection())
    { doMarkPosition();
      if (debug)
        log.fine("cut");
      reversed = doc.getSelectedRegion().reversed;
      lastSel = clippedSelection();
    }
  }
   
  @ActionMethod(label="Paste", tip="Insert the system clipboard into the document at the cursor, and select it")
  public void doPaste()
  { doMarkPosition();
    String curSel = SystemClipboard.get();
    lastSel = curSel;
    typeOver();
    if (curSel != null)
       { doc.pasteAndSelect(curSel, reversed);
         deliberateSelection(); // SPECIAL CASE
       }
  }
   
  @ActionMethod(label="Copy", tip="Set the system clipboard from the document selection")
  public void doCopy()
  {
    if (doc.hasNonemptySelection())
    {
      reversed = doc.getSelectedRegion().reversed;
      lastSel = doc.getSelection();
      SystemClipboard.set(lastSel);
      if (recordCuts) CutRingTool.addToRing(lastSel);
    }
  }
  
  @ActionMethod(label="Swap Clipboard", tip="Swap the system clipboard with the document selection")
  public void doSwapSel()
  { doMarkPosition();
    String clip = SystemClipboard.get();
    if (clip == null)
      clip = "";
    if (doc.hasNonemptySelection())
    {
      reversed = doc.getSelectedRegion().reversed;
      lastSel = clippedSelection();
      doc.pasteAndSelect(clip, reversed);
    }
  }

  public void doInsertUnicode(String hexcode)
  { typeOver();
    try { doc.insert((char) Integer.parseInt(hexcode, 16)); } 
    catch (Exception ex) {}
  }

  public void doLoad(String fileName)
  {
    doc.doLoad(fileName);
  }
   
  public void doLoad(Reader stream)
  {
    doc.doLoad(stream);
  }

  public void doAppend(Reader stream)
  {
    doc.doAppend(stream);
  }

  public void removeDoc()
  {
    if (doc != null)
      display.removeDoc();
  }
  
  /** Abbreviation mapping */
  static protected Map<String, String> abbrs = new TreeMap<String, String>();
  
  /** Abbreviation keys in order of length */ 
  static protected Set<String> keys = new TreeSet<String>
  ( new Comparator<String>()
    {
      public int compare(String s1, String s2) 
      { int n = s1.length()-s2.length(); 
        return n==0 ? s1.compareTo(s2) : n;
      }
    }
  );
  
  /** Add an abbreviation */
  static public void addAbbrev(String name, String value) 
  { abbrs.put(name, value); 
    keys.add(name);
  }
  
  @ActionMethod(label="Abbrev", tip="Find the longest abbreviation that matches text at the left of the cursor and insert the corresponding insertion text. This is undone by doSwapSel.")
  public void doAbbrev()
  { String left   = doc.stringLeft();
    String result = null;
    
    // pick up the longest suffix (inefficient, but who cares)
    for (String key: keys) if (left.endsWith(key)) result = key;

    if (result!=null)
    { doc.setMark(doc.getX()-result.length(), doc.getY());
      clippedSelection();
      doc.pasteAndSelect(abbrs.get(result), true);
    }
  } 
         
  /** A simple editor that doesn't save the edited file */
  public static void main(String[] args) throws Exception
  {
    JFrame frame = new JFrame();
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    Document doc = new Document();
    SimpleEditor ed = new SimpleEditor(new Display(80, 20));
    if (args.length > 0)
    {
      BufferedReader b = new BufferedReader(new FileReader(args[0]));
      doc.doLoad(b);
      doc.setCursor(0, 0);
    }
    ed.setDoc(doc);
    frame.add(ed.getComponent());
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  public void setFont(String text)
  {
    display.setFont(text);
    
  }
}



































