package org.sufrin.dred;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.KeyStroke;

import org.sufrin.logging.Logging;

/**
        A <tt>Bindings</tt> encapsulates a list of <tt>Bindings.Binding</tt> objects.
        A <tt>Bindings.Binding</tt> object is essentially a tuple of strings. 
        See the Dred manual for details.       
*/
public class Bindings implements Iterable<Bindings.Binding>
{ Vector<Binding> bindings;

  static Logging log = Logging.getLog();
  
  public Iterator<Binding> iterator() { return bindings.iterator(); }
  
  public Vector<URL> readURLs = new Vector<URL>();
  
  public Vector<URL> getURLs() { return readURLs; }
  
  public Bindings()
  { bindings = new Vector<Binding>(); }

  protected Bindings(Vector<Binding> bindings)
  { this.bindings = bindings; }
  
  public void clear() { bindings.clear(); readURLs.clear(); }
  
  public boolean isEmpty() { return bindings.isEmpty(); }
  
  /** Strip a trailing unescaped # .... from the String s */
  public static String stripComment(String s)
  { StringBuilder b = new StringBuilder();
    int i=0;
    int l=s.length();
    while (0<l && (s.charAt(l-1)==' ' || s.charAt(l-1)=='\t')) l--;
    while (i<l && (s.charAt(i)==' ' || s.charAt(i)=='\t')) i++;
    while (i<l)
    {  char c = s.charAt(i);
       if (c=='#') break;
       else
       if (c=='\\' && i+1<l && s.charAt(i+1)=='#')
       { b.append("\\#");
         i++;
       }
       else 
         b.append(c);
       i++;
    }
    return b.toString();
  }
  
  /** Turn escapes in the String s into the characters they denote */
  public static String stripEscapes(String s)
  { StringBuilder b = new StringBuilder();
    int i=0;
    int l=s.length();
    while (i<l)
    { if (s.charAt(i)=='\\' && i+1<l)
      switch (s.charAt(i+1))
      { case '\\': b.append("\\"); i++; break;
        case '#' : b.append("#");  i++; break;
        case 't' : b.append("\t"); i++; break;
        case 'n' : b.append("\n"); i++; break;
        case 's' : b.append(" ");  i++; break;
        case 'u' : 
        case 'U' : 
        if (i+5<l) 
        { String n = s.substring(i+2, i+6);
          char   c = (char) Integer.parseInt(n, 16);
          b.append(c);
          i+=6;
          break;
        }
        default:
        { b.append('\\'); 
          b.append(s.charAt(i+1)); 
          i++;
        }      
      }
      else
        b.append(s.charAt(i));
      i++;
    }
    return b.toString();
  }

  public void read(URL url) throws IOException
  { read(url.openStream(), url);
    readURLs.add(url);
  }
    
  public void read(InputStream is, URL url) throws IOException
  { LineNumberReader reader = null;
    try 
    { reader = new LineNumberReader(new InputStreamReader(is, "UTF8")); 
      String line = null;
      while ((line=reader.readLine())!=null)
      { line = stripComment(line);
        if (!line.equals("")) 
        { String[] fields = line.split("[ \t]+");
          for (int i=0; i<fields.length; i++) fields[i]=stripEscapes(fields[i]);
          Binding binding = new Binding(fields);
          bindings.add(binding);
          
          if (fields.length==2 && fields[0].equalsIgnoreCase("include"))
             read(new URL(url, fields[1]));
          else
          if (fields.length==2 && fields[0].equalsIgnoreCase("include?"))
             try { read(new URL(url, fields[1])); } catch (Exception ex) {}
          else
          if (fields.length>1 && fields[0].equalsIgnoreCase("show"))
             System.err.println("[Dred: "+binding.getFields(1)+"]");
        }
      }
    }
    catch (Exception ex) 
    { throw new RuntimeException(ex); }
    finally 
    { try { is.close(); } catch(Exception ex) {} }
  }
  
  public Bindings filter(String ... prefix)
  { return filterAll(prefix); }
  
  public Bindings filterAll(String [] prefix)
  { Vector<Binding> result = new Vector<Binding>();
    for (Binding binding: bindings)
        if (binding.matchesAll(prefix)) result.add(binding);
    return new Bindings(result);
  }
  
  public void printTo(PrintStream out)
  { for (Binding binding: bindings)
    { 
      out.println(binding.toString());
    }            
  }
    
  public static class Binding
  { String[] fields;
    public Binding(String[] fields) { this.fields = fields; }
    
    public String toString()
    { String s = "";
      for (String f:fields) s=s+" "+f;
      return s;
    }
    
    public int length() { return fields.length; }
    
    public boolean matches(Binding other) { return matchesAll(other.fields); }
    
    public boolean matches(String ... prefix) { return matchesAll(prefix); }
    
    public boolean matchesAll(String[] prefix)
    { if (prefix.length>fields.length) return false;
      for (int i=0; i<prefix.length; i++) 
          if (!prefix[i].equalsIgnoreCase(fields[i])) return false;
      return true;
    }

    public String getField(int n) { return fields[n]; }
    
    public String optField(int n) { return n>=fields.length ? "" : fields[n]; }
    
    public String getFields(int n) 
    { StringBuilder b = new StringBuilder();
      while (n<fields.length) 
      { String field = fields[n++];
        b.append(field); 
        b.append(' '); 
      }
      b.setLength(b.length()-1);
      return b.toString();
    }
    
    public String toKey(int n)    { return toKey(n, fields); }
                
    static public String toKey(int n, String[] spec)
    { String s = " "+spec[spec.length-1].toUpperCase()
               .replace("[",           "OPEN_BRACKET")
               .replace("]",           "CLOSE_BRACKET")
               .replace("BACKSPACE",   "BACK_SPACE")
               .replace("BACKSLASH",   "BACK_SLASH")
               .replace("\\",          "BACK_SLASH")
               .replace("{",           "BRACELEFT")
               .replace("}",           "BRACERIGHT")
               ;
      while (n<spec.length-1) s=spec[n++]+" "+s;
      return s;   
    }
    
    static public String fromKey(String key)
    {
      return key.replace("pressed", " ")
      .replaceAll("[ ]+", " ")
      .replace("OPEN_BRACKET",  "[")
      .replace("CLOSE_BRACKET", "]")
      .replace("BACK_SLASH",    "\\")
      .replace("BACK_SPACE",    "BACKSPACE")
      .replace("BRACELEFT",     "{")
      .replace("BRACERIGHT",    "}")
      .trim();
    }
  
  }
  
  public static void main(String[] args) throws Exception
  { Bindings bindings = new Bindings();
    bindings.read(new FileInputStream("testbindings.txt"), new URL("tfile:estbindings.txt"));
    bindings.filterAll(args).printTo(System.err);   
    for (Binding b: bindings) 
    { KeyStroke k = KeyStroke.getKeyStroke(b.toKey(3));
      System.err.println(b.toKey(3) + " "+(k==null?"NULL":k.toString()));
    }
  }
}











