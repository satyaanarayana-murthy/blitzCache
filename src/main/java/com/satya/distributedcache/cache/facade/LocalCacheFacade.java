package com.satya.distributedcache.cache.facade;

import com.satya.distributedcache.cache.mapcache.MapCache;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class LocalCacheFacade implements CacheFacade<String, Object> {

    private final MapCache<String, Object> store;

    private final MapCache<String, Long> cacheTTL;

    private final DelayQueue<CacheItem> delayQueue = new DelayQueue<>();

//    private final CachePurger cachePurger;

//    public LocalCacheFacade(MapCache<String, Object> store, MapCache<String, Long> cacheTTL) {
//        this.store = store;
//        this.cacheTTL = cacheTTL;
//    }

    @PostConstruct
    public void initiatePurer() {
        Thread purger = new Thread(() -> {
            while (true) {
                try {
                    CacheItem expiredKey = delayQueue.take();
                    Long currentExpireAt = cacheTTL.getIfPresent(expiredKey.key);
                    long now = System.currentTimeMillis();
                    // Only delete if the queued expireAt matches current and is due
                    if (currentExpireAt != null && currentExpireAt.equals(expiredKey.ttl) && currentExpireAt <= now) {
                        this.del(expiredKey.key);
                        System.out.println("Evicted: " + expiredKey.key);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        purger.setDaemon(true);
        purger.start();
    }

    @Override
    public Object get(String key) {
       if(checkExpiry(key))
           return null;
        return store.getIfPresent(key);
    }

    @Override
    public void set(String key, Object value) {
        store.put(key, value);
    }

    @Override
    public void del(String key) {
        store.invalidate(key);
        cacheTTL.invalidate(key);
    }

    @Override
    public boolean exists(String key) {
        return get(key) != null;
    }

    @Override
    public boolean expire(String key, long ttlMillis) {
        if (!store.containsKey(key)) {
            return false;
        }
        long expireAt = System.currentTimeMillis() + Math.max(0, ttlMillis);
        cacheTTL.put(key, expireAt);
        putTTL(key, expireAt);
        return true;
    }

    @Override
    public long ttl(String key) {
        if(checkExpiry(key))
            return -2L;
        if (!store.containsKey(key)) {
            return -2L;
        }
        Long expireAt = cacheTTL.getIfPresent(key);
        if (expireAt == null) {
            return -1L;
        }
        return expireAt - System.currentTimeMillis();
    }

    @Override
    public boolean persist(String key){
        // Enforce lazy expiration first; persist only if the key still exists
        if(!exists(key)) {
            return false;
        }
        // Remove TTL entry entirely to represent "no TTL"
        cacheTTL.invalidate(key);
        return true;
    }

    @Override
    public String getStats() {
        return store.getStats();
    }

    public void putTTL(String key, long ttlMillis) {
        delayQueue.put(new CacheItem(key, ttlMillis));
    }

    private boolean checkExpiry(String key) {
        Long expireAt = cacheTTL.getIfPresent(key);
        long now = System.currentTimeMillis();
        if (expireAt != null && expireAt <= now) {
            store.invalidate(key);
            cacheTTL.invalidate(key);
            return true;
        }
        return false;
    }

    @Data
    @AllArgsConstructor
    private static class CacheItem implements Delayed {
        private String key;
        private long ttl;

        @Override
        public long getDelay(TimeUnit unit) {
            long diff = ttl - System.currentTimeMillis();
            return unit.convert(diff, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(this.ttl, ((CacheItem) o).ttl);
        }
    }
}

