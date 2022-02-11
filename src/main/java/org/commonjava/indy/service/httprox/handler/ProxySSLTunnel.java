package org.commonjava.indy.service.httprox.handler;

import org.commonjava.indy.service.httprox.config.ProxyConfiguration;
import org.commonjava.indy.service.httprox.util.ChannelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.conduits.ConduitStreamSinkChannel;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

import static org.commonjava.indy.service.httprox.util.ChannelUtils.DEFAULT_READ_BUF_SIZE;
import static org.commonjava.indy.service.httprox.util.ChannelUtils.flush;

public class ProxySSLTunnel implements Runnable
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    //private volatile Selector selector; // selecting READ events for target channel

    //private static final long SELECTOR_TIMEOUT = 60 * 1000; // 60 seconds

    private final ConduitStreamSinkChannel sinkChannel;

    private final SocketChannel socketChannel;

    private volatile boolean closed;

    private final ProxyConfiguration config;

    public ProxySSLTunnel( ConduitStreamSinkChannel sinkChannel, SocketChannel socketChannel, ProxyConfiguration config )
    {
        this.sinkChannel = sinkChannel;
        this.socketChannel = socketChannel;
        this.config = config;
    }

    @Override
    public void run()
    {
        try
        {
            pipeTargetToSinkChannel( sinkChannel, socketChannel );
        }
        catch ( Exception e )
        {
            logger.error( "Pipe to sink channel failed", e );
        }
    }

    private void pipeTargetToSinkChannel( ConduitStreamSinkChannel sinkChannel, SocketChannel targetChannel )
            throws IOException
    {
        targetChannel.socket().setSoTimeout( (int) TimeUnit.MINUTES.toMillis( config.getMITMSoTimeoutMinutes() ) );
        InputStream inStream = new BufferedInputStream( targetChannel.socket().getInputStream() );
        ReadableByteChannel wrappedChannel = Channels.newChannel( inStream );

        ByteBuffer byteBuffer = ByteBuffer.allocate( DEFAULT_READ_BUF_SIZE );

        int total = 0;
        while ( true )
        {
            if ( closed )
            {
                logger.debug( "Tunnel closed" );
                break;
            }

            int read = -1;
            try
            {
                read = wrappedChannel.read( byteBuffer );
            }
            catch ( IOException e )
            {
                logger.debug( "Read target channel breaks, {}", e.toString() );
                break;
            }

            if ( read <= 0 )
            {
                logger.debug( "Read breaks, read: {}", read );
                break;
            }

            //limit is set to current position and position is set to zero
            byteBuffer.flip();

            //final byte[] bytes = new byte[byteBuffer.limit()];
            //byteBuffer.get( bytes );

            logger.debug( "Write to sink channel, size: {}", byteBuffer.limit() );
            ChannelUtils.write( sinkChannel, byteBuffer );
            sinkChannel.flush();
            byteBuffer.clear();

            total += read;
        }

        logger.debug( "Write to sink channel complete, transferred: {}", total );

        flush( sinkChannel );
        sinkChannel.shutdownWrites();
        sinkChannel.close();

        closed = true;

    }

    public void write( byte[] bytes ) throws IOException
    {
        socketChannel.write( ByteBuffer.wrap( bytes ) );
    }

    public void close()
    {
        try
        {
            //selector.close(); // wake it up to complete the tunnel
            socketChannel.close();
        }
        catch ( IOException e )
        {
            logger.error( "Close tunnel selector failed", e );
        }
    }
}
