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

import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.service.httprox.client.content.ContentRetrievalService;
import org.commonjava.indy.service.httprox.client.repository.RepositoryService;
import org.commonjava.indy.service.httprox.config.ProxyConfiguration;
import org.commonjava.indy.service.httprox.keycloak.KeycloakProxyAuthenticator;
import org.commonjava.indy.service.httprox.util.RepoCreator;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.StreamConnection;
import org.xnio.channels.AcceptingChannel;
import org.xnio.conduits.ConduitStreamSinkChannel;
import org.xnio.conduits.ConduitStreamSourceChannel;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;

@ApplicationScoped
public class ProxyAcceptHandler implements ChannelListener<AcceptingChannel<StreamConnection>> {

    public static final String HTTPROX_ORIGIN = "httprox";

    @Inject
    ProxyConfiguration config;

    @Inject
    @RestClient
    ContentRetrievalService contentRetrievalService;

    @Inject
    @RestClient
    RepositoryService repositoryService;

    @Inject
    KeycloakProxyAuthenticator proxyAuthenticator;

    @Inject
    private ProxyTransfersExecutor proxyExecutor;

    public ProxyAcceptHandler() {

    }

    @Override
    public void handleEvent(AcceptingChannel<StreamConnection> channel) {
        final Logger logger = LoggerFactory.getLogger(getClass());

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

        logger.debug("accepted {}", accepted.getPeerAddress());

        final ConduitStreamSourceChannel source = accepted.getSourceChannel();
        final ConduitStreamSinkChannel sink = accepted.getSinkChannel();

        ProxyRepositoryCreator repoCreator = new RepoCreator();

        final ProxyResponseWriter writer =
                new ProxyResponseWriter( config, repoCreator, accepted, contentRetrievalService, repositoryService, proxyExecutor.getExecutor(), proxyAuthenticator, new IndyObjectMapper(false) );

        logger.debug("Setting writer: {}", writer);
        sink.getWriteSetter().set(writer);

        final ProxyRequestReader reader = new ProxyRequestReader(writer, sink);
        writer.setProxyRequestReader(reader);

        logger.debug("Setting reader: {}", reader);
        source.getReadSetter().set(reader);
        source.resumeReads();
    }
}
