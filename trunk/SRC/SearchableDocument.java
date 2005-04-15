package org.sufrin.dred;
import javax.swing.Timer;
import java.awt.Component;
import java.util.regex.*;
import org.sufrin.logging.Logging;
import org.sufrin.logging.Dialog;

public class SearchableDocument extends Document
{ 
   public SearchableDocument()
   { super(); }

   protected static Logging log   = Logging.getLog("SearchableDocument");
   public    static boolean debug = log.isLoggable("FINE");

   protected Pattern pattern;
   protected String  patternSource;
   protected String  replacement;
   protected String  replacementSource;

   /** Default initial modes of find and replace and bracket matching */
   public static boolean initLitFind=true, 
                         initLitRepl=true, 
                         initMatchBra=true,
                         initMatchLatex=true,
                         initMatchXML=true;
   
   /** Modes of find and replace and bracket matching */
   public boolean litFind=initLitFind, 
                  litRepl=initLitRepl, 
                  matchBra=initMatchBra,
                  matchLatex=initMatchLatex,
                  matchXML=initMatchXML;

   public String regexError="";
   public String regexError() { return regexError; }

   public boolean setPattern(String patternSource)
   { this.patternSource = patternSource;
     try
     {  if (litFind) 
            pattern = Pattern.compile(Pattern.quote(patternSource));
        else
            pattern = Pattern.compile(patternSource);
        regexError = "";
        return true;
     }
     catch (PatternSyntaxException ex)
     { if (debug) ex.printStackTrace();
       String pre  = patternSource.substring(0, ex.getIndex());
       String post = patternSource.substring(ex.getIndex());
       if (!post.equals("")) post="<>"+post;
       regexError = String.format("%s: %s%s", ex.getDescription(), pre, post);
       return false;
     }
   }
   
   public void setReplacement(String replacementSource)
   {
     this.replacementSource = replacementSource;
     replacement = litRepl ? Matcher.quoteReplacement(replacementSource) : replacementSource;
   }

   /** Result (with matchY) of the last successful pattern search of any kind, whether
       invoked internally or by a client. After a successful search for a 
       pattern we have:

   <PRE>
        lineAt(matchY).subSequence(match.start(), match.end()).matches(pattern)
   </PRE>

   */
   private   MatchResult match;

   /** Line number of the last successful search. */
   private   int         matchY;
   
   /** The component to be associated with error messages */
   Component parentComponent = null;
   /** Set the component to be associated with error messages */
   public void setParentComponent(Component parentComponent)
   { if (parentComponent==null)
         this.parentComponent=parentComponent;
   }
   
   /** Searching hasn't been interrupted */   
   boolean searchingOk = true;
   
   /** Upper bound on the time a search can proceed without confirmation */
   public int searchTimeLimit = 4;
   
   /** One-shot timer for searches */
   Timer timer = new Timer(0, new Act("Timer")
   {
      public void run() { interruptSearch();  }
   });
   { timer.setRepeats(false); }
   
   /** Set upper bound on the time a search can proceed without confirmation */
   synchronized public void setSearchTimeLimit(int secs)
   { 
      searchTimeLimit = secs;
      timer.setInitialDelay(1000*searchTimeLimit);
      timer.setDelay(1000*searchTimeLimit);
      timer.setRepeats(false);
      interruptSearch();
   }
   
   /** Stop the timer, re-enable searching, return the given value */
   synchronized boolean stopTimer(boolean value)
   { timer.stop();
     searchingOk = true;
     return value;
   }
   
   /** Start the timer and enable searching */     
   synchronized void startTimer() 
   { setSearchTimeLimit(searchTimeLimit);
     timer.start(); 
     searchingOk = true; 
   }
   
   /** Interrupt the running search */
   synchronized public void interruptSearch()
   { searchingOk = false;
     timer.stop();
   }
   
   static final String[] leaseOptions = { "Cancel", "Stop here", "Continue"};
   
   /** Inform the user that time has run out, and ask what to do */
   public boolean newLease(int x, int y)
   { switch (Dialog.showWarning(parentComponent, String.format("%d seconds' searching has reached line %d.\nWhat do you want to do?", timer.getDelay()/1000, y)
                               , 0, leaseOptions))
     { case 0: return false;
       case 1: setCursor(x, y); return false;
       case 2: startTimer();    return true;
     }
     return false;
   }

