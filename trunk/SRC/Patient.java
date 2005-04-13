package org.sufrin.dred;
/** A Patient object is either waiting or not.
    If it's waiting it may decide to show that somehow.
*/
public interface Patient
{
   public void setWaiting(boolean state);
}

