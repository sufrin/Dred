package org.sufrin.dred;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;
import java.awt.Color;

/**

        A way of customizing Metal GUIs
*/

public class MetalTheme extends DefaultMetalTheme
{
    public String getName() { return  name; }

    public String name = "Custom Metal Theme";
    
    static protected javax.swing.plaf.metal.MetalTheme standard = MetalLookAndFeel.getCurrentTheme();
    
    static public boolean isStandard() 
    { return MetalLookAndFeel.getCurrentTheme()==standard;
    }
    
    public void install(boolean state)
    {   
      MetalLookAndFeel.setCurrentTheme(state ? this : standard);
    }
    
    
    static
    public String[] colors = {"primary1",   "primary2",   "primary3", 
                              "secondary1", "secondary2", "secondary3", 
                              "primaryhighlight",
                              "controlhighlight", 
                              "black",     
                              "white"
                             };


    protected  ColorUIResource primary1     = super.getPrimary1();
    protected  ColorUIResource primary2     = super.getPrimary2();
    protected  ColorUIResource primary3     = super.getPrimary2();

    
    protected  ColorUIResource secondary1   = super.getSecondary1();
    protected  ColorUIResource secondary2   = super.getSecondary2();
    protected  ColorUIResource secondary3   = super.getSecondary3();
    
    protected  ColorUIResource theBlack   = super.getBlack();
    protected  ColorUIResource theWhite   = super.getWhite();

    protected  ColorUIResource primaryHighlight = super.getPrimaryControlHighlight();
    protected  ColorUIResource controlHighlight = super.getControlHighlight();
    { if (primaryHighlight==null) primaryHighlight = theBlack; 
      if (controlHighlight==null) controlHighlight = theBlack; 
    }
    
     
    protected ColorUIResource getPrimary1() { return primary1; }  
    protected ColorUIResource getPrimary2() { return primary2; } 
    protected ColorUIResource getPrimary3() { return primary3; } 
    
    public    ColorUIResource getPrimaryControlHighlight() { return primaryHighlight; }
    public    ColorUIResource getControlHighlight()        { return controlHighlight; }
    
    protected ColorUIResource getSecondary1() { return secondary1; }
    protected ColorUIResource getSecondary2() { return secondary2; }
    protected ColorUIResource getSecondary3() { return secondary3; }
    protected ColorUIResource getBlack()      { return theBlack; }
    protected ColorUIResource getWhite()      { return theWhite; }

    public void setColor(String name, Color color) 
    { ColorUIResource res = new ColorUIResource(color);
      if ("primary1".equals(name)) primary1=res; else
      if ("primary2".equals(name)) primary2=res; else
      if ("primary3".equals(name)) primary3=res; else
      if ("secondary1".equals(name)) secondary1=res; else
      if ("secondary2".equals(name)) secondary2=res; else
      if ("secondary3".equals(name)) secondary3=res; else
      if ("black".equals(name)) theBlack=res; else
      if ("white".equals(name)) theWhite=res; else
      if ("primaryhighlight".equals(name)) primaryHighlight=res; else
      if ("controlhighlight".equals(name)) controlHighlight=res; else
      throw new Error("Cannot set color "+name);
    }

    static public String[] fonts = 
    { "usertextfont", "menutextfont", "systemtextfont" };

    protected FontUIResource theUserTextFont = super.getUserTextFont();
    public    FontUIResource getUserTextFont() { return theUserTextFont; }
    protected FontUIResource theSystemTextFont = super.getSystemTextFont();
    public    FontUIResource getSystemTextFont() { return theSystemTextFont; }
    protected FontUIResource theMenuTextFont = super.getMenuTextFont();
    public    FontUIResource getMenuTextFont() { return theMenuTextFont; }

    public void setFont(String name, java.awt.Font font) 
    { FontUIResource res = new FontUIResource(font);
      if ("UserTextFont".equalsIgnoreCase(name)) theUserTextFont=res; else
      if ("SystemTextFont".equalsIgnoreCase(name)) theSystemTextFont=res; else
      if ("MenuTextFont".equalsIgnoreCase(name)) theMenuTextFont=res; else
      throw new Error("Cannot set font "+name);
    }

}








