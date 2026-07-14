package com.mcwendyqueen.kafka;

import com.mcwendyqueen.model.order.OrderEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO interview-prep consumer:
 * 1) Replace println listener in KafkaConfig with this class.
 * 2) Deserialize OrderEventDTO and route by eventType.
 * 3) Add idempotent processing (eventId dedupe set below).
 * 4) Discuss delivery semantics:
 *    - at-most-once (auto commit before processing)
 *    - at-least-once (commit after processing + idempotent consumer)
 *    - effectively-once (transactions or outbox + dedupe)
 * 5) Add retry/DLT pattern for poison messages.
 */
@Slf4j
@Component
public class OrderEventConsumer {

    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    @KafkaListener(
            topics = "${kafka-topics.prod.orders-topic}",
            groupId = "mcwendyqueen-order-consumers",
            containerFactory = "orderEventKafkaListenerContainerFactory"
    )
    public void onOrderEvent(OrderEventDTO event) {
        // TODO: implement consumer logic (idempotent handler, side effects, metrics)
        log.info("Received order event placeholder: eventId={}, type={}",
                event != null ? event.getEventId() : null,
                event != null ? event.getEventType() : null);
    }

    /**
     * TODO: use this helper to make at-least-once processing idempotent.
     */
    protected boolean markProcessedIfNew(String eventId) {
        return processedEventIds.add(eventId);
    }
}
