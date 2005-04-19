/** VERY forgiving XML (forwards) Lexer.
    This isn't used in Dred. I used it to figure out the heuristics
    for determining XML symbols.
*/
package org.sufrin.dred;
import java.io.*;
%%
%class XmlLex
%public
%function nextSymbol
%type XMLSymbol
%unicode
%line
%column

%{
        public static void main(String[] args) throws Exception
        { XmlLex scanner = new XmlLex(System.in);
          XMLSymbol symbol;
          do 
          { symbol=scanner.nextSymbol();
            System.out.println(scanner.line+"."+scanner.col+" "+symbol); 
          }
          while (symbol!=null); 
        }
        
        int line, col;
        
        public static enum XMLSymbol { Comment, PI, Empty, Open, Close; }
        
        public boolean matchDown()
        { 
          XMLSymbol symbol;
          int level = 0;
          do 
          { try
            { symbol=nextSymbol(); }
            catch (IOException ex)
            {
              return false;
            }
            System.out.println(line+"."+col+" "+symbol); 
            if (symbol==null) return false; else
            switch (symbol)
            { case Comment: case PI: case Empty: 
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
          while (symbol!=null); 
          return false;
        }
               
%}

%state STRING, COMMENT

tag             =       [:jletter:][:jletterdigit:]*
WHITESPACE      =       [\n\t\f ]  

%% 

<YYINITIAL>
{       "<!--" [^>]* "-->"       { line=yyline; col=yycolumn; return XMLSymbol.Comment; }
        "<?"  {tag}  [^>]* "?>"  { line=yyline; col=yycolumn; return XMLSymbol.PI; }
        "<"   {tag}  [^>]* "/>"  { line=yyline; col=yycolumn; return XMLSymbol.Empty; }
        "<"   {tag}  [^>]* ">"   { line=yyline; col=yycolumn; return XMLSymbol.Open; }
        "</"  {tag}  ">"         { line=yyline; col=yycolumn; return XMLSymbol.Close; }
        {WHITESPACE}             {}
}

