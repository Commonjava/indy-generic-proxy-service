package org.commonjava.service.httprox;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import static io.smallrye.common.constraint.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
public class ProxyHttpsTest extends AbstractGenericProxyTest
{

    private static final String USER = "user";

    private static final String PASS = "password";

    private File etcDir;

    String https_url =
            "https://oss.sonatype.org/content/repositories/releases/org/commonjava/indy/indy-api/1.3.1/indy-api-1.3.1.pom";

    @Test
    public void run() throws Exception
    {

        //System.setProperty("jdk.tls.client.protocols", "TLSv1.3");

        etcDir = new File("/tmp");
        //initTestData();

        String ret = get( https_url, true, USER, PASS );
        assertTrue( ret.contains( "<artifactId>indy-api</artifactId>" ) );

    }

    protected String get( String url, boolean withCACert, String user, String pass ) throws Exception
    {
        CloseableHttpClient client;

        if ( withCACert )
        {
            File jks = new File(etcDir, "ssl/ca.jks");
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

    protected int getTestTimeoutMultiplier()
    {
        return 1;
    }

    protected String getAdditionalHttproxConfig()
    {
        return "MITM.enabled=true\nMITM.ca.key=${indy.home}/etc/indy/ssl/ca.der\n"
                + "MITM.ca.cert=${indy.home}/etc/indy/ssl/ca.crt\n"
                + "MITM.dn.template=CN=<host>, O=Test Org";
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
