package com.mcwendyqueen.ratelimit;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T03: app.rate-limit binds from yml and environment, and an invalid value stops startup naming the property.
 */
class RateLimitPropertiesTest {

    private static final String PREFIX = "app.rate-limit.";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(EnableRateLimitProperties.class);

    @Test
    void bind_testYml_matchesPinnedTestValues() {
        runner.withInitializer(ctx -> addYaml(ctx, new ClassPathResource("application.yml")))
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());
                    RateLimitProperties properties = ctx.getBean(RateLimitProperties.class);

                    assertTrue(properties.isEnabled());
                    assertEquals(1000, properties.getMaxTrackedKeys());
                    assertEquals(Duration.ofMinutes(1), properties.getSweepInterval());
                    assertGroup(properties.getGroups().getGeneral(), 3, 1, Duration.ofSeconds(1));
                    assertGroup(properties.getGroups().getOrders(), 2, 1, Duration.ofSeconds(10));
                });
    }

    @Test
    void bind_mainYml_placeholdersBindAndPassValidation() {
        // The test yml shadows the main one on the classpath, so read the ops-facing file from disk.
        runner.withInitializer(ctx -> addYaml(ctx, new FileSystemResource("src/main/resources/application.yml")))
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());
                    RateLimitProperties properties = ctx.getBean(RateLimitProperties.class);

                    assertTrue(properties.isEnabled());
                    assertTrue(properties.getMaxTrackedKeys() >= 1);
                    assertNotNull(properties.getSweepInterval());
                    assertTrue(properties.getGroups().getGeneral().getCapacity() >= 1);
                    assertTrue(properties.getGroups().getOrders().getCapacity() >= 1);
                    assertNotNull(properties.getGroups().getOrders().getRefillPeriod());
                });
    }

    @Test
    void bind_environmentVariable_overridesYml() {
        runner.withInitializer(ctx -> {
                    addYaml(ctx, new ClassPathResource("application.yml"));
                    ctx.getEnvironment().getPropertySources().addFirst(new SystemEnvironmentPropertySource(
                            "test-systemEnvironment", Map.of("APP_RATELIMIT_GROUPS_ORDERS_CAPACITY", "7")));
                })
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());
                    assertEquals(7, ctx.getBean(RateLimitProperties.class).getGroups().getOrders().getCapacity());
                });
    }

    @Test
    void bind_enabledFalse_isExposed() {
        withTestYml(PREFIX + "enabled=false")
                .run(ctx -> assertFalse(ctx.getBean(RateLimitProperties.class).isEnabled()));
    }

    @Test
    void bind_ordersCapacityZero_failsStartupNamingProperty() {
        assertStartupFailsNaming(PREFIX + "groups.orders.capacity=0", "groups.orders.capacity");
    }

    @Test
    void bind_generalCapacityZero_failsStartupNamingProperty() {
        assertStartupFailsNaming(PREFIX + "groups.general.capacity=0", "groups.general.capacity");
    }

    @Test
    void bind_refillTokensZero_failsStartupNamingProperty() {
        assertStartupFailsNaming(PREFIX + "groups.orders.refill-tokens=0", "groups.orders.refillTokens");
    }

    @Test
    void bind_refillPeriodZero_failsStartupNamingProperty() {
        assertStartupFailsNaming(PREFIX + "groups.general.refill-period=0s", "groups.general.refillPeriod");
    }

    @Test
    void bind_maxTrackedKeysZero_failsStartupNamingProperty() {
        assertStartupFailsNaming(PREFIX + "max-tracked-keys=0", "maxTrackedKeys");
    }

    @Test
    void bind_sweepIntervalZero_failsStartupNamingProperty() {
        assertStartupFailsNaming(PREFIX + "sweep-interval=0s", "sweepInterval");
    }

    @Test
    void registration_componentScan_registersExactlyOneBean() {
        // The app registers the class once, by component scan. RateLimitConfig must not enable it again:
        // a refreshed context processes any @EnableConfigurationProperties and would add a second bean.
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        ConfigurationPropertiesAutoConfiguration.class, ValidationAutoConfiguration.class))
                .withUserConfiguration(RateLimitPackageScan.class)
                .withInitializer(ctx -> addYaml(ctx, new ClassPathResource("application.yml")))
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());
                    assertEquals(List.of("rateLimitProperties"), List.of(ctx.getBeanNamesForType(RateLimitProperties.class)));
                });
    }

    private ApplicationContextRunner withTestYml(String... overrides) {
        return runner.withInitializer(ctx -> addYaml(ctx, new ClassPathResource("application.yml")))
                .withPropertyValues(overrides);
    }

    private void assertStartupFailsNaming(String override, String field) {
        withTestYml(override).run(ctx -> {
            Throwable failure = ctx.getStartupFailure();
            assertNotNull(failure, override + " must stop startup");

            String messages = allMessages(failure);
            assertTrue(messages.contains("app.rate-limit") && messages.contains(field),
                    "startup failure must name the property " + field + ", got: " + messages);
        });
    }

    private static void assertGroup(RateLimitProperties.Group group, long capacity, long refillTokens, Duration period) {
        assertEquals(capacity, group.getCapacity());
        assertEquals(refillTokens, group.getRefillTokens());
        assertEquals(period, group.getRefillPeriod());
    }

    private static void addYaml(ConfigurableApplicationContext ctx, Resource resource) {
        try {
            for (PropertySource<?> source : new YamlPropertySourceLoader().load(resource.getDescription(), resource)) {
                ctx.getEnvironment().getPropertySources().addLast(source);
            }
        }
        catch (IOException ex) {
            throw new IllegalStateException("Cannot read " + resource, ex);
        }
    }

    private static String allMessages(Throwable failure) {
        StringBuilder messages = new StringBuilder();

        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            messages.append(cause.getMessage()).append('\n');
        }

        return messages.toString();
    }

    @EnableConfigurationProperties(RateLimitProperties.class)
    static class EnableRateLimitProperties {
    }

    // Scans the main ratelimit classes only; this test class and its nested configs are excluded.
    @Configuration
    @ComponentScan(basePackageClasses = RateLimitProperties.class, useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = {RateLimitProperties.class, RateLimitConfig.class}))
    static class RateLimitPackageScan {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
