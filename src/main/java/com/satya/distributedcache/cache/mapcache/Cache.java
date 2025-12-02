package com.satya.distributedcache.cache.mapcache;

import java.util.Map;

public interface Cache<K, V> {

    long size();

    void invalidate(K key);

    void put(K key, V property);

    void putAll(Map<K, V> cacheMap);

    V getUnchecked(K key);

    boolean containsKey(K key);

    V getFromCache(K key);
}
