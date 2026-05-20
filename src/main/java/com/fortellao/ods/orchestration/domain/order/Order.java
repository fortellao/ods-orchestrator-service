package com.fortellao.ods.orchestration.domain.order;

import java.util.List;

public class Order {
    private String orderId;
    private String customerId;
    private OrderStatus status;
    private String paymentId;
    private List<Item> items;

    public Order(OrderEvent request) {
        this.customerId = request.getCustomerId();
        this.paymentId = request.getPaymentId();
        this.items = request.getItems();
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
