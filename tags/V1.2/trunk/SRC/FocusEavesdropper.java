package org.sufrin.dred;
/**
        Interface satisfied by an object that needs to know when
        the focus changes.
*/
public interface FocusEavesdropper
{ /** Involed when the focus is gained by the given document */
  void focusGained(Document doc);
}
