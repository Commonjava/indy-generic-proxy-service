package org.commonjava.service.httprox.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.http.util.EntityUtils;
import org.commonjava.indy.model.core.*;
import org.commonjava.indy.model.core.dto.StoreListingDTO;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.pkg.PackageTypeConstants;
import org.commonjava.indy.service.httprox.client.repository.RepositoryService;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

@QuarkusTest
public class RepositoryServiceTest
{

    @Inject
    @RestClient
    RepositoryService repositoryService;

    //@Test
    public void testArtifactStoreExists()
    {
        repositoryService.repoExists("maven", "hosted", "pnc-builds");
    }


    //@Test
    public void testCreateStore() throws JsonProcessingException {

        HostedRepository hostedRepository = new HostedRepository(PackageTypeConstants.PKG_TYPE_GENERIC_HTTP,"test-generic-0001");
        hostedRepository.setDescription("test creating hosted via REST");
        hostedRepository.setPathStyle(PathStyle.hashed);

        repositoryService.createStore(PackageTypeConstants.PKG_TYPE_GENERIC_HTTP, "hosted", new IndyObjectMapper(false).writeValueAsString(hostedRepository));
    }

    //@Test
    public void testGetRemoteByUrl()
    {
        Response response = repositoryService.getRemoteByUrl(PackageTypeConstants.PKG_TYPE_GENERIC_HTTP, "remote", "http://download.jboss.org:80/");
        StoreListingDTO<RemoteRepository> dto = response.readEntity(StoreListingDTO.class);
        for( RemoteRepository remoteRepository : dto.getItems() )
        {
            System.out.println(remoteRepository.getName());
        }
    }

    //@Test
    public void testGetArtifactStore()
    {
        Response response = repositoryService.getStore(PackageTypeConstants.PKG_TYPE_GENERIC_HTTP, "group", "g-fasterxml-github-com-build-35505");
        System.out.println(response.getStatus());
        ArtifactStore artifactStore = response.readEntity(ArtifactStore.class);
        System.out.println(artifactStore.getName());

    }

}
