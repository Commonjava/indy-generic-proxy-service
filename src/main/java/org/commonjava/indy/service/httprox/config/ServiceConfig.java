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
