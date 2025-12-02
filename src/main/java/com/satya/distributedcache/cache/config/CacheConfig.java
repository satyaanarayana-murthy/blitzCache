package com.satya.distributedcache.cache.config;

import com.satya.distributedcache.cache.eviction.AsyncEvictionManager;
import com.satya.distributedcache.cache.eviction.EvictionManager;
import com.satya.distributedcache.cache.eviction.EvictionStrategy;
import com.satya.distributedcache.cache.eviction.LRUEvictionStrategy;
import com.satya.distributedcache.cache.eviction.NoopEvictionStrategy;
import com.satya.distributedcache.cache.mapcache.MapCache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    // Data store eviction (LRU)
    @Bean(name = "dataEvictionStrategy")
    public EvictionStrategy<String, Object> dataEvictionStrategy() {
        return new LRUEvictionStrategy<>();
    }

    @Bean(name = "dataEvictionManager")
    public EvictionManager<String, Object> dataEvictionManager(
            @Value("${cache.max-size:2000000}") int maxSize,
            @Qualifier("dataEvictionStrategy") EvictionStrategy<String, Object> evictionStrategy) {
        return new AsyncEvictionManager<>(maxSize, evictionStrategy);
    }

    @Bean
    public MapCache<String, Object> mapCache(
            @Value("${cache.max-size:2000000}") int maxSize,
            @Qualifier("dataEvictionManager") EvictionManager<String, Object> evictionManager) {
        return new MapCache<>(maxSize, evictionManager);
    }

    // TTL index should not be evicted
    @Bean(name = "ttlEvictionStrategy")
    public EvictionStrategy<String, Long> ttlEvictionStrategy() {
        return new NoopEvictionStrategy<>();
    }

    @Bean(name = "ttlEvictionManager")
    public EvictionManager<String, Long> ttlEvictionManager(
            @Value("${cache.max-size:2000000}") int maxSize,
            @Qualifier("ttlEvictionStrategy") EvictionStrategy<String, Long> evictionStrategy) {
        return new AsyncEvictionManager<>(maxSize, evictionStrategy);
    }

    @Bean
    public MapCache<String, Long> ttlCache(
            @Value("${cache.max-size:2000000}") int maxSize,
            @Qualifier("ttlEvictionManager") EvictionManager<String, Long> evictionManager) {
        return new MapCache<>(maxSize, evictionManager);
    }
}
