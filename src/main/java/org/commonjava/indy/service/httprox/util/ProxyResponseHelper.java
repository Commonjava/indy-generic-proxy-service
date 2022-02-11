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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.commonjava.indy.model.core.*;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.pkg.PackageTypeConstants;
import org.commonjava.indy.service.httprox.client.content.ContentRetrievalService;
import org.commonjava.indy.service.httprox.client.repository.RepositoryService;
import org.commonjava.indy.service.httprox.config.ProxyConfiguration;
import org.commonjava.indy.service.httprox.handler.ProxyCreationResult;
import org.commonjava.indy.service.httprox.handler.ProxyRepositoryCreator;
import org.commonjava.indy.service.httprox.handler.TransferStreamingOutput;
import org.commonjava.indy.service.httprox.model.TrackingKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URL;

import static org.commonjava.indy.model.core.ArtifactStore.TRACKING_ID;
import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;
import static org.commonjava.indy.service.httprox.model.TrackingType.ALWAYS;
import static org.commonjava.indy.service.httprox.model.TrackingType.SUFFIX;
import static org.commonjava.indy.service.httprox.util.ChannelUtils.DEFAULT_READ_BUF_SIZE;

public class ProxyResponseHelper
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String TRACKED_USER_SUFFIX = "+tracking";

    private final HttpRequest httpRequest;

    private final ProxyConfiguration config;

    private boolean transferred;

    private ProxyRepositoryCreator repoCreator;

    private ContentRetrievalService contentRetrievalService;

    private RepositoryService repositoryService;

    private IndyObjectMapper indyObjectMapper;

    public ProxyResponseHelper(HttpRequest httpRequest, ProxyConfiguration config, ProxyRepositoryCreator repoCreator, ContentRetrievalService contentRetrievalService, RepositoryService repositoryService, IndyObjectMapper indyObjectMapper )
    {
        this.httpRequest = httpRequest;
        this.config = config;
        this.repoCreator = repoCreator;
        this.contentRetrievalService = contentRetrievalService;
        this.repositoryService = repositoryService;
        this.indyObjectMapper = indyObjectMapper;
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
            final String baseUrl = getBaseUrl( url, false );
            logger.info("baseUrl: {}", baseUrl);

            /*ArtifactStoreQuery<RemoteRepository> query =
                    storeManager.query().storeType( RemoteRepository.class );

            remote = query.getAllRemoteRepositories( GENERIC_PKG_KEY )
                    .stream()
                    .filter( store -> store.getUrl().equals( baseUrl )
                            && store.getMetadata( TRACKING_ID ) == null )
                    .findFirst()
                    .orElse( null );*/

            logger.debug( "Get httproxy remote, remote: {}", remote );
            if ( remote == null )
            {
                logger.debug( "Creating remote repository for HTTProx request: {}", url );
                String name = getRemoteRepositoryName( url );
                logger.info("remote repo name: {} based on url: {}", name, url);
                ProxyCreationResult result = createRepo( null, url, name );
                remote = result.getRemote();
            }
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
            try
            {
                repositoryService.createStore(PackageTypeConstants.PKG_TYPE_GENERIC_HTTP, "remote", indyObjectMapper.writeValueAsString(remote));
            }
            catch (JsonProcessingException e)
            {
                throw new IndyProxyException("");
            }

        }

        HostedRepository hosted = result.getHosted();
        if ( hosted != null )
        {
            try
            {
                repositoryService.createStore(PackageTypeConstants.PKG_TYPE_GENERIC_HTTP, "hosted", indyObjectMapper.writeValueAsString(hosted));
            }
            catch (JsonProcessingException e)
            {
                throw new IndyProxyException("");
            }
        }

        Group group = result.getGroup();
        if ( group != null )
        {
            try
            {
                repositoryService.createStore(PackageTypeConstants.PKG_TYPE_GENERIC_HTTP, "group", indyObjectMapper.writeValueAsString(group));
            }
            catch (JsonProcessingException e)
            {
                throw new IndyProxyException("");
            }
        }

        return result;
    }

    /**
     * if repo with this name already exists, we need to use a different name
     */
    private String getRemoteRepositoryName( URL url ) throws IndyProxyException
    {
        final String name = repoCreator.formatId( url.getHost(), getPort( url ), 0, null, StoreType.remote.name() );
        //TODO
        return name;
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

        //TODO
        Response response = contentRetrievalService.doGet("", "", path);

        if ( response.getStatus() == HttpStatus.SC_OK)
        {
            TransferStreamingOutput responseStream = response.readEntity(TransferStreamingOutput.class);
            logger.info("stream back: {}", path);

            ByteArrayInputStream inputStream;

            try ( ByteArrayOutputStream outputStream = new ByteArrayOutputStream() )
            {
                responseStream.write( outputStream );
                inputStream = new ByteArrayInputStream( outputStream.toByteArray() );
            }

            http.writeExistingTransfer(inputStream, true, response.getHeaders());
        }
        else if ( response.getStatus() == HttpStatus.SC_NOT_FOUND )
        {
            http.writeNotFoundTransfer( store, path );
        }

    }

    public TrackingKey getTrackingKey(UserPass proxyUserPass ) throws IndyProxyException
    {
        TrackingKey tk = null;
        switch ( config.getTrackingType() )
        {
            case ALWAYS:
            {
                if ( proxyUserPass == null )
                {
                    throw new IndyProxyException( ApplicationStatus.BAD_REQUEST.code(),
                            "Tracking is always-on, but no username was provided! Cannot initialize tracking key." );
                }

                tk = new TrackingKey( proxyUserPass.getUser() );

                break;
            }
            case SUFFIX:
            {
                if ( proxyUserPass != null )
                {
                    final String user = proxyUserPass.getUser();

                    if ( user != null && user.endsWith( TRACKED_USER_SUFFIX ) && user.length() > TRACKED_USER_SUFFIX.length() )
                    {
                        tk = new TrackingKey( StringUtils.substring( user, 0, - TRACKED_USER_SUFFIX.length() ) );
                    }
                }

                break;
            }
            default:
            {
            }
        }
        return tk;
    }

}
