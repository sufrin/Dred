package org.sufrin.dred;
import java.awt.Component;
import java.awt.Frame;
import java.util.prefs.Preferences;

import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;

/** This module coordinates Dred's current global Look and Feel setting.
 */
public class GUI
{
  /** Dred preferences */
  protected static Preferences prefs = Preferences.userRoot().node("Dred");
  
  /** Dred ''flat'' look and feel theme.  */
  protected static MetalTheme flatTheme = new MetalTheme();
  
  /** Name of the current L&F: starts as "standard" */
  protected static String currentLook = "standard";
      
  static
  {  
     setLookAndFeel((prefs.getBoolean("Flat L&F", false))? "flat" : "standard");
  }
  
  /** Change the L&F to the named one (if necessary) */
  synchronized public static void setLookAndFeel(final String lNf)
  { if (currentLook.equals(lNf)) return;
    currentLook = lNf;
    flatTheme.install(lNf.equals("flat")); 
    setLookAndFeel(new MetalLookAndFeel());
  }
  
  /** Change the L&F to the given one, and enqueue the changing of 
      existing (visible) components.
   */  
  protected static void setLookAndFeel(final LookAndFeel lNf)
  {  try
     {
        UIManager.setLookAndFeel(lNf);
     }
     catch (Exception ex)
     { 
        Dred.showWarning("Cannot set Look & Feel");
     }
     for (Component f: Frame.getFrames()) updateComponent(f);
  }
  
  public static void updateComponent(final Component frame)
  {
       SwingUtilities.invokeLater
       (new Runnable()
        { public void run()
          {
            try   
            { 
               SwingUtilities.updateComponentTreeUI(frame);
            }
            catch (Exception ex) 
            { System.err.printf("[Dred: %s while changing L&F]%n", ex.toString());  
            } 
          }      
        }); 
  }
  
  public static LookAndFeel standardLnF()
  { String s = UIManager.getCrossPlatformLookAndFeelClassName();
    try
    { return (LookAndFeel) Class.forName(s).newInstance();
    }
    catch (Exception ex)
    { return UIManager.getLookAndFeel(); }
  }
}




