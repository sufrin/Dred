package org.sufrin.dred;
import  javax.swing.*;

public abstract class ToolExtension extends Extension
{  
   public abstract JComponent makeTool(EditorFrame session);

   public ToolExtension(String name)
   { super(name); }

   public void openSession(EditorFrame session)
   { super.openSession(session);
     session.addTool(new ToolExtensionItem(session, name));
   }
   
   protected class ToolExtensionItem extends CheckItem
   {    EditorFrame session;
        
        JComponent theTool = null;
                
        public ToolExtensionItem(EditorFrame session, String title) 
        { super(title, false); 
          this.session=session; 
          theTool = makeTool(session);
        }
        
        public void run()
        {
          if (state)
            session.addToToolBar(theTool);
          else 
            session.removeFromToolBar(theTool);
        }
   }
}





