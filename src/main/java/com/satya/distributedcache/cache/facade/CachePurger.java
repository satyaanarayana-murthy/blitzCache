package com.satya.distributedcache.cache.facade;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Component
public class CachePurger {

    private final LocalCacheFacade localCacheFacade;

    private final DelayQueue<CacheItem> delayQueue = new DelayQueue<>();

    public CachePurger(LocalCacheFacade localCacheFacade) {
        this.localCacheFacade = localCacheFacade;
        Thread purger = new Thread(() -> {
            while (true) {
                try {
                    CacheItem expiredKey = delayQueue.take();
                    localCacheFacade.del(expiredKey.key);
                    System.out.println("Evicted: " + expiredKey.key);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        purger.setDaemon(true);
        purger.start();
    }

    public void put(String key, long ttlMillis) {
        delayQueue.put(new CacheItem(key, ttlMillis));
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

