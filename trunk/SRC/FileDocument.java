package org.sufrin.dred;
import java.net.*;
import java.io.*;
import java.util.*;

import org.sufrin.logging.Logging;

/**
        A FileDocument is a Document that is associated with a
        file in the filestore. 
*/
public class FileDocument extends SearchableDocument
{ /** Path to the associated file in the filestore. */
  protected File fileName;
  
  /** Path to the associated file in the filestore, or raw URL. */
  protected String fileTitle;
  
  /** Time the associated file was last modified in the filestore. 
      This is determined when the document is loaded or saved.
  */
  protected long lastModified;
  
  /** There is no associated file in the filestore. */
  protected boolean anonymous;
  
  /** There is no associated file in the filestore. */
  public boolean isAnonymous() { return anonymous; }
  
  /** Returns the path to the associated file. */
  public File getFileName()   { return fileName; }
  
  /** Returns the title of the associated file. */
  public String getFileTitle()   { return fileTitle; }
  
  /** Returns the modifiecation time of the associated file. */  
  public long lastModified()  { return lastModified; }
  
  /** Log stream for this class of component. */
  public static Logging  log   = Logging.getLog("FileDocument"); 
  /** Is logging happening above the level of INFO? */
  public static boolean  debug = log.isLoggable("FINE");

  /** The Unicode encoding used for this document */
  protected String encoding = "UTF8"; 
  public void setEncoding(String encoding) { this.encoding=encoding; }
  public String getEncoding() { return encoding; }
  
  
  
  /** Construct an anonymous document */
  public  FileDocument(String encoding)
  { this(encoding, new File("Anonymous"), true);
  }
  
  /** Construct a named document */
  public  FileDocument(String encoding, File theFileName)
  { this(encoding, theFileName, false);
  }
  
  /** Construct a possibly-anonymous document */
  public  FileDocument(String encoding, File theFileName, boolean anonymous)
  { lastModified=0;
    this.anonymous=anonymous;
    fileName=theFileName;
    this.encoding = encoding;
    canonicalizeFileName();
  }
  
  /** Trim a long title */
  protected static String trimTitle(String title)
  { if (title.length()<60) return title;
    File f = new File(title);
    title = "";
    do
    { title = f.getName();
      f = f.getParentFile();
      if (f==null) break; else title = File.separator+title;
    }
    while (title.length()<56);
    title = "..."+title;
    return title;
  }
  
  /** Canonicalize the filename if possible; otherwise leave it
      as an absolute path.
  */
  protected void canonicalizeFileName()
  {
    try
    {
      fileName = fileName.getCanonicalFile();
    }
    catch (IOException ex)
    {
    }
    fileTitle = trimTitle(fileName.toString());
  }
  
  /** Load the document from the file with the given name using the charset encoding associated with
   *  the FileDocument
  */
  public void doLoad(String name)
  { fileName = new File(name).getAbsoluteFile();
    canonicalizeFileName();
    if (debug) log.fine("Loading %s (%s)", fileName, fileTitle);
    anonymous = false;
    fileNameSet();
    if (fileName.exists() && fileName.canRead())
    try
    { Reader r = (new InputStreamReader(new FileInputStream(fileName), encoding));
      if (debug) log.fine("Opening file: %s (%s)", fileName, fileTitle);
      readFrom(r);
    }
    catch (Exception ex)
    {  anonymous = true;
       if (debug) ex.printStackTrace();
       throw new RuntimeException(ex.getMessage());
    }
    else
    if (name.startsWith("http:")
    ||  name.startsWith("jar:")
    ||  name.startsWith("class:")
    ||  name.startsWith("file:")
    ||  name.startsWith("ftp:")
    ||  name.startsWith("dred:")
       )
    try
    { 
      URL url   = DredURL.newURL(name);
      anonymous = true;
      fileTitle = trimTitle(url.toString());
      fileName  = new File(name);
      Reader r  = (new InputStreamReader(url.openStream(), encoding));
      if (debug) log.fine("Opening url: %s", url);
      readFrom(r);
    }
    catch (Exception ex)
    {  anonymous = true;
       if (debug) ex.printStackTrace();
       throw new RuntimeException("Cannot open url: "+ ex.getMessage());
    } 
    else
      if (debug) log.fine("Creating new file: %s (%s) %s", fileName, fileTitle, log.stackTrace());       
  }
  
  /** Load this document from the given reader */
  public void readFrom(Reader r) throws Exception
  {
      doLoad(r);
      setCursorXY(0, 0);
      setMark(0, 0);
      setChanged(false);
      lastModified = fileName.lastModified();
      fileNameSet();
  }

  /** Set the path to the associated file. */
  public void setFileName(File fileName)
  { this.fileName = fileName;
    canonicalizeFileName();
    this.lastModified = 0;
    this.anonymous    = false;
  }

