package com.damir.cache.core;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LruCacheTest {

    @Test
    void putThenGetReturnsTheValue() {
        LruCache<String, String> cache = new LruCache<>(2);
        cache.put("a", "1", 0);

        assertEquals(Optional.of("1"), cache.get("a"));
    }

    @Test
    void missingKeyReturnsEmpty() {
        LruCache<String, String> cache = new LruCache<>(2);
        assertTrue(cache.get("nope").isEmpty());
    }

    @Test
    void evictsTheLeastRecentlyUsedWhenOverCapacity() {
        LruCache<String, String> cache = new LruCache<>(2);
        cache.put("a", "1", 0);
        cache.put("b", "2", 0);
        cache.put("c", "3", 0);

        assertTrue(cache.get("a").isEmpty());
        assertEquals(Optional.of("2"), cache.get("b"));
        assertEquals(Optional.of("3"), cache.get("c"));
    }

    @Test
    void accessingAKeyMakesItMostRecentlyUsed() {
        LruCache<String, String> cache = new LruCache<>(2);
        cache.put("a", "1", 0);
        cache.put("b", "2", 0);

        cache.get("a");
        cache.put("c", "3", 0);

        assertTrue(cache.get("b").isEmpty());
        assertEquals(Optional.of("1"), cache.get("a"));
        assertEquals(Optional.of("3"), cache.get("c"));
    }

    @Test
    void updatingAnExistingKeyDoesNotCountAsANewEntry() {
        LruCache<String, String> cache = new LruCache<>(2);
        cache.put("a", "1", 0);
        cache.put("b", "2", 0);
        cache.put("a", "1-updated", 0);

        assertEquals(2, cache.size());
        assertEquals(Optional.of("1-updated"), cache.get("a"));
    }

    @Test
    void expiredEntryIsTreatedAsMissingOnGet() throws InterruptedException {
        LruCache<String, String> cache = new LruCache<>(2);
        cache.put("a", "1", 20);

        assertEquals(Optional.of("1"), cache.get("a"));
        Thread.sleep(50);
        assertTrue(cache.get("a").isEmpty());
    }

    @Test
    void zeroTtlMeansNoExpiry() throws InterruptedException {
        LruCache<String, String> cache = new LruCache<>(2);
        cache.put("a", "1", 0);
        Thread.sleep(30);
        assertEquals(Optional.of("1"), cache.get("a"));
    }

    @Test
    void removeDeletesTheEntry() {
        LruCache<String, String> cache = new LruCache<>(2);
        cache.put("a", "1", 0);

        assertTrue(cache.remove("a"));
        assertTrue(cache.get("a").isEmpty());
        assertFalse(cache.remove("a"));
    }

    @Test
    void statsTrackHitsMissesAndEvictions() {
        LruCache<String, String> cache = new LruCache<>(1);
        cache.put("a", "1", 0);
        cache.get("a");
        cache.get("missing");
        cache.put("b", "2", 0);

        CacheStats stats = cache.stats();
        assertEquals(1, stats.hits());
        assertEquals(1, stats.misses());
        assertEquals(1, stats.evictions());
        assertEquals(1, stats.size());
        assertEquals(1, stats.capacity());
    }

    @Test
    void backgroundSweeperRemovesExpiredEntriesWithoutBeingAccessed() throws InterruptedException {
        LruCache<String, String> cache = new LruCache<>(10, 1);
        cache.put("a", "1", 20);

        Thread.sleep(1500);

        assertEquals(0, cache.size());
        cache.close();
    }

    @Test
    void constructorRejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new LruCache<String, String>(0));
    }
}
