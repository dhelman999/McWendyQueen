package com.mcwendyqueen.ratelimit;

import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties("app.rate-limit")
public class RateLimitProperties {

    private boolean enabled;

    @Min(1)
    private int maxTrackedKeys;

    @NotNull
    @DurationMin(nanos = 0, inclusive = false)
    private Duration sweepInterval;

    @Valid
    @NotNull
    private final Groups groups = new Groups();

    @Getter
    @Setter
    public static class Groups {

        @Valid
        @NotNull
        private final Group general = new Group();

        @Valid
        @NotNull
        private final Group orders = new Group();
    }

    @Getter
    @Setter
    public static class Group {

        @Min(1)
        private long capacity;

        @Min(1)
        private long refillTokens;

        @NotNull
        @DurationMin(nanos = 0, inclusive = false)
        private Duration refillPeriod;
    }
}
