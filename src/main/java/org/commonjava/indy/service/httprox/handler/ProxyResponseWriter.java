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

import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.service.httprox.client.content.ContentRetrievalService;
import org.commonjava.indy.service.httprox.config.ProxyConfiguration;
import org.commonjava.indy.service.httprox.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.StreamConnection;
import org.xnio.conduits.ConduitStreamSinkChannel;
import org.xnio.conduits.ConduitStreamSourceChannel;

import static org.commonjava.indy.service.httprox.util.UserPass.parse;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.URL;

import static org.commonjava.indy.service.httprox.util.HttpProxyConstants.*;

public final class ProxyResponseWriter
        implements ChannelListener<ConduitStreamSinkChannel> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Throwable error;
    private HttpRequest httpRequest;
    private ProxyConfiguration config;
    private ProxyRepositoryCreator repoCreator;

    private ConduitStreamSourceChannel sourceChannel;
    private SocketAddress peerAddress;

    private ContentRetrievalService contentRetrievalService;

    public ProxyResponseWriter(final ProxyConfiguration config, final ProxyRepositoryCreator repoCreator,
                               final StreamConnection accepted, final ContentRetrievalService contentRetrievalService )
    {
        this.config = config;
        this.repoCreator = repoCreator;
        this.peerAddress = accepted.getPeerAddress();
        this.sourceChannel = accepted.getSourceChannel();
        this.contentRetrievalService = contentRetrievalService;
    }

    @Override
    public void handleEvent(final ConduitStreamSinkChannel channel) {
        doHandleEvent(channel);
    }

    private void doHandleEvent(final ConduitStreamSinkChannel sinkChannel) {
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

        });

        logger.debug("\n\n\n>>>>>>> Handle write\n\n\n");
        if (error == null) {

            ProxyResponseHelper proxyResponseHelper =
                    new ProxyResponseHelper( httpRequest, config, repoCreator, contentRetrievalService );

            try
            {

                final UserPass proxyUserPass = parse( ApplicationHeader.proxy_authorization, httpRequest, null );

                String trackingId = null;
                RequestLine requestLine = httpRequest.getRequestLine();
                String method = requestLine.getMethod().toUpperCase();

                switch (method) {
                    case GET_METHOD:
                    case HEAD_METHOD:
                    {
                        final URL url = new URL( requestLine.getUri() );
                        logger.debug( "getArtifactStore starts, trackingId: {}, url: {}", trackingId, url );
                        ArtifactStore store = proxyResponseHelper.getArtifactStore( trackingId, url );
                        proxyResponseHelper.transfer( http, store, url.getPath(), GET_METHOD.equals( method ), proxyUserPass );
                        break;
                    }
                    case OPTIONS_METHOD: {
                        http.writeStatus(ApplicationStatus.OK);
                        http.writeHeader(ApplicationHeader.allow, ALLOW_HEADER_VALUE);
                        break;
                    }
                    default: {
                        http.writeStatus(ApplicationStatus.METHOD_NOT_ALLOWED);
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

        try {
            http.close();
        } catch (final IOException e) {
            logger.error("Failed to shutdown response", e);
        }

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
