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
package org.commonjava.indy.service.httprox.client.content;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import org.commonjava.indy.service.httprox.client.Classifier;
import org.commonjava.indy.service.httprox.util.OtelAdapter;
import org.commonjava.indy.service.httprox.util.UrlUtils;
import org.commonjava.indy.service.httprox.util.WebClientAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.commonjava.indy.service.httprox.util.ProxyUtils.normalizePathAnd;

@ApplicationScoped
public class ContentRetrievalService
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    OtelAdapter otel;

    private static final String API_BASE_URL = "/api/content/generic-http/";

    private static final String API_FOLO_BASE_URL = "/api/folo/track/";

    @Inject
    Classifier classifier;

    public Uni<okhttp3.Response> doGet( String trackingId, String type, String name, String path ) throws Exception
    {
        String path1;
        if ( trackingId != null )
        {
            path1 = UrlUtils.buildUrl(API_FOLO_BASE_URL, trackingId, "generic-http", type, name, path);
        }
        else
        {
            path1 = UrlUtils.buildUrl(API_BASE_URL, type, name, path);
        }
        logger.info("doGet: {}", path1);
        return normalizePathAnd( path1, p -> classifier.classifyAnd( p, HttpMethod.GET, (client, service ) -> wrapAsyncCall(
                client.get( p ).call(), HttpMethod.GET ) ) );
    }

    private Uni<okhttp3.Response> wrapAsyncCall(WebClientAdapter.CallAdapter asyncCall, HttpMethod method )
    {
        return asyncCall.enqueue().onFailure().recoverWithItem(this::handleProxyException);
    }

    /**
     * Send status 500 with error message body.
     * @param t error
     */
    private okhttp3.Response handleProxyException( Throwable t )
    {
        okhttp3.Response.Builder builder = new okhttp3.Response.Builder();
        logger.error( "Proxy error", t );
        builder.code(INTERNAL_SERVER_ERROR.getStatusCode());
        builder.message( t + ". Caused by: " + t.getCause() );
        return builder.build();
    }

}
