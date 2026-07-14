package com.mcwendyqueen.controller;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcwendyqueen.model.condiment.CondimentItemRequestDTO;
import com.mcwendyqueen.model.order.OrderRequestDTO;
import com.mcwendyqueen.service.order.OrderService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.mcwendyqueen.ApiConstants.API_BASE_PATH;
import static com.mcwendyqueen.ApiConstants.CONDIMENTS_PATH;
import static com.mcwendyqueen.ApiConstants.ORDER_PATH;
import static com.mcwendyqueen.ApiConstants.V1_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrdersController.class)
class OrdersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    void createOrder_blankName_returnsBadRequestWithValidationPayload() throws Exception {
        OrderRequestDTO invalidRequest = new OrderRequestDTO(" ");

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void deleteOrderById_missingOrder_returnsNotFound() throws Exception {
        when(orderService.deleteOrder(999L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void deleteCondimentByMissingOrder_returnsNotFound() throws Exception {
        CondimentItemRequestDTO requestBody = new CondimentItemRequestDTO("lettuce");

        when(orderService.removeCondimentFromOrder(eq(999L), any(CondimentItemRequestDTO.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(delete(API_BASE_PATH + V1_PATH + ORDER_PATH + CONDIMENTS_PATH + "/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
