package org.commonjava.service.httprox.client;

import io.quarkus.test.junit.QuarkusTest;
import org.commonjava.indy.service.httprox.client.repository.RepositoryService;
import org.commonjava.indy.service.httprox.client.repository.StoreRequest;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

@QuarkusTest
public class RepositoryServiceTest
{

    @Inject
    @RestClient
    RepositoryService repositoryService;

    //@Test
    public void testGetArtifactStore()
    {
        repositoryService.repoExists("maven", "hosted", "pnc-builds");
    }


    //@Test
    public void testCreateStore()
    {
        StoreRequest storeRequest = new StoreRequest();
        storeRequest.setKey("maven:group:test-0001");
        storeRequest.setDescription("test creating group via REST");
        storeRequest.setName("test-0001");
        storeRequest.setPackageType("maven");
        storeRequest.setType("group");
        storeRequest.setPathStyle("hashed");

        repositoryService.createStore("maven", "group", storeRequest);
    }

}
