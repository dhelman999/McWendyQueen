package com.mcwendyqueen.ratelimit;

import java.util.List;

import com.mcwendyqueen.controller.MenuItemsController;
import com.mcwendyqueen.service.menuitem.MenuItemService;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T04: enabled=false passes everything through. A no-regression guard: it is green on the stubs by design and
 * only means something next to RateLimitFilterTest, which proves the same requests are throttled when enabled.
 */
@WebMvcTest(controllers = MenuItemsController.class)
@Import(RateLimitConfig.class)
@EnableConfigurationProperties(RateLimitProperties.class)
@TestPropertySource(properties = "app.rate-limit.enabled=false")
class RateLimitDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MenuItemService menuItemService;

    @Test
    void disabled_wellOverLimit_allRequestsPass() throws Exception {
        when(menuItemService.getAllMenuItems()).thenReturn(List.of());

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/menu").with(request -> {
                request.setRemoteAddr("10.0.5.1");
                return request;
            })).andExpect(status().isOk());
        }
    }
}
