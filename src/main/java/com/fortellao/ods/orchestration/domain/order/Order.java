package com.fortellao.ods.orchestration.domain.order;

import java.util.List;

public record Order(
    String orderId,
    String customerId,
    OrderStatus status,
    String paymentId,
    List<Item> items
) {

    public Order(String orderId, OrderStatus status, OrderEvent request) {
        this(orderId, request.customerId(), status, request.paymentId(), request.items());
    }
}
