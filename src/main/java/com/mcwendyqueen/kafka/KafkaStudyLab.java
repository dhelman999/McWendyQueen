package com.mcwendyqueen.kafka;

/**
 * Kafka study checklist for interview discussions.
 *
 * Your project already has:
 * - Producer config + KafkaTemplate send on order create
 * - Event envelope (BaseEventDTO + OrderEventDTO payload)
 * - Idempotency on HTTP write path (Idempotency-Key header)
 *
 * Gaps to close in McWendyQueen (recommended order):
 * 1) OrderEventConsumer: real listener + idempotent eventId dedupe
 * 2) KafkaConsumerConfig: manual ack + error handler / DLT topic
 * 3) Producer send callback/logging; use orderId as partition key
 * 4) Optional: outbox table pattern (DB write + event publish atomically)
 *
 * Verbal topics to rehearse:
 * - Consumer groups and rebalance
 * - Partition ordering vs global ordering
 * - Consumer lag monitoring
 * - At-least-once + idempotent consumer vs exactly-once
 */
public final class KafkaStudyLab {

    private KafkaStudyLab() {
    }

    // TODO exercise A: send OrderEventDTO with key = orderId
    // TODO exercise B: consumer updates read model / sends notification
    // TODO exercise C: replay events from earliest offset safely with dedupe
}
