package com.damir.cache.core;

public record CacheStats(long hits, long misses, long evictions, long expirations, int size, int capacity) {
}
