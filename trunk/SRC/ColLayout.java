package GUIBuilder;
/** Column layout -- all components are their preferred height, and
    width is the maximum preferred width of the components.
    Extra height is distributed between the glue components.
    Ordinaty components can be treated as glue if required.
    
    @version $Id$
*/

import  java.awt.*;
import  javax.swing.*;

public class ColLayout implements LayoutManager
{       
        public void addLayoutComponent (String name, Component c) { }
        public void removeLayoutComponent (Component c) { }

        public ColLayout()              { this(0.0); }

        /**
                Make a column layout with extra width at left
                of under-width components premultiplied by <code>just</code>;
                if <code>just</code> is negative, then underwidth components are
                stretched to fill the container's width.
                
                If <code>stretch</code> is true then all components are
                trated as if they were glue, and extra vertical space
                is distributed accordingly.
        */
        public ColLayout(double just, boolean stretch)   { this.just = just; this.stretch=stretch; }
        
        /** Make a non-stretch ColLayout */
        public ColLayout(double just)   { this.just = just; }

        /**
             Multiplier for extra space to put at left of under-width component.   
        */
        protected double 
                just;
                
        protected boolean
                stretch = false;

        protected int 
                glue            = 0, // glue-component count
                preferredheight = 0; 
        
        public void layoutContainer(Container parent)
        { Insets insets = parent.getInsets();
          int width  = parent.getSize().width - (insets.left + insets.right);
          int height = parent.getSize().height - (insets.top + insets.bottom);
          int xtra   = glue==0?0:(height-preferredheight)/glue;
          int comps = parent.getComponentCount();
          int x = insets.left;
          int y = insets.top;
          for (int i=0; i<comps; i++)
          { Component c = parent.getComponent(i);
            Dimension d = c.getPreferredSize();
            boolean fullWidth = c instanceof JSeparator || just<0.0;
            int ladj = fullWidth ? 0 : (int) (just*(width-d.width));
            int cwid = fullWidth ? width : d.width;
            int vadj = isGlue(c) ? xtra : 0;
            c.setBounds(x + ladj, y, cwid,  d.height+vadj);
            y += d.height+vadj;
          }
        }

        protected boolean isGlue(Component c)
        { Dimension p = c.getPreferredSize();
          Dimension m = c.getMinimumSize();
          Dimension x = c.getMaximumSize();
          return stretch ||
          (c instanceof Box.Filler 
             && x.height==Short.MAX_VALUE 
             && x.width==Short.MAX_VALUE 
             && p.width==0 
             && m.width==0 
             && p.height==0 
             && m.height==0);
        }
        
        public Dimension preferredLayoutSize(Container parent)
        { Insets insets = parent.getInsets();
          int comps = parent.getComponentCount();
          int w = 0;
          int h = 0;
          glue = 0;
          for (int i=0; i<comps; i++)
          { Component c = parent.getComponent(i);
            Dimension d = c.getPreferredSize();
            if (w<d.width) w=d.width;
            h += d.height;
            if (isGlue(c)) glue++;
          }
          preferredheight=h;
          return new Dimension(w+insets.left+insets.right, h+insets.top+insets.bottom);
        }
        
        public Dimension minimumLayoutSize(Container parent)
        { return preferredLayoutSize(parent);
        }
}

















