package com.satya.distributedcache.cache.mapcache;

import com.satya.distributedcache.cache.eviction.AsyncEvictionManager;
import com.satya.distributedcache.cache.eviction.EvictionManager;
import com.satya.distributedcache.cache.eviction.EvictionStrategy;
import com.satya.distributedcache.cache.eviction.LRUEvictionStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public EvictionStrategy<String, Object> evictionStrategy() {
        return new LRUEvictionStrategy<>();
    }

    @Bean
    public <K, V> EvictionManager<K, V> evictionManager(
            @Value("${cache.max-size:2000000}") int maxSize,
            EvictionStrategy<K, V> evictionStrategy) {
        return new AsyncEvictionManager<>(maxSize, evictionStrategy);
    }

    @Bean
    public MapCache<String, Object> mapCache(
            @Value("${cache.max-size:2000000}") int maxSize,
            EvictionManager<String, Object> evictionManager) {
        return new MapCache<>(maxSize, evictionManager);
    }
}

