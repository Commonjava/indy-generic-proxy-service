package org.commonjava.indy.service.httprox.util;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
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
