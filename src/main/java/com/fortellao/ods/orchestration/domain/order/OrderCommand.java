package com.fortellao.ods.orchestration.domain.order;

public record OrderCommand(String orderId, OrderStatus status) {}