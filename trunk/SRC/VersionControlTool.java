package org.sufrin.dred;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.border.BevelBorder;

/**
        Tool that puts up a Version Control Toolbar
*/
public class VersionControlTool extends RunTool
{ public VersionControlTool() { super("SVN/CVS/RCS"); }
  public VersionControlTool(Preferences prefs) { super("SVN/CVS/RCS"); this.prefs = prefs; }
  protected Preferences prefs;

  @Override
  public JComponent makeTool(EditorFrame session)
  {
    return new VersionControlToolBar(session);
  }

  static enum VC { SVN, CVS, RCS, UNK }
  
  @SuppressWarnings("serial") class VersionControlToolBar extends TextLine
  {
    /**
     *  The EditorFrame to which this ToolBar is attached.
     */
    private final EditorFrame session;
    
    /** The version control system in use
    */
    VC     vcsName  = null;
    File   fileName = null;
    File   parent   = null;
    String filePath = null;
    String lastName = null;
  
    @SuppressWarnings("serial")
	public VersionControlToolBar(EditorFrame session)
    {
      super(20, null, true, "VersionControl");
      this.session = session;
      fileName     = session.doc.getFileName();
      parent       = fileName.getParentFile();
      lastName     = fileName.getName();
      filePath     = fileName.getAbsolutePath();
      
      // set up the menu bar
      JMenuBar bar = new JMenuBar();
      InputMap m = null; // new ComponentInputMap(bar);
      bar.setInputMap(JComponent.WHEN_FOCUSED, m);
      bar.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, m);
      bar.setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, m);
      
      // Heuristically establish what kind of VCS we are using
      // Resolve ambiguity with priority: RCS(,v), RCS(directory), SVN, CVS
      boolean svn = new File(parent, ".svn").exists();
      boolean rcs = new File(parent, "RCS").exists();
      boolean cvs = new File(parent, "CVS").exists();
      vcsName  = rcs ? VC.RCS : svn ? VC.SVN : cvs ? VC.CVS : VC.UNK;
      // ,v files for RCS take priority over all other clues
      if (vcsName!=VC.RCS 
          &&
          new File(parent, fileName.getName()+",v").exists()) vcsName = VC.RCS;
  
      JMenuItem commit = but(new Act("Commit", "Commit this file using the text field as a comment")
      {
        public void run()
        {
          doCheckin(getText().trim());
        }
      });
      
      JMenuItem diff = but(new Act("Diff", "Get the differences between this file and the numbered (or last) revision of this file")
      {
        public void run()
        {
          doDiff(getText().trim());
        }
      });
      
      JMenuItem log = but(new Act("Log", "Get the log for (the numbered) revision of this file ")
      {
        public void run()
        {
          doLog(getText().trim());
        }
      });
  
      JMenu menu = new JMenu("VC:");
      RadioItem.Group<VC> vcs = new RadioItem.Group<VC> ("VersionController", vcsName, "Set the version control system to this one")
      { { run(); }
        public void run() { vcsName = value; }
      };
      menu.setToolTipText("Version control actions: commit, diff, log");
      menu.add(commit);
      menu.add(diff);
      menu.add(log);
      menu.addSeparator();
      menu.add(new RadioItem<VC>(vcs, "CVS", VC.CVS));
      menu.add(new RadioItem<VC>(vcs, "RCS", VC.RCS));
      menu.add(new RadioItem<VC>(vcs, "SVN", VC.SVN));
      menu.add(new RadioItem<VC>(vcs, "???", VC.UNK));
      bar.add(menu);
      setLabel(bar);
      setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
      setToolTipText("The checkin message or the revision number");
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
      message = message.replaceAll("(['\\\\\"])", "\\\\$1");
      String cmd = null;
      File   cwd = session.getCWD();
      switch (vcsName)
      { case CVS: cmd = String.format("cvs ci -m '%s' %s", message, lastName); cwd = parent; message=""; break;
        case SVN: cmd = String.format("svn ci -m '%s' %s", message, filePath); message=""; break;
        case RCS: cmd = String.format("ci -l %s", filePath); break;
        default:  cmd = String.format("echo unknown VCS commit -m '%s' %s", message, filePath); message=""; break;
      }      
      this.session.startProcess(cwd, cmd, message, reloader, null);
    } 
     
    /**
     * Diff the current document.
     */
    public void doDiff(String revision)
    {
      this.session.doSave();
      @SuppressWarnings("unused") 
      String fileName = session.doc.getFileName().getAbsolutePath();
      String cmd = null;
      File   cwd = session.getCWD();
      if (!revision.equals("")) revision = (vcsName==VC.RCS?"-r":"-r ")+revision;
      switch (vcsName)
      { case CVS: cmd = String.format("cvs diff %s %s", revision, lastName); cwd = parent; break;
        case SVN: cmd = String.format("svn diff %s %s", revision, filePath); break;
        case RCS: cmd = String.format("rcsdiff %s %s", revision, filePath); break;
        default:  cmd = String.format("echo unknown VCS diff %s %s", revision, filePath); break;
      }      
      this.session.startProcess(cwd, cmd, "", null, null);
    }  
    
    /**
     * Log the current document.
     */
    public void doLog(String revision)
    {
      // this.session.doSave();
      @SuppressWarnings("unused") 
      String fileName = session.doc.getFileName().getAbsolutePath();
      String cmd = null;
      File   cwd = session.getCWD();
      if (!revision.equals("")) revision = (vcsName==VC.RCS?"-r":"-r ")+revision;
      switch (vcsName)
      { case CVS: cmd = String.format("cvs log %s %s", revision, lastName); cwd = parent; break;
        case SVN: cmd = String.format("svn log %s %s", revision, filePath); break;
        case RCS: cmd = String.format("rlog %s %s", revision, filePath); break;
        default:  cmd = String.format("echo unknown VCS log %s %s", revision, filePath); break;
      }      
      this.session.startProcess(cwd, cmd, "", null, null);
    }  
  }

}













