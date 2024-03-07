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

import io.opentelemetry.api.trace.Span;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import kotlin.Pair;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.commonjava.indy.model.core.*;
import org.commonjava.indy.model.core.dto.StoreListingDTO;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.pkg.PackageTypeConstants;
import org.commonjava.indy.service.httprox.client.content.ContentRetrievalService;
import org.commonjava.indy.service.httprox.client.repository.RepositoryService;
import org.commonjava.indy.service.httprox.config.ProxyConfiguration;
import org.commonjava.indy.service.httprox.handler.ProxyCreationResult;
import org.commonjava.indy.service.httprox.handler.ProxyRepositoryCreator;
import org.commonjava.indy.service.httprox.model.TrackingKey;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.vertx.core.http.HttpMethod.HEAD;
import static org.commonjava.indy.model.core.ArtifactStore.TRACKING_ID;
import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;
import static org.commonjava.indy.service.httprox.util.HttpProxyConstants.FORBIDDEN_HEADERS;
import static org.commonjava.indy.service.httprox.util.MetricsConstants.PACKAGE_TYPE;
import static org.commonjava.indy.service.httprox.util.MetricsConstants.CONTENT_ENTRY_POINT;
import static org.commonjava.indy.service.httprox.util.MetricsConstants.PATH;
import static org.commonjava.indy.service.httprox.util.MetricsConstants.METADATA_CONTENT;
import static org.commonjava.indy.service.httprox.util.UrlUtils.base64url;

