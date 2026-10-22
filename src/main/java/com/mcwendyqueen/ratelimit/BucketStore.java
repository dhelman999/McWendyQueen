package com.mcwendyqueen.ratelimit;

import java.time.Duration;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import lombok.extern.slf4j.Slf4j;

/**
 * Per-client token buckets: one entry per client address, holding a bucket for each RouteGroup.
 * Every read and write of an entry happens inside one ConcurrentHashMap.compute() on the client key.
 */
@Slf4j
public class BucketStore {

    private final Map<RouteGroup, BucketLimits> limits;

    private final LongSupplier nanoClock;

    private final int maxTrackedKeys;

    private final long capWarnIntervalNanos;

    private final AtomicLong lastCapWarnNanos;

    private final ConcurrentHashMap<String, ClientBuckets> clients = new ConcurrentHashMap<>();

    /** No tracked-key cap. */
    public BucketStore(Map<RouteGroup, BucketLimits> limits, LongSupplier nanoClock) {
        this(limits, nanoClock, Integer.MAX_VALUE, Duration.ofMinutes(1));
    }

    /**
     * Once maxTrackedKeys clients are tracked, a new client passes unlimited and untracked, and a WARN is
     * logged at most once per capWarnInterval. The cap is soft by up to the number of request threads,
     * because size() is read without a lock; that is fine for a memory backstop.
     */
    public BucketStore(Map<RouteGroup, BucketLimits> limits, LongSupplier nanoClock, int maxTrackedKeys,
            Duration capWarnInterval) {
        EnumMap<RouteGroup, BucketLimits> copy = new EnumMap<>(RouteGroup.class);
        copy.putAll(limits);
        if (copy.size() != RouteGroup.values().length) {
            throw new IllegalArgumentException("limits are required for every RouteGroup, got " + copy.keySet());
        }
        if (maxTrackedKeys < 1) {
            throw new IllegalArgumentException("maxTrackedKeys must be at least 1, got " + maxTrackedKeys);
        }
        this.limits = copy;
        this.nanoClock = nanoClock;
        this.maxTrackedKeys = maxTrackedKeys;
        this.capWarnIntervalNanos = capWarnInterval.toNanos();
        // back-dated one interval so the first cap hit warns
        this.lastCapWarnNanos = new AtomicLong(nanoClock.getAsLong() - capWarnIntervalNanos);
    }

    /**
     * Takes one token from every named group for this client, or from none of them.
     */
    public RateLimitDecision tryConsume(String clientKey, RouteGroup... groups) {
        if (groups.length == 0) {
            throw new IllegalArgumentException("at least one RouteGroup is required");
        }
        Set<RouteGroup> named = EnumSet.of(groups[0], groups);
        RateLimitDecision[] decision = new RateLimitDecision[1];

        clients.compute(clientKey, (key, current) -> {
            // read the clock under the key's lock so time never runs backwards for one entry
            long nowNanos = nanoClock.getAsLong();
            if (current == null && clients.size() >= maxTrackedKeys) {
                // returning null leaves the key untracked
                return null;
            }
            ClientBuckets entry = current != null ? current : ClientBuckets.full(limits, nowNanos);
            ClientBuckets.Outcome outcome = entry.tryConsume(limits, nowNanos, named);
            decision[0] = outcome.decision();
            return outcome.buckets();
        });

        if (decision[0] == null) {
            warnCapReached();
            return RateLimitDecision.allow();
        }
        return decision[0];
    }

    /**
     * Removes every client whose buckets are all full, which is the same state a new client starts in.
     * Each removal is a computeIfPresent on the key, so it holds the same lock tryConsume takes.
     *
     * @return the number of clients removed
     */
    public int sweepIdle() {
        int[] removed = new int[1];
        for (String clientKey : clients.keySet()) {
            clients.computeIfPresent(clientKey, (key, current) -> {
                if (current.isFullAt(limits, nanoClock.getAsLong())) {
                    removed[0]++;
                    return null;
                }
                return current;
            });
        }
        return removed[0];
    }

    /** Number of clients currently tracked. */
    public int trackedKeys() {
        return clients.size();
    }

    private void warnCapReached() {
        long nowNanos = nanoClock.getAsLong();
        long last = lastCapWarnNanos.get();
        if (nowNanos - last >= capWarnIntervalNanos && lastCapWarnNanos.compareAndSet(last, nowNanos)) {
            // no client address here: the cap is about many clients, not one
            log.warn("Rate limit tracked-key cap reached: maxTrackedKeys={} tracked={}; new clients pass unlimited",
                    maxTrackedKeys, clients.size());
        }
    }
}