   /** Select (with the cursor to the right of the mark) the next
       instance of the document pattern after the cursor, if there is one.
       Return true iff successful;
   */
   public boolean downFind() { startTimer(); return stopTimer(downFind(pattern)); }

   /** Select (with the cursor to the left of the mark) the previous
       instance of the pattern before the cursor, if there is one.
       Return true iff successful;
   */
   public boolean upFind() { startTimer(); ; return stopTimer(upFind(pattern)); }


   /** Select (with the cursor to the right of the mark) the next
       instance of the given pattern after the cursor, if there is  one.
       Return true iff successful;
   */
   public boolean downFind(Pattern aPattern)
   { if (downSearch(aPattern))
     { super.setCursorAndMark(match.end(), matchY, match.start(), matchY);
       return true;
     }
     else
        return false;
   }
   
   
   /**
        Find the closest instance of pattern below the current position.
   */
   public boolean downSearch(Pattern aPattern) 
   { return downSearch(getX(), getY(), aPattern); }
   
   /**
        Find the closest instance of pattern below x,y.
   */
   public boolean downSearch(int x, int y, Pattern aPattern)
   { CharSequence r = lineAt(y);
     while (searchingOk || newLease(x, y))
     { if (searchFirst(x, aPattern, r))
       { matchY = y;
         return true;
       }
       y++;
       if (y<length())
       { r = lineAt(y);
         x = 0;
       }
       else
       return false;
     }
     return false;
   }
   
   /** Find the first occurence of the pattern after x in the sequence r, 
       and return true; return false if there is no such occurence.
   */
   protected boolean  searchFirst(int x, Pattern aPattern, CharSequence r)
   { Matcher m = aPattern.matcher(r);
     if (debug) log.finer("%s (%s)", aPattern, r);
     if (m.find(x))
     { match = m.toMatchResult();
       if (debug) log.fine("found %d %d", match.start(), match.end());
       return true;
     }
     else
       return false;
   } 

   
   public boolean upFind(Pattern aPattern)
   { if (upSearch(aPattern))
     { 
       super.setCursorAndMark(match.start(), matchY, match.end(), matchY);
       return true;
     }
     else
       return false;
   }

   /**         
      Find the closest instance of pattern above the current position.
   */
   public boolean upSearch(Pattern aPattern)
   { return upSearch(getX(), getY(), aPattern); }
   
   /**         
      Find the closest instance of pattern above the current position.
   */
   public boolean upSearch(int x, int y, Pattern aPattern)
   { CharSequence r = lineAt(y);
     while (searchingOk || newLease(x, y))
     { if (searchLast(x, aPattern, r))
       { matchY = y;
         return true;
       }
       y--;
       if (y>=0)
       { r=lineAt(y);
         x=r.length();
       }
       else
         return false;
     }
     return false;
   }

   /** Find the last occurence of the pattern before x in the sequence r,
       and return true; return false if there is no such occurence.
   */
   protected boolean searchLast(final int x, Pattern aPattern, CharSequence r)
   { r=r.subSequence(0, x); // BUG: m.region(0, x) seems not to work!
     Matcher m = aPattern.matcher(r);
     if (!m.find(0)) return false;
     
     // There's an instance somewhere in r
     int findStartX = m.start();
     int findEndX   = m.end();
     
     if (findEndX==findStartX) 
     { // Pattern matches empty, so truncate the search.
       log.warning("Pattern %s matches empty string", aPattern);
       match = new MatchResult()
       { public int end()           { return x; }
         public int end(int n)      { return x; }
         public int start()         { return x; }
         public int start(int n)    { return x; }
         public int groupCount()    { return 0; }
         public String group()      { return ""; }
         public String group(int n) { return ""; }
       };
       return true;
     }
     
     // Find the last instance
     match = m.toMatchResult();
     while (m.find(findEndX)) 
     { findStartX=m.start(); 
       findEndX=m.end();
       match=m.toMatchResult(); 
     }
     return true;
   } 

