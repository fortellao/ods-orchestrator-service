package com.fortellao.ods.orchestration.domain.product;

import java.math.BigDecimal;
import java.util.List;

public record ProductEvent(
        String orderId,
        boolean success,
        List<ReservedItem> reservedItems,
        BigDecimal totalPrice
) {}