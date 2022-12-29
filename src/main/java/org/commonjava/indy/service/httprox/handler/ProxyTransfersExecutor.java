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

import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftExecutorService;
import org.commonjava.cdi.util.weft.WeftManaged;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static org.commonjava.cdi.util.weft.ExecutorConfig.BooleanLiteral.FALSE;

@ApplicationScoped
public class ProxyTransfersExecutor {

    @Inject
    @WeftManaged
    @ExecutorConfig( named = "mitm-transfers", threads = 20, priority = 5, loadSensitive = FALSE )
    WeftExecutorService executor;

    protected ProxyTransfersExecutor()
    {
    }

    public ProxyTransfersExecutor(WeftExecutorService exec)
    {
        this.executor = exec;
    }

    public WeftExecutorService getExecutor()
    {
        return executor;
    }
}
