package org.sufrin.dred;

import GUIBuilder.RowLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Semaphore;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.sufrin.logging.Logging;


/**
 * A top-level frame holding an editor a menu bar and a TextBar.
 */
public class EditorFrame extends JFrame implements FileDocument.Listener
{ 
  static Logging log   = Logging.getLog("EditorFrame");
  static boolean debug = log.isLoggable("FINE");

  EditorFrame session = this;
  
  protected static Preferences prefs = Preferences.userRoot().node("Dred");
  
  @ActionMethod
  (label="Save Prefs", 
   tip  ="Save the current state of all preferences now")
  public void doSavePrefs()
  { 
     try { prefs.sync(); } catch (Exception ex) { ex.printStackTrace(); }
  }
  
  protected static boolean syncPrefs = true;
  

  /** A local class that defines the menu bar. */
  protected class MenuBar extends JMenuBar
  {
    JMenu menu;
    
    /** Mapping from menu names to menus. */
    Map<String, JMenu> menus = new LinkedHashMap<String, JMenu>();
    
    /** Return a menu with a specific name; 
       make one of that name if it doesn't exist. */
    public JMenu addMenu(String name)
    { JMenu theMenu = menus.get(name);
      if (theMenu==null) 
      { theMenu = new JMenu(name, true); 
        add(theMenu); 
        validate();
        menus.put(name, theMenu);
      }
      return theMenu;
    }
    
    /** Put the named action on the current menu and
        add the name of the current menu to its documentation
        if appropriate.
    */
    public void bind(String actName) 
    { bind(actName, menu.getText());
    }
    
    /** Put the named action on the current menu and
        add the given menu name to its documentation if appropriate.
     */
    public void bind(String actName, String menuName) 
    {  Action act = actions.get(actName);   
      try { ((ActionMethod.Action) act).setMenu(menuName); } catch (Exception ex) {}  
       menu.add(act); 
    }
  
    public MenuBar() {}
    
    /** Construct and populate the standard menus */
    public void bindMenus()
    {
      menu = addMenu("File");     
      bind("doSave");
      bind("doEdit");
      bind("doSaveAs");
      bind("doCWD");
      bind("doNewView");
      menu.addSeparator();
      bind("doEditChoose");
      bind("doSaveAsChoose");
      bind("doChooseCWD");
      menu.addSeparator();
      bind("doQuit");
      menu.addSeparator();
      JMenu menu2 = new JMenu("Preferences");
      menu.add(menu2);
      menu=menu2;
      menu.add
      (new CheckItem
           ("Tooltips enabled", 
            true, 
            "Tooltips enabled on all components",
            prefs)
      { { run(); }
        public void run()
        {
          javax.swing.ToolTipManager.sharedInstance().setEnabled(state);
        }
      });
      menu.add
      (new CheckItem
           ("Save Prefs on Quit", 
            true, 
            "Save the current state of all preferences on quitting the session",
            prefs)
      { { run(); }
        public void run()
        {
          syncPrefs = state;
        }
      });
      menu.add
      (new CheckItem
           ("Secondary backups", 
            true, 
            "When saving, write secondary (~~) backup file if primary (~) backup exists.",
            prefs)
      { { run(); }
        public void run()
        {
          doc.secondaryBackups = state;
        }
      });
      bind("doSavePrefs", "File/Prefs");
      menu.addSeparator();
      bind("doLogger", "File/Prefs");

      menu = addMenu("Edit");
      bind("doReplaceAll");
      menu.addSeparator();
      bind("doFindDown");
      bind("doFindUp");
      menu.addSeparator();
      bind("doReplaceDown");
      bind("doReplaceUp");
      menu.addSeparator();
      menu.addSeparator();
      
      menu.add
      (new CheckItem("Literal Find", 
                     SearchableDocument.initLitFind, 
                     "Interpret Find text literally (alternative is as a regular expression)", 
                     prefs)
      { { run(); }
        public void run()
        {
          doc.litFind = state;
        }
      });
      menu.add(new CheckItem("Literal Replace",
                             SearchableDocument.initLitRepl,
                             "Interpret Repl text literally (alternative is as regular expression substitution)", 
                             prefs)
      { { run(); }
        public void run()
        {
          doc.litRepl = state;
        }
      });
      menu.addSeparator();
      menu.add(new CheckItem("( )   matching",
                             SearchableDocument.initMatchBra,
                             "Automatically select to matching opening/closing ()[]<>{} at appropriate times", 
                             prefs)
      { { run(); }
        public void run()
        {
          doc.matchBra = state;
        }
      });
      menu.add(new CheckItem("XML   matching",
                             SearchableDocument.initMatchXML,
                             "Automatically select to matching opening/closing XML brackets at appropriate times", 
                             prefs)
      { { run(); }
        public void run()
        {
          doc.matchXML = state;
        }
      });
      menu.add(new CheckItem("Latex matching",
                             SearchableDocument.initMatchLatex,
                             "Automatically select to matching opening/closing Latex begin/end environment brackets at appropriate times", 
                             prefs)
      { { run(); }
        public void run()
        {
          doc.matchLatex = state;
        }
      });

      menu = addMenu("Etc");
      bind("doShell");
      menu.add(new Act("fmt -75 -c  < sel'n", "Use an external formatter (fmt) to format the current selection")
      {
        public void run()
        {
          doShell("fmt -c");
        }
      });
      menu.add(new Act("fmt -60 -c  < sel'n")
      {
        public void run()
        {
          doShell("fmt -60 -c");
        }
      });
      menu.add(new Act("fmt -40 -c  < sel'n")
      {
        public void run()
        {
          doShell("fmt -40 -c");
        }
      });
      menu.addSeparator();
      bind("doUppercase");
      bind("doLowercase");
      menu.addSeparator();
      menu.add
      (new Act("Insert Unicode Range", 
               "Insert boilerplate abbreviation lines for the 128 characters with Unicode starting at hex code in ....")
      {
        public void run()
        {
          try
          { int low  = Integer.parseInt(text.argument.getText(), 16);
            int high = low+128;
            while (low<high) 
                  doc.insert(String.format("text abbrev XXX \\u%04X # %c\n", low, (char) low++));
            text.argument.setText(String.format("%04X", low));  
            doc.cursorChanged();        
          }
          catch (Exception ex) { Dred.showWarning(ex.toString()); }
        }
      });      

      toolMenu = menu = addMenu("Tools");
      menu.add(new CheckItem("Style Buttons", false)
      {
        public void run()
        {
          if (state && styleFrame == null)
          {
            styleFrame = new StyleFrame(EditorFrame.this);
          }
          if (styleFrame!=null) 
          {  styleFrame.setVisible(state);
             if (state)
             styleFrame.setExtendedState(EditorFrame.NORMAL);
             
             Rectangle b = session.getBounds();
             Insets i = session.getInsets();
             styleFrame.setLocation(b.x + b.width + i.left, b.y + i.top);
          }
        }
      });
      
      menu = addMenu("Help");
      bind("doHelp");
      if (Dred.sessionSocket != null) bind("doMozilla");
      menu.addSeparator();
      bind("doCurrentBindings");
      bind("doCommands");
      menu.addSeparator();
      bind("doAbout");
      
      add(Box.createHorizontalGlue());

      // Eliminate input maps (pro-tem) to avoid spurious shortcut effects
      setInputMap(JComponent.WHEN_FOCUSED, null);
      setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, null);
      setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, null);
      
