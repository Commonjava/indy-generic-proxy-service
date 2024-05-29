/**
 * Copyright (C) 2021-2023 Red Hat, Inc. (https://github.com/Commonjava/service-parent)
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
package org.commonjava.indy.service.httprox.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.Startup;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

@Startup
@ApplicationScoped
public class ServiceProxyConfig
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final String USER_DIR = System.getProperty( "user.dir" ); // where the JVM was invoked

    @JsonProperty( "read-timeout" )
    private String readTimeout;

    public String getReadTimeout()
    {
        return readTimeout;
    }

    private volatile Retry retry;

    private Set<ServiceConfig> services = Collections.synchronizedSet( new HashSet<>() );

    public Set<ServiceConfig> getServices()
    {
        return services;
    }

    public Retry getRetry()
    {
        return retry;
    }

    @Override
    public String toString()
    {
        return "ProxyConfiguration{" + "readTimeout='" + readTimeout + '\'' + ", retry=" + retry + ", services="
                + services + '}';
    }

    @PostConstruct
    void init()
    {
        load();
        logger.info( "Proxy config, {}", this );
    }

    private static final String PROXY_YAML = "application.yaml";

    /**
     * Load proxy config from '${user.dir}/config/application.yaml'. If not found, load from default classpath resource.
     */
    public void load()
    {
        File file = new File( USER_DIR, "config/" + PROXY_YAML );
        if ( file.exists() )
        {
            logger.info( "Load proxy config from file, {}", file );
            try(FileInputStream fis = new FileInputStream( file ))
            {
                doLoad( fis );
            }
            catch ( IOException e )
            {
                logger.error( "Load failed", e );
                return;
            }
        }
        else
        {
            logger.info( "Skip loading proxy config - no such file: {}", file );
        }
    }

    private void doLoad( InputStream res )
    {
        try
        {
            String str = IOUtils.toString( res, UTF_8 );

            ServiceProxyConfig parsed = parseConfig( str );
            logger.info( "Loaded: {}", parsed );

            if ( parsed.readTimeout != null )
            {
                this.readTimeout = parsed.readTimeout;
            }

            this.retry = parsed.retry;

            if ( parsed.services != null )
            {
                parsed.services.forEach( sv -> {
                    overrideIfPresent( sv );
                } );
            }

        }
        catch ( IOException e )
        {
            logger.error( "Load failed", e );
        }
    }

    private void overrideIfPresent( ServiceConfig sv )
    {
        this.services.remove( sv ); // remove first so it can replace the old one
        this.services.add( sv );
    }

    private ServiceProxyConfig parseConfig( String str )
    {
        Yaml yaml = new Yaml();
        Map<String, Object> obj = yaml.load( str );
        Map<String, Object> proxy = (Map) obj.get( "service_proxy" );
        JsonObject jsonObject = JsonObject.mapFrom( proxy );
        ServiceProxyConfig ret = jsonObject.mapTo( ServiceProxyConfig.class );
        if ( ret.services != null )
        {
            logger.info("load service config........");
            ret.services.forEach( sv -> sv.normalize() );
        }
        return ret;
    }

    public static class Retry
    {
        public int count;

        public long interval; // in millis

        @Override
        public String toString()
        {
            return "Retry{" + "count=" + count + ", interval=" + interval + '}';
        }

    }

}
