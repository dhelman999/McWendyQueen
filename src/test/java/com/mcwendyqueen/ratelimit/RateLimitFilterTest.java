package com.mcwendyqueen.ratelimit;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.mcwendyqueen.controller.KafkaAdminListenerController;
import com.mcwendyqueen.controller.MenuItemsController;
import com.mcwendyqueen.controller.OrdersController;
import com.mcwendyqueen.exception.RequestIdFilter;
import com.mcwendyqueen.model.order.Order;
import com.mcwendyqueen.model.order.OrderRequestDTO;
import com.mcwendyqueen.service.kafka.KafkaAdminListenerService;
import com.mcwendyqueen.service.menuitem.MenuItemService;
import com.mcwendyqueen.service.order.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T04: the rate-limit filter in the MVC slice. Uses the pinned test values (general 3 per 1 token/1s,
 * orders 2 per 1 token/10s) on a frozen clock, so nothing refills. Each test uses its own client address
 * because bucket state lives as long as the cached context.
 */
@WebMvcTest(controllers = {MenuItemsController.class, OrdersController.class, KafkaAdminListenerController.class})
@Import({RateLimitConfig.class, RateLimitFilterTest.FrozenClockConfig.class})
@EnableConfigurationProperties(RateLimitProperties.class)
@ExtendWith(OutputCaptureExtension.class)
class RateLimitFilterTest {

    private static final String MENU = "/api/v1/menu";

    private static final String ORDERS = "/api/v1/orders";

    private static final String ADMIN_LISTENERS = "/api/v1/admin/kafka/listeners";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext context;

    @MockBean
    private MenuItemService menuItemService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private KafkaAdminListenerService kafkaAdminListenerService;

    @BeforeEach
    void stubServices() {
        when(menuItemService.getAllMenuItems()).thenReturn(List.of());
        when(orderService.getAllOrders()).thenReturn(List.of());
        when(orderService.createOrder(any(OrderRequestDTO.class), nullable(String.class))).thenReturn(new Order("probe"));
        when(kafkaAdminListenerService.getAllListeners()).thenReturn(List.of());
    }

    @Test
    void registration_apiV1Only_orderedAfterRequestIdFilter() {
        Optional<FilterRegistrationBean<?>> registration = rateLimitRegistration();

        assertTrue(registration.isPresent(), "a FilterRegistrationBean on /api/v1/* must exist");
        assertEquals(List.of("/api/v1/*"), List.copyOf(registration.get().getUrlPatterns()));
        assertTrue(registration.get().getOrder() > RequestIdFilter.ORDER, "must run after RequestIdFilter");
    }

    @Test
    void general_underLimit_passesThenOverLimitIsThrottled() throws Exception {
        String client = "10.0.4.1";

        for (int i = 0; i < 3; i++) {
            menu(client).andExpect(status().isOk());
        }
        menu(client).andExpect(status().isTooManyRequests());
    }

    @Test
    void general_overLimit_returns429WithRetryAfterAndErrorShape() throws Exception {
        String client = "10.0.4.2";
        drainGeneral(client);

        menu(client)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "1"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(429))
                .andExpect(jsonPath("$.status").value("Too Many Requests"))
                .andExpect(jsonPath("$.type").isNotEmpty())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timeStamp").isNotEmpty())
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void general_overLimit_controllerNotCalled() throws Exception {
        String client = "10.0.4.3";
        drainGeneral(client);

        menu(client).andExpect(status().isTooManyRequests());
        menu(client).andExpect(status().isTooManyRequests());

        verify(menuItemService, times(3)).getAllMenuItems();
    }

    @Test
    void general_firstClientThrottled_secondClientUnaffected() throws Exception {
        drainGeneral("10.0.4.4");
        menu("10.0.4.4").andExpect(status().isTooManyRequests());

        for (int i = 0; i < 3; i++) {
            menu("10.0.4.5").andExpect(status().isOk());
        }
    }

