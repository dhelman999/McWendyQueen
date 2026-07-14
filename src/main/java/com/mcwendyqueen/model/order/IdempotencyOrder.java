package com.mcwendyqueen.model.order;

import java.util.concurrent.ConcurrentHashMap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdempotencyOrder {

    private String idempotencyKey;

    private String orderName;

    private Order orderResponse;

    private Status status;

    public enum Status {
        CREATED,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    public static IdempotencyOrder hydrateIdempotencyOrder(String idempotencyKey, String orderName,
            ConcurrentHashMap<String, IdempotencyOrder> idempotencyOrderMap) {
        if (idempotencyKey == null || orderName == null || idempotencyOrderMap == null) {
            return null;
        }

        IdempotencyOrder idempotencyOrder = idempotencyOrderMap.get(idempotencyKey);

        if (idempotencyOrder == null) {
            idempotencyOrder = new IdempotencyOrder();
            idempotencyOrder.setOrderName(orderName);
            idempotencyOrder.setIdempotencyKey(idempotencyKey);
            idempotencyOrder.setOrderResponse(null);
            idempotencyOrder.setStatus(Status.CREATED);
            idempotencyOrderMap.put(idempotencyKey, idempotencyOrder);
        }

        return idempotencyOrder;
    }
}
