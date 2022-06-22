/**
 * Copyright (C) 2021 Red Hat, Inc. (https://github.com/Commonjava/indy-generic-proxy-service)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.service.httprox.handler;

import io.opentelemetry.api.trace.Span;
import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.commonjava.cdi.util.weft.WeftExecutorService;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.service.httprox.client.content.ContentRetrievalService;
import org.commonjava.indy.service.httprox.client.repository.RepositoryService;
import org.commonjava.indy.service.httprox.config.ProxyConfiguration;
import org.commonjava.indy.service.httprox.keycloak.KeycloakProxyAuthenticator;
import org.commonjava.indy.service.httprox.model.TrackingKey;
import org.commonjava.indy.service.httprox.model.TrackingType;
import org.commonjava.indy.service.httprox.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.StreamConnection;
import org.xnio.conduits.ConduitStreamSinkChannel;
import org.xnio.conduits.ConduitStreamSourceChannel;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.channels.SocketChannel;

import static java.lang.Integer.parseInt;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.commonjava.indy.model.core.ArtifactStore.TRACKING_ID;
import static org.commonjava.indy.service.httprox.util.ApplicationHeader.proxy_authenticate;
import static org.commonjava.indy.service.httprox.util.ApplicationStatus.PROXY_AUTHENTICATION_REQUIRED;
import static org.commonjava.indy.service.httprox.util.HttpProxyConstants.*;
import static org.commonjava.indy.service.httprox.util.UserPass.parse;

public final class ProxyResponseWriter
        implements ChannelListener<ConduitStreamSinkChannel> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Throwable error;
    private HttpRequest httpRequest;
    private ProxyConfiguration config;
    private ProxyRepositoryCreator repoCreator;

    private ConduitStreamSourceChannel sourceChannel;
    private SocketAddress peerAddress;

    private RepositoryService repositoryService;
    private ContentRetrievalService contentRetrievalService;

    private ProxySSLTunnel sslTunnel;
    private boolean directed = false;

    private ProxyRequestReader proxyRequestReader;
    private final WeftExecutorService tunnelAndMITMExecutor;

    private KeycloakProxyAuthenticator proxyAuthenticator;

    private IndyObjectMapper indyObjectMapper;

    private CacheProducer cacheProducer;

    private OtelAdapter otel;

    private long startNanos;

    public ProxyResponseWriter(final ProxyConfiguration config, final ProxyRepositoryCreator repoCreator,
                               final StreamConnection accepted, final RepositoryService repositoryService,
                               final ContentRetrievalService contentRetrievalService, final WeftExecutorService executor,
                               final KeycloakProxyAuthenticator proxyAuthenticator, final IndyObjectMapper indyObjectMapper,
                               final CacheProducer cacheProducer, final long start, final OtelAdapter otel)
    {
        this.config = config;
        this.repoCreator = repoCreator;
        this.peerAddress = accepted.getPeerAddress();
        this.sourceChannel = accepted.getSourceChannel();
        this.repositoryService = repositoryService;
        this.contentRetrievalService = contentRetrievalService;
        this.tunnelAndMITMExecutor = executor;
        this.proxyAuthenticator = proxyAuthenticator;
        this.indyObjectMapper = indyObjectMapper;
        startNanos = start;
        this.cacheProducer = cacheProducer;
        this.otel = otel;
    }

    public ProxyRequestReader getProxyRequestReader() {
        return proxyRequestReader;
    }

    public void setProxyRequestReader(ProxyRequestReader proxyRequestReader) {
        this.proxyRequestReader = proxyRequestReader;
    }

    @Override
    public void handleEvent(final ConduitStreamSinkChannel channel) {
        doHandleEvent(channel);
    }

    private void doHandleEvent(final ConduitStreamSinkChannel sinkChannel)
    {

        if ( directed )
        {
            return;
        }

        ProxyMeter meter =
                new ProxyMeter( httpRequest.getRequestLine().getMethod(), httpRequest.getRequestLine().toString(),
                        startNanos, peerAddress, otel );

        HttpConduitWrapper http = new HttpConduitWrapper(sinkChannel, httpRequest);
        if (httpRequest == null) {
            if (error != null) {
                logger.debug("Handling error from request reader: " + error.getMessage(), error);
                handleError(error, http);
            } else {
                logger.debug("Invalid state (no error or request) from request reader. Sending 400.");
                try {
                    http.writeStatus(ApplicationStatus.BAD_REQUEST);
                } catch (final IOException e) {
                    logger.error("Failed to write BAD REQUEST for missing HTTP first-line to response channel.", e);
                }
            }

            return;
        }

        final String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName("PROXY-" + httpRequest.getRequestLine().toString());
        sinkChannel.getCloseSetter().set((c) -> {
            logger.trace("Sink channel closing.");
            Thread.currentThread().setName(oldThreadName);

            if ( sslTunnel != null )
            {
                logger.trace( "Close ssl tunnel" );
                sslTunnel.close();
            }
        });

        logger.debug("\n\n\n>>>>>>> Handle write\n\n\n");
        if (error == null) {

            ProxyResponseHelper proxyResponseHelper =
                    new ProxyResponseHelper( httpRequest, config, repoCreator, repositoryService, contentRetrievalService, indyObjectMapper, cacheProducer, otel );

            try
            {

                final UserPass proxyUserPass = parse( ApplicationHeader.proxy_authorization, httpRequest, null );
                logger.info( "Using proxy authentication: {}", proxyUserPass );

                logger.debug( "Proxy UserPass: {}\nConfig secured? {}\nConfig tracking type: {}", proxyUserPass,
                        config.isSecured(), config.getTrackingType() );
                if ( proxyUserPass == null && ( config.isSecured() || TrackingType.ALWAYS == config.getTrackingType() ) )
                {

                    String realmInfo = String.format( PROXY_AUTHENTICATE_FORMAT, config.getProxyRealm() );

                    logger.info( "Not authenticated to proxy. Sending response: {} / {}: {}",
                            PROXY_AUTHENTICATION_REQUIRED, proxy_authenticate, realmInfo );

                    http.writeStatus( PROXY_AUTHENTICATION_REQUIRED );
                    http.writeHeader( proxy_authenticate, realmInfo );
                }
                else
                {
                    String trackingId = null;
                    RequestLine requestLine = httpRequest.getRequestLine();
                    String method = requestLine.getMethod().toUpperCase();
                    boolean authenticated = true;

                    if ( proxyUserPass != null )
                    {
                        TrackingKey trackingKey = proxyResponseHelper.getTrackingKey( proxyUserPass );
                        if ( trackingKey != null )
                        {
                            trackingId = trackingKey.getId();
                            if ( otel.enabled() )
                            {
                                Span.current().setAttribute(TRACKING_ID, trackingId);
                            }
                        }

                        /* String authCacheKey = generateAuthCacheKey( proxyUserPass );
                        Boolean isAuthToken = false;//proxyAuthCache.get( authCacheKey );
                        if ( Boolean.TRUE.equals( isAuthToken ) )
                        {
                            authenticated = true;
                            logger.debug( "Found auth key in cache" );
                        }
                        else
                        {
                            logger.debug(
                                    "Passing BASIC authentication credentials to Keycloak bearer-token translation authenticator" );
                            authenticated = proxyAuthenticator.authenticate( proxyUserPass, http );
                            *//*if ( authenticated )
                            {
                                proxyAuthCache.put( authCacheKey, Boolean.TRUE, config.getAuthCacheExpirationHours(), TimeUnit.HOURS );
                            }*//*
                        }
                        logger.debug( "Authentication done, result: {}", authenticated );*/

                    }

                    if ( authenticated )
                    {
                        switch (method) {
                            case GET_METHOD:
                            case HEAD_METHOD:
                            {
                                final URL url = new URL( requestLine.getUri() );
                                logger.debug( "getArtifactStore starts, trackingId: {}, url: {}", trackingId, url );
                                ArtifactStore store = proxyResponseHelper.getArtifactStore( trackingId, url );
                                proxyResponseHelper.transfer( http, store, url.getPath(), GET_METHOD.equals( method ), proxyUserPass, meter );
                                break;
                            }
                            case OPTIONS_METHOD:
                            {
                                http.writeStatus(ApplicationStatus.OK);
                                http.writeHeader(ApplicationHeader.allow, ALLOW_HEADER_VALUE);
                                break;
                            }
                            case CONNECT_METHOD:
                            {
                                if ( !config.isMITMEnabled() )
                                {
                                    logger.debug( "CONNECT method not supported unless MITM-proxying is enabled." );
                                    http.writeStatus( ApplicationStatus.BAD_REQUEST );
                                    break;
                                }

                                String uri = requestLine.getUri(); // e.g, github.com:443
                                logger.debug( "Get CONNECT request, uri: {}", uri );

                                String[] toks = uri.split( ":" );
                                String host = toks[0];
                                int port = parseInt( toks[1] );

                                directed = true;

                                // After this, the proxy simply opens a plain socket to the target server and relays
                                // everything between the initial client and the target server (including the TLS handshake).

                                SocketChannel socketChannel;

                                ProxyMITMSSLServer svr =
                                        new ProxyMITMSSLServer( host, port, trackingId, proxyUserPass,
                                                proxyResponseHelper, config, meter );
                                tunnelAndMITMExecutor.submit( svr );
                                socketChannel = svr.getSocketChannel();

                                if ( socketChannel == null )
                                {
                                    logger.debug( "Failed to get MITM socket channel" );
                                    http.writeStatus( ApplicationStatus.SERVER_ERROR );
                                    svr.stop();
                                    break;
                                }

                                sslTunnel = new ProxySSLTunnel( sinkChannel, socketChannel, config );
                                tunnelAndMITMExecutor.submit( sslTunnel );
                                proxyRequestReader.setProxySSLTunnel( sslTunnel ); // client input will be directed to target socket

                                // When all is ready, send the 200 to client. Client send the SSL handshake to reader,
                                // reader direct it to tunnel to MITM. MITM finish the handshake and read the request data,
                                // retrieve remote content and send back to tunnel to client.
                                http.writeStatus( ApplicationStatus.OK );
                                http.writeHeader( "Status", "200 OK\n" );

                                break;
                            }
                            default: {
                                http.writeStatus(ApplicationStatus.METHOD_NOT_ALLOWED);
                            }
                        }
                    }
                }

                logger.debug("Response complete.");
            } catch (final Throwable e) {
                error = e;
            }
        }

        if (error != null) {
            handleError(error, http);
        }

        try
        {
            if ( directed )
            {
                // do not close sink channel
            }
            else
            {
                http.close();
            }
        }
        catch (final IOException e)
        {
            logger.error("Failed to shutdown response", e);
        }

    }

    private String generateAuthCacheKey( UserPass proxyUserPass )
    {
        return sha256Hex( proxyUserPass.getUser() + ":" + proxyUserPass.getPassword() );
    }

    private void handleError(final Throwable error, final HttpWrapper http) {
        logger.error("HTTProx request failed: " + error.getMessage(), error);
        try {
            if (http.isOpen()) {
                http.writeStatus(ApplicationStatus.SERVER_ERROR);
                http.writeError(error);

                logger.debug("Response error complete.");
            }
        } catch (final IOException closeException) {
            logger.error("Failed to close httprox request: " + error.getMessage(), error);
        }
    }

    public void setError(final Throwable error) {
        this.error = error;
    }

    public void setHttpRequest(final HttpRequest request) {
        this.httpRequest = request;
    }
}
