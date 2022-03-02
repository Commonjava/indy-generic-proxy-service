package org.commonjava.service.httprox;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.commonjava.indy.service.httprox.client.repository.RepositoryService;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
public class HttpProxyTest extends AbstractGenericProxyTest
{

    private static final String USER = "user";

    private static final String PASS = "password";

    private static final String HOST = "127.0.0.1";

    private static int proxyPort = 8085;

    @Inject
    @RestClient
    RepositoryService repositoryService;

    @Test
    public void proxySimplePomAndAutoCreateRemoteRepo()
            throws Exception
    {
        final String testRepo = "test";

        final String url = "http://remote.example:80/test/org/test/simple/1/simple.pom";
        final String testPomContents = loadResource("simple.pom");

        final HttpGet get = new HttpGet( url );
        final CloseableHttpClient client = proxiedHttp();
        CloseableHttpResponse response = null;

        InputStream stream = null;
        try
        {
            response = client.execute( get, proxyContext( USER, PASS ) );
            stream = response.getEntity().getContent();
            final String resultingPom = IOUtils.toString( stream );

            assertThat( resultingPom, notNullValue() );
            assertThat( resultingPom, equalTo( testPomContents ) );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
        }

        //TODO check if remote repo created
        //repositoryService.repoExists("GENERIC_PKG_KEY", StoreType.remote.name(), "httprox_remote-example_80");

    }

    @Test
    public void proxy404()
            throws Exception
    {
        final String testRepo = "test";

        final String url = "http://remote.example:80/test/org/test/simple/1/simple-1.pom";

        final HttpGet get = new HttpGet( url );
        final CloseableHttpClient client = proxiedHttp();
        CloseableHttpResponse response = null;

        final InputStream stream = null;
        try
        {
            response = client.execute( get, proxyContext( USER, PASS ) );
            assertThat( response.getStatusLine().getStatusCode(), equalTo( HttpStatus.SC_NOT_FOUND ) );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
            //HttpUtil.cleanupResources( client, get, response );
        }

    }

    protected HttpClientContext proxyContext(final String user, final String pass )
    {
        final CredentialsProvider creds = new BasicCredentialsProvider();
        creds.setCredentials( new AuthScope( HOST, proxyPort ), new UsernamePasswordCredentials( user, pass ) );
        final HttpClientContext ctx = HttpClientContext.create();
        ctx.setCredentialsProvider( creds );

        return ctx;
    }

    protected CloseableHttpClient proxiedHttp()
            throws Exception
    {
        final HttpRoutePlanner planner = new DefaultProxyRoutePlanner( new HttpHost( HOST, proxyPort ) );
        return HttpClients.custom().setRoutePlanner( planner ).build();
    }

    protected String loadResource(String resource) throws IOException
    {
        final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream( resource );

        return IOUtils.toString( stream );
    }


}
