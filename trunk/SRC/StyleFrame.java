
package org.sufrin.dred;

import GUIBuilder.*;
import java.awt.BorderLayout;
import java.awt.event.*;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JWindow;
import GUIBuilder.Col;

/**
        A Styleframe is JFrame that contains a number of style sheets, each of
        which contains a number of styling descriptors -- each associated with
        a button. 
        
        A styling descriptor describes a transformation that is applied to the current selection
        when the corresponding button is pressed.
 */
public class StyleFrame extends JFrame
{
  
  private final EditorFrame session;

  JPanel col = null, row = null;
  
  protected boolean colDefined()
  { if (col==null) 
    { newCol("...");
      System.err.printf("[Dred warning: declaring a default style sheet]%n");
    }
    return true;
  }

  JTabbedPane pane      = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
  JPanel      bar       = new Row();
  JPanel      base      = new JPanel();
  CheckItem   checkItem = null;
  
  public void setIconified(boolean iconified)
  { if (iconified) 
       setVisible(false); 
    else 
       setVisible(checkItem.getState());  
  }
  
  public StyleFrame(EditorFrame frame, CheckItem checkItem)
  { super(frame.doc.getFileName().getName()+" (styles)");
    // setUndecorated(true);
    session = frame;
    this.checkItem = checkItem;
    base.setLayout(new BorderLayout());
    base.add(pane, "Center");
    base.setBorder(BorderFactory.createTitledBorder("Styles"));
    base.add(bar, "South");
    add(base);
    addMenus();
    addBindings();
    addWindowListener
    (new WindowAdapter()
    { public void windowIconified(WindowEvent e)
      {
        StyleFrame.this.checkItem.doClick();
      }
    }
    );
    pack();
  }
  
  protected void addMenus()
  { 
    bar.setToolTipText("Styleframe actions");
    bar.add(new JButton
    ( new Act("Close", "Close this style panel")
      { public void run() { checkItem.doClick(); }
      }
    ));
    bar.add(new JButton
    ( new Act("Frame", "Put this panel next to its frame")
      { public void run() { checkItem.doClick(); checkItem.doClick(); }
      }
    ));
  }
  
  public void addBindings()
  { if (EditorFrame.protoBindings != null)
    for (Bindings.Binding binding: EditorFrame.protoBindings)
    { int len=binding.length();
      if (binding.matches("style", "---")  && colDefined())
         newRow();
      else
      if (binding.matches("style", "-")  && colDefined())
         row.add(new JSeparator());
      else
      if (binding.matches("style", "--") && colDefined())
      {  row.add(new JSeparator());
         row.add(new JSeparator());
      }
      else
      if (len>2 && binding.matches("style", "sheet"))
         newCol(binding.getFields(2));
      else
      if (len>2 && binding.matches("style", "block") && colDefined())
      {  
        row.add(formatButton(binding, true, 2));
      }
      else
      if (len>2 && binding.matches("style", "line") && colDefined())
      {  
        row.add(formatButton(binding, false, 2));
      }
      else
      if (len>3 && binding.matches("style", "latex", "env") && colDefined())
      { 
        row.add(formatButton(binding, true, 3, "\\begin{%ARG%}", "\\end{%ARG%}"));
      }
      else
      if (len>3 && binding.matches("style", "latex", "macro") && colDefined())
      { 
        row.add(formatButton(binding, false, 3, "\\%ARG%{", "}"));
      }
      else
      if (len>3 && binding.matches("style", "xml", "block") && colDefined())
      { 
        row.add(formatButton(binding, true, 3, "<%ARG%>", "</%ARG%>"));
      }
      else
      if (len>3 && binding.matches("style", "xml", "line") && colDefined())
      { 
        row.add(formatButton(binding, false, 3, "<%ARG%>", "</%ARG%>"));
      }
      else
      if (len>3 && binding.matches("style", "form", "block") && colDefined())
      { row.add
        (  formatForm(binding, true, 3)
        );
      }
      else
      if (len>3 && binding.matches("style", "form", "line") && colDefined())
      { row.add
        (  formatForm(binding, false, 3)
        );
      }
      else
      if (binding.matches("style")) 
      { 
         System.err.printf("[Dred warning: style binding unknown: %s]%n", binding);
      }
    }
  }


  void newCol(String title)
  {
    col = new Col(-1);
    row = col;
    pane.add(title, col);
  }
  
  void newRow()
  {
    row = new Row();
    col.add(row);
  }

  protected JButton formatButton(Bindings.Binding b, boolean block, int fromField)
  { 
    return new JButton(formatAct(b, block, fromField));
  }
  
  protected JButton formatButton(Bindings.Binding b, boolean block, int fromField, String open, String close)
  { String name = b.getField(fromField);
    open  = open.replace("%ARG%", name);
    close = close.replace("%ARG%", name);
    return new JButton(formatAct(b.getField(fromField), open, close, block));
  }
  
  protected ArgTool formatForm(Bindings.Binding b, final boolean block, int fromField)
  { Bindings.Binding format = b.split(fromField+1, "%SEL%");
    final String name  = b.getField(fromField);
    final String open  = format.optField(0);
    final String close = format.optField(1);
    return new ArgTool(name)
    {  public void run()
       {  String arg = getText();
          formatSelection
          (open.replace("%ARG%", arg), 
           close.replace("%ARG%", arg), 
           block);
       }
    };
  }
  
  /**
   * Returns an action, specified by fields fromField ... of the given Binding
   */
  protected Act formatAct(Bindings.Binding b, boolean block, int fromField)
  { Bindings.Binding format = b.split(fromField+1, "%SEL%");
    String name  = b.getField(fromField);
    String open  = format.optField(0);
    String close = format.optField(1);
    return formatAct(name, open, close, block);
  }
  
  /**
   * Returns an action with the given label that brackets the selection when pressed
   */
  public Act formatAct(String label, final String open, final String close, final boolean block)
  {
    return new Act(label)
    {
      public void run()
      {
        formatSelection(open, close, block);
      }
    };
  }
  
  public void formatSelection(final String open, final String close, final boolean block)
  { 
     StyleFrame.this.session.doTransformSelection
     ( new TextTransform()
       { public String transform(String left, String ins, String right)
         { if (block)
           { String  pre   = open;
             String  post  = close;
             boolean blank = left.matches("[ ]*");
             if (!ins.startsWith("\n"))     pre  = pre + '\n';
             if (left.length()>0 && !blank) pre  = '\n'+pre;
             if (!ins.endsWith("\n"))       post = '\n'+post; 
             if (blank && left.length()>0)
             { // Indent the block
               String punct = "", suff="";
               if (ins.endsWith("\n")) 
               { ins=ins.substring(0, ins.length()-1);
                 punct="\n"+left;
                 suff="\n";
               }
               return (pre+(ins.replace("\n"+left, "\n")))
                      .replace("\n", "\n"+left+"  ")
                      +punct
                      +(post.replace("\n", "\n"+left))
                      +suff
                      ;
             }             
             else
               return pre+ins+post+(right.length()!=0?"\n":"");    
           }
           else
              return open+ins+close;
         }
       }
     ); 
  }
}









