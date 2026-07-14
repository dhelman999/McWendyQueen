package com.mcwendyqueen.service.order;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

import com.mcwendyqueen.kafka.KafkaAppConfig;
import com.mcwendyqueen.kafka.KafkaMessageProducer;
import com.mcwendyqueen.model.condiment.CondimentItem;
import com.mcwendyqueen.model.condiment.CondimentItemRequestDTO;
import com.mcwendyqueen.model.menuitem.MenuItem;
import com.mcwendyqueen.model.menuitem.MenuItemRequestDTO;
import com.mcwendyqueen.model.order.IdempotencyOrder;
import com.mcwendyqueen.model.order.Order;
import com.mcwendyqueen.model.order.OrderEventDTO;
import com.mcwendyqueen.model.order.OrderRepository;
import com.mcwendyqueen.model.order.OrderRequestDTO;
import com.mcwendyqueen.service.condiment.CondimentItemService;
import com.mcwendyqueen.service.eligibilityClient.OrderPolicyEligibilityClient.EligibilityStatus;
import com.mcwendyqueen.service.eligibilityClient.ResiliantOrderPolicyEligibilityClient;
import com.mcwendyqueen.service.menuitem.MenuItemService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static com.mcwendyqueen.model.order.IdempotencyOrder.hydrateIdempotencyOrder;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    public static final String SVC_NAME = "/orders-service";

    public static final long UNKNOWN_ORDER = -1;

    private final OrderRepository orderRepository;

    private final MenuItemService menuItemService;

    private final CondimentItemService condimentItemService;

    private final KafkaMessageProducer kafkaMessageProducer;

    private final KafkaAppConfig kafkaAppConfig;

    private final ConcurrentHashMap<String, IdempotencyOrder> idempotencyOrderMap = new ConcurrentHashMap<>();

    private final ResiliantOrderPolicyEligibilityClient resiliantOrderPolicyEligibilityClient;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository, MenuItemService menuItemService,
            CondimentItemService condimentItemService, KafkaMessageProducer kafkaMessageProducer,
            KafkaAppConfig kafkaAppConfig,
            ResiliantOrderPolicyEligibilityClient resiliantOrderPolicyEligibilityClient) {
        this.orderRepository = orderRepository;
        this.menuItemService = menuItemService;
        this.condimentItemService = condimentItemService;
        this.kafkaMessageProducer = kafkaMessageProducer;
        this.kafkaAppConfig = kafkaAppConfig;
        this.resiliantOrderPolicyEligibilityClient = resiliantOrderPolicyEligibilityClient;
    }

    @Override
    public List<Order> getAllOrders() {
        return this.orderRepository.findAll();
    }

    @Override
    public Optional<Order> getOrderById(long orderId) {
        return this.orderRepository.findById(orderId);
    }

    @Override
    public Optional<Order> getOrderByName(String orderName) {
        return this.orderRepository.findByName(orderName);
    }

    @Override
    public Order createOrder(OrderRequestDTO orderItem, String idempotencyKey) {
        EligibilityStatus eligibilityStatus =
                this.resiliantOrderPolicyEligibilityClient.checkEligibility(orderItem);

        if (eligibilityStatus == EligibilityStatus.APPROVED) {
            return createOrder(orderItem.getName(), idempotencyKey);
        }
        else if (eligibilityStatus == EligibilityStatus.DENIED) {
            log.info("Eligibility status is {} for order: {}", eligibilityStatus, orderItem);

            return null;
        }
        else if (eligibilityStatus == EligibilityStatus.MANUAL_REVIEW) {
            this.resiliantOrderPolicyEligibilityClient.fallback(orderItem);
        }

        return null;
    }

    @Override
    public Optional<Order> deleteOrder(Long orderId) {
        Optional<Order> orderToDelete = this.orderRepository.findById(orderId);

        if (orderToDelete.isPresent()) {
            this.orderRepository.deleteById(orderId);
        }

        return orderToDelete;
    }

    @Override
    public Optional<Order> deleteOrder(String orderName) {
        Long orderId = getOrderIdByName(orderName);
        Optional<Order> orderToDelete = this.orderRepository.findById(orderId);

        if (orderToDelete.isPresent()) {
            this.orderRepository.deleteById(orderId);
        }

        return orderToDelete;
    }

    @Override
    public List<Order> deleteAllOrders() {
        List<Order> deletedOrders = this.orderRepository.findAll();

        this.orderRepository.deleteAll();

        return deletedOrders;
    }

    @Override
    public Optional<Order> addCondimentToOrder(Long orderId, CondimentItemRequestDTO newCondiment) {
        Optional<Order> order = this.orderRepository.findById(orderId);
        Optional<CondimentItem> condiment = this.condimentItemService.getCondimentByName(newCondiment.getName());

        if (order.isEmpty() || condiment.isEmpty()) {
            return order;
        }

        if (order.get().getBaseMenuItem() == null) {
            return order;
        }

        this.orderRepository.save(hydrateOrder(order.get(), condiment.get()));

        return order;
    }

    @Override
    public Optional<Order> addCondimentToOrder(String orderName, CondimentItemRequestDTO newCondiment) {
        Optional<Order> order = this.orderRepository.findByName(orderName);
        Optional<CondimentItem> condiment = this.condimentItemService.getCondimentByName(newCondiment.getName());

        if (order.isEmpty() || condiment.isEmpty()) {
            return order;
        }

        if (order.get().getBaseMenuItem() == null) {
            return order;
        }

        this.orderRepository.save(hydrateOrder(order.get(), condiment.get()));

        return order;
    }

    @Override
    public Optional<Order> removeCondimentFromOrder(Long orderId, CondimentItemRequestDTO condimentToRemove) {
        Optional<Order> order = this.orderRepository.findById(orderId);
        Optional<CondimentItem> condiment =
                this.condimentItemService.getCondimentByName(condimentToRemove.getName());

        if (order.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        if (condiment.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Condiment not found");
        }

        CondimentItem condimentItem = order.get().removeCondiment(condiment.get());

        if (condimentItem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Condiment does not exist on order " + orderId);
        }

        this.orderRepository.save(order.get());

        return order;
    }

    @Override
    public Optional<Order> removeCondimentFromOrder(String orderName, CondimentItemRequestDTO condimentToRemove) {
        Optional<Order> order = this.orderRepository.findByName(orderName);
        Optional<CondimentItem> condiment =
                this.condimentItemService.getCondimentByName(condimentToRemove.getName());

        if (order.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        if (condiment.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Condiment not found");
        }

        CondimentItem condimentItem = order.get().removeCondiment(condiment.get());

        if (condimentItem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Condiment does not exist on order: " + orderName);
        }

        this.orderRepository.save(order.get());

        return order;
    }

    @Override
    public Optional<Order> addMenuItemToOrder(Long orderId, MenuItemRequestDTO newMenuItem) {
        Optional<Order> order = this.orderRepository.findById(orderId);
        Optional<MenuItem> menuItem = this.menuItemService.getMenuItemByName(newMenuItem.getName());

        if (order.isEmpty() || menuItem.isEmpty()) {
            return order;
        }

        this.orderRepository.save(hydrateOrder(order.get(), menuItem.get()));

        return order;
    }

    @Override
    public Optional<Order> addMenuItemToOrder(String orderName, MenuItemRequestDTO newMenuItem) {
        Optional<Order> order = this.orderRepository.findByName(orderName);
        Optional<MenuItem> menuItem = this.menuItemService.getMenuItemByName(newMenuItem.getName());

        if (order.isEmpty() || menuItem.isEmpty()) {
            return order;
        }

        this.orderRepository.save(hydrateOrder(order.get(), menuItem.get()));

        return order;
    }

    @Override
    public Optional<Order> removeMenuItemFromOrder(Long orderId, MenuItemRequestDTO menuItemToRemove) {
        Optional<Order> order = this.orderRepository.findById(orderId);
        Optional<MenuItem> menuItem = this.menuItemService.getMenuItemByName(menuItemToRemove.getName());

        if (order.isEmpty() || menuItem.isEmpty()) {
            return order;
        }

        Order currentOrder = order.get();

        currentOrder.setBaseMenuItem(null);
        currentOrder.getCondiments().clear();

        this.orderRepository.save(currentOrder);

        return order;
    }

    @Override
    public Optional<Order> removeMenuItemFromOrder(String orderName, MenuItemRequestDTO menuItemToRemove) {
        Optional<Order> order = this.orderRepository.findByName(orderName);
        Optional<MenuItem> menuItem = this.menuItemService.getMenuItemByName(menuItemToRemove.getName());

        if (order.isEmpty() || menuItem.isEmpty()) {
            return order;
        }

        Order currentOrder = order.get();

        currentOrder.setBaseMenuItem(null);
        currentOrder.getCondiments().clear();

        this.orderRepository.save(currentOrder);

        return order;
    }

    @Override
    public long getOrderIdByName(String name) {
        Optional<Order> order = this.orderRepository.findByName(name);

        return order.map(Order::getId).orElse(UNKNOWN_ORDER);
    }

    private Order hydrateOrder(Order currentOrder, CondimentItem currentCondiment) {
        Set<CondimentItem> condiments = currentOrder.getCondiments();

        condiments.add(currentCondiment);

        return currentOrder;
    }

    public synchronized Order createOrder(String name, String idempotencyKey) {
        IdempotencyOrder idempotencyOrder =
                hydrateIdempotencyOrder(idempotencyKey, name, this.idempotencyOrderMap);
        Order newOrder = null;

        // We will handle idempotency if the key was passed in, but since we allow it to be optional,
        // support the old non-idempoteny behavior
        if (idempotencyOrder != null) {
            newOrder = handleIdempotency(idempotencyOrder, name);
        }

        // This order has been created before, just return
        if (newOrder != null) {
            return newOrder;
        }

        newOrder = new Order(name);

        try {
            this.orderRepository.save(newOrder);

            sendCreatedOrder(newOrder);

            // Save the completed order
            if (idempotencyOrder != null) {
                idempotencyOrder.setOrderResponse(newOrder);
                idempotencyOrder.setStatus(IdempotencyOrder.Status.COMPLETED);
            }
        }
        catch (Exception e) {
            if (idempotencyOrder != null) {
                idempotencyOrder.setStatus(IdempotencyOrder.Status.FAILED);
                this.idempotencyOrderMap.remove(idempotencyOrder.getIdempotencyKey());
            }

            // So global error handler catches and logs accordingly
            throw e;
        }

        return newOrder;
    }

    public Order handleIdempotency(IdempotencyOrder idempotencyOrder, String name) {
        IdempotencyOrder.Status idStatus = idempotencyOrder.getStatus();

        if (!idempotencyOrder.getOrderName().equals(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Duplicate Order request for " + name + " does not match "
                            + idempotencyOrder.getOrderName());
        }

        if (idStatus.equals(IdempotencyOrder.Status.IN_PROGRESS)) {
            // Optional logging of the entire idempotency object
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order " + name + " in progress...");
        }
        else if (idStatus.equals(IdempotencyOrder.Status.COMPLETED)) {
            // Optional logging of the entire idempotency object
            log.info("Duplicate order detected: {}", name);
            idempotencyOrder = this.idempotencyOrderMap.get(idempotencyOrder.getIdempotencyKey());

            // Replay saved order
            return idempotencyOrder.getOrderResponse();
        }

        idempotencyOrder.setStatus(IdempotencyOrder.Status.IN_PROGRESS);

        return null;
    }

    private void sendCreatedOrder(Order order) {
        if (order == null || !this.kafkaAppConfig.isEnabled()) {
            return;
        }

        OrderEventDTO orderEventDTO = new OrderEventDTO(order);

        this.kafkaMessageProducer.sendMessage(this.kafkaAppConfig.getOrdersTopic(), orderEventDTO);
    }

    private Order hydrateOrder(Order currentOrder, MenuItem currentMenuItem) {
        Set<CondimentItem> orderCondiments = currentOrder.getCondiments();

        currentOrder.setBaseMenuItem(currentMenuItem);

        List<CondimentItem> condiments =
                this.condimentItemService.findAllCondimentItemsForMenuItem(currentMenuItem.getId());

        orderCondiments.addAll(condiments);

        return currentOrder;
    }
}