  /** Invent a filename and associate it with this document. */
  public void inventFileName()
  {  
    try 
    {
      fileName  = File.createTempFile("DRED-", ".txt", new File("."));
      anonymous = false;
    }
    catch (Exception ex)
    { 
      ex.printStackTrace();
    }
  }

  /** Save the document in the file with which it is associated, generating
      a backup of the associated file if there is one, and inventing
      a new filename if the document was not associated with a  file.
  */
  public void doSave()
  { if (anonymous) inventFileName();
    if (fileName.exists() && !fileName.canWrite()) 
    { fileReport(String.format("%s cannot be written.", fileName));
      return;
    }
    if (!fileName.exists() || backup()) 
    try
    { PrintWriter p = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName), encoding));
      output(p);
      p.close();
      setChanged(false);
      lastModified = fileName.lastModified();
      fileSaved();
    }
    catch (Exception ex)
    { fileReport(ex.getMessage());
      if (debug) ex.printStackTrace();
    }
  }

  /** Change the path to the associated file, and save the document in it. */
  public void doSaveAs(File aFileName, String anEncoding)
  { setFileName(aFileName);
    setEncoding(anEncoding);
    doSave();
  }
  
  public boolean secondaryBackups = false;
  
  protected final int bufferSize = 32768;

  /** Generate a copy of the associated file in the same directory as the
      associated file; the name of the copy is derived from the name
      of the associated file by appending a tilde to it.  If a file
      with that name exists already then it is deleted beforehand.
      
      DONE (Apr 05 05): change line-by-line backup to bulk byte backup
  */
  public boolean backup()
  { if (!fileName.exists() && fileName.canRead())
    {  fileReport(String .format("%s is unreadable and cannot be backed-up.", fileName));
       return false;
    }
    try
    { File           b;
      try 
      {
        b = File.createTempFile("DRED-", ".tmp", fileName.getParentFile());
      }
      catch (Exception ex)
      { fileReport("Cannot create temporary backup file ("+ex.getMessage()+")\nDirectory: "+fileName.getParentFile());
        return false;
      }
      log.fine("writing to %s", b);
      // We handle our own buffering, thereby avoiding the complexities of BufferedI/OStream
      InputStream  r = new FileInputStream(fileName);
      OutputStream w = new FileOutputStream(b);
      byte [] buffer = new byte[bufferSize];
      int count = 0;
      while (0<=(count=r.read(buffer))) w.write(buffer, 0, count);
      w.close();
      log.fine("closed %s", b);
      // This is where to generate multiple backup files
      File back = new File(fileName.getAbsolutePath()+"~");
      if (secondaryBackups && back.exists()) 
         back = new File(fileName.getAbsolutePath()+"~~");
      back.delete();
      // -----------------------------------------------
      if (b.renameTo(back)) fileBacked(back);
    }
    catch (Exception ex)
    {  fileReport(ex.getMessage());
       ex.printStackTrace();
       return false;
    }
    return true;
  }

  /** Invoked when the document changes; informs registered DocListeners and
      FileDocument.Listeners.
  */
  protected void docChanged()
  { 
    fileChanged(); 
    super.docChanged();
  }

  /**
        A FileDocument.Listener is informed of changes in the status of the
        document.
  */
  public static interface Listener
  {
        void fileNameSet(File file, String fileTitle);
        void fileSaved(File file, String fileTitle);
        void fileBacked(File file, String fileTitle);
        void fileChanged(File file, String fileTitle);
        void fileReport(String report);
  }
  
  /** The set of FileDocument.Listener to this document. */
  protected HashSet<Listener> listeners = new HashSet<Listener>();

  /** Register a listener */
  protected void addListener(Listener l)    
  { listeners.add(l); 
    l.fileNameSet(fileName, fileTitle);
  }
  
  /** Unregister a listener */
    protected void removeListener(Listener l) { listeners.remove(l); }

  /** Inform registered FileDocument.Listeners of a report. */
  protected void fileReport(String report)              
  { 
    for (Listener l:listeners) l.fileReport(report);
  }
  
  /** Inform registered FileDocument.Listeners after a change in filename. */
  protected void fileNameSet()              
  { 
    for (Listener l:listeners) l.fileNameSet(fileName, fileTitle);
  }
  
  /** Inform registered FileDocument.Listeners after a (successful) save. */
  protected void fileSaved()              
  { 
    for (Listener l:listeners) l.fileSaved(fileName, fileTitle);
  }
  
  /** Inform registered FileDocument.Listeners after a change in the document. */
  protected void fileChanged()              
  { 
    for (Listener l:listeners) l.fileChanged(fileName, fileTitle);
  }
  
  /** Inform registered FileDocument.Listeners after the document is backed-up. */
  protected void fileBacked(File backup)              
  { 
    for (Listener l:listeners) l.fileBacked(backup, fileTitle);
  }
}


























