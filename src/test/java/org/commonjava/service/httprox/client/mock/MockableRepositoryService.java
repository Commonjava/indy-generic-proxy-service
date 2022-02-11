package org.commonjava.service.httprox.client.mock;

import io.quarkus.test.Mock;
import org.commonjava.indy.model.core.*;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.service.httprox.client.repository.RepositoryService;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Mock
@RestClient
public class MockableRepositoryService implements RepositoryService
{

    Map<StoreKey, ArtifactStore> artifactStoreMap = new HashMap<>();

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
}
