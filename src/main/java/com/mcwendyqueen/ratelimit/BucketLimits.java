package com.mcwendyqueen.ratelimit;

import java.time.Duration;

/**
 * Bucket shape: holds up to capacity tokens and gains refillTokens every refillPeriod.
 * Buckets count credit in token-nanos: one token costs refillPeriod in nanos, and every elapsed
 * nanosecond adds refillTokens, so fractional refill is exact in integer math.
 */
public record BucketLimits(long capacity, long refillTokens, Duration refillPeriod) {

    public BucketLimits {
        if (capacity < 1 || refillTokens < 1) {
            throw new IllegalArgumentException("capacity and refillTokens must be at least 1");
        }
        if (refillPeriod == null || refillPeriod.isNegative() || refillPeriod.isZero()) {
            throw new IllegalArgumentException("refillPeriod must be positive");
        }
        try {
            Math.multiplyExact(capacity, refillPeriod.toNanos());
        }
        catch (ArithmeticException e) {
            throw new IllegalArgumentException("capacity * refillPeriod in nanos must fit in a long", e);
        }
    }

    /** Credit one token costs. */
    long tokenCost() {
        return refillPeriod.toNanos();
    }

    /** Credit of a full bucket. */
    long maxCredit() {
        return capacity * refillPeriod.toNanos();
    }
}
