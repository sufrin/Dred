package org.sufrin.dred;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.*;

public class WebBrowser
{

   protected JEditorPane pane;

   public WebBrowser(URL url)
   { this();
     showDocument(url);
   }
   
   public WebBrowser()
   {
      pane = new JEditorPane();
      pane.setEditable( false );
      pane.setContentType( "text/html" );

      pane.addHyperlinkListener
      (
         new HyperlinkListener()
         {  public void hyperlinkUpdate( HyperlinkEvent ev )
            {
               if ( ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED )
               {
                  if ( ev instanceof HTMLFrameHyperlinkEvent )
                  {  JEditorPane p = ( JEditorPane ) ev.getSource();
                     HTMLDocument doc = ( HTMLDocument ) p.getDocument();
                     doc.processHTMLFrameHyperlinkEvent( ( HTMLFrameHyperlinkEvent ) ev );
                  }
                  else
                     showDocument( ev.getURL() );
               }
            }
         } 
       );
   }

   public JComponent getComponent()
   {
      return pane;
   }
   
   public JComponent getScrolledComponent()
   {
      return new JScrollPane(pane);
   }

   public void showDocument( URL url )
   {
      try
      {
         pane.setPage( url );
      }
      catch( IOException ioex )
      {
         throw new RuntimeException( ioex );
      }
   }

   public void showHtmlText( String html )
   {
      pane.setText( html );
      pane.setCaretPosition(0);
   }
   
}


