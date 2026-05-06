package com.mcwendyqueen.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class BaseEventDTO {
    public static String BASE_SVC_NAME = "/services";

    private UUID eventId;
    private String eventType;
    private Long timestamp;
    private String source;

    // TODO think about the payload
    private Object payload;

    public BaseEventDTO() {
        eventId = UUID.randomUUID();
        timestamp = System.currentTimeMillis();
    }
}