public class ProxyResponseHelper
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String TRACKED_USER_SUFFIX = "+tracking";

    private final HttpRequest httpRequest;

    private final ProxyConfiguration config;

    private volatile boolean transferred;

    private ProxyRepositoryCreator repoCreator;

    private ContentRetrievalService contentRetrievalService;

    private RepositoryService repositoryService;

    private IndyObjectMapper indyObjectMapper;

    private OtelAdapter otel;

    private CacheProducer cacheProducer;

    public ProxyResponseHelper(HttpRequest httpRequest, ProxyConfiguration config, ProxyRepositoryCreator repoCreator, RepositoryService repositoryService, ContentRetrievalService contentRetrievalService, IndyObjectMapper indyObjectMapper, CacheProducer cacheProducer, OtelAdapter otel )
    {
        this.httpRequest = httpRequest;
        this.config = config;
        this.repoCreator = repoCreator;
        this.repositoryService = repositoryService;
        this.contentRetrievalService = contentRetrievalService;
        this.indyObjectMapper = indyObjectMapper;
        this.cacheProducer = cacheProducer;
        this.otel = otel;
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

                if ( store != null )
                {
                    logger.info("Got the store {} with trackingId {} and url {}", store.getKey(), trackingId, url);
                }
            }
            finally
            {
                //timerContext.stop();
            }
        }

        if ( otel.enabled() )
        {
            Span span = Span.current();
            span.setAttribute("proxy.target.url", String.valueOf(url));
            if ( trackingId != null )
            {
                span.setAttribute( TRACKING_ID, trackingId);
            }
            if ( store != null )
            {
                span.setAttribute( PACKAGE_TYPE, store.getKey().getPackageType());
                span.setAttribute( CONTENT_ENTRY_POINT, store.getKey().toString());
            }
        }

        return store;
    }

    private ArtifactStore doGetArtifactStore(String trackingId, final URL url )
                    throws IndyProxyException
    {

        int port = getPort( url );
        String groupName = repoCreator.formatId( url.getHost(), port, 0, trackingId, "group" );

        Lock lock = LockWrapper.getLockByKey( groupName );
        lock.lock();

        try
        {
            Cache cache = cacheProducer.getCache("artifact_store");

            if ( trackingId != null )
            {
                if ( cache.get( groupName ) != null )
                {
                    return (ArtifactStore) cache.get( groupName );
                }

                Group group = null;
                Response response = null;
                try
                {
                    response = repositoryService.getStore(PackageTypeConstants.PKG_TYPE_GENERIC_HTTP, "group", groupName);

                    if ( response != null && response.getStatus() == HttpStatus.SC_OK )
                    {
                        group = (Group)response.readEntity(ArtifactStore.class);
                        cache.put(groupName, group, 15, TimeUnit.MINUTES);
                    }
                }
                catch ( WebApplicationException e )
                {
                    if (e.getResponse().getStatus() == HttpStatus.SC_NOT_FOUND )
                    {
                        logger.debug( "Creating repositories (group, hosted, remote) for HTTProx request: {}, trackingId: {}",
                                url, trackingId );
                        ProxyCreationResult result = createRepo( trackingId, url, null );
                        group = result.getGroup();
                        cache.put(groupName, group, 15, TimeUnit.MINUTES);
                        return group;
                    }
                    else
                    {
                        throw new IndyProxyException("Get artifact store error.", e);
                    }
                }
                finally
                {
                    if ( response != null )
                    {
                        response.close();
                    }
                }

                return group;
            }
            else
            {
                RemoteRepository remote = null;
                final String baseUrl = getBaseUrl( url, false );
                logger.info("baseUrl: {}", baseUrl);
                Response response = null;
                try
                {
                    response = repositoryService.getRemoteByUrl(PackageTypeConstants.PKG_TYPE_GENERIC_HTTP, "remote", baseUrl);

                    if ( response != null && response.getStatus() == HttpStatus.SC_OK )
                    {
                        StoreListingDTO<RemoteRepository> dto = response.readEntity(StoreListingDTO.class);
                        for( RemoteRepository remoteRepository : dto.getItems() )
                        {
                            if ( remoteRepository.getMetadata( TRACKING_ID ) == null )
                            {
                                remote = remoteRepository;
                                break;
                            }
                        }
                    }
                }
                catch ( WebApplicationException e  )
                {
                    if (e.getResponse().getStatus() == HttpStatus.SC_NOT_FOUND )
                    {
                        logger.debug( "Creating remote repository for HTTProx request: {}", url );
                        String name = getRemoteRepositoryName( url );
                        logger.info("remote repo name: {} based on url: {}", name, url);
                        ProxyCreationResult result = createRepo( null, url, name );
                        remote = result.getRemote();
                        return remote;
                    }
                    else
                    {
                        throw new IndyProxyException("Get artifact store error.", e);
                    }
                }
                finally
                {
                    if ( response != null )
                    {
                        response.close();
                    }
                }


                return remote;
            }
        }
        finally
        {
            lock.unlock();
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

        String changeLog = "Creating HTTProx proxy for: " + info.getUrl();

        RemoteRepository remote = result.getRemote();
        if ( remote != null )
        {

            try
            {
                repositoryService.getStore(PackageTypeConstants.PKG_TYPE_GENERIC_HTTP, "remote", remote.getName());
            }
            catch ( WebApplicationException e )
            {
                if (e.getResponse().getStatus() == HttpStatus.SC_NOT_FOUND )
                {
                    try
                    {
                        remote.setMetadata(ArtifactStore.METADATA_CHANGELOG, changeLog);
                        repositoryService.createStore(PackageTypeConstants.PKG_TYPE_GENERIC_HTTP, "remote", indyObjectMapper.writeValueAsString(remote));
                    }
                    catch ( Exception se )
                    {
                        handleException( se );
                    }
                }
                else
                {
                    handleException( e );
                }
            }

        }

        HostedRepository hosted = result.getHosted();
        if ( hosted != null )
        {
            try
            {
                repositoryService.getStore(PackageTypeConstants.PKG_TYPE_GENERIC_HTTP, "hosted", hosted.getName());
            }
            catch ( WebApplicationException e )
            {
                if (e.getResponse().getStatus() == HttpStatus.SC_NOT_FOUND )
                {
                    try
                    {
                        hosted.setMetadata(ArtifactStore.METADATA_CHANGELOG, changeLog);
                        repositoryService.createStore(PackageTypeConstants.PKG_TYPE_GENERIC_HTTP, "hosted", indyObjectMapper.writeValueAsString(hosted));
                    }
                    catch ( Exception se )
                    {
                        handleException( se );
                    }
                }
                else
                {
                    handleException( e );
                }
            }

        }

        Group group = result.getGroup();
        if ( group != null )
        {
            try
            {
                group.setMetadata(ArtifactStore.METADATA_CHANGELOG, changeLog);
                repositoryService.createStore(PackageTypeConstants.PKG_TYPE_GENERIC_HTTP, "group", indyObjectMapper.writeValueAsString(group));
            }
            catch ( Exception e )
            {
                handleException(e);
            }
        }

        return result;
    }

    private void handleException(Exception e) throws IndyProxyException
    {
        if ( e instanceof WebApplicationException )
        {
            logger.error( "Create store error in repository service, status: {}, error: {}",
                    ((WebApplicationException) e).getResponse().getStatus(), e.getMessage(), e );
        }
        throw new IndyProxyException("Create repository error.", e);
    }

    /**
     * if repo with this name already exists, we need to use a different name
     */
    private String getRemoteRepositoryName( URL url ) throws IndyProxyException
    {

        final String name = repoCreator.formatId( url.getHost(), getPort( url ), 0, null, StoreType.remote.name() );

        logger.debug( "Looking for remote repo starts with name: {}", name );

        final String baseUrl = getBaseUrl( url, false );

        Response response = null;

        try
        {
            response = repositoryService.getRemoteByUrl(GENERIC_PKG_KEY, "remote", baseUrl);

            if ( response != null && response.getStatus() == HttpStatus.SC_OK )
            {
                StoreListingDTO<RemoteRepository> dto = response.readEntity(StoreListingDTO.class);
                Predicate<ArtifactStore> filter = ((RepoCreator)repoCreator).getNameFilter( name );
                List<String> l = dto.getItems().stream()
                        .filter( filter )
                        .map( repository -> repository.getName() )
                        .collect( Collectors.toList() );
                if ( l.isEmpty() )
                {
                    return name;
                }
                return ((RepoCreator)repoCreator).getNextName( l );
            }
            else
            {
                return name;
            }
        }
        catch ( WebApplicationException e )
        {
            if ( e.getResponse().getStatus() == HttpStatus.SC_NOT_FOUND )
            {
                return name;
            }
            else
            {
                throw new IndyProxyException("Get remote repository error.", e);
            }
        }
        finally
        {
            if ( response != null )
            {
                response.close();
            }
        }
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
                   final boolean writeBody, final UserPass proxyUserPass, final ProxyMeter meter )
                    throws IOException, IndyProxyException
    {

        if ( otel.enabled() )
        {
            Span.current().setAttribute( PATH, path );
            Span.current().setAttribute( METADATA_CONTENT, Boolean.FALSE );
        }

        doTransfer( http, store, path, writeBody, proxyUserPass, meter );

    }

    private void doTransfer( final HttpConduitWrapper http, final ArtifactStore store, final String path,
                             final boolean writeBody, final UserPass proxyUserPass, final ProxyMeter meter )
                    throws IOException, IndyProxyException
    {
        if ( transferred )
        {
            logger.info("Transfer already done, store: {}, path: {}", store.getKey(), path);
            return;
        }

        if ( !http.isOpen() )
        {
            throw new IOException( "Sink channel already closed (or null)!" );
        }

        String trackingId = null;
        TrackingKey tk = getTrackingKey( proxyUserPass );

        Boolean isTracking = proxyUserPass != null && proxyUserPass.getUser() != null && proxyUserPass.getUser().endsWith( TRACKED_USER_SUFFIX );

        if ( tk != null && isTracking )
        {
            logger.info( "TRACKING {} in {} (KEY: {})", path, store, tk );
            trackingId = tk.getId();
        }
        else
        {
            logger.debug( "NOT TRACKING: {} in {}", path, store );
        }

        try {
            String encodedPath = base64url(path);
            logger.debug( "Get from content service, store: {}, path: {}", store.getKey(), encodedPath );
            Uni<okhttp3.Response> responseUni = contentRetrievalService.doGet(trackingId, store.getType().name(),
                    store.getName(), encodedPath);

            final CountDownLatch transferLatch = new CountDownLatch(1);

            responseUni.subscribe().with(
                    response ->
                    {
                        ResponseBody responseBody = response.body();
                        try
                        {
                            if ( response.code() == HttpStatus.SC_NOT_FOUND )
                            {
                                http.writeNotFoundTransfer(store, path);
                            }
                            else
                            {
                                try ( InputStream bodyInputStream = responseBody.byteStream() )
                                {
                                    http.writeExistingTransfer(bodyInputStream, writeBody, response.headers());
                                }
                            }
                        }
                        catch (IOException e)
                        {
                            logger.error("write transfer error: {}", e.getMessage(), e);
                        }
                        finally
                        {
                            if ( response != null && responseBody != null )
                            {
                                responseBody.close();
                            }
                            transferred = true;
                            transferLatch.countDown();
                        }
                    },
                    throwable ->
                    {
                        try
                        {
                            http.writeError(throwable);
                        }
                        catch (IOException e)
                        {
                            logger.error("write error: {}", e.getMessage(), e);
                        }
                        finally
                        {
                            transferred = true;
                            transferLatch.countDown();
                        }
                    }
            );

            transferLatch.await( 5, TimeUnit.MINUTES ); // Wait until the item or a failure is emitted

            if ( meter != null )
            {
                meter.reportResponseSummary();
            }
        }
        catch (Exception exception)
        {
            logger.error("doTransfer error: {}", exception.getMessage(), exception);
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

                    if ( user != null )
                    {
                        if ( user.endsWith( TRACKED_USER_SUFFIX ) && user.length() > TRACKED_USER_SUFFIX.length() )
                        {
                            tk = new TrackingKey(StringUtils.substring(user, 0, -TRACKED_USER_SUFFIX.length()));
                        }
                        else
                        {
                            tk = new TrackingKey(user);
                        }
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

    /**
     * Raw content-length/connection header breaks http2 protocol. Exclude them and let lower layer regenerate it.
     * Allow all headers when it is HEAD request.
     */
    private boolean isHeaderAllowed(Pair<? extends String, ? extends String> header, HttpMethod method )
    {
        if ( method == HEAD )
        {
            return true;
        }
        String key = header.getFirst();
        return !FORBIDDEN_HEADERS.contains( key.toLowerCase() );
    }

}
