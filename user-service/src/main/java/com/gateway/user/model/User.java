package com.gateway.user.model;

/** Immutable stored value, safe to share between concurrent requests. */
public record User(Long id, String name, String email) {
}
