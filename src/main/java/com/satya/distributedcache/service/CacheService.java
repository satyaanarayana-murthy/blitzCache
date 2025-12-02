package com.satya.distributedcache.service;

import com.satya.distributedcache.cache.facade.CacheFacade;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    private final CacheFacade<String, Object> cache;

    public CacheService(CacheFacade<String, Object> cache) {
        this.cache = cache;
    }

    public Object get(String key) {
        return cache.get(key);
    }

    public void put(String key, Object value) {
        cache.set(key, value);
    }

    public void remove(String key) {
        cache.del(key);
    }

    public boolean exists(String key) {
        return cache.exists(key);
    }

    public boolean expire(String key,  long ttl) {
        return cache.expire(key, ttl);
    }

    public long ttl(String key) {
        return cache.ttl(key);
    }

    //xx -> set if exist
    public boolean conditionalPut(String key, Object value, Long ttlMillis, boolean xx) {
        if(ttlMillis == null){
            ttlMillis = -1L;
        }
        if((xx && !cache.exists(key) || (!xx && cache.exists(key)))) {
            return false;
        }
        put(key, value);
        expire(key, ttlMillis);
        return true;
    }

    public boolean expireAt(String key, long epochMillis) {
        // If key doesn't exist (after lazy expiration), nothing to do
        if (!cache.exists(key)) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (epochMillis <= now) {
            // Immediate expiration
            return cache.expire(key, 0);
        }
        return cache.expire(key, epochMillis - now);
    }

    public boolean persist(String key) {
        return cache.persist(key);
    }

    public Object getStats() {
        return cache.getStats();
    }
}
