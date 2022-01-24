package org.commonjava.service.httprox.client.mock;

import io.quarkus.test.Mock;
import org.apache.commons.io.FilenameUtils;
import org.commonjava.indy.service.httprox.client.repository.ContentRetrievalService;
import org.commonjava.indy.service.httprox.handler.TransferStreamingOutput;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

@Mock
@RestClient
public class MockableContentRetrievalService implements ContentRetrievalService
{
    @Override
    public Response doGet( String type, String name, String path )
    {

        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream( FilenameUtils.getName(path) );
        if ( in != null )
        {
            final Response.ResponseBuilder builder = Response.ok(
                    new TransferStreamingOutput(in));
            return builder.build();
        }
        else
        {
            Response.ResponseBuilder builder = Response.status(Response.Status.NOT_FOUND ).type( MediaType.TEXT_PLAIN ).entity( "" );
            return builder.build();
        }
    }
}
