package com.satya.distributedcache.cache.eviction;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncEvictionManager<K, V> implements EvictionManager<K, V> {

    private final int maxSize;
    private final EvictionStrategy<K, V> evictionStrategy;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ExecutorService executorService;
    private final AtomicBoolean evictionScheduled = new AtomicBoolean(false);

    public AsyncEvictionManager(int maxSize, EvictionStrategy<K, V> evictionStrategy) {
        this.maxSize = maxSize > 0 ? maxSize : 2000000;
        this.evictionStrategy = evictionStrategy;
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "AsyncEvictionManager-" + System.identityHashCode(this));
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void onPut(K key, Map<K, V> cache, ConcurrentLinkedDeque<K> accessTracker) {
        evictionStrategy.onPut(key, accessTracker);

        if (cache.size() > maxSize && evictionScheduled.compareAndSet(false, true)) {
            executorService.submit(() -> evictAsync(cache, accessTracker));
        }
    }

    @Override
    public void onAccess(K key, ConcurrentLinkedDeque<K> accessTracker) {
        evictionStrategy.onAccess(key, accessTracker);
    }

    @Override
    public void onRemove(K key, ConcurrentLinkedDeque<K> accessTracker) {
        evictionStrategy.onRemove(key, accessTracker);
    }

    private void evictAsync(Map<K, V> cache, ConcurrentLinkedDeque<K> accessTracker) {
        lock.writeLock().lock();
        try {
            if (cache.size() > maxSize) {
                evictionStrategy.evict(cache, accessTracker, maxSize);
            }
        } finally {
            lock.writeLock().unlock();
            evictionScheduled.set(false);
        }
    }

    @Override
    public String getStats() {
        return evictionStrategy.getStats();
    }
}

