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
    @ExecutorConfig( named = "mitm-transfers", threads = 0, priority = 5, loadSensitive = FALSE )
    private WeftExecutorService executor;

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
