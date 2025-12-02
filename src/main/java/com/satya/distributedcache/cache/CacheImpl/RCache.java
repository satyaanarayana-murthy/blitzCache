package com.satya.distributedcache.cache.CacheImpl;

import com.satya.distributedcache.cache.mapcache.Cache;

import java.util.Map;

public class RCache<K, V> implements Cache<K, V> {

    @Override
    public long size() {
        return 0;
    }

    @Override
    public Object getFromCache(Object key) {
        return null;
    }

    @Override
    public boolean containsKey(Object key) {
        return false;
    }

    @Override
    public Object getUnchecked(Object key) {
        return null;
    }

    @Override
    public void putAll(Map cacheMap) {

    }

    @Override
    public void put(Object key, Object property) {

    }

    @Override
    public void invalidate(Object key) {

    }
}
