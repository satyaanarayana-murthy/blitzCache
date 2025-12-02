package com.satya.distributedcache.service;

import com.satya.distributedcache.cache.CacheImpl.RCache;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    RCache<String, Object> cache;

    public Object get(String key) {
        return null;
    }

    public void put(String key, Object value) {
    }

    public void remove(String key) {
    }
}
