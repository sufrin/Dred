package org.sufrin.dred;
/**
        A DocListener expects to be informed when there is a change
        in the content, the cursor, or the selection of the doc
        to which it is listening.  For the moment listeners are
        only interested in the first and last lines of the scope
        of a change.

        <PRE>$Id: DocListener.java,v 1.2 2005/01/04 18:50:02 sufrin Exp $</PRE>
*/
public interface DocListener
{
  void docChanged(int first, int last);    
  void cursorChanged(int first, int last); 
  void selectionChanged(int first, int last); 
}



