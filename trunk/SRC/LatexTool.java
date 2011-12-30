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


  @SuppressWarnings("serial") class LatexToolBar extends TextLine
  {
    /**
     *  The EditorFrame to which this ToolBar is attached.
     */
    private final EditorFrame session;
    private boolean usetex2pdf = false;
  
    private boolean usepdf = true, useopen = true;    

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
  
      JMenuItem trans = but(new Act("Translate", "Translate to ps or pdf")
      {
        public void run()
        {
          doTranslate(getText().trim());
        }
      });
  
      JMenu menu = new JMenu("Latex");
      menu.setToolTipText("Translate and view latex");
      menu.add(trans);
      menu.addSeparator();
      final CheckItem useTex2Pdf = 
      new CheckItem("Use tex2pdf", usetex2pdf, "Generate pdf using tex2pdf script instead of pdflatex", prefs)
      {
        public void run()
        {
          usetex2pdf = state;
        }
      };
      final CheckItem usePdf = 
      new CheckItem("Use PDF", usepdf, "Generate pdf using either pdflatex or tex2pdf; view with an appropriate pdf viewer", prefs)
      {
        public void run()
        {
          usepdf = state;
          if (!usepdf) { useTex2Pdf.setState(false); }
          useTex2Pdf.setEnabled(usepdf); 
        }
      };
      final CheckItem useOPEN = 
      new CheckItem("View output", useopen, "Use pdfopen (or psopen) to start viewer after generating pdf (or ps) file", prefs)
      { 
        public void run()
        {
          useopen = state;
        }
      };
      usePdf.run();
      menu.add(usePdf);
      menu.add(useTex2Pdf);
      menu.add(useOPEN);
      bar.add(menu);
      setLabel(bar);
      setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
      setToolTipText("Name of the file to translate. Current file is translated when this is blank");
    }
  
    JMenuItem but(Act a)
    {
      return new JMenuItem(a);
    }
    
    /** Save the current document, then run tex2ps or tex2pdf or pdflatex on it. */
    public void doTranslate()
    {
      doTranslate("");
    }
  
    /**
     * Save the current document, then run tex2ps or pdflatex on the
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
      
      String pdfName = texName.replaceAll("\\.tex$", ".pdf");
      String psName  = texName.replaceAll("\\.tex$", ".ps");
      String open    = usepdf ? ("; (pdfopen --file "+pdfName+" 2> /dev/null)") 
                              : ("; (psopen --file "+psName+" 2> /dev/null)");
      this.session.startProcess((usepdf ? (usetex2pdf ? "tex2pdf " : "pdflatex -interaction=errorstopmode " ) 
                                        : "tex2ps ") + texName + (useopen ? open : ""), "");
    }  
  }

}














