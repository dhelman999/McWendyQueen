package com.mcwendyqueen.ratelimit;

import java.util.Map;
import java.util.Set;

/**
 * Immutable per-client entry: one TokenBucket per RouteGroup. Each operation returns a new
 * instance, so BucketStore's compute() swaps it and no state escapes the lock.
 */
final class ClientBuckets {

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final TokenBucket[] buckets;

    private ClientBuckets(TokenBucket[] buckets) {
        this.buckets = buckets;
    }

    static ClientBuckets full(Map<RouteGroup, BucketLimits> limits, long nowNanos) {
        RouteGroup[] groups = RouteGroup.values();
        TokenBucket[] buckets = new TokenBucket[groups.length];
        for (RouteGroup group : groups) {
            buckets[group.ordinal()] = TokenBucket.full(limits.get(group), nowNanos);
        }
        return new ClientBuckets(buckets);
    }

    /**
     * Refills the named groups, then takes one token from each of them or from none. A denial carries
     * the longest wait among the groups that are short, in whole seconds rounded up, at least 1.
     */
    Outcome tryConsume(Map<RouteGroup, BucketLimits> limits, long nowNanos, Set<RouteGroup> groups) {
        TokenBucket[] next = buckets.clone();
        long longestWaitNanos = 0;
        for (RouteGroup group : groups) {
            BucketLimits groupLimits = limits.get(group);
            TokenBucket refilled = next[group.ordinal()].refill(groupLimits, nowNanos);
            next[group.ordinal()] = refilled;
            longestWaitNanos = Math.max(longestWaitNanos, refilled.nanosUntilToken(groupLimits));
        }

        if (longestWaitNanos > 0) {
            long retryAfterSeconds = Math.max(1, TokenBucket.ceilDiv(longestWaitNanos, NANOS_PER_SECOND));
            return new Outcome(new ClientBuckets(next), RateLimitDecision.deny(retryAfterSeconds));
        }
        for (RouteGroup group : groups) {
            next[group.ordinal()] = next[group.ordinal()].consume(limits.get(group));
        }
        return new Outcome(new ClientBuckets(next), RateLimitDecision.allow());
    }

    /** True when every bucket would be full at nowNanos, so dropping this entry loses nothing. */
    boolean isFullAt(Map<RouteGroup, BucketLimits> limits, long nowNanos) {
        for (RouteGroup group : RouteGroup.values()) {
            BucketLimits groupLimits = limits.get(group);
            if (!buckets[group.ordinal()].refill(groupLimits, nowNanos).isFull(groupLimits)) {
                return false;
            }
        }
        return true;
    }

    record Outcome(ClientBuckets buckets, RateLimitDecision decision) {
    }
}
