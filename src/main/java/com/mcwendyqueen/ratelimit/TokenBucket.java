package com.mcwendyqueen.ratelimit;

/**
 * Immutable bucket state. credit is in token-nanos (see BucketLimits); lastRefillNanos is the clock
 * reading credit was last brought up to date at. Only ever used inside BucketStore's compute().
 */
record TokenBucket(long credit, long lastRefillNanos) {

    static TokenBucket full(BucketLimits limits, long nowNanos) {
        return new TokenBucket(limits.maxCredit(), nowNanos);
    }

    /**
     * Brings credit up to nowNanos, never above capacity. Elapsed time is now - last, which stays
     * correct across a nanoTime wrap, and is clamped to the time-to-full before multiplying.
     */
    TokenBucket refill(BucketLimits limits, long nowNanos) {
        long elapsed = nowNanos - lastRefillNanos;
        if (elapsed <= 0) {
            return this;
        }
        long missing = limits.maxCredit() - credit;
        if (elapsed >= ceilDiv(missing, limits.refillTokens())) {
            return new TokenBucket(limits.maxCredit(), nowNanos);
        }
        // elapsed < missing / refillTokens here, so the product cannot overflow
        return new TokenBucket(credit + elapsed * limits.refillTokens(), nowNanos);
    }

    boolean hasToken(BucketLimits limits) {
        return credit >= limits.tokenCost();
    }

    TokenBucket consume(BucketLimits limits) {
        return new TokenBucket(credit - limits.tokenCost(), lastRefillNanos);
    }

    /** Nanos until one whole token is available; 0 if one already is. */
    long nanosUntilToken(BucketLimits limits) {
        long shortfall = limits.tokenCost() - credit;
        return shortfall <= 0 ? 0 : ceilDiv(shortfall, limits.refillTokens());
    }

    boolean isFull(BucketLimits limits) {
        return credit >= limits.maxCredit();
    }

    /** Ceiling division for a non-negative dividend and positive divisor (Math.ceilDiv is Java 18+). */
    static long ceilDiv(long dividend, long divisor) {
        long quotient = dividend / divisor;
        return dividend % divisor == 0 ? quotient : quotient + 1;
    }
}
