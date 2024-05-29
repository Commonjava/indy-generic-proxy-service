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

public class MetricsConstants
{
    
    public static final String ACCESS_CHANNEL = "access-channel";

    public static final String REQUEST_LATENCY_NS = "request-latency-ns";

    public static final String REQUEST_PHASE = "request-phase";

    public static final String PACKAGE_TYPE = "package-type";

    public static final String METADATA_CONTENT = "metadata-content";

    public static final String CONTENT_ENTRY_POINT = "content-entry-point";

    public static final String HTTP_METHOD = "http-method";

    public static final String PATH = "path";

    public static final String HTTP_STATUS = "http-status";

    // these are well-known values we'll be using in our log aggregation filters
    public static final String REQUEST_PHASE_START = "start";

    public static final String REQUEST_PHASE_END = "end";

}
