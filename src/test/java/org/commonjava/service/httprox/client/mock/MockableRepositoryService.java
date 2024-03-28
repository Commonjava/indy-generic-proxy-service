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
package org.commonjava.service.httprox.client.mock;

import io.quarkus.test.Mock;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.dto.StoreListingDTO;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.service.httprox.client.repository.RepositoryService;
import org.commonjava.indy.service.httprox.util.UrlInfo;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mock
@RestClient
public class MockableRepositoryService implements RepositoryService
{

    Map<StoreKey, ArtifactStore> artifactStoreMap = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public Response repoExists(String packageType, String type, String name)
    {
        Response response;
        StoreKey key = new StoreKey(packageType, StoreType.get(type), name);
        if ( artifactStoreMap.containsKey(key) )
        {
            response = Response.ok().build();
        }
        else
        {
            response = Response.status(Response.Status.NOT_FOUND).build();
        }
        return response;
    }

    @Override
    public Response getStore(String packageType, String type, String name) {
        final StoreType st = StoreType.get(type);
        final StoreKey key = new StoreKey(packageType, st, name);
        if ( !artifactStoreMap.containsKey(key) )
        {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }
        else
        {
            final IndyObjectMapper objectMapper = new IndyObjectMapper(false);
            Response.ResponseBuilder builder = Response.ok(new DTOStreamingOutput(objectMapper, artifactStoreMap.get(key)),
                    MediaType.APPLICATION_JSON);
            return builder.build();
        }
    }

    @Override
    public Response createStore(String packageType, String type, String storeInJson)
    {

        final StoreType st = StoreType.get(type);
        final IndyObjectMapper objectMapper = new IndyObjectMapper(false);
        ArtifactStore store;
        try
        {
            store = objectMapper.readValue(storeInJson, st.getStoreClass());
        }
        catch (final IOException e)
        {
            final String message = "Failed to parse " + st.getStoreClass()
                    .getSimpleName() + " from request body.";

            Response.ResponseBuilder builder = Response.status( Response.Status.INTERNAL_SERVER_ERROR ).type( MediaType.TEXT_PLAIN ).entity( message );
            return builder.build();
        }
        if ( store != null )
        {
            //artifactStoreMap.put(store.getKey(), store);
        }
        URI location = UriBuilder.fromUri("mock_indy")
                .path("/api/admin/stores")
                .path(store.getPackageType())
                .path(store.getType().singularEndpointName())
                .build(store.getName());
        Response.ResponseBuilder builder = Response.created( location )
                .entity( new DTOStreamingOutput( objectMapper, store ) )
                .type( MediaType.APPLICATION_JSON );
        return builder.build();
    }

    @Override
    public Response getRemoteByUrl(String packageType, String type, String url) {
        List<RemoteRepository> remotes = new ArrayList<>();
        logger.info("getRemoteByUrl: {}", url);
        UrlInfo temp = null;
        try
        {
            temp = new UrlInfo( url );
        }
        catch ( Exception error )
        {
            logger.warn( "Failed to find repository, url: '{}'. Reason: {}", url, error.getMessage() );
        }

        for ( ArtifactStore store : artifactStoreMap.values() )
        {
            if ( store instanceof RemoteRepository )
            {
                RemoteRepository remoteRepository = (RemoteRepository) store;

                UrlInfo targetUrlInfo = null;
                try
                {
                    targetUrlInfo = new UrlInfo( remoteRepository.getUrl() );
                }
                catch ( Exception error )
                {
                    logger.warn( "Invalid repository, store: {}, url: '{}'. Reason: {}", store.getKey(), remoteRepository.getUrl(), error.getMessage() );
                }
                if ( targetUrlInfo != null )
                {
                    if ( temp.getUrlWithNoSchemeAndLastSlash().equals( targetUrlInfo.getUrlWithNoSchemeAndLastSlash() )
                            && temp.getProtocol().equals( targetUrlInfo.getProtocol() ) )
                    {
                        remotes.add(remoteRepository);
                    }
                }
            }
        }

        final IndyObjectMapper objectMapper = new IndyObjectMapper(false);

        if (remotes == null || remotes.isEmpty())
        {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }
        else
        {
            final StoreListingDTO<RemoteRepository> dto = new StoreListingDTO<>(remotes);
            //TODO fix the response, which does not work as expected
            Response.ResponseBuilder builder = Response.ok( new DTOStreamingOutput( objectMapper, dto ),
                    MediaType.APPLICATION_JSON );
            return builder.build();
        }
    }
}
