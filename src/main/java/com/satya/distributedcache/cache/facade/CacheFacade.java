package com.satya.distributedcache.cache.facade;

public interface CacheFacade<K, V> {

    V get(K key);

    void set(K key, V value);

    void del(K key);

    boolean exists(K key);

    boolean expire(K key, long ttlMillis);

    long ttl(K key);

    boolean persist(K key);

    String getStats();
}

