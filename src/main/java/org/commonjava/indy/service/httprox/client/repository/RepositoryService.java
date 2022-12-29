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
package org.commonjava.indy.service.httprox.client.repository;

import org.commonjava.indy.service.httprox.util.CustomClientRequestFilter;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/api/admin/stores")
@RegisterRestClient(configKey="repo-service-api")
@RegisterProvider(CustomClientRequestFilter.class)
public interface RepositoryService
{

    @HEAD
    @Path("/{packageType}/{type: (hosted|group|remote)}/{name}")
    Response repoExists(@PathParam("packageType") String packageType, @PathParam("type") String type, @PathParam("name") String name );

    @GET
    @Path("/{packageType}/{type: (hosted|group|remote)}/{name}")
    Response getStore(@PathParam("packageType") String packageType, @PathParam("type") String type, @PathParam("name") String name);

    @POST
    @Path("/{packageType}/{type: (hosted|group|remote)}")
    Response createStore(@PathParam("packageType") String packageType, @PathParam("type") String type, String store );

    @GET
    @Path("/{packageType}/{type: (remote)}/query/byUrl")
    Response getRemoteByUrl(@PathParam("packageType") String packageType, @PathParam("type") String type, @QueryParam("url") String url );
}
