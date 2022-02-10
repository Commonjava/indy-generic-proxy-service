package org.commonjava.service.httprox;

import io.quarkus.test.junit.QuarkusTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.dto.StoreListingDTO;
import org.junit.jupiter.api.Test;

import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class ProxyHttpsWithTrackingIdTest extends AbstractGenericProxyTest
{

    private static final String TRACKING_ID = "A8DinNReIBj9NH";

    private static final String USER = TRACKING_ID + "+tracking";

    private static final String PASS = "password";

    String https_url =
            "https://oss.sonatype.org/content/repositories/releases/org/commonjava/indy/indy-api/1.3.1/indy-api-1.3.1.pom";

    /*protected String getBaseHttproxConfig()
    {
        return DEFAULT_BASE_HTTPROX_CONFIG + "\nsecured=true";
    }*/

    @Test
    public void run() throws Exception
    {
        String ret = get( https_url, true, USER, PASS );
        assertTrue( ret.contains( "<artifactId>indy-api</artifactId>" ) );

    }

}