   /** Replace the current selection with the current replacement,
       providing the current selection matches the current pattern.
   */
   public String replace(boolean upwards)
   { if (pattern==null)
     { regexError = "No pattern";
       return null;
     }
     if (!hasSelection())
     { regexError = "No selection";
       return null;
     }
     String  s = getSelection();
     Matcher m = pattern.matcher(s); 
     if (debug) log.fine("%s selected", s);
     if (m.matches()) 
     { 
       try
       { String t=m.replaceAll(replacement);
         if (debug) log.fine("%s => %s", s, t);
         quietCutSelection();
         pasteAndSelect(t, !upwards);
         return s;
       }
       catch (IndexOutOfBoundsException ex)
       {
         regexError = ex.getMessage();
         return null;
       }
     }
     else
     { regexError = "Selection does not match pattern";
       return null;
     }
   }

   /** Replace all instances of the current pattern with the current replacement 
       within the current selection.
   */
   
   public String replaceAll()
   { if (pattern==null)
     { regexError = "No pattern";
       return null;
     }
     String  s = getSelection();
     Matcher m = pattern.matcher(s); 
     if (debug) log.fine("%s selected", s);
     try
     { String t=m.replaceAll(replacement);
       if (debug) log.fine("%s => %s", s, t);
       quietCutSelection();
       pasteAndSelect(t, false);
       return s;
     }
     catch (IndexOutOfBoundsException ex)
     {
       regexError = ex.getMessage();
       return null;
     }
   }
   
   /////////////////////  Pattern-specified bracket matching ////////////////

   /** True of there's an instance of the given pattern at the right of the given position */
   protected boolean atRight(int x, int y, Pattern aPattern)
   { CharSequence line = lineAt(y);
     line=line.subSequence(x, line.length());
     if (debug) log.finer("%d %d (%s) %s", x, y, aPattern, line);
     Matcher m = aPattern.matcher(line);
     boolean result = m.lookingAt();
     if (debug) log.finer("%s", m);
     return result;
   }
   
   /** If the cursor is positioned just before an instance of bra, then
       move the mark to after the next matched closing instance of ket.
   */
   public boolean matchDown(String bra, String ket)
   { Pattern pbra = Pattern.compile(bra);
     Pattern pket = Pattern.compile(ket);
     if (matchDown(getX(), getY(), pbra, pket))  
     { setMark(match.end(), matchY); 
       return true; 
     }
     else
       return false;
   }
   
   /** 
     If there is an instance of bra just to the right of x,y then
     find the corresponding matched closing instance  of ket, if any, and
     set matchY and match at that instance; ie such that 
     
     lineAt(matchY).subSequence(match.start(), match.end()).matches(ket)
   */
   public boolean matchDown(int x, int y, Pattern bra, Pattern ket)
   { int n=0;
     Pattern both = Pattern.compile("("+bra.pattern()+")|("+ket.pattern()+")");
     if (!atRight(x, y, bra)) return false;
     while (downSearch(x, y, both)) 
     { 
       x=match.start();
       y=matchY;
       if (atRight(x, y, bra)) 
       { n++; x=match.end(); } 
       else
       { n--; x=match.end(); if (n==0) return true; }
     }
     return false;
   }
   
   /** A non-nesting variant of matchDown */
   public boolean skipDown(int x, int y, Pattern bra, Pattern ket)
   {
     if (!atRight(x, y, bra)) return false;
     return downSearch(x, y, ket);
   }
   
   /** A non-nesting variant of matchDown */
   public boolean skipDown(Pattern bra, Pattern ket)
   {
     if (skipDown(getX(), getY(), bra, ket))
     { setMark(match.end(), matchY);
       return true;
     }
     else
       return false;
   }
   
   /** A non-nesting variant of matchUp */
   public boolean skipUp(int x, int y, Pattern bra, Pattern ket)
   {
     if (!atLeft(x, y, ket)) return false;
     return upSearch(x, y, ket); 
   }
   
   /** A non-nesting variant of matchDown */
   public boolean skipUp(Pattern bra, Pattern ket)
   {
     if (skipUp(getX(), getY(), bra, ket))
     { setMark(match.start(), matchY);
       return true;
     }
     else
       return false;
   }
   
   /** True of there's an instance of the given pattern at the left of the given position */
   protected boolean atLeft(int x, int y, Pattern aPattern)
   { CharSequence line = lineAt(y);
     line=line.subSequence(0, x);
     if (debug) log.finer("%d %d (%s) %s", x, y, aPattern, line);
     Matcher m = aPattern.matcher(line);
     boolean result = m.lookingAt();
     if (debug) log.finer("%s", m);
     return result;
   }

