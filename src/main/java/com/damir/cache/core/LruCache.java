package com.damir.cache.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class LruCache<K, V> implements AutoCloseable {

    private static final long NO_EXPIRY = -1L;

    private final class Node {
        final K key;
        V value;
        long expiresAtMillis;
        Node prev;
        Node next;

        Node(K key, V value, long expiresAtMillis) {
            this.key = key;
            this.value = value;
            this.expiresAtMillis = expiresAtMillis;
        }
    }

    private final int capacity;
    private final Map<K, Node> index = new HashMap<>();
    private final Node head = new Node(null, null, NO_EXPIRY);
    private final Node tail = new Node(null, null, NO_EXPIRY);

    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();
    private final AtomicLong expirations = new AtomicLong();

    private final ScheduledExecutorService sweeper;

    public LruCache(int capacity) {
        this(capacity, 0);
    }

    public LruCache(int capacity, long sweepIntervalSeconds) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;

        if (sweepIntervalSeconds > 0) {
            this.sweeper = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "lru-cache-sweeper");
                t.setDaemon(true);
                return t;
            });
            this.sweeper.scheduleAtFixedRate(
                    this::sweepExpired, sweepIntervalSeconds, sweepIntervalSeconds, TimeUnit.SECONDS);
        } else {
            this.sweeper = null;
        }
    }

    public synchronized void put(K key, V value, long ttlMillis) {
        long expiresAt = ttlMillis > 0 ? System.currentTimeMillis() + ttlMillis : NO_EXPIRY;

        Node existing = index.get(key);
        if (existing != null) {
            existing.value = value;
            existing.expiresAtMillis = expiresAt;
            moveToFront(existing);
            return;
        }

        Node node = new Node(key, value, expiresAt);
        index.put(key, node);
        addToFront(node);

        if (index.size() > capacity) {
            Node lru = tail.prev;
            removeNode(lru);
            index.remove(lru.key);
            evictions.incrementAndGet();
        }
    }

    public synchronized Optional<V> get(K key) {
        Node node = index.get(key);
        if (node == null) {
            misses.incrementAndGet();
            return Optional.empty();
        }
        if (isExpired(node)) {
            removeNode(node);
            index.remove(key);
            expirations.incrementAndGet();
            misses.incrementAndGet();
            return Optional.empty();
        }
        moveToFront(node);
        hits.incrementAndGet();
        return Optional.of(node.value);
    }

    public synchronized boolean remove(K key) {
        Node node = index.remove(key);
        if (node == null) {
            return false;
        }
        removeNode(node);
        return true;
    }

    public synchronized int size() {
        return index.size();
    }

    public synchronized CacheStats stats() {
        return new CacheStats(hits.get(), misses.get(), evictions.get(), expirations.get(), index.size(), capacity);
    }

    private synchronized void sweepExpired() {
        Node node = head.next;
        while (node != tail) {
            Node next = node.next;
            if (isExpired(node)) {
                removeNode(node);
                index.remove(node.key);
                expirations.incrementAndGet();
            }
            node = next;
        }
    }

    private boolean isExpired(Node node) {
        return node.expiresAtMillis != NO_EXPIRY && System.currentTimeMillis() >= node.expiresAtMillis;
    }

    private void addToFront(Node node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void moveToFront(Node node) {
        removeNode(node);
        addToFront(node);
    }

    @Override
    public void close() {
        if (sweeper != null) {
            sweeper.shutdownNow();
        }
    }
}
