package org.sufrin.dred;
import java.awt.event.ActionEvent;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.KeyStroke;
/**     
        <tt><p>@ActionMethod</tt> annotations are placed on void methods
        that are intended to be invokeable from a user interface.</p>


        <p>An <tt>ActionMethod.Map</tt> extends the functionality of
        an ActionMap by providing a method <tt>register(Object o)</tt>
        that registers all the <tt>@ActionMethod</tt>-annotated methods
        of that object in their own name as AbstractAction objects. The
        actionPerformed method of the registered action invokes the
        annotated method.</p>
*/
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME) public @interface ActionMethod  
{
    String tip()    default "";    
    String label()  default "";
       
    public static class Action extends AbstractAction implements Comparable
    { public Action(Object object, Method method, ActionMethod act) 
      { super(act.label()); 
        this.method = method; 
        this.object = object;
        this.act    = act;
        
        if (!act.tip().equals("")) putValue(AbstractAction.SHORT_DESCRIPTION, act.tip());
      }
    
      final Method       method;
      final Object       object;
      final ActionMethod act;
      
      Set<KeyStroke> activatingKeys = new HashSet<KeyStroke>();
     
      public void activatedBy(KeyStroke key) { activatingKeys.add(key); }
      
      public String  activatedByMenu = null;
      public boolean isActivatedByMenu()    { return activatedByMenu!=null; }
      public void    setMenu(String menu)   { activatedByMenu = menu; }
      public String  getMenu()              { return activatedByMenu; }
      
      public String toString()
      { 
        return activatingKeys.toString()
                             .replace(",", " or ")
                             .replace("[", "")
                             .replace("]","")+" "+act.tip();
      }
      
      public String getName() { return method.getName(); }
      
      public String getLabel() { return act.label(); }
      
      public String getTip() { return act.tip(); }
      
      public Set<String> getKeyNames()
      {
        Set<String> r = new TreeSet<String>();
        for (KeyStroke k: activatingKeys) r.add(k.toString().replace("pressed", "").trim());
        return r;
      }
      
      public int compareTo(Object action) { return compareTo((Action) action); }
      
      public int compareTo(Action action) { return getName().compareTo(action.getName()); }
      
      public Set<KeyStroke> getKeyStrokes()
      {
        return activatingKeys;
      }
      
      public void actionPerformed(ActionEvent ev)                     
      {
         try   { method.invoke(object); } 
         catch (IllegalAccessException ex)    {}
         catch (InvocationTargetException ex) {}
      }      
    }
    
    public static class Map extends ActionMap
    { public Map()                    {}    
      public Map(final Object object) { register(object); }
            
      public static java.util.Map<String, ActionMethod> getActionMethods
                                                        (Class klass, boolean declared)
      { java.util.Map<String, ActionMethod> methods = new TreeMap<String, ActionMethod>();
        for (Method method: declared ? klass.getDeclaredMethods() : klass.getMethods())
        {  for (Annotation annotation: method.getAnnotations())  
           {   if (annotation instanceof ActionMethod) 
                  methods.put(method.getName(), (ActionMethod) annotation);
           }
        }
        return methods;
      }
      
      public static java.util.Map<String, ActionMethod> getActionMethods
                                                        (Object object, boolean declared)
      { return getActionMethods(object.getClass(), declared); }
      
      public  java.util.Set<Action> getActions
                                    (Class klass, boolean declared)
      { java.util.Set<Action> methods = new TreeSet<Action>();
        for (Method method: declared ? klass.getDeclaredMethods() : klass.getMethods())
        {  for (Annotation annotation: method.getAnnotations())  
           {   if (annotation instanceof ActionMethod) 
                  methods.add((Action) this.get(method.getName()));
           }
        }
        return methods;
      }
      
      public  java.util.Set<Action> getActions
                                    (Object object, boolean declared)
                                    { return getActions(object.getClass(), declared); }

      
      public void register(final Object object)
      {   for (Method method: object.getClass().getMethods())
          {  for (Annotation annotation: method.getAnnotations())  
             {   if (annotation instanceof ActionMethod) 
                 { 
                   put(method.getName(), new Action(object, method, (ActionMethod) annotation));
                 }           
             } 
          }
      } 
            
      /** Get texts describing all the actions in this map */
      public String getBindingsText(final String prefix, Object object, boolean local)
      { 
        StringBuilder   b = new StringBuilder(50);
        for (Action act: getActions(object, local))
        {
           Set<String> keys = act.getKeyNames();
           String tip = act.getTip().replace("\n", "\n                                                          # ");
           if (keys.isEmpty())
             b.append(String.format("%-15s %-20s %-20s # %s%n", prefix, act.getName(), 
                                    "#keystroke#", tip));
           else for (String key: keys)
             b.append(String.format("%-15s %-20s %-20s # %s%n", prefix, act.getName(), 
                                    key, tip));                                        
        }
        return b.toString();
      }
      
      /** Get HTML describing all the actions in this map */      
      public String getBindingsHTML(String caption, Object object, boolean local)
      { StringBuilder b = new StringBuilder();
        b.append(String.format("<table border='1'><caption>%s</caption>", caption));
        b.append("<tr> <th>Action Name</th><th>Shortcut(s)</th><th>Description</th><th>Menu Label</th> </tr>");
        for (Action act: getActions(object, local))
        {
           Set<String> keys     = act.getKeyNames();
           String      tip      = act.getTip();
           String      shortcut = keys.isEmpty()?"&nbsp;" : keys.toString().replace("[","").replace("]","").replace(",","<br></br>");
           String      menuItem = act.isActivatedByMenu() ? "<b>"+act.getMenu()+"</b><br></br>"+act.getLabel() : "&nbsp;";
           b.append(String.format("<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>%n", act.getName(), shortcut, tip, menuItem));
        }
        b.append("</table>");
        return b.toString();
      }                 
    }
}











