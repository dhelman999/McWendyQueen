package com.mcwendyqueen.model.order;

import com.mcwendyqueen.model.event.BaseEventDTO;
import com.mcwendyqueen.model.event.EventTypes;
import com.mcwendyqueen.service.order.OrderServiceImpl;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrderEventDTO extends BaseEventDTO<OrderEventLightPayload> {
    public OrderEventDTO() {
        super();
        setEventType(EventTypes.ORDER.getShortName());
        setSource(BASE_SVC_NAME + OrderServiceImpl.SVC_NAME);
    }

    public OrderEventDTO(Order order) {
        super();
        setEventType(EventTypes.ORDER.getShortName());
        setSource(BASE_SVC_NAME + OrderServiceImpl.SVC_NAME);

        OrderEventLightPayload payload = new OrderEventLightPayload(
                order.getId(), order.getName(), order.getOrderStatus());

        setPayload(payload);
    }
}
