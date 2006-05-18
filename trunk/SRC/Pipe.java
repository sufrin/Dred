package org.sufrin.dred;
import java.util.*;
import java.io.*;

/** This module provides the means by which operating system commands
    can be invoked; its methods are all static.
    
    <code>$Id$</code>
*/

public class Pipe
{ /**
    <pre>
            execute(cwd, command, input, cont, true)
    </pre>
  */
  
  public static Process execute(File cwd, String command, final String input, final Continue cont)
  {
    return execute(cwd, command, input, cont, true);
  }
  
  /**
     <p>    
            Start running the given command as a native process in
            the given working directory, and return the corresponding
            Process object (so that the native process can be stopped
            from the GUI). The <code>String</code> input is passed to the process on
            its standard input channel. The ``cont'' parameter is used
            to communicate the output and/or outcome of the process back
            to the caller.
    </p>
            
    <b>
            The standard output and error channels of the process are
            merged. If the ``collect'' parameter is true, then the output
            is collected and passed to the result method of the ``cont''
            parameter when the process terminates normally. If ``collect''
            is false, then a BufferedReader from which the output can be
            read is passed to the ``consumeOutput'' method of ``cont'',
            which is run in a separate thread.
    </b>
            
    <p>
            If the running process throws an exception then the ``fail''
            method of ``cont'' is called with that exception as parameter.
    </p>
                   
  */
  public static Process execute(File cwd, String command, final String input, final Continue cont, final boolean collect)
  { List<String> args = new Vector<String>();
    if (Dred.onWindows()) 
    {
      args.add("cmd");
      args.add("/c");
    }
    else
    {
      args.add("/bin/sh");
      args.add("-c");
    }
    args.add(command);
    ProcessBuilder pb = new ProcessBuilder(args);
    pb.directory(cwd);
    try
    {
      pb.redirectErrorStream(true);
      final Process        pr = pb.start();
      final PrintWriter    ws = new PrintWriter(new OutputStreamWriter(pr.getOutputStream(), "UTF8"));
      final BufferedReader rs = new BufferedReader(new InputStreamReader(pr.getInputStream(), "UTF8"));
      final StringWriter   st = new StringWriter();
      Thread s = new Thread() // Sending thread
      { public void run()
        { 
          try
          { ws.print(input);
            ws.flush();
            ws.close();
          }
          catch (Exception ex) 
          {
            cont.fail(ex);
          }
        }
      };
      Thread r = new Thread() // Receiving thread
      { public void run()
        {
         try
         { if (collect)
           try
           { String line;
             while (null!=(line=rs.readLine()))
             { st.append(line);
               st.append('\n');
             }
           }
           catch (Exception ex) 
           {}
           else 
             cont.consumeOutput(rs);
           int exitCode = pr.waitFor();
           cont.result(exitCode, st.toString());
         }
         catch (InterruptedException ex)
         {
           cont.fail(ex);
         }
       }
      };
      s.start();
      r.start();
      return pr;
    }
    catch (Exception ex)
    {
      cont.fail(ex);
      return null;
    }
  }
  
  /** Interface that determines how a process started by ``execute'' 
      will communicate with the host program.
  */
  public interface Continue 
  { /** Invoked when the running process terminates. The ``exitcode'' is the
        operating-system-specific termination code of the process. The
        ``output'' is the accumulated output of the process (if the
        output was being collected).
    */
    void result(int exitcode, String output);
    
    /** Invoked if the operating system process cannot be started */
    void fail(Exception ex);
    
    /** Invoked (in a separate thread) to consume the (merged) output and
        error streams of the operating system process. This method is called
        only if the ``collect'' parameter to execute is false.
    */
    void consumeOutput(BufferedReader reader);
  }
}









