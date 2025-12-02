package com.satya.distributedcache.controller;

import com.satya.distributedcache.service.CacheService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CacheController {

    private final CacheService cacheService;

    public CacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{key}")
    public ResponseEntity<?> getValue(@PathVariable String key) {
        Object value = cacheService.get(key);
        if (value == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(value);
    }

    // Accept JSON payloads
    @PostMapping(path = "/{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> setValueJson(@PathVariable String key, @RequestBody Object value) {
        cacheService.put(key, value);
        return ResponseEntity.ok().build();
    }

    // Accept plain text payloads
    @PostMapping(path = "/{key}", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> setValueText(@PathVariable String key, @RequestBody String value) {
        cacheService.put(key, value);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<?> deleteValue(@PathVariable String key) {
        cacheService.remove(key);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exists/{key}")
    public ResponseEntity<?> exists(@PathVariable String key) {
        return ResponseEntity.ok(cacheService.exists(key));
    }

    @PostMapping("/expire/{key}/{ttl}")
    public ResponseEntity<?> expire(@PathVariable String key, @PathVariable long ttl) {
        return ResponseEntity.ok(cacheService.expire(key, ttl));
    }

    @GetMapping("/ttl/{key}")
    public ResponseEntity<?> ttl(@PathVariable String key) {
        return ResponseEntity.ok(cacheService.ttl(key));
    }

    @PostMapping("/conditional/{key}/{xx}/{ttl}")
    public ResponseEntity<?> conditionalSet(@PathVariable String key, @PathVariable boolean xx, @PathVariable Long ttl, @RequestBody Object value) {
        return ResponseEntity.ok(cacheService.conditionalPut(key, value, ttl, xx));
    }

    @PostMapping("/expireAt/{key}/{ttl}")
    public ResponseEntity<?> expireAt(@PathVariable String key, @PathVariable long ttl) {
        return ResponseEntity.ok(cacheService.expireAt(key, ttl));
    }

    @PostMapping("/persist/{key}")
    public ResponseEntity<?> persist(@PathVariable String key) {
        return ResponseEntity.ok(cacheService.persist(key));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(cacheService.getStats());
    }

}
