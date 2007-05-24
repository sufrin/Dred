package org.sufrin.dred;
import java.awt.event.ActionEvent;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
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
{   /** Tooltip */
    String  tip()     default "";   
    /** Label to go on the button */ 
    String  label()   default "";
    /** Whether the action is to be executed offline or on the GUI thread */
    boolean offline() default true;
    
    /** 
        When an object is registered with an ActionMethod.Map, each of its
        its <code>@ActionMethod</code>-annotated methods  give rise to an
        <code>Action</code> which records the method, the annotation, and
        the object. The <code>Action</code>'s <code>actionPerformed</code>
        method invokes the method in the object (using reflection).  If
        the annotation's <code>offline</code> value is <code>true</code>,
        then this happens in a separately running thread, otherwise
        the method is called from the UI thread.  In addition to this,
        the <code>Action</code>'s tooltip is set to the annotation's
        <code>tip</code> value, and its label is set to the annotation's
        <code>label</code> value. 
    */  
    @SuppressWarnings("serial")
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
      
      public String getName()   { return method.getName(); }
      
      public String getLabel() { return act.label(); }
      
      public String getTip()   { return act.tip(); }
      
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
      
      /** Worker thread for offline actions  */      
      static ExecutorService offlineThread = Executors.newSingleThreadExecutor();
      
      /** Execute a job in the worker thread */
      synchronized public static void execute(Runnable job) { offlineThread.execute(job); }
            
      /** Stop the worker thread */
      synchronized public static void shutdownNow()
      {  
         offlineThread.shutdownNow();
      }
            
      /** Flush the worker thread's queue  */
      synchronized public static int flushEvents()
      {  
         int n = offlineThread.shutdownNow().size();
         offlineThread = Executors.newSingleThreadExecutor();
         return n;
      }
            
      public void actionPerformed(final ActionEvent ev)                     
      {  if (act.offline())
         execute
         ( new Runnable()
           { public void run()
             {  Object source = ev.getSource();
                Patient patient = source instanceof Patient ? (Patient) source : null;
                if (patient!=null) patient.setWaiting(true);
                try   { method.invoke(object); if (patient!=null) patient.setWaiting(false); } 
                catch (IllegalAccessException ex)    {}
                catch (InvocationTargetException ex) {}
                catch (Exception ex) { ex.printStackTrace(); }
             }
           }
         );
         else
         {
            try   { method.invoke(object); } 
            catch (IllegalAccessException ex)    {}
            catch (InvocationTargetException ex) {}
            catch (Exception ex) { ex.printStackTrace(); }
         }
         
      }      
    }
    
    /**
        An ActionMap that can register all the @ActionMethod-annotated methods
        of an object under their own names. 
    */
    @SuppressWarnings("serial")
	public static class Map extends Hashtable<String, AbstractAction> // ActionMap
    { public Map()                    {}    
      public Map(final Object object) { register(object); }
            
      /** Return a mapping from the name of each ActionMethod-annotated method
          of a class (defined locally if declared is true, inherited
          or declared otherwise) to its annotation.
      */
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
      
      /** Same as <code>getActionMethods(object.getClass(), declared)</code>  */
      public static java.util.Map<String, ActionMethod> getActionMethods
                                                        (Object object, boolean declared)
      { return getActionMethods(object.getClass(), declared); }
      
      /** Return the set of ActionMethod-annotated actions 
          (defined locally if declared is true, inherited
          or declared otherwise) in the given class
          that are recorded in this Map.
      */
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

      /** Register all the ActionMethod-annotated methods of the given object */
      public void register(final Object object)
      {   for (Method method: object.getClass().getMethods())
          {  for (Annotation annotation: method.getAnnotations())  
             {   if (annotation instanceof ActionMethod) 
                 { 
                 put(method.getName(), new Action(object, method, (ActionMethod) annotation));
                 // put(method.getName().toUpperCase(), new Action(object, method, (ActionMethod) annotation)); //**
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
















