package org.sufrin.dred;
/**
        A transformation that gets applied to the selection. 
*/
public interface TextTransform
{ /** Transform the selection (*) 
<pre>
        ######**********
        **************
        *********@@@@
</pre>
  in the given context: leftcxt (#) is the unselected part of the top selection line,
  and rightcxt (@) is the unselected part of the botton selection line.
  */
  public String transform(String leftcxt, String sel, String rightcxt);
}
