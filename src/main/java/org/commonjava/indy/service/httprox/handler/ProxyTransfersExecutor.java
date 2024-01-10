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
package org.commonjava.indy.service.httprox.handler;

import jakarta.inject.Named;
import jakarta.ws.rs.Produces;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftExecutorService;
import org.commonjava.cdi.util.weft.WeftManaged;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;

import static org.commonjava.cdi.util.weft.ExecutorConfig.BooleanLiteral.FALSE;


public class ProxyTransfersExecutor {

    @Named("mitm-transfers")
    @ApplicationScoped
    @Produces
    public ManagedExecutor getExecutor()
    {

        return ManagedExecutor.builder().maxAsync(100)
            .maxQueued(200)
            .build();
    }
}
