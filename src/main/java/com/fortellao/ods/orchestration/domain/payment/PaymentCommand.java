package com.fortellao.ods.orchestration.domain.payment;

import java.math.BigDecimal;

public class PaymentCommand {
    private String orderId;
    private String paymentId;
    private BigDecimal totalAmount;

    public PaymentCommand() {}

    public PaymentCommand(String orderId, String paymentId, BigDecimal totalAmount) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.totalAmount = totalAmount;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
