package com.satya.distributedcache.cache.mapcache;

import com.satya.distributedcache.cache.eviction.EvictionManager;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class MapCache<K, V> extends AbstractCache<K, V> {
    private final Map<K, V> cache;
    private final int maxSize;
    private final ConcurrentLinkedDeque<K> accessTracker;
    private final EvictionManager<K, V> evictionManager;

    public MapCache(@Value("${cache.max-size:2000000}") int maxSize,
                    EvictionManager<K, V> evictionManager) {
        this.maxSize = maxSize > 0 ? maxSize : 2000000;
        this.cache = new ConcurrentHashMap<>(Math.min(this.maxSize, 16384));
        this.accessTracker = new ConcurrentLinkedDeque<>();
        this.evictionManager = evictionManager;
    }

    @Override
    public long size() {
        return cache.size();
    }

    @Override
    public void invalidate(K key) {
        cache.remove(key);
        evictionManager.onRemove(key, accessTracker);
    }

    @Override
    public void put(K key, V property) {
        cache.put(key, property);
        evictionManager.onPut(key, cache, accessTracker);
    }

    @Override
    public void putAll(Map<K, V> elems) {
        cache.putAll(elems);
        for (K key : elems.keySet()) {
            evictionManager.onPut(key, cache, accessTracker);
        }
    }

    @Override
    public boolean containsKey(K key) {
        return cache.containsKey(key);
    }

    @Override
    public V getFromCache(K key) {
        V value = cache.get(key);
        if (value != null) {
            evictionManager.onAccess(key, accessTracker);
        }
        return value;
    }

    public String getStats() {
        return String.format(
            "MapCache Stats: [size=%d, maxSize=%d, %s]",
            cache.size(), maxSize, evictionManager.getStats()
        );
    }
}
