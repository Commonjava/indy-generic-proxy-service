package org.commonjava.service.httprox;

import io.quarkus.test.junit.QuarkusMock;
import io.smallrye.mutiny.Uni;
import okhttp3.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
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
import org.commonjava.indy.service.httprox.client.content.ContentRetrievalService;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mockito;

import java.io.*;
import java.security.KeyStore;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;

public class AbstractGenericProxyTest
{

    private static final String HOST = "127.0.0.1";

    private static int proxyPort = 8085;

    protected static File etcDir;

    @BeforeAll
    public static void setup() throws Exception
    {

        ContentRetrievalService contentRetrievalService = Mockito.mock(ContentRetrievalService.class);

        Mockito.when(contentRetrievalService.doGet(any(), any(), any(), contains("indy-api-1.3.1.pom"))).thenReturn(Uni.createFrom().item(buildResponse("indy-api-1.3.1.pom")));
        Mockito.when(contentRetrievalService.doGet(any(), any(), any(), contains("fsevents-1.2.4.tgz"))).thenReturn(Uni.createFrom().item(buildResponse("fsevents-1.2.4.tgz")));
        Mockito.when(contentRetrievalService.doGet(any(), any(), any(), contains("simple.pom"))).thenReturn(Uni.createFrom().item(buildResponse("simple.pom")));
        Mockito.when(contentRetrievalService.doGet(any(), any(), any(), contains("simple-1.pom"))).thenReturn(Uni.createFrom().item(buildResponse("simple-1.pom")));
        Mockito.when(contentRetrievalService.doGet(any(), any(), any(), contains("no.pom"))).thenReturn(Uni.createFrom().item(buildResponse("no.pom")));

        QuarkusMock.installMockForType(contentRetrievalService, ContentRetrievalService.class);

        etcDir = new File("/tmp");
        initTestData();
    }

    private static Response buildResponse(String fileName)
    {

        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(FilenameUtils.getName(fileName));

        Response.Builder baseBuilder = new Response.Builder()
                .request(new Request.Builder().url("http://url.com").build())
                .protocol(Protocol.HTTP_1_1);

        if ( in != null ) {
            try {
                return baseBuilder.code(200)
                        .body(ResponseBody.create(toByteArray(in), MediaType.parse("application/json")))
                        .message("Mock response from inputStream.").build();
            }
            catch (IOException e)
            {
                System.out.println("error>>:" + e.getMessage());
            }
        }
        else
        {
            return baseBuilder.code(404).message("Mock 404 response.").build();
        }

        return null;
    }

    protected static byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            byte[] b = new byte[2014*3];
            int n = 0;
            while ((n = is.read(b)) != -1) {
                output.write(b, 0, n);
            }
            return output.toByteArray();
        } finally {
            output.close();
        }
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

    protected static void initTestData() throws IOException
    {
        copyToConfigFile( "ssl/ca.der", "ssl/ca.der" );
        copyToConfigFile( "ssl/ca.crt", "ssl/ca.crt" );
        copyToConfigFile( "ssl/ca.jks", "ssl/ca.jks" );
    }

    protected static void copyToConfigFile( String resourcePath, String path ) throws IOException
    {
        File file = new File( etcDir, path );
        file.getParentFile().mkdirs();
        FileUtils.copyInputStreamToFile(
                Thread.currentThread().getContextClassLoader().getResourceAsStream( resourcePath ), file );
    }

}
