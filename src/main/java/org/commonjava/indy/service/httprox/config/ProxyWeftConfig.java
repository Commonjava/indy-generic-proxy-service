package org.commonjava.indy.service.httprox.config;


import org.commonjava.cdi.util.weft.config.DefaultWeftConfig;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

public class ProxyWeftConfig
{

    private final DefaultWeftConfig weftConfig = new DefaultWeftConfig();

    @Produces
    @Default
    public DefaultWeftConfig getWeftConfig()
    {
        return weftConfig;
    }

}
