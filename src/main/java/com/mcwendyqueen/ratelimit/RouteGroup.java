package com.mcwendyqueen.ratelimit;

/**
 * Route groups that each get their own bucket per client. Names match the keys under
 * app.rate-limit.groups.
 */
public enum RouteGroup {
    GENERAL,
    ORDERS
}
