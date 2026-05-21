package com.fortellao.ods.orchestration.saga;

import com.fortellao.ods.orchestration.domain.product.ProductCommand;
import com.fortellao.ods.orchestration.domain.product.ProductEvent;
import com.fortellao.ods.orchestration.domain.product.ProductOperation;
import com.fortellao.ods.orchestration.domain.order.Order;
import com.fortellao.ods.orchestration.domain.order.OrderCommand;
import com.fortellao.ods.orchestration.domain.order.OrderEvent;
import com.fortellao.ods.orchestration.domain.order.OrderStatus;
import com.fortellao.ods.orchestration.domain.payment.PaymentCommand;
import com.fortellao.ods.orchestration.domain.payment.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderOrchestratorService implements OrchestratorEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderOrchestratorService.class);

    private final OrchestratorCommandPublisher publisher;
    private final SagaOrderStore orderStore;

    public OrderOrchestratorService(OrchestratorCommandPublisher publisher, SagaOrderStore orderStore) {
        this.publisher = publisher;
        this.orderStore = orderStore;
    }

    @Override
    public void onOrderReceived(OrderEvent request) {
        String orderId = UUID.randomUUID().toString();
        Order order = new Order(request);
        order.setOrderId(orderId);
        order.setStatus(OrderStatus.PENDING);
        orderStore.save(order);

        publisher.sendInventoryCommand(new ProductCommand(orderId, ProductOperation.CHECKOUT, request.getItems()));
        log.info("Order {} - inventory checkout requested", orderId);
    }

    @Override
    public void onInventoryEvent(ProductEvent event) {
        if (event.isSuccess()) {
            onInventorySuccess(event);
        } else {
            log.warn("Order {} - inventory checkout failed", event.getOrderId());
            failOrder(event.getOrderId());
        }
    }

    @Override
    public void onPaymentEvent(PaymentEvent event) {
        if (event.isSuccess()) {
            onPaymentSuccess(event);
        } else {
            log.warn("Order {} - payment validation failed", event.getOrderId());
            compensateInventoryAndFail(event.getOrderId());
        }
    }

    private void onInventorySuccess(ProductEvent event) {
        Order order = orderStore.find(event.getOrderId());
        if (order == null) {
            log.warn("Received inventory event for unknown order {}", event.getOrderId());
            return;
        }

        publisher.sendPaymentCommand(new PaymentCommand(event.getOrderId(), order.getPaymentId(), event.getTotalPrice()));
        log.info("Order {} - payment validation requested, totalPrice={}", event.getOrderId(), event.getTotalPrice());
    }

    private void onPaymentSuccess(PaymentEvent event) {
        Order order = orderStore.remove(event.getOrderId());
        if (order == null) {
            log.warn("Received payment event for unknown order {}", event.getOrderId());
            return;
        }

        publisher.sendOrderCommand(new OrderCommand(event.getOrderId(), OrderStatus.CONFIRMED));
        log.info("Order {} - confirmed", event.getOrderId());
    }

    private void compensateInventoryAndFail(String orderId) {
        publisher.sendInventoryCommand(new ProductCommand(orderId, ProductOperation.RELEASE));
        log.info("Order {} - inventory release requested", orderId);
        failOrder(orderId);
    }

    private void failOrder(String orderId) {
        orderStore.remove(orderId);
        publisher.sendOrderCommand(new OrderCommand(orderId, OrderStatus.FAILED));
        log.info("Order {} - failed", orderId);
    }
}