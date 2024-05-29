/**
 * Copyright (C) 2021-2023 Red Hat, Inc. (https://github.com/Commonjava/service-parent)
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
package org.commonjava.indy.service.httprox;


import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.commonjava.indy.service.httprox.config.ProxyConfiguration;
import org.commonjava.indy.service.httprox.handler.ProxyAcceptHandler;
import org.commonjava.indy.service.httprox.util.PortFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.*;
import org.xnio.channels.AcceptingChannel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.InetSocketAddress;

@ApplicationScoped
public class HttpProxy {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    ProxyConfiguration config;

    @Inject
    ProxyAcceptHandler acceptHandler;

    private AcceptingChannel<StreamConnection> server;


    protected HttpProxy() {
    }

    public HttpProxy(final ProxyConfiguration config, ProxyAcceptHandler acceptHandler) {
        this.config = config;
        this.acceptHandler = acceptHandler;
    }

    public void onStart(@Observes StartupEvent ev) {
        String bind = "0.0.0.0";

        logger.info("Starting HTTProx proxy on: {}:{}", bind, config.getPort());

        XnioWorker worker;
        try {
            worker = Xnio.getInstance()
                    .createWorker(OptionMap.builder()
                            .set(Options.WORKER_IO_THREADS, config.getIoThreads())
                            .set(Options.WORKER_TASK_CORE_THREADS, config.getTaskThreads())
                            .getMap());

            final InetSocketAddress addr;
            if (config.getPort() < 1) {
                ThreadLocal<InetSocketAddress> using = new ThreadLocal<>();
                server = PortFinder.findPortFor(16, (foundPort) -> {
                    InetSocketAddress a = new InetSocketAddress(bind, config.getPort());
                    AcceptingChannel<StreamConnection> result =
                            worker.createStreamConnectionServer(a, acceptHandler, OptionMap.EMPTY);

                    result.resumeAccepts();
                    using.set(a);

                    return result;
                });

                addr = using.get();
                config.setPort(addr.getPort());
            } else {
                addr = new InetSocketAddress(bind, config.getPort());
                server = worker.createStreamConnectionServer(addr, acceptHandler, config.getHighWater() == 0 ? OptionMap.EMPTY : OptionMap.builder().set(Options.CONNECTION_HIGH_WATER,  config.getHighWater()).getMap());

                server.resumeAccepts();
            }
            logger.info("HTTProxy listening on: {}", addr);
        } catch (IllegalArgumentException | IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void onStop(@Observes ShutdownEvent ev) {
        if (server != null) {
            try {
                logger.info("stopping server");
                server.suspendAccepts();
                server.close();
            } catch (final IOException e) {
                logger.error("Failed to stop: " + e.getMessage(), e);
            }
        }
    }
}
