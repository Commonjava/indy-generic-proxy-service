package org.commonjava.service.httprox.client.mock;

import io.quarkus.test.Mock;
import org.commonjava.indy.model.core.*;
import org.commonjava.indy.model.core.dto.StoreListingDTO;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.service.httprox.client.repository.RepositoryService;
import org.commonjava.indy.service.httprox.util.UrlInfo;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        else
        {
            final IndyObjectMapper objectMapper = new IndyObjectMapper(false);
            Response.ResponseBuilder builder = Response.ok(new DTOStreamingOutput(objectMapper, artifactStoreMap.get(key)),
                    "application/json");
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
            artifactStoreMap.put(store.getKey(), store);
        }
        URI location = UriBuilder.fromUri("mock_indy")
                .path("/api/admin/stores")
                .path(store.getPackageType())
                .path(store.getType().singularEndpointName())
                .build(store.getName());
        Response.ResponseBuilder builder = Response.created( location )
                .entity( new DTOStreamingOutput( objectMapper, store ) )
                .type( "application/json" );
        return builder.build();
    }

    @Override
    public Response getRemoteByUrl(String packageType, String type, String url) {
        List<RemoteRepository> remotes = new ArrayList<>();

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
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        else
        {
            final StoreListingDTO<RemoteRepository> dto = new StoreListingDTO<>(remotes);
            Response.ResponseBuilder builder = Response.ok( new DTOStreamingOutput( objectMapper, dto ),
                    "application/json" );
            return builder.build();
        }
    }
}
