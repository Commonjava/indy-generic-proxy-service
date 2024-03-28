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
package org.commonjava.indy.service.httprox.util;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class CacheProducer
{

    private EmbeddedCacheManager cacheManager;

    private Map<String, Cache> caches = new ConcurrentHashMap<>();

    @PostConstruct
    public void start()
    {
        startEmbeddedManager();
    }

    private void startEmbeddedManager()
    {
        cacheManager = new DefaultCacheManager();
    }

    public synchronized <K, V> Cache<K, V> getCache( String named )
    {
        return caches.computeIfAbsent( named, ( k ) -> buildCache(named));
    }

    private Cache buildCache(String named)
    {
        cacheManager.defineConfiguration( named, new ConfigurationBuilder().build() );
        return cacheManager.getCache(named, Boolean.TRUE);
    }


}
