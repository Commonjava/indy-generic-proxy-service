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
package org.commonjava.indy.service.httprox.client;

import io.opentelemetry.api.trace.Span;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import org.apache.commons.io.FilenameUtils;
import org.commonjava.indy.service.httprox.config.ServiceConfig;
import org.commonjava.indy.service.httprox.config.ServiceProxyConfig;
import org.commonjava.indy.service.httprox.util.OtelAdapter;
import org.commonjava.indy.service.httprox.util.WebClientAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.management.ServiceNotFoundException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

@ApplicationScoped
public class Classifier
{
    private static final long DEFAULT_TIMEOUT = TimeUnit.MINUTES.toMillis( 5 );

    private final AtomicLong timeout = new AtomicLong( DEFAULT_TIMEOUT );

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Map<ServiceConfig, WebClientAdapter> clientMap = new ConcurrentHashMap<>();

    @Inject
    ServiceProxyConfig proxyConfiguration;

    @Inject
    OtelAdapter otel;

    public <R> R classifyAnd( String path, HttpServerRequest request,
                              BiFunction<WebClientAdapter, ServiceConfig, R> action ) throws Exception
    {
        return classifyAnd( path, request.method(), action );
    }

    public <R> R classifyAnd(String path, HttpMethod method, BiFunction<WebClientAdapter, ServiceConfig, R> action )
            throws Exception
    {
        if ( otel.enabled() )
        {
            Span span = Span.current();
            span.setAttribute( "service_name", "generic-proxy" );
            span.setAttribute( "name", method.name() );
            span.setAttribute( "path.ext", FilenameUtils.getExtension( path ) );
        }

        ServiceConfig service = getServiceConfig( path, method );
        if ( service == null )
        {
            if ( otel.enabled() )
            {
                Span.current().setAttribute( "serviced", 0 );
                Span.current().setAttribute( "missing.path", path );
                Span.current().setAttribute( "missing.method", method.name() );
            }

            throw new ServiceNotFoundException( "Service not found, path: " + path + ", method: " + method );
        }
        if ( otel.enabled() )
        {
            Span span = Span.current();
            Span.current().setAttribute( "serviced", 1 );
            span.setAttribute( "target.host", service.host );
            span.setAttribute( "target.port", service.port );
            span.setAttribute( "target.method", method.name() );
            span.setAttribute( "target.path", path );
        }
        return action.apply( getWebClient( service ), service );
    }

    private ServiceConfig getServiceConfig( String path, HttpMethod method )
    {
        ServiceConfig service = null;

        Set<ServiceConfig> services = proxyConfiguration.getServices();
        if ( services != null )
        {
            for ( ServiceConfig sv : services )
            {
                if ( path.matches( sv.pathPattern ) && ( sv.methods == null || sv.methods.contains( method.name() ) ) )
                {
                    service = sv;
                    break;
                }
            }
        }
        return service;
    }

    private WebClientAdapter getWebClient( ServiceConfig service ) throws Exception
    {
        return clientMap.computeIfAbsent( service,
                sc -> new WebClientAdapter( sc, proxyConfiguration, timeout, otel ) );
    }
}
