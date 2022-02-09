package org.commonjava.service.httprox;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AbstractGenericProxyTest
{

    private static final String HOST = "127.0.0.1";

    private static int proxyPort = 8085;

    protected File etcDir;

    @BeforeEach
    public void setup() throws Exception
    {
        //TODO
        etcDir = new File("/tmp");
        initTestData();
    }

    protected CloseableHttpClient proxiedHttp(final String user, final String pass )
            throws Exception
    {
        return proxiedHttp(user, pass, null);
    }

    protected CloseableHttpClient proxiedHttp(final String user, final String pass, SSLSocketFactory socketFactory )
            throws Exception
    {
        CredentialsProvider creds = null;

        if ( user != null )
        {
            creds = new BasicCredentialsProvider();
            creds.setCredentials( new AuthScope( HOST, proxyPort ), new UsernamePasswordCredentials( user, pass ) );
        }

        HttpHost proxy = new HttpHost( HOST, proxyPort );

        final HttpRoutePlanner planner = new DefaultProxyRoutePlanner( proxy );
        HttpClientBuilder builder = HttpClients.custom()
                .setRoutePlanner( planner )
                .setDefaultCredentialsProvider( creds )
                .setProxy( proxy )
                .setSSLSocketFactory( socketFactory );

        return builder.build();
    }

    protected HttpClientContext proxyContext(final String user, final String pass )
    {
        final CredentialsProvider creds = new BasicCredentialsProvider();
        creds.setCredentials( new AuthScope( HOST, proxyPort ), new UsernamePasswordCredentials( user, pass ) );
        final HttpClientContext ctx = HttpClientContext.create();
        ctx.setCredentialsProvider( creds );

        return ctx;
    }

    protected String get( String url, boolean withCACert, String user, String pass ) throws Exception
    {
        CloseableHttpClient client;

        if ( withCACert )
        {
            File jks = new File( etcDir, "ssl/ca.jks" );
            KeyStore trustStore = getTrustStore( jks );
            SSLSocketFactory socketFactory = new SSLSocketFactory( trustStore );
            client = proxiedHttp( user, pass, socketFactory );
        }
        else
        {
            client = proxiedHttp( user, pass );
        }

        HttpGet get = new HttpGet( url );
        CloseableHttpResponse response = null;

        InputStream stream = null;
        try
        {
            response = client.execute( get, proxyContext( user, pass ) );
            StatusLine status = response.getStatusLine();
            System.out.println( "status >>>> " + status );

            if ( status.getStatusCode() == 404 )
            {
                return status.toString();
            }

            stream = response.getEntity().getContent();
            final String resulting = IOUtils.toString( stream );

            assertThat( resulting, notNullValue() );
            System.out.println( "\n\n>>>>>>>\n\n" + resulting + "\n\n" );

            return resulting;
        }
        finally
        {
            IOUtils.closeQuietly( stream );
            //HttpResources.cleanupResources( get, response, client );
        }
    }

    protected KeyStore getTrustStore( File jks ) throws Exception
    {
        KeyStore trustStore = KeyStore.getInstance( KeyStore.getDefaultType() );
        try (FileInputStream instream = new FileInputStream( jks ))
        {
            trustStore.load( instream, "passwd".toCharArray() );
        }
        return trustStore;
    }

    protected void initTestData() throws IOException
    {
        copyToConfigFile( "ssl/ca.der", "ssl/ca.der" );
        copyToConfigFile( "ssl/ca.crt", "ssl/ca.crt" );
        copyToConfigFile( "ssl/ca.jks", "ssl/ca.jks" );
    }

    protected void copyToConfigFile( String resourcePath, String path ) throws IOException
    {
        File file = new File( etcDir, path );
        file.getParentFile().mkdirs();
        FileUtils.copyInputStreamToFile(
                Thread.currentThread().getContextClassLoader().getResourceAsStream( resourcePath ), file );
    }

}
