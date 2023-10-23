/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/service-parent)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import javax.ws.rs.WebApplicationException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
    }

    /**
     * If path contains '?', the proxy will create the remote repo with attribute 'path-encode':'base64'. When sending
     * the request to remote site, the 'path+query' will be encoded.
     */
    @Test
    public void proxySimplePomWithQueryParameter()
            throws Exception
    {
        final String url = "http://remote.example:80/org/test/simple.pom?version=2.0";
        final String testPomContents = loadResource("simple-2.0.pom");

        final HttpGet get = new HttpGet( url );
        final CloseableHttpClient client = proxiedHttp();
        InputStream stream = null;
        try
        {
            CloseableHttpResponse response = client.execute( get, proxyContext( USER, PASS ) );
            stream = response.getEntity().getContent();
            final String resultingPom = IOUtils.toString( stream, StandardCharsets.UTF_8);

            assertThat( resultingPom, notNullValue() );
            assertThat( resultingPom, equalTo( testPomContents ) );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
        }
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
        }
        catch ( WebApplicationException e )
        {
            assertThat( e.getResponse().getStatus(), equalTo( HttpStatus.SC_NOT_FOUND ) );
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

}