    @Test
    void orders_overOrdersLimit_returns429WithOrdersRetryAfter() throws Exception {
        String client = "10.0.4.6";

        createOrder(client).andExpect(status().isCreated());
        createOrder(client).andExpect(status().isCreated());

        createOrder(client)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "10"))
                .andExpect(jsonPath("$.code").value(429));
        verify(orderService, times(2)).createOrder(any(OrderRequestDTO.class), nullable(String.class));
    }

    @Test
    void orders_alsoCountAgainstGeneral() throws Exception {
        String client = "10.0.4.7";
        createOrder(client).andExpect(status().isCreated());
        createOrder(client).andExpect(status().isCreated());

        // general started at 3; the two orders took 2 of them
        menu(client).andExpect(status().isOk());
        menu(client).andExpect(status().isTooManyRequests());
    }

    @Test
    void orders_generalDrained_orderIsThrottledToo() throws Exception {
        String client = "10.0.4.8";
        drainGeneral(client);

        createOrder(client)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "1"));
        verify(orderService, times(0)).createOrder(any(OrderRequestDTO.class), nullable(String.class));
    }

    @Test
    void getOrders_isGeneralNotOrders() throws Exception {
        String client = "10.0.4.9";
        perform(get(ORDERS), client).andExpect(status().isOk());
        perform(get(ORDERS), client).andExpect(status().isOk());

        // if GET /orders had counted as orders, this would be denied
        createOrder(client).andExpect(status().isCreated());
        perform(get(ORDERS), client).andExpect(status().isTooManyRequests());
    }

    @Test
    void admin_isExemptAndDoesNotSpendGeneral() throws Exception {
        String client = "10.0.4.10";

        for (int i = 0; i < 10; i++) {
            perform(get(ADMIN_LISTENERS), client).andExpect(status().isOk());
        }
        for (int i = 0; i < 3; i++) {
            menu(client).andExpect(status().isOk());
        }
    }

    @Test
    void admin_whileClientThrottled_stillAnswers() throws Exception {
        String client = "10.0.4.11";
        drainGeneral(client);
        menu(client).andExpect(status().isTooManyRequests());

        perform(get(ADMIN_LISTENERS), client).andExpect(status().isOk());
    }

    @Test
    void forwardedHeaders_doNotChangeClientIdentity() throws Exception {
        String client = "10.0.4.12";

        for (int i = 0; i < 3; i++) {
            perform(get(MENU).header("X-Forwarded-For", "203.0.113." + i), client).andExpect(status().isOk());
        }
        perform(get(MENU).header("X-Forwarded-For", "203.0.113.99"), client).andExpect(status().isTooManyRequests());
        perform(get(MENU).header("Forwarded", "for=203.0.113.98"), client).andExpect(status().isTooManyRequests());
    }

    @Test
    void forwardedHeaders_sameHeaderDifferentAddress_areSeparateClients() throws Exception {
        for (int i = 0; i < 3; i++) {
            perform(get(MENU).header("X-Forwarded-For", "203.0.113.50"), "10.0.4.13").andExpect(status().isOk());
        }
        perform(get(MENU).header("X-Forwarded-For", "203.0.113.50"), "10.0.4.13").andExpect(status().isTooManyRequests());

        perform(get(MENU).header("X-Forwarded-For", "203.0.113.50"), "10.0.4.14").andExpect(status().isOk());
    }

    @Test
    void throttle_logsOneWarnWithClientGroupAndRequestId(CapturedOutput output) throws Exception {
        String client = "10.0.4.15";
        drainGeneral(client);

        MvcResult throttled = menu(client).andExpect(status().isTooManyRequests()).andReturn();
        String requestId = JsonPath.read(throttled.getResponse().getContentAsString(), "$.requestId");

        List<String> lines = output.getAll().lines().filter(line -> line.contains(requestId)).toList();
        assertEquals(1, lines.size(), "exactly one log line must carry the throttled request id: " + lines);
        String line = lines.get(0);
        assertTrue(line.contains("WARN"), line);
        assertTrue(line.contains(client), line);
        assertTrue(line.toLowerCase().contains("general"), line);
    }

    @Test
    void allowedRequest_logsNoThrottleLine(CapturedOutput output) throws Exception {
        String client = "10.0.4.16";
        menu(client).andExpect(status().isOk());

        assertFalse(output.getAll().contains(client), "an allowed request must not log a throttle line");
    }

    private void drainGeneral(String client) throws Exception {
        for (int i = 0; i < 3; i++) {
            menu(client).andExpect(status().isOk());
        }
    }

    private ResultActions menu(String client) throws Exception {
        return perform(get(MENU), client);
    }

    private ResultActions createOrder(String client) throws Exception {
        return perform(post(ORDERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new OrderRequestDTO("probe"))), client);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String client) throws Exception {
        return mockMvc.perform(request.with(remoteAddress(client)));
    }

    private static RequestPostProcessor remoteAddress(String client) {
        return request -> {
            request.setRemoteAddr(client);
            return request;
        };
    }

    @SuppressWarnings("rawtypes")
    private Optional<FilterRegistrationBean<?>> rateLimitRegistration() {
        Collection<FilterRegistrationBean> registrations = context.getBeansOfType(FilterRegistrationBean.class).values();

        return registrations.stream()
                .filter(registration -> registration.getUrlPatterns().contains("/api/v1/*"))
                .<FilterRegistrationBean<?>>map(registration -> registration)
                .findFirst();
    }

    @TestConfiguration
    static class FrozenClockConfig {

        // RateLimitConfig takes its clock as a LongSupplier by type, so @Primary replaces System::nanoTime.
        @Bean
        @Primary
        LongSupplier frozenRateLimitClock() {
            return () -> 5_000_000_000_000L;
        }
    }
}
