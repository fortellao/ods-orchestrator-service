package com.fortellao.ods.orchestration.domain.payment;

public class PaymentEvent {
    private String orderId;
    private boolean success;

    public PaymentEvent() {}

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}
