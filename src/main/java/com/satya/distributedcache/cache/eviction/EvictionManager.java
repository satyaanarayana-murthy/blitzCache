package com.satya.distributedcache.cache.eviction;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

public interface EvictionManager<K, V> {

    void onPut(K key, Map<K, V> cache, ConcurrentLinkedDeque<K> accessTracker);

    void onAccess(K key, ConcurrentLinkedDeque<K> accessTracker);

    void onRemove(K key, ConcurrentLinkedDeque<K> accessTracker);

    String getStats();
}