      refresh();
    }
     
    /**
        Ensure that the layout of the session is up to date. This is
        used after the menubar layout has changed.
    */
    public void refresh()
    {
      invalidate();
      session.getRootPane().validate();
      repaint();
    }
  }
  
  /* Load all the standard extensions */
  static
  { 
    Extension.register(new LatexTool(prefs));
    Extension.register(new AntTool());
    Extension.register(new MakeTool());
    Extension.register(new ShellTool());
    Extension.register(new KeystrokeTool());
    File extensions = null;
    try
    {  
       extensions = new File(new File(System.getProperty("user.home"), ".dred"), "ext.jar");
       if (extensions.exists() && extensions.canRead())
          Extension.addRoot(extensions.toString());
    }
    catch (Exception ex) {}
  }

  
  /**
   * An editor that consumes output generated by processes
   * that were started in this Editor.
   */
  protected class ProcessFrame extends EditorFrame
  {
    JButton clearButton = new JButton(new Act("Clear", "Clear the log window")
    {
      public void run()
      {
        doc.deleteAll();
      }
    });

    String command;

    public ProcessFrame(String command)
    {
      super(80, 24, command);
      setDoc(new FileDocument());
      menuBar.add(clearButton);
      this.command = command;
    }

    /** Quit without the usual song and dance over saving */
    @ActionMethod(label="Quit", tip="Quit this process-monitoring window without the usual ``do you want to save'' dialogue")
    public void doQuit()
    {
      EditorFrame.this.closeProcessFrame(); // Subtle: it's the parent close we want
    }

    /**
     * NO-OP: we don't need position information in a
     * Process Frame.
     */
    public void enableDocFeedback()
    {
    }

    public void setCommand(String command)
    {
      setTitle(command);
      this.command = command;
    }
    
    public void setEnabled(boolean enabled)
    {
      clearButton.setEnabled(enabled);
    }

    /** NO-OP: we want the title to remain constant */
    public void setTitle(String title)
    {
    }
      
  }

  static ImageIcon dnought = new ImageIcon(Dred.class.getResource("dnought.jpg"));

  /**
   * Count of the number of frames/sessions that have been
   * constructed.
   */
  static int frames = 0;

  /** Return the common prefix of a and b */
  protected static String commonPrefix(String a, String b)
  {
    int m = Math.min(a.length(), b.length());
    for (int i = 0; i < m; i++)
      if (a.charAt(i) != b.charAt(i))
        return a.substring(0, i);
    return a.substring(0, m);
  }

  /** The application bars */
  JPanel bars = new JPanel();

  protected String caption = "";

  /** The working directory for this session */
  File cwd = new File(".");

  /** The document being edited/ */
  FileDocument doc;
  
  /** Get the document being edited/ */
  public FileDocument getDoc() { return doc; }
  
  /** Add a focus eavesdropper to the underlying document */
  public void addFocusEavesdropper(FocusEavesdropper d) 
  {
    doc.addFocusEavesdropper(d);
    text.addFocusEavesdropper(d);
  }

  /** The main editor. */
  SimpleEditor ed;
  
  /** Get the main editor. */
  public SimpleEditor getEditor() { return ed; } 

  /**
   * The panel at the foot of the editor which contains
   * the feedback labels
   */
  JPanel feedback;

  /**
   * The labels at the foot of the editor which show
   * feedback.
   */
  JLabel labelC, labelL, labelR;
  
  /** The file chooser */
  JFileChooser fileChooser = new JFileChooser();
    
  /** The MenuBar */
  MenuBar menuBar = new MenuBar();
  
  /** The Tool Menu */
  JMenu toolMenu;
  
  // ////////////////////////////////////////////////////////////////////////////////////////////
  
  /** Add a component to the Menubar: used by extensions  */
  public void addToToolBar(JComponent c)
  { session.validate(); toolbars.add(c); toolbars.invalidate(); session.validate(); menuBar.refresh(); }

  /** Remove a component from the Menubar: used by extensions  */
  public void removeFromToolBar(JComponent c)
  { session.validate(); toolbars.remove(c); toolbars.invalidate(); session.validate(); menuBar.refresh(); }

  /** Add a component to the tool Menu: used by extensions  */
  public void addTool(JComponent c)
  { toolMenu.add(c); }

  /** Remove a component from the tool Menu: used by extensions  */
  public void removeTool(JComponent c)
  { toolMenu.remove(c); }
  
  /** Return a menu with a specific name; make one of that name if it doesn't exist. */
  public JMenu addMenu(String name)
  { return menuBar.addMenu(name); 
  }

  // ////////////////////////////////////////////////////////////////////////////////////////////
  
  /** Alternative system for binding actions to keys */
  public abstract class ActNamed extends AbstractAction
  { public ActNamed(String actName)
    { super();
      actions.put(actName, this);
    }
    public ActNamed(String label, String actName)
    { super(label);
      actions.put(actName, this);
    }
    public          void actionPerformed(ActionEvent ev) { run(); }
    public abstract void run();
  }
  
  public ActionMethod.Map actions = new ActionMethod.Map();
  
  protected void buildActionMap()
  { 
    actions.register(this);
  }
  
  @ActionMethod(label="Undent", tip="Remove a leading space from each line of the selection")
  public void doUndent() { doShiftLeft(" "); }
  @ActionMethod(label="Indent", tip="Add a leading space to each line of the selection")
  public void doIndent() { doShiftRight(" "); }
  @ActionMethod(label="Undent", tip="Remove the text in .... from the start of each line of the selection")
  public void doUnPrefix() { doShiftLeft(text.argument.getText()); }
  @ActionMethod(label="Indent", tip="Add the text in .... to the start of each line of the selection")
  public void doPrefix() { doShiftRight(text.argument.getText()); }
  
  @ActionMethod(label="Next bracket", tip="Find next (balanced) bracket that matches the opening bracket at the cursor")
  public void doMatchDown() { doc.matchDown(); }
  
  @ActionMethod(label="Prev bracket", tip="Find previous (balanced) bracket that matches the opening bracket at the cursor")
  public void doMatchUp() { doc.matchUp(); }
  
  @ActionMethod(label="Unicode Character", tip="Insert Unicode character whose (hex) code is in ....")
  public void doUnicode()
  { ed.doInsertUnicode(text.argument.getText()); 
    edFocus(); 
  } 
    
  protected static Bindings protoBindings = null;
  public    static void  setBindings(Bindings thePrototype) { protoBindings = thePrototype; }
  
  protected void bindKeys()
  { if (protoBindings==null || protoBindings.isEmpty())
    {
      bindAll("ctrl INSERT",            "doClearArgument");
      bindAll("ctrl alt INSERT",        "doClearFind");
      bindAll("ctrl alt shift INSERT",  "doClearRepl");
      bindAll("control E",              "doEdit");
      bindAll("TAB",                    "doComplete", false); // Don't override TAB in main
      bindAll("ctrl F",                 "doFindDown");
      bindAll("ctrl shift F",           "doFindUp");
      bindAll("ctrl alt F",             "doFindSelDown");
      bindAll("ctrl alt shift F",       "doFindSelUp");
      bindAll("control G",              "doGoToXY");
      bindAll("control K",              "doKillProcess");
      bindAll("control Q",              "doQuit");
      bindAll("ctrl R",                 "doReplaceDown");
      bindAll("ctrl shift R",           "doReplaceUp");
      bindAll("control S",              "doSave");     
      bindAll("control alt LEFT",       "doUnPrefix");
      bindAll("control LEFT",           "doUndent");
      bindAll("control alt RIGHT",      "doPrefix");
      bindAll("control RIGHT",          "doIndent");
      bindAll("alt U",                  "doUnicode");
      bindAll("control OPEN_BRACKET",   "doMatchDown");
      bindAll("control CLOSE_BRACKET",  "doMatchUp");
    }
    else
    { for (Bindings.Binding binding: protoBindings) 
          if (binding.length()>=3 && binding.matches("Editor", "action"))
             bindAll(binding.toKey(3), binding.getField(2));
          else
          if (binding.length()>=4 && binding.matches("Editor", "minitext", "action"))  
             bindAll(binding.toKey(4), binding.getField(3), false);    
    }
  }

  /** Currently-running operating-system process, if any. */
  protected Process process = null;

  // /////////////////////////////////////////////////////////////////////

  // //////////// Generic external process stuff

  /** The editor that consumes process output. */
  protected ProcessFrame processFrame = null;

  // //////////////////////////////////////////////////////////////////////////////////////////

  /** File has changed since it was saved or loaded */
  protected boolean safe = false;

  JFrame styleFrame = null;

  /** The TextBar */
  TextBar text = new TextBar();

  /** The application toolbars */
  JPanel toolbars = new JPanel();

  // /////////////////////////////////////////////////////////////////////

  // ///////////// Latex

  protected boolean usepdf = false, smallscale = true;

  protected Process viewer = null;

  protected Semaphore wait = new Semaphore(0);

  public EditorFrame(int cols, int rows)
  {
    this(cols, rows, String.format("Dred%d", frames));
  }
  
  /** Bind the key <i>key</i> to the named action in all the
      text-parameter areas and (if bindEd is true) the main editor window. */
  public void bindAll(String key, String actionName, boolean bindEd)
  {
    Action act = actions.get(actionName);
    if (act==null) throw new RuntimeException(String.format("Binding Key %s to unknown action: %s", key, actionName));
    text.bind(key, act);
    if (bindEd) ed.bind(key, act);
  }
  
  /** Bind the key <i>key</i> to the named action in all the
      text-parameter areas and the main editor window. */
  public void bindAll(String key, String actionName)
  {
     bindAll(key, actionName, true);
  }
  
  /** Bind the key <i>key</i> to the given action in all the
      text-parameter areas and (if bindEd is true) the main editor window. 
  */  
  public void bindAll(String key, Action act, boolean bindEd)
  {
     text.bind(key, act);
     if (bindEd) ed.bind(key, act);
  }
  
  /** Bind the key <i>key</i> to the given action in all the
      text-parameter areas and the main editor window. 
  */  
  public void bindAll(String key, Action act)
  {
     bindAll(key, act, true);
  }
  

  public EditorFrame(int cols, int rows, String title)
  {
    super(title);
    setIconImage(dnought.getImage());
    frames++;
    setLayout(new BorderLayout());
    ed = new SimpleEditor(cols, rows);

    feedback = new JPanel();
    feedback.setLayout(new BoxLayout(feedback, BoxLayout.LINE_AXIS));
    feedback.setBorder(BorderFactory.createEtchedBorder());
    labelC = new JLabel("", JLabel.LEFT);
    labelL = new JLabel("", JLabel.LEFT);
    labelR = new JLabel("", JLabel.RIGHT);
    feedback.add(labelL);
    feedback.add(Box.createHorizontalGlue());
    feedback.add(labelC);
    feedback.add(Box.createHorizontalGlue());
    feedback.add(labelR);

    bars.setLayout(new BoxLayout(bars, BoxLayout.Y_AXIS));
    toolbars.setLayout(new RowLayout(-1, true)); // (new BoxLayout(toolbars, BoxLayout.X_AXIS));
    bars.add(toolbars);
    bars.add(text);

    getContentPane().add(bars, "North");
    getContentPane().add(ed.getComponent(), "Center");
    getContentPane().add(feedback, "South");

    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter()
    {
      public void windowClosing(WindowEvent e)
      {
        doQuit();
      }
    });
    
    log.finer("Building action map");
    buildActionMap();
    log.finer("Binding keys");
    bindKeys();
    
    // Make visible
    pack();
    setLocation(10 + 5 * frames, 30 + 5 * frames);
    setVisible(true);
  }

  public void await()
  {
    try
    {
      wait.acquire();
    }
    catch (Exception ex)
    {
    }
  }

  protected File canonical(File path)
  {
    try
    {
      return path.getCanonicalFile();
    }
    catch (IOException ex)
    {
      return path.getAbsoluteFile();
    }
  }

  // ////////////////////////////////////////////////////////////////////////////////////////////

  // /// Selection processing

  /**
   * Returns the current selection after placing it on the
   * system clipboard and cutting it from the document.
   */ 
  public String clippedSelection()
  {
    return ed.clippedSelection();
  }

  protected void closeProcessFrame()
  {
    processFrame.dispose();
    processFrame = null;
  }

  public String completeFilename(String name)
  {
    name = desugarFilename(name);
    File path = new File(name);
    File cdir = path.getParentFile() == null
                ? doc.getFileName()
                     .getParentFile()
                : path.isDirectory()
                                    ? path
                                    : path.getParentFile();
    String[] files = cdir.list();
    String prefix = path.isDirectory() ? "" : path.getName();
    LinkedList<String> candidates = new LinkedList<String>();
    if (files == null)
      return "";
    for (String file : files)
      if (file.startsWith(prefix))
        candidates.add(file);
    if (candidates.isEmpty())
      return "";
    String candidate = candidates.removeFirst();
    while (!candidates.isEmpty())
      candidate = commonPrefix(candidate, candidates.removeFirst());
    File it = new File(cdir, candidate);
    return it.toString() + (it.isDirectory() ? File.separator : "");
  }

  /**
   * Desugar the text of a filename.
   * 
   * <pre>
   * 
   *  
   *   
   *        Replace &tilde;/    with the user's home directory, and
   *        ./    with the current directory of the current editing session
   *        ../   with the parent directory of the current editing session
   *        Prefix an ``orphaned'' filename with the parent directory of the current document
   *        
   *   
   *  
   * </pre>
   */
  protected String desugarFilename(String name)
  {
    // eliminate user name abbreviation
    if (name.startsWith("~" + File.separator))
      name = System.getProperty("user.home") + name.substring(1);
    else
    // transform relative addresses to ``relative to cwd''
    if (name.startsWith(".." + File.separator)
        || name.startsWith("." + File.separator))
      name = cwd.toString() + File.separator + name;
    File file = new File(name);
    // orphan filename has same parent as current document
    if (file.getParentFile() == null)
      file = new File(doc.getFileName().getParentFile(), name);
    return canonical(file).toString();
  }

  /**
   * Dispose of the frame and decouple the underlying
   * document from it.
   */
  public void dispose()
  {
    super.dispose();
    // Decouple the associated document
    ed.removeDoc();
    // Remove associated windows
    if (styleFrame != null)
      styleFrame.dispose();
    Dred.removeSession(this);
    if (viewer != null)
      viewer.destroy();
    if (process != null)
      process.destroy();
    wait.release();
  }
  
  /** Show version details */
  @ActionMethod(label="About", tip="Show version details of the currently-running Dred")
  public void doAbout()
  {
    String message = "<html>Dred $Revision$<br>"
                     + "(C) 2004, 2005 Bernard Sufrin<br>"
                     + "Bernard.Sufrin@sufrin.org.uk<br>"
                     + "<br></br>"
                     + (Dred.sessionSocket == null
                       ? ""
                       : ("Serving on port: " + Dred.sessionSocket.getPort()))
                     + "<br></br>"
                     + (Dred.loggingSocket == null
                       ? ""
                       : ("Logging on port: " + Dred.loggingSocket.getPort()))
                     + "</html>";
    JOptionPane.showMessageDialog(this, message, "About Dred", JOptionPane.PLAIN_MESSAGE, dnought);
  }
  
  /**
   * Append the document from the given reader. Lines are
   * inserted when the GUI thread is idling -- this is
   * intended to avoid races between the painting method
   * and the document insertion method(s).
   */
  /*
   * Remark: speed is perfectly adequate for our needs,
   * but we could speed up even more if we scrolled the
   * page image up when the Display pans downwards, rather
   * than regenerating all the lines.
   */
  public void doAppend(BufferedReader reader)
  {
    String line = null;
    try
    {
      while ((line = reader.readLine()) != null)
      {
        final String theLine = line;
        SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {
            doc.setCursorXY(0, doc.length());
            doc.insert(theLine);
            doc.insert("\n");
          }
        });
      }
    }
    catch (Exception ex)
    {
      throw new RuntimeException(ex);
    }
  }

  @ActionMethod(label="cd (browse)", tip="Browse for a directory then change directory to it")
  public void doChooseCWD()
  {
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fileChooser.setCurrentDirectory(cwd); // doc.getFileName().getParentFile());
    int res = fileChooser.showOpenDialog(this);
    if (res == JFileChooser.APPROVE_OPTION)
    {
      File newcwd = fileChooser.getSelectedFile();
      if (newcwd.isDirectory())
      {
        cwd = newcwd;
        tempCaption(String.format("Directory: %s", newcwd.getAbsolutePath()));
      }
      else tempCaption(String.format("%s is not a directory.", newcwd.getAbsolutePath()));
    }
  }

  /** Clear and activate the Argument text. */
  @ActionMethod(label="clear ....", tip="Clear the .... field and send focus there")
  public void doClearArgument()
  {
    text.argument.setText("");
    text.argument.activate();
  }

  // ///////////////////////////////////////////////////////////////////

  /** Clear and activate the Find text. */
  @ActionMethod(label="clear find", tip="Clear the find field and send focus there")
  public void doClearFind()
  {
    text.find.setText("");
    text.find.activate();
  }

  /** Clear and activate the Repl text. */
  @ActionMethod(label="clear repl", tip="Clear the repl field and send focus there")
  public void doClearRepl()
  {
    text.repl.setText("");
    text.repl.activate();
  }

  /**
   * Try to complete the filename in the ... window. Edit
   * the file if the filename is completed.
   */
  @ActionMethod(label="complete ....", tip="Try to complete the filename in the .... field")
  public void doComplete()
  {
    String name = text.argument.getText();
    if ("".equals(name))
    {
      doEditChoose();
      return;
    }
    String candidate = completeFilename(name);
    if (!candidate.equals(""))
    {
      text.argument.setText(candidate);
    }
  }

  @ActionMethod(label="cd ....", tip="Change the directory of this session to the directory named in the ... field")
  public void doCWD()
  {
    File newcwd = new File(desugarFilename(text.argument.getText()));
    if (newcwd.isDirectory())
    {
      cwd = canonical(newcwd);
      tempCaption(String.format("Directory: %s", cwd));
    }
    else tempCaption(String.format("%s is not a directory.", cwd));
  }

  /**
   * Start editing the document in the filestore at the
   * path specified by the argument (....) text. If this
   * text doesn't specify a folder, then the folder is
   * taken to be that of the file currently being edited.
   */
  @ActionMethod(label="Edit ....", tip="Edit the file named in the ... field")
  public void doEdit()
  { 
    String name = text.argument.getText();
    name = desugarFilename(name);
    if ("".equals(name))
    {
      doEditChoose();
      return;
    }
    File file = new File(name);
    Dred.startLocalSession(file.getAbsolutePath());
  }

  /**
   * Interactively choose the path to a file to edit in a
   * new session.
   */
  @ActionMethod(label="Edit (browse)", tip="Browse for a file and start editing it")
  public void doEditChoose()
  {
    fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fileChooser.setCurrentDirectory(cwd); // doc.getFileName().getParentFile());
    fileChooser.setFileHidingEnabled(false);
    int res = fileChooser.showOpenDialog(this);
    if (res == JFileChooser.APPROVE_OPTION)
      Dred.startLocalSession(fileChooser.getSelectedFile().getAbsolutePath());
  }

  @ActionMethod(label="Find next", tip="Find the next instance of the pattern in the Find field")
  public void doFindDown() { doFind(false); }
  
  @ActionMethod(label="Find previous", tip="Find the previous instance of the pattern in the Find field")
  public void doFindUp() { doFind(true); }
  
  @ActionMethod(label="Replace (down)", tip="Replace the current instance of the find pattern with the replacement text")
  public void doReplaceDown() { doReplace(false); }
  
  @ActionMethod(label="Replace (up)", tip="Replace the current instance of the find pattern with the replacement text")
  public void doReplaceUp() { doReplace(true); }
  
  
  @ActionMethod(label="Find next sel", tip="Set the find field to the selection, then find its next instance")
  public void doFindSelDown() { doFindSel(false); }
  
  @ActionMethod(label="Find previous sel", tip="Set the find field to the selection, then find its previous instance")
  public void doFindSelUp() { doFindSel(true); }
  
  /**
   * Find and select the next instance of the pattern
   * specified by the Find text (and the literal find
   * switches).
   */
  public void doFind(boolean upwards)
  {
    if (!doc.setPattern(text.find.getText()))
    {
      tempCaption(doc.regexError());
      return;
    }
    edFocus();
    boolean ok = upwards ? doc.upFind() : doc.downFind();
    if (!ok)
      tempCaption("Pattern not found");
  }

  /**
   * Find and select the next instance of the (literal)
   * text specified by the current selection.
   */
  public void doFindSel(boolean upwards)
  {
    if (!hasNonemptySelection())
      return;
    text.find.setText(doc.getSelection());
    Pattern pat = Pattern.compile(Pattern.quote(doc.getSelection()));
    edFocus();
    boolean ok = upwards ? doc.upFind(pat) : doc.downFind(pat);
    if (!ok)
      tempCaption("Pattern not found");
  }

  /**
   * Format the current selection 
   */
  public void doFormatSelection(String open, String close, boolean block)
  { Document.Region region = doc.selectedRegion;
    String          ins    = hasNonemptySelection() ? doc.getSelection() : "";
    // Calculations must be done before the selection is clipped!
    if (debug) log.fine("%s: %s", region, ins);
    if (block)
    { if (!ins.startsWith("\n")) open  +='\n';
      if (!ins.endsWith("\n"))   close ='\n'+close;
      if (region.startx!=0)     open  = "\n"+open;
      
      CharSequence line = doc.lineAt(region.endy);
      if (region.endx!=line.length()) close +='\n';
    }
    if (debug) log.fine("%s", open+ins+close);
    if (hasNonemptySelection()) clippedSelection();
    doc.pasteAndSelect(open+ins+close, true);
  }

  // ////////////////////////////////////////////////////////////////////

  /** Go to the row.col specified by the (....) text. */
  @ActionMethod(label="Goto", tip="Go to the row.col specified by the .... field")
  public void doGoToXY()
  {
    String arg = text.argument.getText();
    Scanner s = new Scanner(arg).useDelimiter("\\s*\\.\\s*");
    try
    {
      int y = s.nextInt() - 1; // World does 1-origin
      // addressing
      int x = s.hasNextInt() ? s.nextInt() : 0;
      edFocus();
      doc.setCursorAndMark(x, y, x, y);
    }
    catch (Exception ex)
    {
      tempCaption("Not a location: " + ex);
    }
  }
  
  /** Insert the skeleton of a bindings file  */
  @ActionMethod(label="Insert Bindings", tip="Insert (and select) the skeleton of a bindings file (showing current bindings) into the current document.")
  public void doCommands()
  {
    if (doc.hasSelection())
    {
      SystemClipboard.set(doc.getSelection());
      doc.cutSelection();
    }
    doc.pasteAndSelect(getBindingsText(), false);    
  }

  @ActionMethod(label="Browse Help", tip="Browse Dred help using Dred's built-in browser")
  public void doHelp()
  {     
    WebBrowser b = showBrowser("Dred Help");
    b.showDocument(Dred.class.getResource("index.html"));
  }
  
  @ActionMethod(label="Show bindings", tip="Show the current bindings and abbreviations in a window")
  public void doCurrentBindings()
  { String bindings = getBindingsHTML();
    showBrowser("Current Dred Bindings", bindings);
  }

  @ActionMethod(label="Browse Help with Mozilla", tip="Browse the help text (from Dred's built-in server) using mozilla.")
  public void doMozilla()
  {
        { try
           {  Runtime.getRuntime().exec(String.format("mozilla http://localhost:%d/index.html", Dred.sessionSocket.getPort()));
           }
           catch (IOException ex)
           { ex.printStackTrace();
           }
         }  
  }
  
  /** Returns HTML text describing the current bindings and abbreviations
   */
  protected String getBindingsHTML()
  { StringBuilder b = new StringBuilder();
    if (protoBindings==null || protoBindings.isEmpty())
       b.append("<center><b>Fallback bindings</b></center><br></br>");
    else
    {
       b.append("<center><b>Bindings from:<br></br>");
       for (URL url: protoBindings.getURLs())
           b.append(String.format("%s<br></br>%n", url));
       b.append("</b></center><br></br>");
    }
    return "<html><body>"
         + b.toString()
         + "<br></br><center>"
         + ed.getBindingsHTML("<b>Text Editing Actions (all texts)</b>", false)
         + "</center><br></br><center>"
         + ed.getAbbreviationsHTML()
         + "</center><br></br><center>"
         + actions.getBindingsHTML("<b>Main Window Actions (document and minitexts)</b>", this, false)
         + "</center><br></br><center>"
         + text.argument.getBindingsHTML("<b>Minitext-specific Actions</b>", true)
         + "</center></body></html>"
         ;
  }
  
  protected String getBindingsText()
  {
    return actions.getBindingsText("editor action", this, false)
         + ed.getBindingsText("text action", false)
         + text.argument.getBindingsText("minitext action", true)
    ;
  }
  
  /** Build and show a (possibly-empty) browser window */
  protected WebBrowser showBrowser(String title, String ... html)
  {
    final JFrame f = new JFrame(title);
    JMenuBar menu = new JMenuBar();
    f.setJMenuBar(menu);
    JButton quit = new JButton
    (new Act("OK", "Quit this window")
     {
       public void run() { f.dispose(); }
     }
    );
    menu.add(quit);    
    WebBrowser b = new WebBrowser(); 
    for (String h:html) b.showHtmlText(h);
    f.add(b.getScrolledComponent());
    f.pack();
    f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    f.setSize(800, 600);
    f.setLocationRelativeTo(null);
    f.setVisible(true);
    return b;
  }
  
  /** Kill the currently-running OS-process, if any. */
  @ActionMethod(label="Kill", tip="Kill any currently-running background process")
  public void doKillProcess()
  {
    if (process != null)
    {
      process.destroy();
      process = null;
    }
  }

  /**
   * Load the document from the given reader.
   */
  public void doLoad(BufferedReader reader)
  {
    ed.doLoad(reader);
  }

  /**
   * Load the file with the given path into the document
   * associated with the editor.
   */
  public void doLoad(String path)
  {
    ed.doLoad(path);
  }
    
  /**
   * Swap the current selection with its lowercased
   * translation.
   */
  @ActionMethod(label="Lowercase", tip="Swap the current selection with its lowercased translation")
  public void doLowercase()
  {
    if (hasNonemptySelection())
    {
      doc.pasteAndSelect(clippedSelection().toLowerCase(), true);
    }
  }

  /**
   * Swap the current selection with its uppercased
   * translation.
   */
  @ActionMethod(label="Uppercase", tip="Swap the current selection with its uppercased translation")
  public void doUppercase()
  {
    if (hasNonemptySelection())
    {
      doc.pasteAndSelect(clippedSelection().toUpperCase(), true);
    }
  }

  /**
   * Make a new Frame that is a view of the document being
   * edited in this frame.
   */
  @ActionMethod(label="New View", tip="Open another view on the document being edited")
  public void doNewView()
  {
    EditorFrame f = new EditorFrame(80, 24, String.format("%s-%d", doc.getFileName()
                                                          .getName(),
                                              doc.countListeners()));
    f.setDoc(doc);
    f.repaint();
  }
  
  /** Start a network logger interface */
  @ActionMethod(label="Start Log", tip="Start the HTTP logging interface at the port number specified in ....")
  public void doLogger()
  {
      Dred.startLogger(text.argument.getText()); 
  }
  

  // /////////////////////////////////////////////////////////////////

  /**
   * Quit editing the current document, giving the user
   * the chance to save it if it has been changed since it
   * was last saved.
   */
  @ActionMethod(label="Quit", tip="Quit the current file-editing window. Offers a dialogue if the document being edited has channged since it was last saved.")
  public void doQuit()
  { 
    if (doc.hasChanged())
    {
      String msg = String.format("%s has been changed", doc.getFileName());
      Object[] options =
      {
              "Keep editing", "Save it and quit", "Just Quit"
      };
      int option = Dred.showWarning(this, msg, 0, options);
      if (option == 1)
        doSave();
      if (option >= 1)
        dispose();
    }
    else dispose();
    Extension.sessionQuit(this);
    if (syncPrefs) doSavePrefs();
  }

  /**
   * If the current selection is an instance of the
   * pattern specified by the Find text (and the literal
   * find switches), then replace it with the Repl text.
   */
  public void doReplace(boolean upwards)
  {
    doc.setReplacement(text.repl.getText());
    edFocus();
    String lastSel = doc.replace(upwards);
    if (lastSel == null)
      tempCaption(doc.regexError());
    else SystemClipboard.set(lastSel);
  }

  /**
   * Replace all instances of the Find pattern within the
   * selection with the Repl text.
   */
  @ActionMethod(label="Replace all", tip="Replace all instances of the Find pattern within the selection with the Repl text.")
  public void doReplaceAll()
  {
    if (!doc.setPattern(text.find.getText()))
    {
      tempCaption(doc.regexError());
      return;
    }
    doc.setReplacement(text.repl.getText());
    edFocus();
    String lastSel = doc.replaceAll();
    if (lastSel == null)
      tempCaption(doc.regexError());
    else SystemClipboard.set(lastSel);
  }

  /**
   * Save the current document if it has been changed in
   * this session since it was last loaded or saved. Give
   * the user the chance to cancel the operation if the
   * filestore copy of the document has been changed since
   * it was loaded or saved in this session.
   */
  @ActionMethod(label="Save", tip="Save the document being edited if it has changed since it was last saved")
  public void doSave()
  { Extension.sessionSaved(this);
    if (doc.isAnonymous())
    {
      doSaveAsChoose();
    }
    else if (doc.hasChanged())
    {
      boolean save = true;
      if (doc.lastModified() != 0 && doc.getFileName().exists()
          && doc.lastModified() < doc.getFileName().lastModified())
      {
        String msg = String
                           .format(
                                   "%s %s\nLoaded here: %Tc\nChanged elsewhere: %Tc",
                                   doc.getFileName(),
                                   save ? " has been changed here."
                                       : " has not been changed here.",
                                   doc.lastModified(), doc.getFileName()
                                                          .lastModified());
        Object[] options =
        {
                  "Save it anyway", "Cancel"
        };
        save = 0 == Dred.showWarning(this, msg, 1, options);
      }
      if (save)
        doc.doSave();
    }
    else tempCaption("No need to save document");
  }

  /**
   * Save the document in the filestore at the path
   * specified by the argument text. Give the user the
   * chance to cancel the operation if the filestore copy
   * of the document has been changed since it was loaded
   * or saved in this session.
   */
  @ActionMethod(label="Save As ....", tip="Save the document being edited in the file named by the .... field")
  public void doSaveAs()
  {
    String name = desugarFilename(text.argument.getText());
    File file = new File(name).getAbsoluteFile();
    doSaveItAs(file);
  }

  /**
   * Interactively choose the path to a file in which to
   * save the current document.
   */
  @ActionMethod(label="Save As (browse)", tip="Browse for a filename in which to save the document being edited")
  public void doSaveAsChoose()
  {
    fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fileChooser.setCurrentDirectory(cwd); // doc.getFileName().getParentFile());
    fileChooser.setFileHidingEnabled(true);
    int res = fileChooser.showSaveDialog(this);
    if (res == JFileChooser.APPROVE_OPTION)
      doSaveItAs(fileChooser.getSelectedFile().getAbsoluteFile());
  }

  /**
   * Save the document in the filestore at the path
   * specified by the given File. Give the user the chance
   * to cancel the operation if the filestore copy of the
   * document has been changed since it was loaded or
   * saved in this session.
   */
  public void doSaveItAs(File file)
  {
    boolean save = true;
    if (file.exists())
    {
      String msg = String.format("%s exists.\nChanged at %Tc", file,
                                 file.lastModified());
      Object[] options =
      {
              "Save it anyway", "Cancel"
      };
      save = 0 == Dred.showWarning(this, msg, 1, options);
    }
    if (save)
      doc.doSaveAs(file);
  }

  /**
   * Pipe the current selection (if any) through the
   * program specified in the textline labelled (....).
   */
  @ActionMethod(label="sh .... < sel'n", tip="Pipe the current selection through the shell command given in the .... field")
  public void doShell()
  {
    doShell(text.argument.getText());
  }

  /**
   * Pipe the current selection (if any) through the
   * program specified by the parameter.
   */
  public void doShell(final String program)
  {
    doKillProcess();
    final String sel = doc.hasSelection() ? doc.getSelection() : "";
    final boolean reversed = doc.getSelectedRegion().reversed;
    Pipe.Continue cont = new Pipe.Continue()
    {
      public void consumeOutput(BufferedReader reader)
      {
      }

      public void fail(Exception ex)
      {
        ex.printStackTrace();
        process = null;
      }

      public void result(int exitCode, String output)
      {
        if (doc.hasSelection())
        {
          SystemClipboard.set(doc.getSelection());
          doc.cutSelection();
        }
        doc.pasteAndSelect(output, reversed);
        tempCaption(String.format("[%o] ....", exitCode));
        process = null;
      }
    };
    process = Pipe.execute(cwd, program, sel, cont);
  }

  /**
   * Shift the lines of the current selection left by
   * removing the given text from the start of each line
   * that it starts.
   */
  public void doShiftLeft(String ins)
  {
    if (hasNonemptySelection())
    {
      String  sel = clippedSelection();
      ins = Pattern.quote(ins);
      doc.pasteAndSelect(sel.replaceAll("(^|\\n)" + ins, "$1"),
                         true);
    }
  }

  /**
   * Shift the lines of the current selection right by
   * inserting the given text at the start of each one.
   */
  public void doShiftRight(String ins)
  {
    if (hasNonemptySelection())
    {
      String  sel = clippedSelection();
      boolean lnl = sel.endsWith("\n");
      if (lnl) sel=sel.substring(0, sel.length()-1);
      ins = Matcher.quoteReplacement(ins);
      doc.pasteAndSelect(sel.replaceAll("(^|\\n)", "$1" + ins)+(lnl?"\n":""),
                         true);
    }
  }

  /** View the output of the most recent doTranslate. */
  public void doView(String spec)
  {
    Pipe.Continue cont = new Pipe.Continue()
    {
      public void consumeOutput(BufferedReader reader)
      {
      }

      public void fail(Exception ex)
      {
        ex.printStackTrace();
        viewer = null;
      }

      public void result(int exitCode, String output)
      { // System.err.printf("%s Dred viewer terminated:
        // %s%n", new Date(), output);
        tempCaption(String.format("[%d] %s", exitCode, output));
        viewer = null;
      }
    };

    if (viewer != null)
    {
      viewer.destroy();
      viewer = null;
    }

    String name = spec.equals("") ? doc.getFileName().getAbsolutePath()
                                 : spec;
    if (new File(name).getParent() == null)
      name = new File(doc.getFileName().getParent(), spec).getAbsolutePath();
    name = name.replaceAll("\\.tex$", "");
    name += usepdf ? ".pdf" : ".ps";
    // Automatic scaling based on a heuristic
    smallscale = doc.lineAt(0).toString().matches(".*documentclass.*foil.*");
    //
    String command = usepdf
                           ? "xpdf "
                           : ("gv -spartan -scale " + (smallscale ? -2 : -1) + " -geometry -1+0 ");
    viewer = Pipe.execute(cwd, command + name, "", cont);
  }

  /**
   * Request the keyboard focus, and make the current
   * editor window active.
   */
  public void edFocus()
  {
    ed.makeActive();
    ed.requestFocus();
  }

  /**
   * Add a Doc listener to the document that shows current
   * location on the GUI; overridden in ``fast'' frames.
   */
  public void enableDocFeedback()
  {
    doc.addDocListener(new DocListener()
    {
      public void cursorChanged(int first, int last)
      {
        showDocFeedback();
      }

      public void docChanged(int first, int last)
      {
        showDocFeedback();
      }

      public void selectionChanged(int first, int last)
      {
        showDocFeedback();
      }
    });
  }

  /**
   * Invoked by the associated document when its backup
   * file has been written.
   */
  public void fileBacked(File file)
  {
    setTitle(file.getName());
    setCaption(file.toString());
    labelC.setForeground(Color.GREEN);
    labelC.setText("backed");
  }

  /**
   * Invoked by the associated document when it is
   * changed.
   */
  public void fileChanged(File file)
  {
    if (safe)
    {
      setTitle(file.getName() + " (!)");
      setCaption(file.toString());
      labelC.setForeground(Color.RED);
      labelC.setText(doc.isAnonymous() ? "(needs naming)" : "!");
      safe = false;
    }
  }

  /**
   * Invoked by the associated document when its file name
   * is set.
   */
  public void fileNameSet(File file)
  {
    setTitle(file.getName());
    setCaption(file.toString());
    safe = true;
    showDocFeedback();
  }

  /**
   * Invoked by the associated document if something has
   * gone wrong with an operation.
   */
  public void fileReport(String report)
  {
    Dred.showWarning(report, 0, new Object[]
    {
      "OK"
    });
    tempCaption(report);
  }

  /** Invoked by the associated document when it is saved. */
  public void fileSaved(File file)
  {
    setTitle(file.getName());
    setCaption(file.toString());
    labelC.setForeground(Color.GREEN);
    labelC.setText("(saved)");
    safe = true;
  }

  public boolean hasNonemptySelection()
  {
    return doc.hasSelection() && !"".equals(doc.getSelection());
  }


  // ////////////////////////////////////////////////////////////////////////////////////////////

  /** Request the keyboard focus. */
  public void requestFocus()
  {
    if (Dred.debug)
      Dred.log.fine("%s", super.requestFocus(true));
    ed.requestFocus();
  }

  /** Show the current baseline feedback. */
  public void setCaption()
  {
    labelL.setText(caption);
  }

  /** Set the current baseline feedback, and show it. */
  public void setCaption(String s)
  {
    caption = s;
    labelL.setText(s);
  }

  /**
   * Set the working directory for this session: affects
   * shell runnables
   */
  public void setCWD(File cwd)
  {
    this.cwd = cwd;
  }

  /** Set the document associated with the editor. */
  public void setDoc(FileDocument doc)
  {
    this.doc = doc;
    ed.setDoc(doc);
    doc.addListener(this);
    enableDocFeedback();
    menuBar = new MenuBar();
    setJMenuBar(menuBar);
    log.finer("Binding menus");
    menuBar.bindMenus();
  }

  /** Show current interesting statistics. */
  public void showDocFeedback()
  {
    int length = doc == null ? 0 : doc.length();
    int x = doc == null ? 0 : doc.getX();
    int y = doc == null ? 0 : doc.getY();
    String feedbackText = String.format("%d.%d/%d", y, x, length);
    labelR.setText(feedbackText);
  }

  /**
   * Run the given command with the given input and spool
   * its output into the (newly-created if necessary)
   * ProcessFrame.
   */
  public void startProcess(final String command, String input)
  {
    doKillProcess();
    if (processFrame == null)
      processFrame = new ProcessFrame(command);
    else processFrame.setCommand(command);
    Pipe.Continue cont = new Pipe.Continue()
    {
      public void consumeOutput(BufferedReader reader)
      {
        processFrame.setEnabled(false);
        processFrame.doAppend(reader); // Ignores cursor
        // positioning
        processFrame.setEnabled(true);
      }

      public void fail(Exception ex)
      {
        ex.printStackTrace();
        process = null;
      }

      public void result(int exitCode, String output)
      {
        tempCaption(String.format("[%o] (%s)", exitCode, command));
        process = null;
      }
    };
    process = Pipe.execute(cwd, command, input, cont, false);
  }

  /**
   * Show the given string as feedback for about 5
   * seconds, then revert to the current baseline
   * feedback.
   */
  public void tempCaption(String s)
  {
    labelL.setText(s);
    Timer t = new Timer(7000, null);
    t.setRepeats(false);
    t.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        setCaption();
      }
    });
    t.start();
  }

  public String toHTMLString()
  {
    long lm = doc.lastModified();
    String mod = lm == 0 ? "(unsaved)" : new Date(lm).toString();
    return "<td>" + doc.getFileName().toString() + "</td><td>" + mod
           + "</td>";
  }

  public String toString()
  { 
    long lm = doc==null ? 0: doc.lastModified();
    String mod = lm == 0 ? "(unsaved)" : new Date(lm).toString();
    return doc.getFileName().toString() + " " + mod;
  }

}







































