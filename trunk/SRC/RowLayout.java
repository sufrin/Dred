package GUIBuilder;
/** Row layout -- all components are their preferred width, and
    height is the maximum preferred height of the components.
    Extra width is distributed between the glue components.
    Ordinary components can be treated as glue, if required.
    
    @version $Id$
*/
import  java.awt.*;
import  javax.swing.*;

public class RowLayout implements LayoutManager
{       
        
        public RowLayout()            { this.just=0.0; }
        
        /**
                Make a row layout with extra height above of under-height
                components premultiplied by <code>just</code>;
                if <code>just</code> is negative, then underheight
                components are stretched to fill the container's height.

                If <code>stretch</code> is true then underwidth components
                are treated as if they were glue: any extra space in
                the container is distributed equally among them.

                The method fix(n) overrides the treatment of component
                n as glue.

                <p> Constraint-solving would, of course, have been better,
                but I just couldn't get to grips with the SpringLayout
                documentation in the time I had available.  </p>

        */
        public RowLayout(double just, boolean stretch) { this.just = just; this.stretch=stretch; }
        
        /**
                Make a non-stretched RowLayout
        */
        public RowLayout(double just)                  { this.just = just; }
        
        /**
             Multiplier for extra space to put above an under-height component.   
        */
        protected double 
                just;  
                
        protected boolean
                stretch = false;    

        protected int 
                glue           = 0,  // Number of glue components
                preferredwidth = 0;  // Preferred width 
                
        protected long 
                fixedsize = 0;      // bitmap representation of fixed-width component #s
        
        /** Always treat component n (0<=n<63) as a fixed-width component */
        public RowLayout fix(int n) 
        { fixedsize |= (1<<n);
          return this;
        }
        
        protected boolean isFixed(int n) 
        { return ((1<<n)&fixedsize) != 0;
        }
                
        public void addLayoutComponent (String name, Component c) { }
        public void removeLayoutComponent (Component c) { }
        
        public void layoutContainer(Container parent)
        { Insets insets = parent.getInsets();
          int width  = parent.getSize().width - (insets.left + insets.right);
          int height = parent.getSize().height - (insets.top + insets.bottom);
          int xtra   = glue==0?0:(width-preferredwidth)/glue;
          int comps = parent.getComponentCount();
          int x = insets.left;
          int y = insets.top;
          for (int i=0; i<comps; i++)
          { Component c = parent.getComponent(i);
            Dimension d = c.getPreferredSize();
            boolean fullHeight = c instanceof JSeparator || just<0.0;
            int tadj = fullHeight ? 0 : (int) (just*(height-d.height));
            int cht  = fullHeight ? height : d.height;
            int hadj = isGlue(c) && !isFixed(i) ? xtra : 0;
            c.setBounds(x, y+tadj, d.width+hadj, cht);
            x += d.width+hadj;
          }
        }
        
        protected boolean isGlue(Component c)
        { Dimension p = c.getPreferredSize();
          Dimension m = c.getMinimumSize();
          Dimension x = c.getMaximumSize();
          return stretch || 
          (  c instanceof Box.Filler
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
            if (h<d.height) h=d.height;
            w += d.width;
            if (isGlue(c) && !isFixed(i) ) glue++;
          }
          preferredwidth=w;
          return new Dimension(w+insets.left+insets.right, h+insets.top+insets.bottom);
        }
        
        public Dimension minimumLayoutSize(Container parent)
        { return preferredLayoutSize(parent);
        }
}

















