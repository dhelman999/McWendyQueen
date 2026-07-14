package com.mcwendyqueen.service.order;

import java.util.Optional;
import java.util.UUID;

import com.mcwendyqueen.kafka.KafkaAppConfig;
import com.mcwendyqueen.kafka.KafkaMessageProducer;
import com.mcwendyqueen.model.order.Order;
import com.mcwendyqueen.model.order.OrderRepository;
import com.mcwendyqueen.model.order.OrderRequestDTO;
import com.mcwendyqueen.service.condiment.CondimentItemService;
import com.mcwendyqueen.service.eligibilityClient.OrderPolicyEligibilityClient.EligibilityStatus;
import com.mcwendyqueen.service.eligibilityClient.ResiliantOrderPolicyEligibilityClient;
import com.mcwendyqueen.service.menuitem.MenuItemService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceImplTest {

    private OrderRepository orderRepository;

    private MenuItemService menuItemService;

    private CondimentItemService condimentItemService;

    private OrderServiceImpl orderService;

    private KafkaMessageProducer kafkaMessageProducer;

    private KafkaAppConfig kafkaAppConfig;

    private ResiliantOrderPolicyEligibilityClient resiliantOrderPolicyEligibilityClient;

    @BeforeEach
    void setUp() {
        this.orderRepository = Mockito.mock(OrderRepository.class);
        this.menuItemService = Mockito.mock(MenuItemService.class);
        this.condimentItemService = Mockito.mock(CondimentItemService.class);
        this.kafkaMessageProducer = Mockito.mock(KafkaMessageProducer.class);
        this.kafkaAppConfig = Mockito.mock(KafkaAppConfig.class);
        this.resiliantOrderPolicyEligibilityClient =
                Mockito.mock(ResiliantOrderPolicyEligibilityClient.class);

        this.orderService = new OrderServiceImpl(orderRepository, menuItemService,
                condimentItemService, kafkaMessageProducer, kafkaAppConfig,
                resiliantOrderPolicyEligibilityClient);
    }

    @Test
    void createOrder_happyPath_savesAndReturns() {
        OrderRequestDTO request = new OrderRequestDTO("david");

        when(resiliantOrderPolicyEligibilityClient.checkEligibility(any(OrderRequestDTO.class)))
                .thenReturn(EligibilityStatus.APPROVED);
        when(orderRepository.findByName("david")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);

            order.setId(11L);

            return order;
        });

        String idempotencyKey = UUID.randomUUID().toString();
        Order created = orderService.createOrder(request, idempotencyKey);

        assertEquals(11L, created.getId());
        assertEquals("david", created.getName());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void getOrderByName_happyPath_returnsOrder() {
        Order existing = new Order(8L, "amy", null, null, 0L, 0L, null);

        when(orderRepository.findByName("amy")).thenReturn(Optional.of(existing));

        Optional<Order> result = orderService.getOrderByName("amy");

        assertTrue(result.isPresent());
        assertEquals("amy", result.get().getName());
    }

    @Test
    void deleteOrderById_happyPath_deletesAndReturns() {
        Order existing = new Order(6L, "nina", null, null, 0L, 0L, null);

        when(orderRepository.findById(6L)).thenReturn(Optional.of(existing));

        Optional<Order> deleted = orderService.deleteOrder(6L);

        assertTrue(deleted.isPresent());
        assertEquals(6L, deleted.get().getId());
        verify(orderRepository, times(1)).deleteById(6L);
    }
}
