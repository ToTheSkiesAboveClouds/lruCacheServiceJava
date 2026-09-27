package com.damir.cache.web;

public class KeyNotFoundException extends RuntimeException {
    public KeyNotFoundException(String key) {
        super("key not found (or expired): " + key);
    }
}
