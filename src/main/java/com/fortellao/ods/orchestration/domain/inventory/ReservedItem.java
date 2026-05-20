package com.fortellao.ods.orchestration.domain.inventory;

import java.math.BigDecimal;

public class ReservedItem {
    private String productId;
    private int quantity;
    private BigDecimal unitPrice;

    public ReservedItem() {}

    public ReservedItem(String productId, int quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
