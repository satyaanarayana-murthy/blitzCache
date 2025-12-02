package com.satya.distributedcache.cache.mapcache;

public abstract class AbstractCache<K, V> implements Cache<K, V> {

    @Override
    public V getUnchecked(K key){
        if(this.containsKey(key)){
            return getFromCache(key);
        }
        return null;
    }

}
