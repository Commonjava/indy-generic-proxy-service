/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.apache.http.HttpRequest;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.service.httprox.client.repository.*;
import org.commonjava.indy.service.httprox.config.IndyGenericProxyConfiguration;
import org.commonjava.indy.service.httprox.handler.ProxyCreationResult;
import org.commonjava.indy.service.httprox.handler.ProxyRepositoryCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;

public class ProxyResponseHelper
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String TRACKED_USER_SUFFIX = "+tracking";

    private final HttpRequest httpRequest;

    private final IndyGenericProxyConfiguration config;

    private boolean transferred;

    private ProxyRepositoryCreator repoCreator;

    public ProxyResponseHelper(HttpRequest httpRequest, IndyGenericProxyConfiguration config, ProxyRepositoryCreator repoCreator )
    {
        this.httpRequest = httpRequest;
        this.config = config;
        this.repoCreator = repoCreator;
    }

    public ArtifactStore getArtifactStore(String trackingId, final URL url )
                    throws IndyProxyException
    {
        ArtifactStore store = null;

        if ( store == null )
        {
            try
            {
                store = doGetArtifactStore( trackingId, url );
            }
            finally
            {
                //timerContext.stop();
            }
        }

        return store;
    }

    private ArtifactStore doGetArtifactStore(String trackingId, final URL url )
                    throws IndyProxyException
    {
        int port = getPort( url );

        if ( trackingId != null )
        {
            String groupName = repoCreator.formatId( url.getHost(), port, 0, trackingId, "group" );

            Group group = null;
            //TODO check if the gorup exists

            if ( group == null )
            {
                logger.debug( "Creating repositories (group, hosted, remote) for HTTProx request: {}, trackingId: {}",
                        url, trackingId );
                ProxyCreationResult result = createRepo( trackingId, url, null );
                group = result.getGroup();
            }
            return group;
        }
        else
        {
            RemoteRepository remote = null;
            //TODO check if the remote repo exists, if not try to create it first via invoking indy REST API
            return remote;
        }
    }

    /**
     * Create repositories (group, remote, hosted) when trackingId is present. Otherwise create normal remote
     * repository with specified name.
     *
     * @param trackingId
     * @param url
     * @param name distinct remote repository name. null if trackingId is given
     */
    private ProxyCreationResult createRepo(String trackingId, URL url, String name )
                    throws IndyProxyException
    {
        UrlInfo info = new UrlInfo( url.toExternalForm() );

        UserPass up = UserPass.parse( ApplicationHeader.authorization, httpRequest, url.getAuthority() );
        String baseUrl = getBaseUrl( url, false );

        logger.debug( ">>>> Create repo: trackingId=" + trackingId + ", name=" + name );
        ProxyCreationResult result = repoCreator.create( trackingId, name, baseUrl, info, up,
                LoggerFactory.getLogger( repoCreator.getClass() ) );
        /*ChangeSummary changeSummary =
                new ChangeSummary( ChangeSummary.SYSTEM_USER, "Creating HTTProx proxy for: " + info.getUrl() );*/

        RemoteRepository remote = result.getRemote();
        if ( remote != null )
        {
            //storeManager.storeArtifactStore( remote, changeSummary, false, true, new EventMetadata() );
        }

        HostedRepository hosted = result.getHosted();
        if ( hosted != null )
        {
            //storeManager.storeArtifactStore( hosted, changeSummary, false, true, new EventMetadata() );
        }

        Group group = result.getGroup();
        if ( group != null )
        {
            //storeManager.storeArtifactStore( group, changeSummary, false, true, new EventMetadata() );
        }

        return result;
    }

    /**
     * if repo with this name already exists, we need to use a different name
     */
    private String getRemoteRepositoryName( URL url ) throws IndyProxyException
    {
        //TODO
        return null;
    }

    private int getPort( URL url )
    {
        int port = url.getPort();
        if ( port < 1 )
        {
            port = url.getDefaultPort();
        }
        return port;
    }

    private String getBaseUrl( URL url, boolean includeDefaultPort )
    {
        int port = getPort( url );
        String portStr;
        if ( includeDefaultPort || port != url.getDefaultPort() )
        {
            portStr = ":" + port;
        }
        else
        {
            portStr = "";
        }
        return String.format( "%s://%s%s/", url.getProtocol(), url.getHost(), portStr );
    }

    public void transfer( final HttpConduitWrapper http, final ArtifactStore store, final String path,
                   final boolean writeBody, final UserPass proxyUserPass )
                    throws IOException, IndyProxyException
    {

        try
        {
            doTransfer( http, store, path, writeBody, proxyUserPass );
        }
        finally
        {
            //timerContext.stop();
        }
    }

    private void doTransfer( final HttpConduitWrapper http, final ArtifactStore store, final String path,
                             final boolean writeBody, final UserPass proxyUserPass )
                    throws IOException, IndyProxyException
    {
        if ( transferred )
        {
            return;
        }

        transferred = true;
        if ( !http.isOpen() )
        {
            throw new IOException( "Sink channel already closed (or null)!" );
        }

        Transfer txfr = null;
        //TODO Get transfer and stream it back to client
    }

}
