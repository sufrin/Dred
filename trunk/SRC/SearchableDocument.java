package org.sufrin.dred;
import javax.swing.Timer;
import java.io.Reader;
import java.awt.Component;
import java.util.regex.*;
import org.sufrin.logging.Logging;
import org.sufrin.logging.Dialog;

/** A SearchableDocument extends a Document with facilities to search, 
    match brackets, etc.
*/
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
      
   /** Re-enable searching, return the given value */
   synchronized boolean stopWaiting(boolean value)
   { 
     searchingOk = true;
     for (DocListener listener: docListeners) listener.stopWaiting();
     return value;
   }
   
   /** Enable searching */     
   synchronized void startWaiting() 
   { 
     searchingOk = true; 
     for (DocListener listener: docListeners) listener.startWaiting();
   }
   
   /** Interrupt the running search */
   synchronized public void interruptSearch()
   { searchingOk = false;
   }
   
   static final String[] leaseOptions = { "Cancel", "Stop here", "Continue"};
   
   static final boolean dialogBugFix = true; 
   /** Inform the user that time has run out, and ask what to do
       using a Dialog.
       If <tt>dialogBugFix</tt>, then just return false. 
       TODO:
       There's an interaction with the cancellation machinery that I don't
       understand, yet. If we are in a state where we have (at any time in the past)
       interrupted the offline thread executor, the Dialog box always returns its
       default option. There is evidence that the Dialog box gets interrupted
       while it is loading its icon. I'm certain that I need a subtler way
       of interrupting the thread executor if this is ever to be fixed.       
   */
   public boolean newLease(int x, int y)
   { if (dialogBugFix) return stopWaiting(false);
     switch (Dialog.showWarning(parentComponent, String.format("Searching has reached line %d.\nWhat do you want to do?", y)
                               , 0, leaseOptions))
     { 
       case 1: setCursor(x, y); 
       case 0: return stopWaiting(false);
       case 2: stopWaiting(true);
     }
     return false;
   }

   /** Select (with the cursor to the right of the mark) the next
       instance of the document pattern after the cursor, if there is one.
       Return true iff successful;
   */
   public boolean downFind() { return (downFind(pattern)); }

   /** Select (with the cursor to the left of the mark) the previous
       instance of the pattern before the cursor, if there is one.
       Return true iff successful;
   */
   public boolean upFind() { return (upFind(pattern)); }


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
     startWaiting();
     while (searchingOk || newLease(x, y))
     { if (searchFirst(x, aPattern, r))
       { matchY = y;
         return stopWaiting(true);
       }
       y++;
       if (y<length())
       { r = lineAt(y);
         x = 0;
       }
       else
       return stopWaiting(false);
     }
     return stopWaiting(false);
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
     startWaiting();
     while (searchingOk || newLease(x, y))
     { if (searchLast(x, aPattern, r))
       { matchY = y;
         return stopWaiting(true);
       }
       y--;
       if (y>=0)
       { r=lineAt(y);
         x=r.length();
       }
       else
         return stopWaiting(false);
     }
     return stopWaiting(false);
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
       return (true); 
     }
     else
       return (false);
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
       return (true); 
     }
     else
       return (false);
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
   
   public boolean matchDownXMLEnv()
   { startWaiting();
     XmlLex scanner = new XmlLex(this);
     if (scanner.matchDown())
     { setMark(scanner.x, scanner.y);
       return stopWaiting(true);
     }
     else
       return matchDown('<', '>');
   }
   
   public boolean matchUpXMLEnv()
   { startWaiting();
     XmlLex scanner = new XmlLex(this);
     if (scanner.matchUp())
     { setMark(scanner.x, scanner.y);
       return stopWaiting(true);
     }
     else
       return matchUp('<', '>');
   }
   
   /** An ad-hoc, very forgiving, lexer & bracket-matcher for XML */
   public static class XmlLex extends Cursor
   { public XmlLex (SearchableDocument doc) { super(doc); }
     
     public static enum XMLSymbol { Comment, PI, Empty, Open, Close, CData, None; }
     
     char ch, ch3=0, ch2=0, ch1=0;
     
     void nextChar() 
     { moveRight(); ch3=ch2; ch2=ch1; ch1=ch; ch=doc.searchingOk?rightChar():'\000'; }
     
     void prevChar() 
     { moveLeft(); ch3=ch2; ch2=ch1; ch1=ch; ch=doc.searchingOk?leftChar():'\000'; }

     public boolean matchDown()
     { ch=rightChar();
       XMLSymbol symbol;
       int level = 0;
       do 
       { symbol=nextSymbol(); 
         if (symbol==null) return false; else
         switch (symbol)
         { case Comment: case PI: case Empty: case CData:
                if (level==0) return true;
           break;
           case Open: 
                level++;
           break;
           case Close:
                level--;
                if (level<=0) return true;
           break;
         }
       }
       while (true); 
     }
     
     public boolean matchUp()
     { ch=leftChar();
       XMLSymbol symbol;
       int level = 0;
       do 
       { 
         symbol=prevSymbol(); 
         if (symbol==null) return false; else
         switch (symbol)
         { case Comment: case PI: case Empty: case CData:
                if (level==0) return true;
           break;
           case Close: 
                level++;
           break;
           case Open:
                level--;
                if (level<=0) return true;
           break;
         }
       }
       while (true); 
     }
     
     public XMLSymbol result(XMLSymbol res)
     { if (res!=null) nextChar();
       return res;
     }
     
     public XMLSymbol presult(XMLSymbol res)
     { if (res!=null) prevChar();
       return res;
     }
       
     public XMLSymbol nextSymbol()
     { 
       while (ch!='\000' && ch!='<') nextChar();
       nextChar();
       switch (ch)
       { case '!': 
              nextChar();
              switch (ch)
              { default:
                   while (ch!=0 && !(ch=='>' && ch1=='-' && ch2=='-')) nextChar();
                   return result(ch==0?null : ch=='>' ? XMLSymbol.Comment : XMLSymbol.None);
                case '[':
                   while (ch!=0 && !(ch=='>' && ch1==']' && ch2==']')) nextChar();
                   return result(ch==0?null : ch=='>' ? XMLSymbol.CData : XMLSymbol.None);
              }
         case '?': nextChar();
                   while (ch!=0 && !(ch=='>' && ch1=='?')) nextChar();
                   return result(ch==0?null : ch=='>' ? XMLSymbol.PI : XMLSymbol.None);
         case '/': 
            nextChar();
            while (ch!=0 && !(ch=='>')) nextChar();
            return result(ch==0?null : ch=='>' ? XMLSymbol.Close : XMLSymbol.None);
         default:
          nextChar();
          while (ch!=0 && !(ch=='>')) nextChar();
          return result(ch==0?null : ch=='>' ? (ch1=='/' ? XMLSymbol.Empty : XMLSymbol.Open) : XMLSymbol.None);
       }
     }
     
     public XMLSymbol prevSymbol()
     { 
       while (ch!='\000' && ch!='>') prevChar();
       prevChar();
       switch (ch)
       { case '/': 
              do { prevChar(); } while (ch!=0 && ch!='<');
              return presult(ch==0 ? null : XMLSymbol.Empty);
         case '-':
              do { prevChar(); } while (ch!=0 && !(ch=='<' && ch1=='!' && ch2=='-' && ch3=='-'));
              return presult(ch==0 ? null : XMLSymbol.Comment);
         case ']':
              do { prevChar(); } while (ch!=0 && !(ch=='<' && ch1=='[' && ch2=='C'));
              return presult(ch==0 ? null : XMLSymbol.CData);
         case '?':
              do { prevChar(); } while (ch!=0 && !(ch=='<' && ch1=='?'));
              return presult(ch==0 ? null : XMLSymbol.PI);             
         default:
              do { prevChar(); } while (ch!=0 && ch!='<');
              return presult(ch==0 ? null : ch1=='/' ? XMLSymbol.Close : XMLSymbol.Open);
       }          
     }
   }   
      
   /////////////// Specialised behaviour for insert and setCursor... //////////////
   
   Runnable matchUpJob = new Runnable()
   {
      public void run() { matchUp(); tentativeSelection(); }   
   };
   
   Runnable matchUpDownJob = new Runnable()
   {  public void run() 
      { if (matchUp() || matchDown())  tentativeSelection();
      }    
   };

   /** Insert c in the document and move the mark to the matching
       opening bracket (if there is one)
   */
   public void insert(char c)
   { 
     super.insert(c);    
     if (matchBra) ActionMethod.Action.execute(matchUpJob);
   }

   /** Reposition cursor and mark, then move the mark to the matching closing bracket if
       the cursor is at an opening bracket. 

       Invoke this only when changing position with the mouse -- it attempts
       to find matching bracketing constructs.
   */
   public void setCursorAndMark(int x, int y)
   { super.setCursorAndMark(x, y);     
     if (matchBra) ActionMethod.Action.execute(matchUpDownJob);     
   }

   /////////////////////// Fast character-specified bracket matching //////////////

   /** Move the mark downwards to the closest matched ket, if possible. */
   public boolean matchDown(char bra, char ket)
   { Cursor c = new Cursor(this);
     startWaiting();
     int n=0;
     while (searchingOk && !c.atEnd()) 
     { if (c.rightChar()==bra) n++; else
       if (c.rightChar()==ket) { n--; if (n==0) break; }
       c.moveRight(); 
     }
     if (c.rightChar()==ket) { c.moveRight(); setMark(c.x, c.y); return stopWaiting(true); }
     return stopWaiting(false);
   }
   
   /** Move the mark upwards to the closest matched bra, if possible. */
   public boolean matchUp(char bra, char ket)
   { Cursor c = new Cursor(this);
     startWaiting();
     int n=0;
     while (searchingOk && !c.atStart()) 
     { c.moveLeft(); 
       if (c.rightChar()==ket) n++; else
       if (c.rightChar()==bra) { n--; if (n==0) break; }
     }
     if (c.rightChar()==bra) { setMark(c.x, c.y); return stopWaiting(true); }
     return stopWaiting(false);
   }

   /** A cursor presents a sequentially streamed view of the characters
       (including newlines) in a specific document. The document
       must not be changed while the cursor is in use.
   */
   protected static class Cursor
   { public    int          x, y;
     protected SearchableDocument     doc;
     /** Invariant: line.equals(doc.lineAt(y)) /\ 0&lt;=x&lt;=line.length() */
     protected CharSequence line;
     public Cursor(SearchableDocument doc) { this(doc, false); }
     
     public Cursor(SearchableDocument doc, boolean atMark)
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
     public char leftChar()   { return atStart() ? '\000' : x<=0 ? '\n' : line.charAt(x-1); }

     /** Return the character to the right of the cursor (newline
         if the cursor is at the end of a line)
     */
     public char rightChar()  { return atEnd() ? '\000' : (x<0 || x==line.length()) ? '\n' : line.charAt(x); }
     
     /** Move to the end of the previous line, if possible */
     protected void leftLine()
     { if (y>0) 
       { y--;
         line=doc.lineAt(y);
         x=line.length();
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

























