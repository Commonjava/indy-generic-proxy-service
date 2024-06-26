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
package org.commonjava.indy.service.httprox.util;

import io.opentelemetry.api.trace.Span;

import java.net.SocketAddress;

import static org.commonjava.indy.service.httprox.util.MetricsConstants.REQUEST_LATENCY_NS;
import static org.commonjava.indy.service.httprox.util.MetricsConstants.HTTP_METHOD;
import static org.commonjava.indy.service.httprox.util.MetricsConstants.REQUEST_PHASE;
import static org.commonjava.indy.service.httprox.util.MetricsConstants.REQUEST_PHASE_END;

public class ProxyMeter
{
    private boolean summaryReported;

    private final String method;

    private final String requestLine;

    private final long startNanos;

    private final SocketAddress peerAddress;

    private final OtelAdapter otel;

    public ProxyMeter( final String method, final String requestLine, final long startNanos,
                       final SocketAddress peerAddress, final OtelAdapter otel )
    {
        this.method = method;
        this.requestLine = requestLine;
        this.startNanos = startNanos;
        this.peerAddress = peerAddress;
        this.otel = otel;
    }

    public void reportResponseSummary()
    {
        /*
         Here, we make this call idempotent to make the logic easier in the doHandleEvent method.
         This way, for content-transfer requests we will call this JUST BEFORE the transfer begins,
         while for all other requests we will handle it in the finally block of the doHandleEvent() method.

         NOTE: This will probably result in incorrect latency measurements for any client using HTTPS via the
         CONNECT method.
        */
        if ( !summaryReported )
        {
            summaryReported = true;

            long latency = System.nanoTime() - startNanos;

            if ( otel.enabled() )
            {
                Span.current().setAttribute( REQUEST_LATENCY_NS, latency );
                Span.current().setAttribute( HTTP_METHOD, method );
                Span.current().setAttribute( REQUEST_PHASE, REQUEST_PHASE_END );
            }

        }
    }

    public ProxyMeter copy( final long startNanos, final String method, final String requestLine )
    {
        return new ProxyMeter( method, requestLine, startNanos, peerAddress, otel );
    }

}
