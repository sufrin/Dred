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
   
   static protected int      ringSize = 20, last = 0;
   static protected String[] cutRing = new String[ringSize];
   
   static int    getRingSize() { return ringSize; }

   static String get(int i)
   { 
     return cutRing[(last+i) % ringSize];
   }
      
   static public void setRingSize(int _ringSize) 
   { if (_ringSize<ringSize) return;
     String[] _cutRing = new String[_ringSize];
     for (int i=0; i<ringSize; i++) _cutRing[i] = get(i);
     ringSize = _ringSize; 
     cutRing  = _cutRing;     
   }
   
   static public void addToRing(String sel)
   { cutRing[last] = sel;
     last = (last+1) % ringSize;
   }

}



