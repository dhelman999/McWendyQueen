package com.mcwendyqueen.ratelimit;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.mcwendyqueen.ratelimit.RouteGroup.GENERAL;
import static com.mcwendyqueen.ratelimit.RouteGroup.ORDERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T05: the idle sweep and the tracked-key cap, on a hand-driven clock. Logic tests call sweepOnce()
 * directly; only the shutdown tests start the timer.
 */
@ExtendWith(OutputCaptureExtension.class)
class BucketSweeperTest {

    private static final String CAP_WARN = "Rate limit tracked-key cap reached";

    private static final long SECOND = 1_000_000_000L;

    private static final Duration INTERVAL = Duration.ofMinutes(1);

    // general: 3 tokens, 1 per second (full again 3s after draining); orders: 2 tokens, 1 per 10s
    private static final Map<RouteGroup, BucketLimits> LIMITS = Map.of(
            GENERAL, new BucketLimits(3, 1, Duration.ofSeconds(1)),
            ORDERS, new BucketLimits(2, 1, Duration.ofSeconds(10)));

    private final AtomicLong clock = new AtomicLong(1_000 * SECOND);

    private final ExecutorService pool = Executors.newFixedThreadPool(2);

    @AfterEach
    void stopPool() {
        pool.shutdownNow();
    }

    @Test
    void sweepOnce_bucketsRefilledToFull_removesThem() {
        BucketStore store = store(1000);
        BucketSweeper sweeper = new BucketSweeper(store, INTERVAL);
        store.tryConsume("10.5.0.1", GENERAL);
        store.tryConsume("10.5.0.2", ORDERS, GENERAL);

        // stored credit is still short; only the refill up to sweep time makes them full
        clock.addAndGet(10 * SECOND);

        assertEquals(2, sweeper.sweepOnce());
        assertEquals(0, store.trackedKeys());
    }

    @Test
    void sweepOnce_partDrainedBucket_keepsItAndItsState() {
        BucketStore store = store(1000);
        BucketSweeper sweeper = new BucketSweeper(store, INTERVAL);
        store.tryConsume("10.5.1.1", ORDERS, GENERAL);
        store.tryConsume("10.5.1.1", ORDERS, GENERAL);
        store.tryConsume("10.5.1.2", GENERAL);

        // general is full again after 3s, but orders needs 20s: client 1 is part-drained, client 2 is full
        clock.addAndGet(5 * SECOND);

        assertEquals(1, sweeper.sweepOnce());
        assertEquals(1, store.trackedKeys());
        // client 1 kept its state: orders is still half a token short, where a fresh bucket would allow
        assertFalse(store.tryConsume("10.5.1.1", ORDERS).allowed());
    }

    @Test
    void sweepOnce_sweptKeySeenAgain_behavesLikeFreshFullBucket() {
        BucketStore store = store(1000);
        BucketSweeper sweeper = new BucketSweeper(store, INTERVAL);
        for (int i = 0; i < 3; i++) {
            assertTrue(store.tryConsume("10.5.2.1", GENERAL).allowed());
        }
        clock.addAndGet(3 * SECOND);
        assertEquals(1, sweeper.sweepOnce());

        for (int i = 0; i < 3; i++) {
            assertTrue(store.tryConsume("10.5.2.1", GENERAL).allowed(), "token " + i);
        }
        RateLimitDecision fourth = store.tryConsume("10.5.2.1", GENERAL);
        assertFalse(fourth.allowed());
        assertEquals(1, fourth.retryAfterSeconds());
    }

    @Test
    void tryConsume_capReached_newKeyPassesUnlimitedAndWarnsOncePerInterval(CapturedOutput output) {
        BucketStore store = store(2);
        store.tryConsume("10.5.3.1", GENERAL);
        store.tryConsume("10.5.3.2", GENERAL);

        // far past general capacity (3), all allowed and never tracked
        for (int i = 0; i < 10; i++) {
            assertTrue(store.tryConsume("10.5.3.3", ORDERS, GENERAL).allowed(), "call " + i);
            assertTrue(store.tryConsume("10.5.3.4", GENERAL).allowed(), "call " + i);
        }
        assertEquals(2, store.trackedKeys());
        assertEquals(1, countCapWarnings(output));
        assertFalse(output.getAll().contains("10.5.3."), "the cap WARN must not log client addresses");

        // tracked keys are still limited while the cap is reached
        store.tryConsume("10.5.3.1", GENERAL);
        store.tryConsume("10.5.3.1", GENERAL);
        assertFalse(store.tryConsume("10.5.3.1", GENERAL).allowed());

        clock.addAndGet(INTERVAL.toNanos() - 1);
        store.tryConsume("10.5.3.5", GENERAL);
        assertEquals(1, countCapWarnings(output), "no second WARN inside one interval");

        clock.addAndGet(1);
        store.tryConsume("10.5.3.5", GENERAL);
        assertEquals(2, countCapWarnings(output), "one more WARN once the interval has passed");
    }

    @Test
    void tryConsume_capFreedBySweep_tracksNewKeyAgain() {
        BucketStore store = store(1);
        BucketSweeper sweeper = new BucketSweeper(store, INTERVAL);
        store.tryConsume("10.5.4.1", GENERAL);
        clock.addAndGet(SECOND);
        sweeper.sweepOnce();

        for (int i = 0; i < 3; i++) {
            assertTrue(store.tryConsume("10.5.4.2", GENERAL).allowed());
        }
        assertFalse(store.tryConsume("10.5.4.2", GENERAL).allowed());
        assertEquals(1, store.trackedKeys());
    }

