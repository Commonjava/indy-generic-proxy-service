package org.commonjava.service.httprox;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.smallrye.common.constraint.Assert.assertTrue;

@QuarkusTest
public class ProxyHttpsTest extends AbstractGenericProxyTest
{

    private static final String USER = "user";

    private static final String PASS = "password";

    String https_url =
            "https://oss.sonatype.org/content/repositories/releases/org/commonjava/indy/indy-api/1.3.1/indy-api-1.3.1.pom";

    @Test
    public void run() throws Exception
    {
        String ret = get( https_url, true, USER, PASS );
        assertTrue( ret.contains( "<artifactId>indy-api</artifactId>" ) );

    }
}
