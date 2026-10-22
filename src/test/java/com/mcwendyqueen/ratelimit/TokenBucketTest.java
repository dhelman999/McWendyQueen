package com.mcwendyqueen.ratelimit;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static com.mcwendyqueen.ratelimit.RouteGroup.GENERAL;
import static com.mcwendyqueen.ratelimit.RouteGroup.ORDERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T02: bucket arithmetic, driven through the pinned BucketStore contract with a fake nanosecond clock.
 * Limits mirror the pinned test values: general 3, 1 token per 1s; orders 2, 1 token per 10s.
 */
class TokenBucketTest {

    private static final long SECOND = 1_000_000_000L;

    private static final String CLIENT = "10.0.0.1";

    private static final BucketLimits GENERAL_LIMITS = new BucketLimits(3, 1, Duration.ofSeconds(1));

    private static final BucketLimits ORDERS_LIMITS = new BucketLimits(2, 1, Duration.ofSeconds(10));

    private final AtomicLong now = new AtomicLong(1_000 * SECOND);

    private final BucketStore store = new BucketStore(Map.of(GENERAL, GENERAL_LIMITS, ORDERS, ORDERS_LIMITS), now::get);

    @Test
    void tryConsume_freshClient_allowsFullBurstThenDenies() {
        assertAllowed(GENERAL, 3);

        RateLimitDecision denied = store.tryConsume(CLIENT, GENERAL);
        assertFalse(denied.allowed());
        assertEquals(1, denied.retryAfterSeconds());
    }

    @Test
    void tryConsume_afterKnownElapsedTime_refillsExactlyThatMany() {
        assertAllowed(GENERAL, 3);
        now.addAndGet(2 * SECOND);

        assertAllowed(GENERAL, 2);
        assertFalse(store.tryConsume(CLIENT, GENERAL).allowed());
    }

    @Test
    void tryConsume_fractionalRefillAcrossDeniedCalls_isNotLost() {
        assertAllowed(GENERAL, 3);

        now.addAndGet(SECOND / 2);
        assertFalse(store.tryConsume(CLIENT, GENERAL).allowed());
        now.addAndGet(SECOND / 2);

        assertTrue(store.tryConsume(CLIENT, GENERAL).allowed());
    }

    @Test
    void retryAfter_justDrained_isFullRefillPeriod() {
        assertAllowed(ORDERS, 2);

        assertEquals(10, store.tryConsume(CLIENT, ORDERS).retryAfterSeconds());
    }

    @Test
    void retryAfter_remainingExactlyWholeSeconds_isExact() {
        assertAllowed(ORDERS, 2);
        now.addAndGet(5 * SECOND);

        assertEquals(5, store.tryConsume(CLIENT, ORDERS).retryAfterSeconds());
    }

    @Test
    void retryAfter_remainingJustOverWholeSecond_roundsUp() {
        assertAllowed(ORDERS, 2);
        now.addAndGet(5 * SECOND - 1);

        assertEquals(6, store.tryConsume(CLIENT, ORDERS).retryAfterSeconds());
    }

    @Test
    void retryAfter_remainingJustUnderWholeSecond_roundsUpToIt() {
        assertAllowed(ORDERS, 2);
        now.addAndGet(5 * SECOND + 1);

        assertEquals(5, store.tryConsume(CLIENT, ORDERS).retryAfterSeconds());
    }

    @Test
    void retryAfter_oneNanosecondRemaining_isAtLeastOne() {
        assertAllowed(ORDERS, 2);
        now.addAndGet(10 * SECOND - 1);

        RateLimitDecision denied = store.tryConsume(CLIENT, ORDERS);
        assertFalse(denied.allowed());
        assertEquals(1, denied.retryAfterSeconds());
    }

    @Test
    void tryConsume_fullRefillPeriodElapsed_allowsAgain() {
        assertAllowed(ORDERS, 2);
        now.addAndGet(10 * SECOND);

        assertTrue(store.tryConsume(CLIENT, ORDERS).allowed());
    }

