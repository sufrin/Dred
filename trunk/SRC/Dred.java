package org.sufrin.dred;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.Vector;
import java.util.prefs.*;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.BorderFactory;

import org.sufrin.urlfactory.*;
import org.sufrin.logging.Logging;
import org.sufrin.logging.LoggingSocket;

/**
 * A simple editor that uses a scrollable display.
 * <br>
 * TODO:
  <ul>
  <li>toolbars should be user-configurable;</li>
  <li>ditto style frames;
  <li>style frames should open/close with their host ed window;
  (should there be just one style frame?); </li>
  <li>use styleframes as a basis for virtual keyboard;</li>
  <li>Session list</li>
  <li>LineMarks in documents</li>
  <li>UNDO/LAST PLACE/CUT-LIST</li>
  <li>Session-specific and file-specific bindings</li>
  <li>Structured/scoped binding declarations</li>
  <li>Structured/scoped binding declarations (and includes)</li>
  <li>UTF8 printer</li>
  <li>Encodings menu for input and output</li>
  <li>Bindings visitor</li>

  </ul>
 * 
 * <PRE> 
 * $Id$
 * </PRE>
 * 
 */

public class Dred
{

  protected static Logging log   = Logging.getLog("Dred");
  public    static boolean debug = log.isLoggable("FINE");
  
  /** Currently-active sessions in order of creation */
  protected static Vector<EditorFrame> sessions = new Vector<EditorFrame>();

  static LoggingSocket loggingSocket = null;
  static SessionSocket sessionSocket = null;
  
  /** Session calls this when making a new session Frame */
  public static void addSession(EditorFrame session)
  { 
    sessions.add(session);
    // Tell the extensions
    Extension.sessionOpened(session);
  }
  
  /** True if forced to use fallback bindings */
  protected static boolean fallBack = false;
  
  /**
   * Start a session (or listener) for each argument, or an
   * anonymous session if there are no arguments.
   */
  public static void main(String[] args) throws Exception
  { URL.setURLStreamHandlerFactory(new ClassURLFactory());
    boolean wait = false;
    if (args.length > 0)
    { for (String arg : args)
        if (arg.equals("-w") || arg.equals("--wait"))
          wait=true;
        else if (arg.equals("--serving"))
        { System.out.println(prefs.getInt("port", 0));
          System.exit(isServing() ? 0 : 1);
        }
        else if (arg.startsWith("--serve="))
        { int port = Integer.parseInt(arg.substring("--serve=".length()));
          startServer(port);
        }
        else if (arg.equals("--serve"))
        { startServer(0);       
        }
        else if (arg.startsWith("--enc="))
        { String name = arg.substring("--enc=".length());
          if (EditorFrame.fileChooser.getCodings().contains(name))
             EncodingName = name;
          else
             showWarning(name+" is not a recognised encoding!");
        }
        else if (arg.startsWith("--logger="))
          startLogger(Integer.parseInt(arg.substring("--logger=".length())));
        else if (arg.startsWith("--logger"))
          startLogger("60001");
        else if (arg.equals("--bindings="))
          fallBack=true;
        else if (arg.startsWith("--bindings="))
          readBindings(arg.substring("-bindings=".length()), true);
        else if (wait) 
          startLocalSession(arg, EncodingName); 
        else 
          startRemoteSession(arg);
        if (wait && sessions.isEmpty())  
           startLocalSession(null, EncodingName); 
     }
    else 
      startServer(0);
  }
  
  /** Return true if there's REALLY a server running */
  public static boolean isServing()
  { int port = prefs.getInt("port", 0);
    if (port==0) return false;
    try
    {
      URL url = new URL("http", "localhost", port, "/serving");
      url.openStream().close();
      return true;
    }
    catch (IOException ex)
    { return false;
    }
  }
  
  protected static Bindings bindings = new Bindings();
  
  /** Read a bindings file if one exists */
  public static void readBindings(String url, boolean warn) 
  { File file = new File(url);
    if (file.exists() && file.canRead()) url="file:"+file.getAbsolutePath();
    try 
    { readBindings(new URL(url)); }
    catch (Exception ex)
    { if (warn) 
      { showWarning("Cannot read bindings from: "+url);
        if (bindings.isEmpty()) 
           showWarning("Using fallback bindings.");        
      }
    }
  }
  
