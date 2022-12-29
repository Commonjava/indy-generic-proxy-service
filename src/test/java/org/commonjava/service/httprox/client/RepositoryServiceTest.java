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