    @Test
    void tryConsume_longIdle_capsAtCapacity() {
        assertAllowed(GENERAL, 3);
        now.addAndGet(3_600 * SECOND);

        assertAllowed(GENERAL, 3);
        assertFalse(store.tryConsume(CLIENT, GENERAL).allowed());
    }

    @Test
    void tryConsume_hugeIdleWithFastRefill_doesNotOverflow() {
        BucketLimits fast = new BucketLimits(5, 1_000_000, Duration.ofNanos(1));
        BucketStore fastStore = new BucketStore(Map.of(GENERAL, fast, ORDERS, ORDERS_LIMITS), now::get);
        for (int i = 0; i < 5; i++) {
            assertTrue(fastStore.tryConsume(CLIENT, GENERAL).allowed());
        }
        now.addAndGet(Long.MAX_VALUE / 4);

        for (int i = 0; i < 5; i++) {
            assertTrue(fastStore.tryConsume(CLIENT, GENERAL).allowed(), "token " + (i + 1) + " after a long idle");
        }
        assertFalse(fastStore.tryConsume(CLIENT, GENERAL).allowed());
    }

    @Test
    void tryConsume_nanoTimeWrapsPastLongMax_refillsNormally() {
        now.set(Long.MAX_VALUE - SECOND / 2);
        assertAllowed(GENERAL, 3);
        now.addAndGet(SECOND);

        assertTrue(now.get() < 0, "the fake clock must have wrapped for this test to mean anything");
        assertAllowed(GENERAL, 1);
        RateLimitDecision denied = store.tryConsume(CLIENT, GENERAL);
        assertFalse(denied.allowed());
        assertEquals(1, denied.retryAfterSeconds());
    }

    @Test
    void tryConsume_twoClients_haveSeparateBuckets() {
        assertAllowed(GENERAL, 3);
        assertFalse(store.tryConsume(CLIENT, GENERAL).allowed());

        assertTrue(store.tryConsume("10.0.0.2", GENERAL).allowed());
    }

    @Test
    void tryConsume_ordersAndGeneral_ordersEmpty_takesFromNeither() {
        assertTrue(store.tryConsume(CLIENT, ORDERS, GENERAL).allowed());
        assertTrue(store.tryConsume(CLIENT, ORDERS, GENERAL).allowed());

        assertFalse(store.tryConsume(CLIENT, ORDERS, GENERAL).allowed());
        assertFalse(store.tryConsume(CLIENT, ORDERS, GENERAL).allowed());

        // general started at 3 and only the two allowed calls took from it
        assertAllowed(GENERAL, 1);
        assertFalse(store.tryConsume(CLIENT, GENERAL).allowed());
    }

    @Test
    void tryConsume_ordersAndGeneral_generalEmpty_takesFromNeither() {
        assertAllowed(GENERAL, 3);

        RateLimitDecision denied = store.tryConsume(CLIENT, ORDERS, GENERAL);
        assertFalse(denied.allowed());
        assertEquals(1, denied.retryAfterSeconds());

        assertAllowed(ORDERS, 2);
    }

    @Test
    void retryAfter_bothGroupsEmpty_isLongestWait() {
        assertTrue(store.tryConsume(CLIENT, ORDERS, GENERAL).allowed());
        assertTrue(store.tryConsume(CLIENT, ORDERS, GENERAL).allowed());
        assertAllowed(GENERAL, 1);

        assertEquals(10, store.tryConsume(CLIENT, ORDERS, GENERAL).retryAfterSeconds());
    }

    private void assertAllowed(RouteGroup group, int times) {
        for (int i = 0; i < times; i++) {
            assertTrue(store.tryConsume(CLIENT, group).allowed(), group + " call " + (i + 1) + " of " + times);
        }
    }
}
