package com.mcwendyqueen.model.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageListenerResponseDTO {
    String listenerId;
    String groupId;
    boolean running;
    boolean pauseRequested;
    boolean containerPaused;
    boolean inExpectedState;
    boolean autoStartup;
    int assignedPartitionCount;
    List<String> assignedPartitions;
}
