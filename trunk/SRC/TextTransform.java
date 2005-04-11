package org.sufrin.dred;
/**
        A transformation that gets applied to a contextualised selection.
*/
public interface TextTransform
{ 

/** Transform the selection (S)
<pre>
        LLLLLLSSSSSSSSSS
        SSSSSSSSSSSSSS
        SSSSSSSSSRRRRRRRRRRRRR
</pre>
  in the given context: leftcxt (L) is the unselected part of the top selection line,
  and rightcxt (R) is the unselected part of the bottom selection line.
  */
  public String transform(String leftcxt, String sel, String rightcxt);
}

