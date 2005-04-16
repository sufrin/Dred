import org.sufrin.dred.*;
import java.util.regex.*;
import javax.swing.*;

/** 
<p>
        An example of a simple Dred extension.  A MarkTool places a
        "Mark" menu on the session menubar, with buttons to insert,
        and find each of the (first five) special mark characters,
        and a button to find any mark character. 
</p>
        
<p>
        Compile with:
        <pre>
           javac -cp Dred.jar MarkTool.java 
        </pre>   
        Move the resulting .class files to (say) <tt>/home/mumble/dred/EXT</tt>, and then ensure that
        the following bindings are present somewhere in one of the
        bindings files you load.
        <pre>
           extension path /home/mumble/dred/EXT
           extension load MarkTool                  
        </pre>
</p>
*/
public class MarkTool extends Extension
{     
   public void openSession(EditorFrame session)
   { super.openSession(session);
     new Tool(session);
   }    
   
   /** Constants shared by all instances of the tool */
   static char[]    marks   = Document.getMarkCharacters();
   static Pattern   anyMark = null;
   static Pattern[] theMark = new Pattern[5];
   
   /** Initialize the constants: catches and report exceptions locally
       because Extension classes are loaded by the reflection machinery,
       and it doesn't give much information when something goes wrong.
   */
   static void initConstants()
   {   try
       {
         String any = "";
         int i=0; for (char c: marks) 
         { if (i < theMark.length) theMark[i] = Pattern.compile(Pattern.quote(c+""));
           any = any+c+"|";
           i++;
         }
         anyMark = Pattern.compile(any.substring(0, any.length()-1));
       }
       catch (Exception ex)
       { 
         ex.printStackTrace();
       }
   }
   
   static { initConstants(); }
   
   /** Each session constructs one of these */
   static class Tool 
   { final EditorFrame session;
     final SearchableDocument doc;
     
     /** Find the next instance of the given pattern, 
         going around from the top of the document if necessary. 
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
     { JMenu menu = session.addMenu("Marks");
       this.session = session;
       this.doc     = session.getDoc();  
       menu.add
       (new Act("Find any mark", "Find any mark")
          { public void run() { aroundFind(anyMark); }
          }
       );           
       int i=0; for (char c: marks) if (i<theMark.length)
       { final String  ch  = ""+c;
         final Pattern pat = theMark[i];
         menu.addSeparator();
         menu.add
         (new Act(Document.transMark(c)+": set", "Set the mark "+Document.transMark(c))
          { public void run() { doc.insert(ch); }
          }
         );
         menu.add
         (new Act(Document.transMark(c)+": find", "Find the mark "+Document.transMark(c))
          { public void run() { aroundFind(pat); }
          }
         );
         i++;
      }
     }
   }
}


