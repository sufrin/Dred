package org.sufrin.dred;
import java.awt.*;
import java.awt.datatransfer.*;

/**
        SystemClipboard provides access to the system Clipboard.
*/
public class SystemClipboard
{
   static Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();

   private static ClipboardOwner owner = new ClipboardOwner()
   {
     public void lostOwnership(Clipboard sysClipBoard, Transferable tx) { }
   };

   public static void set(String s)
   { 
     sysClip.setContents(new StringSelection(s), owner);
   }
   
   public static String get()
   { String result = null;
     Transferable tx = sysClip.getContents(owner);
     if ( tx!=null && tx.isDataFlavorSupported (DataFlavor.stringFlavor) )
     { try
       { result = (String) tx.getTransferData(DataFlavor.stringFlavor);
       }
       catch (Exception ex)
       { }
     }
     return result;
   }
}




