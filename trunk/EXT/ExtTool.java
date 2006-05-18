import org.sufrin.dred.*;
import java.util.regex.*;
import javax.swing.*;

/** 
        Skeleton of a simple Dred extension that adds a menu to 
        the toolbar.  
        <p>
        Compile with <code>javac -cp Dred.jar ExtTool.java</code>
        </p>
        <p>
        Install by copying the resulting class files to <code>$HOME/.dred</code>
        and adding a line of the form <code>extension load ExtTool</code> to
        the appropriate bindings file.
        </p>
*/
public class ExtTool extends Extension
{     
   public void openSession(EditorFrame session)
   { super.openSession(session);
     new Tool(session);
   }    
   
    /** Each session constructs one of these */
   static class Tool 
   { final EditorFrame session;
     final SearchableDocument doc;
     
     /** Find the next instance of the given
         pattern, going around from the top of
         the document if necessary.
     */

     public void aroundFind(Pattern pat)
     {
        if (!doc.downFind(pat))
        { int x = doc.getX(), y=doc.getY();
          doc.setCursor(0, 0);
          if (!doc.downFind(pat)) doc.setCursor(x, y);
        };     
     }
     
     public Tool(final EditorFrame session)
     { JMenu menu   = session.addMenu("Ext");
       this.session = session;
       this.doc     = session.getDoc();  
       menu.add
       (new Act("Ext menu entry", "Ext menu entry")
          { public void run() {  }
          }
       );           
     }
   }
}



