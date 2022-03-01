package org.commonjava.indy.service.httprox.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class ServiceConfig
{
    public String host;

    public int port;

    public boolean ssl;

    public String methods;

    @JsonProperty( "path-pattern" )
    public String pathPattern;

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
            return true;
        if ( o == null || getClass() != o.getClass() )
            return false;
        ServiceConfig that = (ServiceConfig) o;
        return Objects.equals( methods, that.methods ) && pathPattern.equals( that.pathPattern );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( methods, pathPattern );
    }

    @Override
    public String toString()
    {
        return "ServiceConfig{" + "host='" + host + '\'' + ", port=" + port + ", ssl=" + ssl + ", methods='" + methods
                + '\'' + ", pathPattern='" + pathPattern + '\'' + '}';
    }

    void normalize()
    {
        if ( methods != null )
        {
            methods = methods.toUpperCase();
        }
    }
}
