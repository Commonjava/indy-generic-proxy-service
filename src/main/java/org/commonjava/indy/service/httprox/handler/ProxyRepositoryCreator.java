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
package org.commonjava.indy.service.httprox.handler;

import org.commonjava.indy.service.httprox.util.UrlInfo;
import org.commonjava.indy.service.httprox.util.UserPass;
import org.slf4j.Logger;

public interface ProxyRepositoryCreator
{
    /**
     * It creates a normal remote repository when trackingID is null. It creates group, hosted, and remote
     * when trackingID != null.
     * 1. hosted repo allows any content
     * 2. remote repo looks just like in the trackingID == null case, but with passthrough flag set to false
     * 3. group contains members hosted and remote
     *
     * @param trackingID
     * @param name result of formatId. It is not used when trackingID is given
     * @param baseUrl
     * @param urlInfo
     * @param userPass
     * @param logger
     * @return ProxyCreationResult containing the remote repo or (group, remote, hosted)
     */
    ProxyCreationResult create(String trackingID, String name, String baseUrl, UrlInfo urlInfo, UserPass userPass,
                               Logger logger );

    /**
     * Format repo names. By default, when trackingId is null, it returns "httproxy_host_port_index".
     * When trackingId is given, it return 'g/h/r-<upstream-hostname>-<trackingID>' for group/hosted/remote
     * respectively.
     *
     * @param host upstream hostname
     * @param port upstream port
     * @param index appended when the repository name already exists
     * @param trackingID
     * @param storeType group, remote, or hosted
     * @return
     */
    String formatId( String host, int port, int index, String trackingID, String storeType );
}