   /** If the cursor is positioned just after an instance of ket, then
       move the mark upwards to the previous matched opening bra.
   */
   public boolean matchUp(String bra, String ket)
   { Pattern pbra = Pattern.compile(bra);
     Pattern pket = Pattern.compile(ket);
     if (matchUp(getX(), getY(), pbra, pket))  
     { setMark(match.start(), matchY); 
       return true; 
     }
     else
       return false;
   }
   
   /** If the cursor c is positioned just after an instance of ket, then
       move it upwards to the previous matched opening bra.
   */
   public boolean matchUp(int x, int y, Pattern bra, Pattern ket)
   { Pattern both = Pattern.compile("("+bra.pattern()+")|("+ket.pattern()+")");
     Pattern lket = Pattern.compile("(.*)("+ket.pattern()+")");
     int n=0;
     if (!atLeft(x, y, lket)) return false;
     while (upSearch(x, y, both)) 
     { x=match.end();
       y=matchY;
       if (atLeft(x, y, lket)) 
       { n++; x=match.start(); } 
       else
       { n--; x=match.start(); if (n==0) return true; }
     }
     return false;
   }
   
   //////////////// Recognition of bracketing constructs ////////////////
   
   /** If the character just before the cursor is a closing bracket,
       then move the mark upwards to the matching opening bracket. 
   */
   public boolean matchUp()
   { char c=current.leftChar();
     if (debug) log.finest("%c", c);
     switch(c)
     {  case ')': return matchUp('(', ')');
        case '}': return (matchLatex && matchUpLatexEnv()) || matchUp('{', '}');
        case ']': return matchUp('[', ']');
        case '>': return matchXML && matchUpXMLEnv();
        case '/': return matchUp("\\Q/*\\E", "\\Q*/\\E");
        default:  return false;
     }
   }
   
   /** If the character just after the cursor is an opening bracket,
       then move the mark downwards to the matching closing bracket. 
   */
   public boolean matchDown()
   {   char c=current.rightChar();
       switch(c)
       {  case '(':  return matchDown('(', ')'); 
          case '{':  return matchDown('{', '}'); 
          case '[':  return matchDown('[', ']'); 
          case '<':  return matchXML && matchDownXMLEnv(); 
          case '/':  return matchDown("\\Q/*\\E", "\\Q*/\\E");
          case '\\': return matchLatex && matchDownLatexEnv();
          default:   return false;
       }
   }

   public boolean matchDownLatexEnv()
   { 
     if (matchDown("\\\\begin\\{[^\\}]*\\}", "\\\\end\\{[^\\}]*\\}")) 
     { setMark(match.end(), matchY);
       return true;
     }
     else 
       return false;
   }
   
   public boolean matchUpLatexEnv()
   { if (matchUp("\\\\begin\\{[^\\}]*\\}", "\\\\end\\{[^\\}]*\\}"))  
     { setMark(match.start(), matchY);
       return true;
     }
     else 
       return matchUp('{', '}');
   }
   
   /////////////////////////// XML //////////////////////////////////////////
   
   // Horrors! This only works properly for one-line opentags. 
   // We'll need a proper lexer eventually.
   // Meanwhile we'll treat an opentag broken at a line boundary as an opentag
   
   String xopen  = "<\\w+[^<>]*[^/]>|<\\w>|<\\w+[^<>]*$",
          xclose = "</\\w+>|^\\s*/>$|^\\s*>$|^[^<>]*/>";
          
   Pattern cdopen  = Pattern.compile("\\Q<![CDATA[\\E"),
           cdclose = Pattern.compile("\\Q]]>\\E"),
           ccopen  = Pattern.compile("\\Q<!--\\E"),
           ccclose = Pattern.compile("\\Q-->\\E");
   
   public boolean matchDownXMLEnv()
   { 
     if (matchDown(xopen, xclose)) 
     { setMark(match.end(), matchY);
       return true;
     }
     else 
       return skipDown(ccopen, ccclose)
           || skipDown(cdopen, cdclose)
           || matchDown('<', '>');
   }
      
