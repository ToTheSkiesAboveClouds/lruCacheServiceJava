package com.damir.cache.web;

import com.damir.cache.core.CacheStats;
import com.damir.cache.core.LruCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/cache")
public class CacheController {

    public record PutRequest(String value, Long ttlSeconds) {
    }

    public record CacheEntryResponse(String key, String value) {
    }

    private final LruCache<String, String> cache;

    public CacheController(LruCache<String, String> cache) {
        this.cache = cache;
    }

    @PutMapping("/{key}")
    public ResponseEntity<Void> put(@PathVariable String key, @RequestBody PutRequest request) {
        if (request.value() == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        long ttlMillis = request.ttlSeconds() != null
                ? TimeUnit.SECONDS.toMillis(request.ttlSeconds())
                : 0;
        cache.put(key, request.value(), ttlMillis);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{key}")
    public CacheEntryResponse get(@PathVariable String key) {
        String value = cache.get(key).orElseThrow(() -> new KeyNotFoundException(key));
        return new CacheEntryResponse(key, value);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        boolean removed = cache.remove(key);
        if (!removed) {
            throw new KeyNotFoundException(key);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public CacheStats stats() {
        return cache.stats();
    }
}
