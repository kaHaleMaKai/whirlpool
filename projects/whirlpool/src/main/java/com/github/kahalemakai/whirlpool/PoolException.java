package com.github.kahalemakai.whirlpool;

import lombok.val;

/**
 * The {@code PoolException} should be thrown
 * whenever an interaction with a {@link Poolable}
 * instance fails and no other more significant action
 * is available, or if a checked exception needs to be
 * wrapped.
 */
public class PoolException extends RuntimeException {
    public PoolException() {
    }

    public PoolException(String message) {
        super(message);
    }

    public PoolException(String message, Throwable cause) {
        super(message, cause);
    }

    public PoolException(Throwable cause) {
        super(cause);
    }

    public static PoolException poolClosed() {
        val msg = "the pool instance has been closed";
        return new PoolException(msg);
    }
}
