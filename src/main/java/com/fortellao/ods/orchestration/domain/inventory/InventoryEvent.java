package com.fortellao.ods.orchestration.domain.inventory;

import java.math.BigDecimal;
import java.util.List;

public class InventoryEvent {
    private String orderId;
    private boolean success;
    private List<ReservedItem> reservedItems;
    private BigDecimal totalPrice;

    public InventoryEvent() {}

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public List<ReservedItem> getReservedItems() { return reservedItems; }
    public void setReservedItems(List<ReservedItem> reservedItems) { this.reservedItems = reservedItems; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
}