  public static void readBindings(URL url) throws Exception
  {  if (url!=null)
     { bindings.read(url);
       SimpleEditor.setBindings(bindings);
       EditorFrame.setBindings(bindings);
       TextLine.setBindings(bindings);
       Extension.setBindings(bindings);
     }
  }
  
  public static void resetBindings(URL url) throws Exception
  {  if (url!=null)
     { bindings.read(url);
       SimpleEditor.setBindings(bindings);
       EditorFrame.setBindings(bindings);
       TextLine.setBindings(bindings);
       Extension.setBindings(bindings);
     }
  }
  
  /**
   * A session Frame calls this when it has finished. When
   * there are no more unfinished frames the program exits.
   */
  public static void removeSession(EditorFrame session)
  {
    sessions.remove(session);
    if (sessions.isEmpty() && !serverRunning)       
       ActionMethod.Action.shutdownNow();      
  }
  
  protected static boolean serverRunning = false;

  public synchronized static void loadBindings()
  { if (!fallBack && bindings.isEmpty())
    {
      String rootBindings = System.getProperty("DREDBINDINGS");
      if (rootBindings==null) rootBindings = System.getenv("DREDBINDINGS");
      if (rootBindings==null) rootBindings = System.getProperty("user.home")+File.separator+".dred"+File.separator+"dred.bindings";
      if (rootBindings==null) rootBindings = System.getProperty("user.home")+File.separator+"DRED"+File.separator+"dred.bindings";
      if (new File(rootBindings).canRead())
          readBindings("file:"+rootBindings, true);
      else
          showWarning(String.format("Cannot read bindings %s; using built-in fallback bindings.", rootBindings));
    }
  }
  
  protected static String EncodingName = "UTF8";
 
  /**
   * Construct a new Editor session editing the document
   * with the given path (or an anonymous document if the
   * given path is null)
   */
  public synchronized static EditorFrame startLocalSession(final String path, String encoding)
  { loadBindings();
    FileDocument doc = new FileDocument(encoding);
    EditorFrame f = path == null ? new EditorFrame(80, 24)
                                 : new EditorFrame(80, 24, path);
    try
    {
      if (path != null)
        doc.doLoad(path);
    }
    catch (Exception ex)
    { // Document couldn't be loaded even though it exists:
      // maybe a directory
      f.fileReport(ex.getMessage());
    }
    f.setDoc(doc);
    f.repaint();
    addSession(f);
    return f;
  }

  protected static Preferences prefs = Preferences.userRoot().node("Dred");
  
  /** After starting a server (if there is none running) on the default
      server port, start a remote editing session for the given path.
   */
  public static void startRemoteSession(String path) throws Exception
  {     String cwd  = System.getProperty("user.dir");
        File   file = new File(path);
        int    port = prefs.getInt("port", 0);  
        if (port==0) 
        {  port = startServer(port);
           System.err.printf("[Dred: started server on port %d]%n", port);
        }
        
        int retries = 1;
        while (retries>=0)
        try
        {
          URL              url    = new URL("http", "localhost", port, "/edit?FILE="+file.getAbsolutePath()+"&CWD="+cwd+"&ENCODING="+EncodingName);
          LineNumberReader reader = new LineNumberReader(new InputStreamReader(url.openStream(), "UTF8")); 
          reader.close();
          return;
        }
        catch (ConnectException ex)
        { 
          System.err.println("[DRED: NO SERVER AT: "+port+" STARTING ANOTHER]"); 
          port = startServer(0);
          retries--;
        }
  }

  
  /** Close all the editing sessions */
  public static void closeAll()
  {
    for (EditorFrame frame : new Vector<EditorFrame>(sessions))
    {
      frame.doQuit();
    }
  }
  
  /** Close all the editing sessions, and quit the server */
  public static void closeServer()
  {
    closeAll();
    if (sessions.isEmpty()) 
    { if (sessionSocket!=null) sessionSocket.close(); 
      serverRunning = false;  
      ActionMethod.Action.shutdownNow();      
      System.exit(0); 
    }
  }
  
  /** Returns the name of my host */
  public static String currentHost()
  {
     try
     {
        java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();    
        return localMachine.getHostName();
     }
     catch(java.net.UnknownHostException uhe)
     {
        return "";
     }
  }
  
