package org.commonjava.service.httprox.client.mock;

import io.quarkus.test.Mock;
import org.commonjava.indy.service.httprox.client.repository.RepositoryService;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.ws.rs.core.Response;

@Mock
@RestClient
public class MockableRepositoryService implements RepositoryService
{
    @Override
    public Response repoExists(String packageType, String type, String name) {
        return null;
    }

    @Override
    public Response createStore(String packageType, String type, String store) {
        return null;
    }
}
