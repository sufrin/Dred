
package org.sufrin.dred;

import java.nio.*;

/**
 * An extensible character buffer.
 * 
 * <PRE>
 * 
 * $Id$
 * 
 * </PRE>
 <PRE>
 * 
 * Represents: chars: &lt;char&gt; Invariant: 0<=l<=r<=buffer.length
 * length = l+buffer.length-r 0 <= position <= length chars =
 * buffer to l ++ buffer from r length = #chars
 * 
 * buffer: xxxxxxxxxxxxxxxxxx) GAP )xxxxxxxxxxxxxx) l r
 * length
 * 
 * </PRE>
 * 
 */

public class Buffer
{
  /** How much to grow when about to overflow. */
  protected int quantum = 20;

  /** Character storage. */
  protected char[] buffer;

  /**
   * Left and right ends of the gap; current position;
   * current length.
   */
  protected int l, r, length, position;

  /** Construct a buffer with the given initial capacity. */
  public Buffer(int initialsize)
  {
    buffer = new char[initialsize];
    clear();
  }

  /** Empty the buffer (without shrinking it) */
  public void clear()
  {
    l = 0;
    r = buffer.length;
    length = 0;
    position = 0;
  }

  /** Ensure that the gap is of size at least n */
  void makeSpace(int n)
  {
    if (n > r - l)
    {
      char[] newbuf = new char[buffer.length + n];
      System.arraycopy(buffer, 0, newbuf, 0, l);
      System.arraycopy(buffer, r, newbuf, r + n, buffer.length - r);
      buffer = newbuf;
      r = r + n;
    }
  }

  /**
   * Insert the given string into the buffer at the
   * position, and move the position to just after it.
   */
  public void insString(String s)
  {
    for (int i = 0; i < s.length(); i++)
      insert(s.charAt(i));
  }

  /**
   * Insert the given character into the buffer at the
   * position, and move the position to just after it.
   */
  public void insert(char c)
  {
    syncPosition();
    makeSpace(quantum);
    buffer[l++] = c;
    length++;
    position++;
  }

  /**
   * Set the buffer to hold exactly the given string, with
   * the position just after it.
   */
  public void setBuffer(String s)
  {
    clear();
    makeSpace(quantum + s.length());
    length = s.length();
    for (int i = 0; i < length; i++)
      buffer[l++] = s.charAt(i);
    position = length;
  }

  /** Move the position leftwards a character, if possible */
  public void leftMove()
  {
    if (position != 0) position--;
  }

  /** Move the position rightwards a character, if possible */
  public void rightMove()
  {
    if (position != length) position++;
  }

  /**
   * Delete the character to the left of the position and
   * move the position leftwards, if possible.
   */
  public void leftDel()
  {
    syncPosition();
    if (l != 0)
    {
      l--;
      length--;
      position--;
    }
  }

  /**
   * Delete the character to the right of the position and
   * move the position rightwards, if possible.
   */
  public void rightDel()
  {
    syncPosition();
    if (r != buffer.length)
    {
      r++;
      length--;
    }
  }

  /** Move the position to the given place, if possible. */
  public void moveTo(int aPosition)
  {
    assert 0 <= aPosition && aPosition <= length : "Cannot moveTo position outside buffer";
    this.position = aPosition;
  }

  /** Move the gap to the current position */
  final private void syncPosition()
  {
    while (l > position)
      buffer[--r] = buffer[--l];
    while (l < position)
      buffer[l++] = buffer[r++];
  }

  /** Is the position at the left end? */
  public boolean atLeft()
  {
    return position == 0;
  }

  /** Is the position at the right end? */
  public boolean atRight()
  {
    return position == length;
  }

  /** Return the current position. */
  public int getPosition()
  {
    return position;
  }

  /**
   * ************** CharSequence views ***********************
   */

  /**
   * Characters to the left of the position, viewed as a
   * charsequence.
   */
  public CharSequence leftSeq()
  {
    while (l < position)
      buffer[l++] = buffer[r++];
    return CharBuffer.wrap(buffer, 0, position);
  }

  /**
   * Characters to the right of the position, viewed as a
   * charsequence.
   */
  public CharSequence rightSeq()
  {
    while (l > position)
      buffer[--r] = buffer[--l];
    return CharBuffer.wrap(buffer, r, buffer.length - r);
  }

  /**
   * ************** CharSequence implementation **********************
   */

  /** Return the current length */
  public int length()
  {
    return length;
  }

  /** Return the representation as a string. */
  public String toString()
  { // assemble a new string
    StringBuilder result = new StringBuilder(leftSeq());
    result.append(rightSeq());
    return result.toString();
  }

  /**
   * Return the subsequence of characters between the given
   * limits.
   */
  public CharSequence subSequence(int start, int end)
  {
    assert start <= length : "start should be less than end.";
    CharSequence result;
    if (end <= l)
      result = CharBuffer.wrap(buffer, start, end - start);
    else if (start >= l)
      result = CharBuffer.wrap(buffer, r + start - l, end - start);
    else 
      result = toString().subSequence(start, end); 
    // this should be rather rare
    return result;
  }

  /** Return the nth character. */
  public char charAt(int n)
  {
    return n < l ? buffer[n] : buffer[n + buffer.length - length];
  }

  /**
   * Return a representation of the current state (for
   * debugging)
   */
  public String stateToString()
  {
    String mid = "<" + position + "/" + l + ">";
    StringBuilder res = new StringBuilder();
    for (int i = 0; i < position; i++)
      res.append(charAt(i));
    res.append(mid);
    for (int i = position; i < length(); i++)
      res.append(charAt(i));
    res.append("[" + length() + "]");
    return res.toString();
  }
}

