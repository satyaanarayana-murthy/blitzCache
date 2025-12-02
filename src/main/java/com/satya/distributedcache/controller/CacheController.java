package com.satya.distributedcache.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CacheController {

    @GetMapping("health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{key}")
    public ResponseEntity<?> getValue(@PathVariable String key) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{key}")
    public ResponseEntity<?> setValue(@PathVariable String key, @RequestBody Object value) {
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<?> deleteValue(@PathVariable String key) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/exists/{key}")
    public ResponseEntity<?> exists(@PathVariable String key) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/expire/{key}/{ttl}")
    public ResponseEntity<?> expire(@PathVariable String key, @PathVariable long ttl) {
        return ResponseEntity.ok().build();
    }
}
