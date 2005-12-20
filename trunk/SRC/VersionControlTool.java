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
{ public VersionControlTool() { super("SVN/CVS/RCS"); }
  public VersionControlTool(Preferences prefs) { super("SVN/CVS/RCS"); this.prefs = prefs; }
  protected Preferences prefs;

  @Override
  public JComponent makeTool(EditorFrame session)
  {
    return new VersionControlToolBar(session);
  }

  static enum VC { SVN, CVS, RCS }
  
  class VersionControlToolBar extends TextLine
  {
    /**
     *  The EditorFrame to which this ToolBar is attached.
     */
    private final EditorFrame session;
    
    /** The version control system in use
    */
    VC vcsName = VC.SVN;
  
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
  
      JMenuItem commit = but(new Act("Commit", "Commit this file using the selected version control system")
      {
        public void run()
        {
          doCheckin(getText().trim());
        }
      });
      JMenuItem diff = but(new Act("Diff", "Get the differences between this file and a committed version")
      {
        public void run()
        {
          doDiff(getText().trim());
        }
      });
  
      JMenu menu = new JMenu("VC:");
      RadioItem.Group<VC> vcs = new RadioItem.Group<VC> ("VersionController", vcsName, "Set the version control system")
      { { run(); }
        public void run() { vcsName = value; }
      };
      menu.setToolTipText("Checkin this file");
      menu.add(commit);
      menu.add(diff);
      menu.addSeparator();
      menu.add(new RadioItem<VC>(vcs, "CVS", VC.CVS));
      menu.add(new RadioItem<VC>(vcs, "RCS", VC.RCS));
      menu.add(new RadioItem<VC>(vcs, "SVN", VC.SVN));
      menu.add(new RadioItem<VC>(vcs, "?",   null));
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
      String cmd = null;
      switch (vcsName)
      { case CVS: cmd = String.format("cvs ci -m '%s' %s", message, fileName); message=""; break;
        case SVN: cmd = String.format("svn ci -m '%s' %s", message, fileName); message=""; break;
        case RCS: cmd = String.format("ci -l %s", fileName); break;
        default:  cmd = String.format("echo commit -m '%s' %s", message, fileName); message=""; break;
      }      
      this.session.startProcess(cmd, message, reloader, null);
    } 
     
    /**
     * Diff the current document.
     */
    public void doDiff(String revision)
    {
      this.session.doSave();
      String fileName = session.doc.getFileName().getAbsolutePath();
      String cmd = null;
      if (!revision.equals("")) revision = (vcsName==VC.RCS?"-r":"-r ")+revision;
      switch (vcsName)
      { case CVS: cmd = String.format("cvs diff %s %s", revision, fileName); break;
        case SVN: cmd = String.format("svn diff %s %s", revision, fileName); break;
        case RCS: cmd = String.format("rcsdiff %s %s", revision, fileName); break;
        default:  cmd = String.format("echo diff %s %s", revision, fileName); break;
      }      
      this.session.startProcess(cmd, "");
    }  
  }

}