   public boolean matchUpXMLEnv()
   { if (matchUp(xopen, xclose))  
     { setMark(match.start(), matchY);
       return true;
     }
     else 
       return skipUp(ccopen, ccclose)
           || skipUp(cdopen, cdclose)
           || matchUp('<', '>');
   }

   /////////////// Specialised behaviour for insert and setCursor... //////////////

   /** Insert c in the document and move the mark to the matching
       opening bracket (if there is one)
   */
   public void insert(char c)
   { 
     super.insert(c);
     if (matchBra) { matchUp(); tentativeSelection(); }
   }

   /** Reposition cursor and mark, then move the mark to the matching closing bracket if
       the cursor is at an opening bracket. 

       Invoke this only when changing position with the mouse -- it attempts
       to find matching bracketing constructs.
   */
   public void setCursorAndMark(int x, int y)
   { super.setCursorAndMark(x, y);
     if (matchBra) 
     { if (matchUp() || matchDown()) 
          tentativeSelection();
     }     
   }

   /////////////////////// Fast character-specified bracket matching //////////////

   /** Move the mark downwards to the closest matched ket, if possible. */
   public boolean matchDown(char bra, char ket)
   { Cursor c = new Cursor(this);
     int n=0;
     while (!c.atEnd()) 
     { if (c.rightChar()==bra) n++; else
       if (c.rightChar()==ket) { n--; if (n==0) break; }
       c.moveRight(); 
     }
     if (c.rightChar()==ket) { c.moveRight(); setMark(c.x, c.y); return true; }
     return false;
   }
   
   /** Move the mark upwards to the closest matched bra, if possible. */
   public boolean matchUp(char bra, char ket)
   { Cursor c = new Cursor(this);
     int n=0;
     while (!c.atStart()) 
     { c.moveLeft(); 
       if (c.rightChar()==ket) n++; else
       if (c.rightChar()==bra) { n--; if (n==0) break; }
     }
     if (c.rightChar()==bra) { setMark(c.x, c.y); return true; }
     return false;
   }

   /** A cursor presents a sequential view of the characters
       (including newlines) in a specific document. The document
       must not be changed while the cursor is in use.
   */
   protected static class Cursor
   { public    int          x, y;
     protected Document     doc;
     /** Invariant: line.equals(doc.lineAt(y)) ??? 0???x???line.line.length() */
     protected CharSequence line;
     public Cursor(Document doc) { this(doc, false); }
     
     public Cursor(Document doc, boolean atMark)
     { this.doc=doc;
       if (atMark)
       { this.x=doc.getMarkX();
         this.y=doc.getMarkY();
         this.line=doc.lineAt(y);
       }
       else
       { this.x=doc.getX();
         this.y=doc.getY();
         this.line=doc.lineAt(y);
       }
     }


     /** Cursor is at the start of the document. */
     public boolean atStart() { return x<=0 && y<=0; }

     /** Cursor is at the end of the document. */
     public boolean atEnd()   { return y==doc.length()-1 && x==line.length(); }

     /** Move the cursor left by a character */
     public void moveLeft()   { if (x<=0) leftLine(); else x--; }


     /** Move the cursor right by a character */
     public void moveRight()  { if (x==line.length()) rightLine(); else x++; }

     /** Move the cursor right by n characters, if possible */
     public void moveRight(int n)  
     { while (n-->0) moveRight(); }

     /** Move the cursor left by n characters, if possible */
     public void moveLeft(int n)  
     { while (n-->0) moveLeft(); }

     /** Return the character to the left of the cursor (newline
         if the cursor is at the start of a line)
     */
     public char leftChar()   { return x<=0 ? '\n' : line.charAt(x-1); }

     /** Return the character to the right of the cursor (newline
         if the cursor is at the end of a line)
     */
     public char rightChar()  { return x<0 || x==line.length() ? '\n' : line.charAt(x); }
     
     /** Move to the end of the previous line, if possible */
     protected void leftLine()
     { if (y>0) 
       { y--;
         line=doc.lineAt(y);
         x=line.length()-1;
       }
     }
     
     /** Move to the start of the previous line, if possible */
     protected void rightLine()
     { if (y<doc.length()-1) 
       { y++;
         line=doc.lineAt(y);
         x=0;
       }
     }
   }
}


