  /** Start a Dred server on the given port and return it; choose
      an ephemeral port if port==0 and we're not on Windows.
  */
  public static int startServer(int port)
  { boolean pseudoServer = false;
    serverRunning = true;
    String user = System.getProperty("user.name");
    try
    { if (port>0 || (File.separator.equals("/") && port>=0))
      { 
        sessionSocket = new SessionSocket(port, prefs);
        port = sessionSocket.getPort();
        prefs.putInt("port", port);
        try { prefs.sync(); } catch (BackingStoreException ex) { ex.printStackTrace(); }
      }
      else
        pseudoServer = true;
      
      final JFrame frame = new JFrame(pseudoServer ? "[[[Dred]]]" : "[[[Dred " + user + "@"+currentHost()+ "]]]");
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.addWindowListener(new WindowAdapter()
      {
        public void windowClosed(WindowEvent e)
        {
          closeServer();
        }
      });
      frame.setLayout(new GUIBuilder.ColLayout(-1));
      
      JLabel label = new JLabel
      ("<html><center>Dred<br></br> " 
      + user 
      + "@" + currentHost()
      + (pseudoServer ? "" : (":"+ port))
      +"</center></html>"
      );
      label.setHorizontalAlignment(SwingConstants.CENTER);
      label.setBorder(BorderFactory.createEtchedBorder());
      frame.getRootPane().setBorder(BorderFactory.createEtchedBorder());
      frame.add(label);
      
      JButton button = new JButton("Open");
      button.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent ev)
        {
          EditorFrame.openSession(frame, null);
        }
      });
      frame.add(button);
      button.setToolTipText("Open an editing session on an existing file");
      
      button = new JButton("New");
      button.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent ev)
        {
          startLocalSession(null, EncodingName);
        }
      });
      frame.add(button);
      button.setToolTipText("Open an editing session on a new file");
            
      button = new JButton("Close All");
      frame.add(button);
      button.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent ev)
        {
          closeAll();
        }
      });
      button.setToolTipText("Close all editing sessions but keep the server running.");
      
      button = new JButton("Exit Server");
      frame.add(button);
      button.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent ev)
        {
          closeServer();
        }
      });
      button.setToolTipText("Close all editing sessions and shut down the server.");

      frame.setIconImage(EditorFrame.dnought.getImage());
      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
      if (port!=0) frame.setState(JFrame.ICONIFIED);
    }
    catch (IOException ex)
    {
      System.err.println("[Dred: cannot start server on port "+port+"]");
      System.exit(1);
    }
    return port;
  }
  
  /** Start the HTTP logger interface on the given port */  
  static void startLogger(String port) { startLogger(Integer.parseInt(port)); }
  
  /** Start the HTTP logger interface on the given port */
  static void startLogger(int port)
  {
    if (loggingSocket != null)
    {
      showWarning("Logger port: " + loggingSocket.getPort());
    }
    else try
    {
      loggingSocket = new LoggingSocket(port);
    }
    catch (IOException ex)
    {
      showWarning("Cannot open logger port " + port + ": " + ex.getMessage());
    }
    catch (NumberFormatException ex)
    {
      showWarning("Logger port must be between 1024 and 65535");
    }
  }

  
   /**
   * Show a warning message in a dialogue window with
   * various option buttons. When a button is pressed return
   * its number. The default button (the one fired by
   * pressing ENTER) is indicated by the given integer
   * parameter. The dialogue's parent is the given
   * component.
   */
  protected static int showWarning(Component parent, String msg, int dflt,
                                   Object[] options)
  {
    int option = JOptionPane.showOptionDialog(parent, msg, "Warning",
                                              JOptionPane.DEFAULT_OPTION,
                                              JOptionPane.WARNING_MESSAGE,
                                              null, options, options[dflt]);
    return option;
  }

  protected static int showWarning(String msg)
  {
    return showWarning(null, msg, 0, new Object[]
    {
      "OK"
    });
  }

  /**
   * Show a warning message in a dialogue window with
   * various option buttons. When a button is pressed return
   * its number. The default button (the one fired by
   * pressing ENTER) is indicated by the given integer
   * parameter.
   */
  protected static int showWarning(String msg, int dflt, Object[] options)
  {
    return showWarning(null, msg, dflt, options);
  }


}




