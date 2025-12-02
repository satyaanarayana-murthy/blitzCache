package com.satya.distributedcache.cache.mapcache;

public abstract class AbstractCache<K, V> implements Cache<K, V> {

    @Override
    public V getIfPresent(K key){
        return getFromCache(key);
    }

}
