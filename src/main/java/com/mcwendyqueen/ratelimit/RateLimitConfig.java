package com.mcwendyqueen.ratelimit;

import java.util.Map;
import java.util.function.LongSupplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcwendyqueen.exception.RequestIdFilter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.mcwendyqueen.ApiConstants.API_BASE_PATH;
import static com.mcwendyqueen.ApiConstants.V1_PATH;

/**
 * Builds the rate limiter. RateLimitProperties is a @Component, so it is injected here rather than
 * enabled with @EnableConfigurationProperties, which would register a second bean of the same type.
 */
@Configuration
public class RateLimitConfig {

    public static final int FILTER_ORDER = RequestIdFilter.ORDER + 10;

    // Injected by type everywhere, so tests can replace it with a @Primary frozen clock.
    @Bean
    public LongSupplier rateLimitClock() {
        return System::nanoTime;
    }

    @Bean
    public BucketStore bucketStore(RateLimitProperties properties, LongSupplier nanoClock) {
        RateLimitProperties.Groups groups = properties.getGroups();

        return new BucketStore(Map.of(
                RouteGroup.GENERAL, limits("general", groups.getGeneral()),
                RouteGroup.ORDERS, limits("orders", groups.getOrders())), nanoClock,
                properties.getMaxTrackedKeys(), properties.getSweepInterval());
    }

    // Spring infers close() as the destroy method, which stops the sweeper thread on shutdown.
    @Bean(initMethod = "start")
    public BucketSweeper bucketSweeper(BucketStore bucketStore, RateLimitProperties properties) {
        return new BucketSweeper(bucketStore, properties.getSweepInterval());
    }

    @Bean
    public RateLimitFilter rateLimitFilter(BucketStore bucketStore, RateLimitProperties properties,
            ObjectMapper objectMapper) {
        return new RateLimitFilter(bucketStore, properties, objectMapper);
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter rateLimitFilter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(rateLimitFilter);
        registration.addUrlPatterns(API_BASE_PATH + V1_PATH + "/*");
        registration.setOrder(FILTER_ORDER);

        return registration;
    }

    // Bean validation cannot see the capacity * refill-period overflow, so name the group when it fails.
    private static BucketLimits limits(String name, RateLimitProperties.Group group) {
        try {
            return new BucketLimits(group.getCapacity(), group.getRefillTokens(), group.getRefillPeriod());
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid app.rate-limit.groups." + name + ": " + e.getMessage(), e);
        }
    }
}
