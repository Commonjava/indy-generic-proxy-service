package org.commonjava.service.httprox;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.io.IOUtils;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class ProxyHttpsDownloadTgzTest extends AbstractGenericProxyTest
{

    private static final String USER = "user";

    private static final String PASS = "password";

    String https_url = "https://registry.npmjs.org/fsevents/-/fsevents-1.2.4.tgz";

    @Test
    public void run() throws Exception
    {
        File ret = getDownloadedFile( https_url, true, USER, PASS );
        assertTrue( ret != null && ret.exists() );
        //System.out.println( "File size >>> " + ret.length() );
        assertEquals( ret.length(), 784846 ); // content-length: 784846
    }

    protected File getDownloadedFile( String url, boolean withCACert, String user, String pass ) throws Exception
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
                return null;
            }

            stream = response.getEntity().getContent();
            File file = new File("/tmp/a.tgz");
            FileOutputStream fileOutputStream = new FileOutputStream( file );
            IOUtils.copy( stream, fileOutputStream );
            fileOutputStream.close();

            return file;
        }
        finally
        {
            IOUtils.closeQuietly( stream );
            //HttpResources.cleanupResources( get, response, client );
        }
    }

}
