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
package org.commonjava.indy.service.httprox.stats;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path( "/api/stats" )
public class StatsHandler
{
    @Inject
    Versioning versioning;

    @Path( "version-info" )
    @GET
    @Produces( APPLICATION_JSON )
    public Response getAppVersion()
    {
        return formatOkResponseWithJsonEntity( versioning );
    }

    private Response formatOkResponseWithJsonEntity( final Object dto )
    {
        if ( dto == null )
        {
            return Response.noContent().build();
        }
        Response.ResponseBuilder builder = Response.ok( dto, APPLICATION_JSON );
        return builder.build();
    }
}
