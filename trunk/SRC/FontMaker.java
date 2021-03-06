
package org.sufrin.font;

import java.awt.Font;
import java.io.File;
import java.net.URL;
import java.util.Hashtable;

import org.sufrin.dred.Dred;
import org.sufrin.logging.Logging;

/**
   A static font factory set up to decode font names in one
   of the forms:
 
   <pre>
     filename
     filename-pointsize
     filename-pointsize-options
  </pre>
  
  Where there can be up to three options, each of one of the forms
  <pre>
     b                  -- make the font bold
     i                  -- make the font italic
     m                  -- simulate a monospaced (M-width) font
     uXXXX              -- (XXX are hex digits) simulate a monospaced font
                           where the width is the width of the character unicoded XXXX
  </pre>
  
  The pointsize can be a fixed or floating point number. If
  the filename ends in <tt>.ttf</tt> it is taken to
  describe a truetype font; otherwise it is taken to
  describe a type 1 font.
 */

public class FontMaker
{
  /** Logging for the current class */
  static Logging log = Logging.getLog("FontMaker");

  /**
   * Debugging is enabled for this class: true if the log
   * named Display has level FINE or above.
   */
  public static boolean debug = log.isLoggable("FINE");

  /**
   * Return the font corresponding to the given name
   */
  static public Font decode(String fontName, boolean ttf)
  {
    fontName = fontName.replaceAll("[ ]+", " ");
    Font f = fetchFont(fontName, ttf);
    log.fine("%s:%s => %s", ttf?"truetype":"type1", fontName, f);
    return f;
  }
  
  /** The fixed-width character for the font (if it's pseudofixed) */
  static public char fixedChar(String fontName, boolean ttf)
  {
    fontName = fontName.replaceAll("[ ]+", " ");
    fetchFont(fontName, ttf);
    return fixedWidthChars.get(fontName);
  }

  /** Normalized font name to font mapping */
  static Hashtable<String, Font> fontmap = new Hashtable<String, Font>();
  
  /** Normalized font name to pseudofixed wodth */
  static Hashtable<String, Character> fixedWidthChars = new Hashtable<String, Character>();

  /** Build a font from a (normalized) font name: */
  static protected Font fetchFont(String name, boolean ttf)
  {
    Font result = fontmap.get(name);
    if (result == null)
    {
      result = makeFont(name, ttf);
      fontmap.put(name, result);
    }
    return result;
  }

  /**
   * Make and cache a font from the named font
   */
  static protected Font makeFont(String fontName, boolean ttf)
  { // System.err.println("Making: "+fontName);
    File     f           = new File(fontName);
    String[] fileAndOpts = f.getName().split("@");
    String   file        = new File(f.getParent(), fileAndOpts[0]).toString();
    String[] opts        = (fileAndOpts.length>1 ? fileAndOpts[1] : "14").split("-");
    float    size        = 14.0f;
    int      style       = Font.PLAIN;
    int      fixedWidthChar = 0;
    String   fixedWidthSpec = "";
    try
    {
      switch (opts.length)
      { 
        default: break;
        case 4:
          switch (opts[3].toLowerCase().charAt(0))
          {
            case 'i':
              style = Font.ITALIC;
            break;
            case 'b':
              style = Font.BOLD;
            break;
            case 'u':
              fixedWidthSpec = opts[3].substring(1);
            break;
            case 'f':
            case 'm':
              fixedWidthSpec = "004d";
            break;
            default:
          }
        case 3:
          switch (opts[2].toLowerCase().charAt(0))
          {
            case 'i':
              style = Font.ITALIC;
            break;
            case 'b':
              style = Font.BOLD;
            break;
            case 'u':
              fixedWidthSpec = opts[2].substring(1);
            break;
            case 'f':
            case 'm':
              fixedWidthSpec = "004d";
            break;
            default:
          }
        case 2:
          switch (opts[1].toLowerCase().charAt(0))
          {
            case 'i':
              style |= Font.ITALIC;
            break;
            case 'b':
              style |= Font.BOLD;
            break;
            case 'u':
              fixedWidthSpec = opts[1].substring(1);
            break;
            case 'm':
            case 'f':
              fixedWidthSpec = "004d";
            break;
            default:
          }
        case 1:
          size = Float.parseFloat(opts[0]);
      }
      if (!fixedWidthSpec.equals(""))
      try 
      { 
        fixedWidthChar = (char) Integer.parseInt(fixedWidthSpec, 16);
      }
      catch (Exception ex)
      {
        log.warning("Bad Unicode Character Description u:s", fixedWidthSpec);
      }
      fixedWidthChars.put(fontName, (char) fixedWidthChar);
      return fetchRawFont(file, ttf).deriveFont(style, size);
    }
    catch (Exception ex)
    { Dred.showWarning(String.format("Cannot load font %s (%s)\nLoading system default instead", fontName, ex.getMessage()));
      log.warning("Substituting system default font for: %s (%s)", fontName,
                  ex.getMessage());
      fixedWidthChars.put(fontName, (char) 0);
      return Font.decode("MONOSPACED 14");
    }
  }

  /**
   * Fetch a raw font from the given url
   */
  static protected Font fetchRawFont(String url, boolean ttf) throws Exception
  { if (!url.matches("[A-Za-z:]+:.*")) url = "file:"+url;
    if (ttf && !url.endsWith(".ttf") && !url.endsWith(".TTF"))
       url = url+".ttf";
    URL resource = new URL(url);
    Font result = fontmap.get(url);
    if (result == null)
    { // System.err.println("Fetching: "+url);
      int ty = ttf ? Font.TRUETYPE_FONT : Font.TYPE1_FONT;
      result = Font.createFont(ty, resource.openStream());
      fontmap.put(url, result);
    }
    return result;
  }
}




