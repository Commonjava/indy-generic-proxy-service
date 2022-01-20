package org.commonjava.service.httprox.client;

import io.quarkus.test.junit.QuarkusTest;
import org.commonjava.indy.service.httprox.client.repository.ContentRetrievalService;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

@QuarkusTest
public class ContentRetrievalServiceTest
{

    @Inject
    @RestClient
    ContentRetrievalService contentRetrievalService;

    @Test
    public void testRetrievalContent()
    {

        contentRetrievalService.doGet("group", "pnc-builds", "org/apache/activemq/artemis-tools/2.7.0.redhat-00054/artemis-tools-2.7.0.redhat-00054.jar");

    }

}
