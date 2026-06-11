package com.fortellao.ods.orchestration.domain.payment;

public record PaymentEvent(String orderId, boolean success) {}