package org.sufrin.dred;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Point;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import GUIBuilder.RowLayout;

/** 
 *    An editable single-line of text with a caption/title. 
 */
public class TextLine extends JPanel
{ Document     doc; 
  SimpleEditor ed; 
  int          cols;
  String       title;
  JComponent   label;
  
  public TextLine(int cols, String title)
  { this(cols, new JLabel(title), true); }
  
  public void setToolTipText(String tip)
  { super.setToolTipText(tip);
    ed.setToolTipText(tip);
  }
  
  public TextLine(int cols, JComponent label, boolean border)
  { setLayout(new RowLayout(-1, true).fix(0)); 
    this.label=label;
    this.cols=cols;
    doc = new Document();
    ed  = new SimpleEditor(new Display(cols, 1));
    ed.actions.register(this);
    // Register default actions
    bind("ENTER",         "doTellBig"); 
    bind("control U",     "doClear");
    bind("control ENTER", "doBig"); 
    // Register user-specified bindings
    if (protoBindings!=null)
       for (Bindings.Binding binding: protoBindings)
       {
         if (binding.length()>=3 && binding.matches("minitext", "action"))
           bind(binding.toKey(3), binding.getField(2));
       }
    ed.setDoc(doc);
    if (label!=null) 
    { add(label, "West");
    }
    if (border) 
    { JPanel p = new JPanel();
      p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
      p.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
      p.add(ed.getComponent());
      add(p, "Center");
    }
    else
    { JComponent p = ed.getComponent();
      add(p, "Center");
    }
  }
  
  public JComponent getLabel()                 { return label; }
  public void       setLabel(JComponent label) 
  { if (label!=null) remove(label);
    this.label=label; 
    add(label, 0);
  }

  /** Returns the current text. */
  public String getText()            { return doc.getText(); }

  /** Set the text. */
  public void   setText(String text) { doc.setText(text); }

  /** Request the keyboard focus and make this line's display the
      current ``active'' display.
  */
  public void activate()
  { ed.requestFocus();
    ed.makeActive();
  }
  
  protected static Bindings protoBindings = null;
  public    static void  setBindings(Bindings thePrototype) { protoBindings = thePrototype; }
  
  public String getBindingsText(String prefix, boolean local)
  {
    return ed.actions.getBindingsText(prefix, this, local);
  }

  public String getBindingsHTML(String prefix, boolean local)
  {
    return ed.actions.getBindingsHTML(prefix, this, local);
  }

  JFrame auxFrame;
  
  /** Make an additional, large,  editor for this text line
      (so multi-line texts can be edited).
  */
  @ActionMethod(label="MultiLine", tip="to make a multiline-view minitext of this \none-line-view minitext")
  public void doBig()
  { // Perhaps this should be simplified: just increase the size of the Display!
    if (auxFrame != null) return;
    auxFrame = new JFrame(TextLine.this.title);
    final SimpleEditor ned = new SimpleEditor(cols, 3, true);
    ned.setDoc(doc);
    
    auxFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    auxFrame.addWindowListener
    ( new WindowAdapter()
      {
        public void windowClosing(WindowEvent e)
        { ned.removeDoc();
          auxFrame.dispose();
          auxFrame=null;
        }
      }
    );
    auxFrame.add(ned.getComponent());
    auxFrame.pack();
    auxFrame.setLocationRelativeTo(TextLine.this);
    Point loc = auxFrame.getLocation();
    loc.translate(0, 90);
    auxFrame.setLocation(loc);
    
    auxFrame.setVisible(true);
  }
  
  /** Tell the user that control-B makes a new big window onto this text field. */
  @ActionMethod(tip="Tell the user how to make a new big window onto this text field")
  public void doTellBig()
  { Action act     = ed.getAction("doBig");
    String message = "The minitext here can be many lines long, but there is no way of viewing all the lines";
    if (act!=null) 
    {
       message=act.toString();
    }
    JOptionPane.showMessageDialog(this, message);
  }
  
  /** Clear this text field */
  @ActionMethod(tip="Clear this text field")
  public void doClear()
  { setText("");
  }
   
  /** Bind a key to an action */
  public void bind(String key, Action action)
  {
    ed.bind(key, action);   
  }
    
  /** Bind a key to an action specified by name */
  public void bind(String key, String action)
  {
    ed.bind(key, action);   
  }
    
  /** Add a focus eavesdropper to the underlying document */
  public void addFocusEavesdropper(FocusEavesdropper d) 
  {
    doc.addFocusEavesdropper(d);
  }
 
}

















