package org.commonjava.service.httprox;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;

public class AbstractGenericProxyTest
{

    private static final String HOST = "127.0.0.1";

    private static int proxyPort = 8085;

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

}
