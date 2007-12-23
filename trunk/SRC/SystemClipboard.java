package org.sufrin.dred;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;

/**
        SystemClipboard provides access to the system Clipboard.
*/
public class SystemClipboard
{
   static Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
   
   static DataFlavor unicodeText = DataFlavor.getTextPlainUnicodeFlavor();

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
     
     
     if ( tx!=null && tx.isDataFlavorSupported (unicodeText) )
     try
     { 
         LineNumberReader r = new LineNumberReader(unicodeText.getReaderForText(tx));
         StringBuilder    b = new StringBuilder();
         String line = null;
       try
       {
         while ((line = r.readLine())!=null) 
         { b.append(line);
           b.append("\n");
         }
       }
       catch (Exception ex)
       { }
       return b.toString();
     }
     catch (Exception ex)
     { }
     if ( tx!=null && tx.isDataFlavorSupported (DataFlavor.stringFlavor) )
     { try
       { result = (String) tx.getTransferData(DataFlavor.stringFlavor);
         result = result.replace('\u0004', '\n');
       }
       catch (Exception ex)
       { }
     }
     return result;
   }
}





