package com.mcwendyqueen.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcwendyqueen.model.condiment.CondimentItem;
import com.mcwendyqueen.model.condiment.CondimentItemRequestDTO;
import com.mcwendyqueen.service.condiment.CondimentItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CondimentItemsController.class)
class CondimentItemsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CondimentItemService condimentItemService;

    @Test
    void createCondiment_happyPath_returnsCreated() throws Exception {
        when(condimentItemService.createCondimentItem(any(CondimentItemRequestDTO.class)))
                .thenReturn(new CondimentItem(1L, "ketchup"));

        CondimentItemRequestDTO request = new CondimentItemRequestDTO("ketchup");

        mockMvc.perform(post("/api/v1/condiment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("ketchup"));
    }

    @Test
    void createCondiment_blankName_returnsBadRequest() throws Exception {
        CondimentItemRequestDTO invalidRequest = new CondimentItemRequestDTO(" ");

        mockMvc.perform(post("/api/v1/condiment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }
}
