
package org.sufrin.font;

import java.awt.Font;
import java.net.URL;
import java.util.Hashtable;

import org.sufrin.logging.Logging;

/**
 * A static font factory set up to decode font names in one
 * of the forms:
 * 
 * <pre>
 * 
 *  &lt;i&gt;filename&lt;/i&gt;
 *  &lt;i&gt;filename pointsize&lt;/i&gt;
 *  &lt;i&gt;filename pointsize&lt;/i&gt; 
 * <tt>
 * r
 * </tt>
 * 
 *  &lt;i&gt;filename pointsize&lt;/i&gt; 
 * <tt>
 * i
 * </tt>
 * 
 *  &lt;i&gt;filename pointsize&lt;/i&gt; 
 * <tt>
 * r i
 * </tt>
 * 
 *  &lt;i&gt;filename pointsize&lt;/i&gt; 
 * <tt>
 * i r
 * </tt>
 </pre>
 * 
 * The pointsize can be a fixed or floating point number. If
 * the filename ends in <tt>.ttf</tt> it is taken to
 * describe a truetype font; otherwise it is taken to
 * describe a type 1 font.
 */

public class FontMaker
{
  /** Logging for the current class */
  static Logging log = Logging.getLog("Display");

  /**
   * Debugging is enabled for this class: true if the log
   * named Display has level FINER or above.
   */
  public static boolean debug = log.isLoggable("FINE");

  /**
   * Return the font corresponding to the given name
   */
  static public Font decode(String fontName)
  {
    fontName = fontName.replaceAll("[ ]+", " ");
    return fetchFont(fontName);
  }

  /** Normalized font name to font mapping */
  static Hashtable<String, Font> fontmap = new Hashtable<String, Font>();

  /** Build a font from a (normalized) font name: */
  static protected Font fetchFont(String name)
  {
    Font result = fontmap.get(name);
    if (result == null)
    {
      result = makeFont(name);
      fontmap.put(name, result);
    }
    return result;
  }

  /**
   * Make and cache a font from the named font
   */
  static protected Font makeFont(String fontName)
  { // System.err.println("Making: "+fontName);
    String[] name = fontName.split("[ ]+");
    String file = null;
    float size = 14.0f;
    int style = Font.PLAIN;
    try
    {
      switch (name.length)
      {
        default:
        case 4:
          switch (name[3].toLowerCase().charAt(0))
          {
            case 'i':
              style = Font.ITALIC;
            break;
            case 'b':
              style = Font.BOLD;
            break;
            default:
          }
        case 3:
          switch (name[2].toLowerCase().charAt(0))
          {
            case 'i':
              style |= Font.ITALIC;
            break;
            case 'b':
              style |= Font.BOLD;
            break;
            default:
          }
        case 2:
          size = Float.parseFloat(name[1]);
        case 1:
          file = name[0];
      }
      return fetchRawFont(file).deriveFont(style, size);
    }
    catch (Exception ex)
    {
      log.warning("Substituting system default font for: %s (%s)", fontName,
                  ex.getMessage());
      return Font.decode(null);
    }
  }

  /**
   * Fetch a raw font from the given url
   */
  static protected Font fetchRawFont(String url) throws Exception
  {
    URL resource = new URL(url);
    Font result = fontmap.get(url);
    if (result == null)
    { // System.err.println("Fetching: "+url);
      int ty = url.endsWith(".ttf") ? Font.TRUETYPE_FONT : Font.TYPE1_FONT;
      result = Font.createFont(ty, resource.openStream());
      fontmap.put(url, result);
    }
    return result;
  }
}
