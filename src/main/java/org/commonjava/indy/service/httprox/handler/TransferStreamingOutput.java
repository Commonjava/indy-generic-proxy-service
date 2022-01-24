package org.commonjava.indy.service.httprox.handler;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TransferStreamingOutput
        implements StreamingOutput
{

    private static final String TRANSFER_METRIC_NAME = "indy.transferred.content";

    private InputStream stream;


    public TransferStreamingOutput( final InputStream stream )
    {
        this.stream = stream;
    }

    @Override
    public void write( final OutputStream out )
            throws IOException, WebApplicationException
    {
        try
        {
            CountingOutputStream cout = new CountingOutputStream( out );
            IOUtils.copy( stream, cout );

            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.trace( "Wrote: {} bytes", cout.getByteCount() );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
        }
    }

}
