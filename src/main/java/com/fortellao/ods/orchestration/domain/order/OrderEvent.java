package com.fortellao.ods.orchestration.domain.order;

import java.util.List;

public record OrderEvent(
        String customerId,
        String paymentId,
        List<Item> items
) {}