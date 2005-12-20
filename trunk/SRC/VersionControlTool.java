package org.sufrin.dred;
import java.util.prefs.*;

import javax.swing.BorderFactory;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.border.BevelBorder;
import java.io.File;

/**
        Tool that puts up a Latex Toolbar
*/
public class VersionControlTool extends RunTool
{ public VersionControlTool() { super("SVN/CVS"); }
  public VersionControlTool(Preferences prefs) { super("SVN/CVS"); this.prefs = prefs; }
  protected Preferences prefs;

  @Override
  public JComponent makeTool(EditorFrame session)
  {
    return new VersionControlToolBar(session);
  }


  class VersionControlToolBar extends TextLine
  {
    /**
     *  The EditorFrame to which this ToolBar is attached.
     */
    private final EditorFrame session;
    
    /** The version control system in use
    */
    String vcsName = "svn";
  
    public VersionControlToolBar(EditorFrame session)
    {
      super(20, null, true, "VersionControl");
      this.session = session;
      JMenuBar bar = new JMenuBar();
      // Eliminate input maps (pro-tem) to avoid spurious
      // effects
      InputMap m = null; // new ComponentInputMap(bar);
      bar.setInputMap(JComponent.WHEN_FOCUSED, m);
      bar.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, m);
      bar.setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, m);
  
      JMenuItem commit = but(new Act("Commit", "Commit using svn or cvs or rcs")
      {
        public void run()
        {
          doCheckin(getText().trim());
        }
      });
  
      JMenu menu = new JMenu("SVN");
      RadioItem.Group<String> vcs = new RadioItem.Group<String> ("VersionController", vcsName, "Set the version control system")
      { { run(); }
        public void run() { vcsName = value; }
      };
      menu.setToolTipText("Checkin this file");
      menu.add(commit);
      menu.addSeparator();
      menu.add(new RadioItem<String>(vcs, "CVS", "cvs"));
      menu.add(new RadioItem<String>(vcs, "RCS", "rcs"));
      menu.add(new RadioItem<String>(vcs, "SVN", "svn"));
      menu.add(new RadioItem<String>(vcs, "?",   "?"));
      bar.add(menu);
      setLabel(bar);
      setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
      setToolTipText("The checkin message");
    }
  
    JMenuItem but(Act a)
    {
      return new JMenuItem(a);
    }
    
    /** Save the current document, then reload it. */
    public void doCheckin()
    {
      doCheckin("");
    }
    
    Runnable reloader = new Runnable()
    { 
      public void run() { VersionControlToolBar.this.session.reloadDocFromFilestore(); }
    };
  
    /**
     * Checkin and reload the current document.
     */
    public void doCheckin(String message)
    {
      this.session.doSave();
      String fileName = session.doc.getFileName().getAbsolutePath();
      message = message.replaceAll("(['\\\\\"])", "\\\\$1");
      if (message.equals("") && vcsName.equals("rcs")) message="%%";
      String cmd = String.format( vcsName.equals("cvs") ? "cvs ci -m '%s' %s" : 
                                  vcsName.equals("svn") ? "svn ci -m '%s' %s" :
                                  vcsName.equals("rcs") ? "ci -l -m'%s' %s"   : "echo version control -m '%s' %s", message, fileName);
      this.session.startProcess(cmd, "", reloader, null);
    }  
  }

}









