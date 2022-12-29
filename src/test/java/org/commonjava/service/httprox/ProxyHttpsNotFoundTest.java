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
package org.commonjava.service.httprox;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class ProxyHttpsNotFoundTest extends AbstractGenericProxyTest
{

    private static final String USER = "user";

    private static final String PASS = "password";

    String https_url =
            "https://oss.sonatype.org/content/repositories/releases/org/commonjava/indy/indy-api/no.pom";

    @Test
    public void run() throws Exception
    {
        String ret = get( https_url, true, USER, PASS );
        assertTrue( ret.contains( "404 Not Found" ) );
    }

}
