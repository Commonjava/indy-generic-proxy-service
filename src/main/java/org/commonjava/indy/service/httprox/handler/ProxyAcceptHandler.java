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
package org.commonjava.indy.service.httprox.handler;

import io.opentelemetry.api.trace.Span;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.service.httprox.client.content.ContentRetrievalService;
import org.commonjava.indy.service.httprox.client.repository.RepositoryService;
import org.commonjava.indy.service.httprox.config.ProxyConfiguration;
import org.commonjava.indy.service.httprox.keycloak.KeycloakProxyAuthenticator;
import org.commonjava.indy.service.httprox.util.CacheProducer;
import org.commonjava.indy.service.httprox.util.OtelAdapter;
import org.commonjava.indy.service.httprox.util.RepoCreator;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.StreamConnection;
import org.xnio.channels.AcceptingChannel;
import org.xnio.conduits.ConduitStreamSinkChannel;
import org.xnio.conduits.ConduitStreamSourceChannel;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;

import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_GENERIC_HTTP;
import static org.commonjava.indy.service.httprox.util.MetricsConstants.*;

@ApplicationScoped
public class ProxyAcceptHandler implements ChannelListener<AcceptingChannel<StreamConnection>> {

    public static final String HTTPROX_ORIGIN = "httprox";

    @Inject
    ProxyConfiguration config;

    @Inject
    @RestClient
    RepositoryService repositoryService;

    @Inject
    ContentRetrievalService contentRetrievalService;

    @Inject
    KeycloakProxyAuthenticator proxyAuthenticator;

    @Inject
    ManagedExecutor proxyExecutor;

    @Inject
    OtelAdapter otel;

    @Inject
    CacheProducer cacheProducer;

    IndyObjectMapper objectMapper;

    public ProxyAcceptHandler() {

    }

    @PostConstruct
    public void post()
    {
        objectMapper = new IndyObjectMapper(false);
    }

    @Override
    public void handleEvent(AcceptingChannel<StreamConnection> channel) {
        final Logger logger = LoggerFactory.getLogger(getClass());
        long start = System.nanoTime();
        if ( otel.enabled() )
        {
            Span.current().setAttribute(ACCESS_CHANNEL, PKG_TYPE_GENERIC_HTTP);
            Span.current().setAttribute(PACKAGE_TYPE, PKG_TYPE_GENERIC_HTTP);
        }

        StreamConnection accepted;
        try {
            accepted = channel.accept();
        } catch (IOException e) {
            logger.error("Failed to accept httprox connection: " + e.getMessage(), e);
            accepted = null;
        }

        // to remove the return in the catch clause, which is bad form...
        if (accepted == null) {
            return;
        }

        if ( otel.enabled() )
        {
            Span.current().setAttribute( REQUEST_PHASE, REQUEST_PHASE_START );
        }

        logger.info("accepted request from address: {}", accepted.getPeerAddress());

        final ConduitStreamSourceChannel source = accepted.getSourceChannel();
        final ConduitStreamSinkChannel sink = accepted.getSinkChannel();

        ProxyRepositoryCreator repoCreator = new RepoCreator( config );

        final ProxyResponseWriter writer =
                new ProxyResponseWriter( config, repoCreator, accepted, repositoryService, contentRetrievalService, proxyExecutor, proxyAuthenticator, objectMapper, cacheProducer, start, otel );

        logger.debug("Setting writer: {}", writer);
        sink.getWriteSetter().set(writer);

        final ProxyRequestReader reader = new ProxyRequestReader(writer, sink);
        writer.setProxyRequestReader(reader);

        logger.debug("Setting reader: {}", reader);
        source.getReadSetter().set(reader);
        source.resumeReads();
    }
}
