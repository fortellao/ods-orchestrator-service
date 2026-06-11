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
        Order order = new Order(orderId, OrderStatus.PENDING, request);
        orderStore.save(order);

        publisher.sendInventoryCommand(new ProductCommand(orderId, ProductOperation.CHECKOUT, request.items()));
        log.info("Order {} - inventory checkout requested", orderId);
    }

    @Override
    public void onInventoryEvent(ProductEvent event) {
        if (event.success()) {
            onInventorySuccess(event);
        } else {
            log.warn("Order {} - inventory checkout failed", event.orderId());
            failOrder(event.orderId());
        }
    }

    @Override
    public void onPaymentEvent(PaymentEvent event) {
        if (event.success()) {
            onPaymentSuccess(event);
        } else {
            log.warn("Order {} - payment validation failed", event.orderId());
            compensateInventoryAndFail(event.orderId());
        }
    }

    private void onInventorySuccess(ProductEvent event) {
        Order order = orderStore.find(event.orderId());
        if (order == null) {
            log.warn("Received inventory event for unknown order {}", event.orderId());
            return;
        }

        publisher.sendPaymentCommand(new PaymentCommand(event.orderId(), order.paymentId(), event.totalPrice()));
        log.info("Order {} - payment validation requested, totalPrice={}", event.orderId(), event.totalPrice());
    }

    private void onPaymentSuccess(PaymentEvent event) {
        Order order = orderStore.remove(event.orderId());
        if (order == null) {
            log.warn("Received payment event for unknown order {}", event.orderId());
            return;
        }

        publisher.sendOrderCommand(new OrderCommand(event.orderId(), OrderStatus.CONFIRMED));
        log.info("Order {} - confirmed", event.orderId());
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