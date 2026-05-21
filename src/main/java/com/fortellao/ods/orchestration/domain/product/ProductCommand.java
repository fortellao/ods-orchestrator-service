package com.fortellao.ods.orchestration.domain.product;

import com.fortellao.ods.orchestration.domain.order.Item;

import java.util.List;

public class ProductCommand {
    private String orderId;
    private ProductOperation operation;
    private List<Item> items;

    public ProductCommand() {}

    public ProductCommand(String orderId, ProductOperation operation, List<Item> items) {
        this.orderId = orderId;
        this.operation = operation;
        this.items = items;
    }

    public ProductCommand(String orderId, ProductOperation operation) {
        this.orderId = orderId;
        this.operation = operation;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public ProductOperation getOperation() { return operation; }
    public void setOperation(ProductOperation operation) { this.operation = operation; }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
}