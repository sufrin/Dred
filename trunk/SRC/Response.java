package org.sufrin.nanohttp;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * HTTP response.
 * Return one of these from serve().
 */
public class Response implements HTTP
{      
       
    /**
     * Default constructor: response = HTTP_OK, data = mime = 'null'
     */
    public Response()
    {
            this.status = HTTP_OK;
    }
    
    /**
     * Response constructed from various headers
     */
    public static Response NEW(String status, String ... headers)
    {       Response r = new Response();
            r.status = status;
            for (int i=0; i<headers.length; i+=2)
                r.addHeader(headers[i], headers[i+1]);
            return r;
    }
    
    /**
     * Basic constructor.
     */
    public Response( String status, String mimeType, InputStream data )
    {
            this.status   = status;
            this.mimeType = mimeType;
            this.data     = data;
    }

    /**
     * Convenience method that makes an InputStream out of
     * given text.
     */
    public Response( String status, String mimeType, String txt )
    {
            this.status = status;
            this.mimeType = mimeType;
            try { this.data = new ByteArrayInputStream( txt.getBytes("UTF8")); } catch (Exception e) {}
    }

    /**
     * Adds given line to the header.
     */
    public void addHeader( String name, String value )
    {
            header.put( name, value );
    }
    
    /**
     * HTTP status code after processing, e.g. "200 OK", HTTP_OK
     */
    public String status;
    
    /**
     * MIME type of content, e.g. "text/html"
     */
    public String mimeType;
    
    /**
     * Data of the response, may be null.
     */
    public InputStream data;
    
    /**
     * Headers for the HTTP response. Use addHeader()
     * to add lines.
     */
    public Properties header = new Properties();
}

