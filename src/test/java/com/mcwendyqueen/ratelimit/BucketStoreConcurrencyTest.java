package com.mcwendyqueen.ratelimit;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.mcwendyqueen.ratelimit.RouteGroup.GENERAL;
import static com.mcwendyqueen.ratelimit.RouteGroup.ORDERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T02: with a frozen clock nothing refills, so the number of allowed calls must equal capacity exactly.
 */
class BucketStoreConcurrencyTest {

    private static final int ROUNDS = 200;

    private static final int THREADS = 16;

    private static final int CALLS_PER_THREAD = 25;

    private static final long FROZEN_NANOS = 42_000_000_000L;

    private final ExecutorService pool = Executors.newFixedThreadPool(THREADS);

    @AfterEach
    void stopPool() {
        pool.shutdownNow();
    }

    @Test
    void tryConsume_manyThreadsOneKey_allowsExactlyCapacity() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            BucketStore store = store(50, 2);
            AtomicInteger allowed = new AtomicInteger();

            race(thread -> {
                if (store.tryConsume("10.0.0.1", GENERAL).allowed()) {
                    allowed.incrementAndGet();
                }
            });

            assertEquals(50, allowed.get(), "round " + round);
        }
    }

    @Test
    void tryConsume_manyThreadsSeveralKeys_eachKeyAllowsExactlyCapacity() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            BucketStore store = store(10, 2);
            AtomicInteger[] allowedPerKey = {new AtomicInteger(), new AtomicInteger(), new AtomicInteger(), new AtomicInteger()};

            race(thread -> {
                int key = thread % allowedPerKey.length;
                if (store.tryConsume("10.0.1." + key, GENERAL).allowed()) {
                    allowedPerKey[key].incrementAndGet();
                }
            });

            for (int key = 0; key < allowedPerKey.length; key++) {
                assertEquals(10, allowedPerKey[key].get(), "round " + round + ", key " + key);
            }
        }
    }

    @Test
    void tryConsume_combinedAndGeneralRacing_neverLeaksGeneralTokens() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            BucketStore store = store(30, 20);
            AtomicInteger combinedAllowed = new AtomicInteger();
            AtomicInteger generalOnlyAllowed = new AtomicInteger();

            race(thread -> {
                if (thread % 2 == 0) {
                    if (store.tryConsume("10.0.2.1", ORDERS, GENERAL).allowed()) {
                        combinedAllowed.incrementAndGet();
                    }
                }
                else if (store.tryConsume("10.0.2.1", GENERAL).allowed()) {
                    generalOnlyAllowed.incrementAndGet();
                }
            });

            assertTrue(combinedAllowed.get() <= 20, "round " + round + ": orders overspent: " + combinedAllowed.get());
            // general-only demand alone exceeds 30, so every general token is spent by an allowed call
            assertEquals(30, combinedAllowed.get() + generalOnlyAllowed.get(), "round " + round);
        }
    }

    private static BucketStore store(long generalCapacity, long ordersCapacity) {
        return new BucketStore(Map.of(
                GENERAL, new BucketLimits(generalCapacity, 1, Duration.ofSeconds(1)),
                ORDERS, new BucketLimits(ordersCapacity, 1, Duration.ofSeconds(10))),
                () -> FROZEN_NANOS);
    }

    private void race(ThreadBody body) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int thread = 0; thread < THREADS; thread++) {
            int threadIndex = thread;
            futures.add(pool.submit(() -> {
                start.await();
                for (int call = 0; call < CALLS_PER_THREAD; call++) {
                    body.run(threadIndex);
                }
                return null;
            }));
        }
        start.countDown();

        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
    }

    @FunctionalInterface
    private interface ThreadBody {
        void run(int threadIndex);
    }
}
