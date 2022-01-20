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
import org.commonjava.indy.service.httprox.client.repository.ArtifactStore;
import org.commonjava.indy.service.httprox.client.repository.Group;
import org.commonjava.indy.service.httprox.client.repository.RemoteRepository;
import org.commonjava.indy.service.httprox.client.repository.Transfer;
import org.commonjava.indy.service.httprox.config.IndyGenericProxyConfiguration;
import org.commonjava.indy.service.httprox.handler.ProxyCreationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

public class ProxyResponseHelper
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String TRACKED_USER_SUFFIX = "+tracking";

    private final HttpRequest httpRequest;

    private final IndyGenericProxyConfiguration config;

    private boolean transferred;

    public ProxyResponseHelper(HttpRequest httpRequest, IndyGenericProxyConfiguration config )
    {
        this.httpRequest = httpRequest;
        this.config = config;
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

    private ArtifactStore doGetArtifactStore( String trackingId, final URL url )
                    throws IndyProxyException
    {
        int port = getPort( url );

        if ( trackingId != null )
        {
            Group group = null;
            //TODO check if the group exists, if not try to create it first via invoking indy REST API
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
        // TODO create repo via invoking indy REST api

        return null;
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
