package org.commonjava.indy.service.httprox.client.content;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/api/content/generic-http")
@RegisterRestClient(configKey="content-retrieval-service-api")
public interface ContentRetrievalService
{

    @GET
    @Path("/{type: (hosted|group|remote)}/{name}/{path: (.+)}")
    Response doGet(@PathParam( "type" ) String type, @PathParam( "name" ) String name, final @PathParam( "path" ) String path);

}
