package com.mcwendyqueen.ratelimit;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * Sweeps idle clients out of the BucketStore every interval on its own daemon thread. Scheduling is
 * local on purpose: the app has no @EnableScheduling, and turning that on app-wide is a separate call.
 * Spring calls close() on shutdown, which stops the thread.
 */
@Slf4j
public class BucketSweeper implements AutoCloseable {

    static final String THREAD_NAME = "rate-limit-sweeper";

    private static final long SHUTDOWN_WAIT_SECONDS = 5;

    private final BucketStore store;

    private final Duration interval;

    private ScheduledExecutorService executor;

    public BucketSweeper(BucketStore store, Duration interval) {
        if (interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("interval must be positive, got " + interval);
        }
        this.store = store;
        this.interval = interval;
    }

    public synchronized void start() {
        if (executor != null) {
            throw new IllegalStateException("already started");
        }
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
        long intervalNanos = interval.toNanos();
        executor.scheduleWithFixedDelay(this::sweepOnce, intervalNanos, intervalNanos, TimeUnit.NANOSECONDS);
    }

    /** One sweep. Never throws, because an exception would cancel every later run of the schedule. */
    public int sweepOnce() {
        try {
            int removed = store.sweepIdle();
            log.debug("Rate limit sweep removed {} idle clients, {} tracked", removed, store.trackedKeys());
            return removed;
        }
        catch (RuntimeException e) {
            log.error("Rate limit sweep failed", e);
            return 0;
        }
    }

    @Override
    public synchronized void close() {
        if (executor == null) {
            return;
        }
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS)) {
                log.warn("Rate limit sweeper did not stop within {}s", SHUTDOWN_WAIT_SECONDS);
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor = null;
    }
}
