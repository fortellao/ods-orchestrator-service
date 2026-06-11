package com.fortellao.ods.orchestration.domain.payment;

import java.math.BigDecimal;

public record PaymentCommand(String orderId, String paymentId, BigDecimal totalAmount) {}