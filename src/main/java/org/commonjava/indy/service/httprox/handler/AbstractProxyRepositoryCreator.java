/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.commonjava.indy.model.core.*;
import org.commonjava.indy.service.httprox.util.UrlInfo;
import org.commonjava.indy.service.httprox.util.UserPass;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static org.commonjava.indy.model.core.ArtifactStore.METADATA_ORIGIN;
import static org.commonjava.indy.model.core.ArtifactStore.TRACKING_ID;
import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;
import static org.commonjava.indy.model.core.PathStyle.hashed;
import static org.commonjava.indy.service.httprox.util.HttpProxyConstants.PROXY_REPO_PREFIX;

public abstract class AbstractProxyRepositoryCreator
                implements ProxyRepositoryCreator
{
    @Override
    public abstract ProxyCreationResult create(String trackingID, String name, String baseUrl, UrlInfo urlInfo,
                                               UserPass userPass, Logger logger );

    @Override
    public String formatId( String host, int port, int index, String trackingID, String storeType )
    {
        host = host.replaceAll( "\\.", "-" );
        if ( trackingID == null )
        {
            return PROXY_REPO_PREFIX + host + "_" + port + ( index > 0 ? "_" + index : "" );
        }
        else
        {
            String prefix = null;
            switch ( storeType )
            {
                case "group":
                    prefix = "g";
                    break;
                case "remote":
                    prefix = "r";
                    break;
                case "hosted":
                    prefix = "h";
                    break;
                default:
                    break;
            }
            return prefix + "-" + host + "-" + trackingID;
        }
    }

    protected RemoteRepository createRemote(String name, String baseUrl, UrlInfo info, UserPass up, Logger logger )
    {
        return createRemote( null, name, baseUrl, info, up, logger );
    }

    protected RemoteRepository createRemote( String trackingID, String name, String baseUrl, UrlInfo info, UserPass up,
                                             Logger logger )
    {
        RemoteRepository remote = new RemoteRepository( GENERIC_PKG_KEY, name, baseUrl );
        if ( up != null )
        {
            remote.setUser( up.getUser() );
            remote.setPassword( up.getPassword() );
        }
        if ( trackingID == null )
        {
            remote.setPassthrough( true ); // to prevent long-term caching of content, default false
        }

        remote.setTimeoutSeconds( 5 * 60 ); // 5 minutes
        remote.setIgnoreHostnameVerification( true ); // not verify peer cert against hostname to avoid some redirected hostname inconsistency

        setPropsAndMetadata( remote, trackingID, info );
        return remote;
    }

    protected HostedRepository createHosted(String trackingID, String name, UrlInfo info, Logger logger )
    {
        HostedRepository hosted = new HostedRepository( GENERIC_PKG_KEY, name );
        setPropsAndMetadata( hosted, trackingID, info );
        return hosted;
    }

    protected Group createGroup(String trackingID, String name, UrlInfo info, Logger logger, StoreKey... constituents )
    {
        Group group = new Group( GENERIC_PKG_KEY, name, constituents );
        setPropsAndMetadata( group, trackingID, info );
        return group;
    }

    private void setPropsAndMetadata(ArtifactStore store, String trackingID, UrlInfo info )
    {
        store.setDescription( "HTTProx proxy based on: " + info.getUrl() );
        store.setPathStyle( hashed );

        store.setMetadata( METADATA_ORIGIN, ProxyAcceptHandler.HTTPROX_ORIGIN );
        if ( trackingID != null )
        {
            store.setMetadata( TRACKING_ID, trackingID );
        }
    }

    /**
     * Get the remote repo names with name starts with base name (PROXY_REPO_PREFIX + host + "_" + port)
     */
    public Predicate<ArtifactStore> getNameFilter(String name )
    {
        return store -> store.getName().startsWith( name );
    }

    /**
     * Get the next distinct name based on the query result by filter of getNameFilter
     * @param names
     * @return
     */
    public String getNextName( List<String> names )
    {
        if ( names.isEmpty() )
        {
            return null;
        }
        String name0 = names.get( 0 );
        if ( names.size() == 1 )
        {
            return name0 + "_1";
        }
        Collections.sort( names );
        String last = names.get( names.size() - 1 );
        String index = last.substring( last.lastIndexOf( "_" ) + 1 );
        return name0 + "_" + ( Integer.parseInt( index ) + 1 );
    }

}
