package com.mcwendyqueen.controller;

import com.mcwendyqueen.service.kafka.KafkaAdminListenerService;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = KafkaAdminListenerController.class)
class KafkaAdminListenerControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KafkaAdminListenerService kafkaAdminListenerService;

    @Test
    void getAllListeners_happyPath_returnsMappedListenerDtos() throws Exception {
        MessageListenerContainer listener = mock(MessageListenerContainer.class);
        when(listener.getListenerId()).thenReturn("ordersListener");
        when(listener.getGroupId()).thenReturn("listenerId");
        when(listener.isRunning()).thenReturn(true);
        when(listener.isPauseRequested()).thenReturn(false);
        when(listener.isContainerPaused()).thenReturn(false);
        when(listener.isInExpectedState()).thenReturn(true);
        when(listener.isAutoStartup()).thenReturn(true);
        when(listener.getAssignedPartitions()).thenReturn(List.of(new TopicPartition("order_events_topic", 0)));

        when(kafkaAdminListenerService.getAllListeners()).thenReturn(List.of(listener));

        mockMvc.perform(get("/api/v1/admin/kafka/listeners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].listenerId").value("ordersListener"))
                .andExpect(jsonPath("$[0].groupId").value("listenerId"))
                .andExpect(jsonPath("$[0].assignedPartitionCount").value(1));
    }

    @Test
    void pauseListener_notFound_returns404() throws Exception {
        when(kafkaAdminListenerService.pauseListener("missingListener")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/admin/kafka/listeners/pause/missingListener"))
                .andExpect(status().isNotFound());
    }

    @Test
    void resumeListener_happyPath_returnsListenerDto() throws Exception {
        MessageListenerContainer listener = mock(MessageListenerContainer.class);
        when(listener.getListenerId()).thenReturn("ordersListener");
        when(listener.getGroupId()).thenReturn("listenerId");
        when(listener.isRunning()).thenReturn(true);
        when(listener.isPauseRequested()).thenReturn(false);
        when(listener.isContainerPaused()).thenReturn(false);
        when(listener.isInExpectedState()).thenReturn(true);
        when(listener.isAutoStartup()).thenReturn(true);
        when(listener.getAssignedPartitions()).thenReturn(List.of(new TopicPartition("order_events_topic", 0)));
        when(kafkaAdminListenerService.resumeListener("ordersListener")).thenReturn(Optional.of(listener));

        mockMvc.perform(post("/api/v1/admin/kafka/listeners/resume/ordersListener"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listenerId").value("ordersListener"))
                .andExpect(jsonPath("$.assignedPartitionCount").value(1));
    }
}
