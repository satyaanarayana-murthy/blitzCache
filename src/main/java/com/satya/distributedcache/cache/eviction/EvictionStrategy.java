package com.satya.distributedcache.cache.eviction;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

public interface EvictionStrategy<K, V> {

    void onAccess(K key, ConcurrentLinkedDeque<K> accessTracker);

    void onPut(K key, ConcurrentLinkedDeque<K> accessTracker);

    int evict(Map<K, V> cache, ConcurrentLinkedDeque<K> accessTracker, int maxSize);

    void onRemove(K key, ConcurrentLinkedDeque<K> accessTracker);

    String getName();

    String getStats();
}

