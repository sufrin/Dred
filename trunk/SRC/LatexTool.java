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
public class LatexTool extends RunTool
{ public LatexTool() { super("Latex Bar"); }
  public LatexTool(Preferences prefs) { super("Latex Bar"); this.prefs = prefs; }
  protected Preferences prefs;

  @Override
  public JComponent makeTool(EditorFrame session)
  {
    return new LatexToolBar(session);
  }


  class LatexToolBar extends TextLine
  {
    /**
     *  The EditorFrame to which this ToolBar is attached.
     */
    private final EditorFrame session;
  
    public LatexToolBar(EditorFrame session)
    {
      super(20, null, true, "Latex Bar");
      this.session = session;
      JMenuBar bar = new JMenuBar();
      // Eliminate input maps (pro-tem) to avoid spurious
      // effects
      InputMap m = null; // new ComponentInputMap(bar);
      bar.setInputMap(JComponent.WHEN_FOCUSED, m);
      bar.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, m);
      bar.setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, m);
  
      JMenuItem trans = but(new Act("Translate", "Translate using tex2ps or pdflatex")
      {
        public void run()
        {
          doTranslate(getText().trim());
        }
      });
      JMenuItem view = but(new Act("View", "View using gv or acroread")
      {
        public void run()
        {
          LatexToolBar.this.session.doView(getText().trim());
        }
      });
  
      JMenu menu = new JMenu("Latex");
      menu.setToolTipText("Translate and view latex");
      menu.add(trans);
      menu.add(view);
      menu.addSeparator();
      CheckItem usePdf = 
      new CheckItem("Use PDF", this.session.usepdf, "Generate pdf using pdflatex; view with acroread", prefs)
      {
        public void run()
        {
          LatexToolBar.this.session.usepdf = state;
        }
      };
      usePdf.run();
      menu.add(usePdf);
      bar.add(menu);
      setLabel(bar);
      setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
      setToolTipText("Name of the file to translate. Current file is translated when this is blank");
    }
  
    JMenuItem but(Act a)
    {
      return new JMenuItem(a);
    }
    
    /** Save the current document, then run tex2ps on it. */
    public void doTranslate()
    {
      doTranslate("");
    }
  
    /**
     * Save the current document, then run tex2ps on the
     * specified file. If the spec is empty, the file is
     * taken to be the one being edited; if nonempty then
     * "./xxxx" is interpreted as "xxxx" in the current
     * working directory of the editing session, while
     * "xxxx" is interpreted as "xxxx" in the parent
     * directory of the file being edited in the current
     * session.
     */
    public void doTranslate(String spec)
    {
      this.session.doSave();
      String texName = spec.equals("") ? session.doc.getFileName().getAbsolutePath()
                                       : spec;
      if (new File(texName).getParent() == null)
        texName = new File(session.doc.getFileName().getParent(), spec).getAbsolutePath();
  
      this.session.startProcess((this.session.usepdf ? "pdflatex -interaction=errorstopmode " : "tex2ps ") + texName, "");
    }  
  }

}








