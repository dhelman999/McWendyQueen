package com.mcwendyqueen.ratelimit;

/**
 * Outcome of one tryConsume call. retryAfterSeconds is only meaningful when denied.
 */
public record RateLimitDecision(boolean allowed, long retryAfterSeconds) {

    private static final RateLimitDecision ALLOWED = new RateLimitDecision(true, 0);

    public static RateLimitDecision allow() {
        return ALLOWED;
    }

    public static RateLimitDecision deny(long retryAfterSeconds) {
        return new RateLimitDecision(false, retryAfterSeconds);
    }
}