    /**
     * Each cycle advances the clock until general is full again, then asks for capacity + 1 tokens. With
     * an atomic sweep every cycle allows exactly capacity. A check-then-remove sweep could drop the entry
     * between two consumes, and the next consume would see a fresh bucket and get an extra token.
     */
    @Test
    void sweepOnce_racingTryConsumeOnOneKey_neverGrantsExtraTokens() throws Exception {
        for (int round = 0; round < 20; round++) {
            BucketStore store = store(1000);
            BucketSweeper sweeper = new BucketSweeper(store, INTERVAL);
            int cycles = 5_000;
            AtomicBoolean consuming = new AtomicBoolean(true);
            CountDownLatch start = new CountDownLatch(1);

            Future<?> sweeps = pool.submit(() -> {
                start.await();
                while (consuming.get()) {
                    sweeper.sweepOnce();
                }
                return null;
            });
            Future<Integer> consumes = pool.submit(() -> {
                start.await();
                int allowed = 0;
                try {
                    for (int cycle = 0; cycle < cycles; cycle++) {
                        clock.addAndGet(3 * SECOND);
                        for (int call = 0; call < 4; call++) {
                            if (store.tryConsume("10.5.5.1", GENERAL).allowed()) {
                                allowed++;
                            }
                        }
                    }
                }
                finally {
                    consuming.set(false);
                }
                return allowed;
            });
            start.countDown();

            assertEquals(3 * cycles, consumes.get(60, TimeUnit.SECONDS), "round " + round);
            sweeps.get(60, TimeUnit.SECONDS);
        }
    }

    @Test
    void start_tenMillisecondInterval_sweepsOnTheTimer() throws Exception {
        BucketStore store = store(1000);
        BucketSweeper sweeper = new BucketSweeper(store, Duration.ofMillis(10));
        store.tryConsume("10.5.6.1", GENERAL);
        clock.addAndGet(60 * SECOND);
        assertEquals(1, store.trackedKeys());

        sweeper.start();
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (store.trackedKeys() > 0 && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            assertEquals(0, store.trackedKeys(), "the scheduled sweep never ran within 5s");
        }
        finally {
            sweeper.close();
        }
    }

    @Test
    void close_startedSweeper_stopsItsThread() throws Exception {
        Set<Thread> before = sweeperThreads();
        BucketSweeper sweeper = new BucketSweeper(store(1000), Duration.ofMillis(10));
        sweeper.start();
        // the executor starts its thread when the first run is scheduled
        Set<Thread> started = sweeperThreads();
        started.removeAll(before);
        assertEquals(1, started.size(), "start() runs exactly one sweeper thread");
        assertTrue(started.iterator().next().isDaemon());

        sweeper.close();

        for (Thread thread : started) {
            thread.join(TimeUnit.SECONDS.toMillis(5));
            assertFalse(thread.isAlive(), "sweeper thread leaked after close()");
        }
        // a second close is a no-op
        sweeper.close();
    }

    @Test
    void contextClose_sweeperBean_stopsItsThread() throws Exception {
        Set<Thread> before = sweeperThreads();
        Set<Thread> started = new HashSet<>();

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
                .withUserConfiguration(RateLimitBeans.class, RateLimitConfig.class)
                .withPropertyValues(
                        "app.rate-limit.enabled=true",
                        "app.rate-limit.max-tracked-keys=10",
                        "app.rate-limit.sweep-interval=10ms",
                        "app.rate-limit.groups.general.capacity=3",
                        "app.rate-limit.groups.general.refill-tokens=1",
                        "app.rate-limit.groups.general.refill-period=1s",
                        "app.rate-limit.groups.orders.capacity=2",
                        "app.rate-limit.groups.orders.refill-tokens=1",
                        "app.rate-limit.groups.orders.refill-period=10s")
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());
                    started.addAll(sweeperThreads());
                    started.removeAll(before);

                    // max-tracked-keys=10 must reach the store: the 11th new client is not tracked
                    BucketStore store = ctx.getBean(BucketStore.class);
                    for (int i = 0; i < 11; i++) {
                        assertTrue(store.tryConsume("10.5.7." + i, GENERAL).allowed());
                    }
                    assertEquals(10, store.trackedKeys());
                });

        assertEquals(1, started.size(), "the context starts exactly one sweeper thread");
        for (Thread thread : started) {
            thread.join(TimeUnit.SECONDS.toMillis(5));
            assertFalse(thread.isAlive(), "sweeper thread leaked after context close");
        }
    }

    private BucketStore store(int maxTrackedKeys) {
        return new BucketStore(LIMITS, clock::get, maxTrackedKeys, INTERVAL);
    }

    private static long countCapWarnings(CapturedOutput output) {
        return output.getAll().lines().filter(line -> line.contains("WARN") && line.contains(CAP_WARN)).count();
    }

    private static Set<Thread> sweeperThreads() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().equals(BucketSweeper.THREAD_NAME))
                .collect(Collectors.toSet());
    }

    @Configuration
    @EnableConfigurationProperties(RateLimitProperties.class)
    static class RateLimitBeans {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
