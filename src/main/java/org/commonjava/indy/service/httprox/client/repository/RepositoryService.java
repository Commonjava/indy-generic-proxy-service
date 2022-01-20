package org.commonjava.indy.service.httprox.client.repository;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/api/admin/stores")
@RegisterRestClient(configKey="repo-service-api")
public interface RepositoryService
{

    @HEAD
    @Path("/{packageType}/{type: (hosted|group|remote)}/{name}")
    Response repoExists(@PathParam("packageType") String packageType, @PathParam("type") String type, @PathParam("name") String name );

}
