package org.commonjava.indy.service.httprox.client.repository;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/api/admin/stores")
@RegisterRestClient(configKey="repo-service-api")
public interface RepositoryService
{

    @HEAD
    @Path("/{packageType}/{type: (hosted|group|remote)}/{name}")
    Response repoExists(@PathParam("packageType") String packageType, @PathParam("type") String type, @PathParam("name") String name );


    @POST
    @Path("/{packageType}/{type: (hosted|group|remote)}")
    Response createStore(@PathParam("packageType") String packageType, @PathParam("type") String type, String store );

    @GET
    @Path("/{packageType}/{type: (remote)}/query/byUrl")
    Response getRemoteByUrl(@PathParam("packageType") String packageType, @PathParam("type") String type, @QueryParam("url") String url );
}
