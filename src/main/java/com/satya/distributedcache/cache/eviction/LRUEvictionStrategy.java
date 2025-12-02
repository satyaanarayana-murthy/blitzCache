package com.satya.distributedcache.cache.eviction;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

public class LRUEvictionStrategy<K, V> implements EvictionStrategy<K, V> {

    private final AtomicLong accessCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);
    private final AtomicLong hitCount = new AtomicLong(0);

    @Override
    public void onAccess(K key, ConcurrentLinkedDeque<K> accessTracker) {
        if (key == null) {
            return;
        }
        accessTracker.remove(key);
        accessTracker.addLast(key);
        accessCount.incrementAndGet();
        hitCount.incrementAndGet();
    }

    @Override
    public void onPut(K key, ConcurrentLinkedDeque<K> accessTracker) {
        if (key == null) {
            return;
        }
        accessTracker.remove(key);
        accessTracker.addLast(key);
        accessCount.incrementAndGet();
    }

    @Override
    public int evict(Map<K, V> cache, ConcurrentLinkedDeque<K> accessTracker, int maxSize) {
        int evicted = 0;
        while (cache.size() > maxSize) {
            K lruKey = accessTracker.pollFirst();
            if (lruKey == null) {
                break;
            }
            cache.remove(lruKey);
            evicted++;
            evictionCount.incrementAndGet();
        }
        return evicted;
    }

    @Override
    public void onRemove(K key, ConcurrentLinkedDeque<K> accessTracker) {
        if (key == null) {
            return;
        }
        accessTracker.remove(key);
    }

    @Override
    public String getName() {
        return "LRU";
    }

    @Override
    public String getStats() {
        long total = accessCount.get();
        long hits = hitCount.get();
        double hitRate = total > 0 ? (hits * 100.0 / total) : 0.0;
        return String.format(
            "LRU Strategy Stats: [accesses=%d, hits=%d, evictions=%d, hitRate=%.2f%%]",
            total, hits, evictionCount.get(), hitRate
        );
    }
}

