
package org.sufrin.dred;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import GUIBuilder.Col;

/**
 * 
 *
 *
 */
public class StyleFrame extends JFrame
{
  
  private final EditorFrame session;

  JPanel col = null;
  
  protected boolean colDefined()
  { if (col==null) 
    { newCol("...");
      System.err.printf("[Dred warning: declaring a default style sheet]%n");
    }
    return true;
  }

  JTabbedPane pane = new JTabbedPane();

  public StyleFrame(EditorFrame frame)
  {
    super("Styles");
    session = frame;
    setDefaultCloseOperation(EditorFrame.DO_NOTHING_ON_CLOSE);
    setIconImage(EditorFrame.dnought.getImage());
    add(pane);
    addBindings();
    pack();
  }
  
  public void addBindings()
  { if (EditorFrame.protoBindings != null)
    for (Bindings.Binding binding: EditorFrame.protoBindings)
    { int len=binding.length();
      if (binding.matches("style", "-")  && colDefined())
         col.add(new JSeparator());
      else
      if (binding.matches("style", "--") && colDefined())
      {  col.add(new JSeparator());
         col.add(new JSeparator());
      }
      else
      if (len>2 && binding.matches("style", "sheet"))
         newCol(binding.getFields(2));
      else
      if (len>2 && binding.matches("style", "block") && colDefined())
      {  
        col.add(new JButton(formatAct(binding.getField(2), binding.optField(3), binding.optField(4), true)));
      }
      else
      if (len>2 && binding.matches("style", "line") && colDefined())
      {  
        col.add(new JButton(formatAct(binding.getField(2), binding.optField(3), binding.optField(4), false)));
      }
      else
      if (len>3 && binding.matches("style", "latex", "env") && colDefined())
      { String name = binding.getField(3);
        col.add(new JButton(formatAct(name, String.format("\\begin{%s}", name), String.format("\\end{%s}", name), true)));
      }
      else
      if (len>3 && binding.matches("style", "latex", "macro") && colDefined())
      { String name = binding.getField(3);
        col.add(new JButton(formatAct(name, String.format("\\%s{", name), "}", false)));
      }
      else
      if (len>3 && binding.matches("style", "xml", "block") && colDefined())
      { String name = binding.getField(3);
        col.add(new JButton(formatAct(name, String.format("<%s>", name), String.format("</%s>", name), true)));
      }
      else
      if (len>3 && binding.matches("style", "xml", "line") && colDefined())
      { String name = binding.getField(3);
        col.add(new JButton(formatAct(name, String.format("<%s>", name), String.format("</%s>", name), false)));
      }
      else
      if (len>2 && binding.matches("style", "form") && colDefined())
      { final  String name   = binding.getField(2);
        final  String open   = binding.optField(3);
        final  String close  = binding.optField(4);
        final  boolean block  = binding.optField(5).equals("block");
        col.add
        (  new ArgTool(name)
           {  public void run()
              {  String s = getText();
                 StyleFrame.this.session.doFormatSelection
                 (open.replace("%s", s), 
                  close.replace("%s", s), 
                  block);
              }
           }
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
    pane.add(title, col);
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
        StyleFrame.this.session.doFormatSelection(open, close, block);
      }
    };
  }
}







