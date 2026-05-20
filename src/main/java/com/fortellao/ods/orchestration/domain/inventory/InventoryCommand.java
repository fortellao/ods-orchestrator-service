package com.fortellao.ods.orchestration.domain.inventory;

import com.fortellao.ods.orchestration.domain.order.Item;

import java.util.List;

public class InventoryCommand {
    private String orderId;
    private InventoryOperation operation;
    private List<Item> items;

    public InventoryCommand() {}

    public InventoryCommand(String orderId, InventoryOperation operation, List<Item> items) {
        this.orderId = orderId;
        this.operation = operation;
        this.items = items;
    }

    public InventoryCommand(String orderId, InventoryOperation operation) {
        this.orderId = orderId;
        this.operation = operation;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public InventoryOperation getOperation() { return operation; }
    public void setOperation(InventoryOperation operation) { this.operation = operation; }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
}