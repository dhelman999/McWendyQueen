package com.mcwendyqueen.model.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.mcwendyqueen.model.order.OrderStatus.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderEventLightPayload {
    private long id;

    private String name;

    private OrderStatusEnum orderStatus;
}
