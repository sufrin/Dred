import org.sufrin.dred.*;
import javax.swing.*;

/** 
        An example of a simple Dred extension.  A MarkTool places a
        "Mark" menu on the session menubar, with buttons to insert,
        find, and remove the special mark characters.
*/
public class MarkTool extends Extension
{  public void openSession(EditorFrame session)
   { super.openSession(session);
     new Tool(session);
   } 
   
   static char[] marks = Document.getMarkCharacters();
   
   static class Tool 
   { final EditorFrame session;
     public Tool(final EditorFrame session)
     { JMenu menu = session.addMenu("Marks");
       this.session=session;
       for (char c: marks)
       { final char ch=c;
         menu.add
         (new Act(Document.transMark(ch)+": set", "Set the mark "+Document.transMark(ch))
          { public void run()
            { session.insert(""+ch);
            }
          }
         );
      }
     }
   }
}
