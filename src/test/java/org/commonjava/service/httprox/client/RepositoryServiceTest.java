package org.commonjava.service.httprox.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.test.junit.QuarkusTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.PathStyle;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.pkg.PackageTypeConstants;
import org.commonjava.indy.service.httprox.client.repository.RepositoryService;
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
    public void testCreateStore() throws JsonProcessingException {

        HostedRepository hostedRepository = new HostedRepository(PackageTypeConstants.PKG_TYPE_GENERIC_HTTP,"test-generic-0001");
        hostedRepository.setDescription("test creating hosted via REST");
        hostedRepository.setPathStyle(PathStyle.hashed);

        repositoryService.createStore(PackageTypeConstants.PKG_TYPE_GENERIC_HTTP, "hosted", new IndyObjectMapper(false).writeValueAsString(hostedRepository));
    }

}
