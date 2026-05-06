package com.mcwendyqueen.model.order;

import lombok.Getter;

public class OrderStatus {
    @Getter
    public enum OrderStatusEnum {
        CREATED("created"),
        PENDING("pending"),
        QUEUED("queued"),
        IN_PROCESS("in_process"),
        FAILED("failed"),
        CANCELED("canceled"),
        COMPLETED("completed");

        private final String shortName;

        OrderStatusEnum(String shortName) {
            this.shortName = shortName;
        }

        @Override
        public String toString() {
            return shortName;
        }
    }
}
