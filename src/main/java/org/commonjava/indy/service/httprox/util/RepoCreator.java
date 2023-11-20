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
package org.commonjava.indy.service.httprox.util;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.PathStyle;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.service.httprox.config.ProxyConfiguration;
import org.commonjava.indy.service.httprox.handler.AbstractProxyRepositoryCreator;
import org.commonjava.indy.service.httprox.handler.ProxyCreationResult;
import org.slf4j.Logger;

import java.util.function.Predicate;

public class RepoCreator extends AbstractProxyRepositoryCreator
{
    private ProxyConfiguration config;
    public RepoCreator( ProxyConfiguration config )
    {
        this.config = config;
    }

    public Predicate<ArtifactStore> getNameFilter(String name )
    {
        return store -> store.getName().startsWith( name );
    }

    @Override
    public ProxyCreationResult create(String trackingID, String name, String baseUrl, UrlInfo urlInfo, UserPass userPass, Logger logger) {
        ProxyCreationResult ret = new ProxyCreationResult();
        if (trackingID == null) {
            RemoteRepository remote = createRemote(name, baseUrl, urlInfo, userPass, logger);
            ret.setRemote(remote);
        } else {
            String host = urlInfo.getHost();
            int port = urlInfo.getPort();

            String remoteName = formatId(host, port, 0, trackingID, "remote");
            RemoteRepository remote = createRemote(trackingID, remoteName, baseUrl, urlInfo, userPass, logger);
            ret.setRemote(remote);

            String hostedName = formatId(host, port, 0, trackingID, "hosted");
            HostedRepository hosted = createHosted(trackingID, hostedName, urlInfo, logger);
            ret.setHosted(hosted);

            String groupName = formatId(host, port, 0, trackingID, "group");
            ret.setGroup(createGroup(trackingID, groupName, urlInfo, logger, remote.getKey(), hosted.getKey()));
        }
        return ret;
    }

    @Override
    protected PathStyle getPathStyle()
    {
        return PathStyle.valueOf( config.getStoragePathStyle() );
    }


}
