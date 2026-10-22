package com.mcwendyqueen.exception;

import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.servlet.Filter;

import com.mcwendyqueen.controller.MenuItemsController;
import com.mcwendyqueen.service.menuitem.MenuItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T01: RequestIdFilter runs at a pinned, early position so every later filter sees the request id.
 */
@WebMvcTest(controllers = MenuItemsController.class)
@Import(RequestIdFilterTest.ProbeFilterConfig.class)
class RequestIdFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @MockBean
    private MenuItemService menuItemService;

    @BeforeEach
    void clearProbe() {
        ProbeFilterConfig.SEEN_REQUEST_IDS.clear();
    }

    @Test
    void requestIdFilter_declaresPinnedOrder_orderConstantIsUsed() {
        assertEquals(RequestIdFilter.ORDER, orderOf(requestIdFilter));
    }

    @Test
    void laterFilter_singleRequest_seesUuidRequestIdInMdc() throws Exception {
        mockMvc.perform(get("/api/v1/menu")).andExpect(status().isOk());

        assertEquals(1, ProbeFilterConfig.SEEN_REQUEST_IDS.size());
        String seen = ProbeFilterConfig.SEEN_REQUEST_IDS.get(0);
        assertNotNull(seen, "a filter ordered after RequestIdFilter.ORDER must see requestId in the MDC");
        assertDoesNotThrow(() -> UUID.fromString(seen));
    }

    @Test
    void laterFilter_twoRequests_seesFreshRequestIdEachTime() throws Exception {
        mockMvc.perform(get("/api/v1/menu")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/menu")).andExpect(status().isOk());

        assertEquals(2, ProbeFilterConfig.SEEN_REQUEST_IDS.size());
        assertNotNull(ProbeFilterConfig.SEEN_REQUEST_IDS.get(0));
        assertNotEquals(ProbeFilterConfig.SEEN_REQUEST_IDS.get(0), ProbeFilterConfig.SEEN_REQUEST_IDS.get(1));
    }

    // Characterization: green before T01, and must stay green after it.
    @Test
    void requestIdFilter_afterRequest_clearsMdc() throws Exception {
        mockMvc.perform(get("/api/v1/menu")).andExpect(status().isOk());

        assertNull(MDC.get(BaseExceptionMessage.REQUEST_HEADER_ID));
    }

    private static int orderOf(Object bean) {
        if (bean instanceof Ordered ordered) {
            return ordered.getOrder();
        }

        return OrderUtils.getOrder(bean.getClass(), Ordered.LOWEST_PRECEDENCE);
    }

    @TestConfiguration
    static class ProbeFilterConfig {

        static final CopyOnWriteArrayList<String> SEEN_REQUEST_IDS = new CopyOnWriteArrayList<>();

        @Bean
        FilterRegistrationBean<Filter> requestIdProbeFilter() {
            Filter probe = (request, response, chain) -> {
                SEEN_REQUEST_IDS.add(MDC.get(BaseExceptionMessage.REQUEST_HEADER_ID));
                chain.doFilter(request, response);
            };
            FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(probe);
            registration.setOrder(RequestIdFilter.ORDER + 1);

            return registration;
        }
    }
}
