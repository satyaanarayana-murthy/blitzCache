package com.satya.distributedcache.cache.eviction;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

public class NoopEvictionStrategy<K, V> implements EvictionStrategy<K, V> {
    @Override
    public void onAccess(K key, ConcurrentLinkedDeque<K> accessTracker) {
        // no-op
    }

    @Override
    public void onPut(K key, ConcurrentLinkedDeque<K> accessTracker) {
        // no-op
    }

    @Override
    public int evict(Map<K, V> cache, ConcurrentLinkedDeque<K> accessTracker, int maxSize) {
        // never evict for TTL index
        return 0;
    }

    @Override
    public void onRemove(K key, ConcurrentLinkedDeque<K> accessTracker) {
        // no-op
    }

    @Override
    public String getName() {
        return "NOOP";
    }

    @Override
    public String getStats() {
        return "NoopEvictionStrategy: [no evictions]";
    }
}

