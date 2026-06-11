package com.fortellao.ods.orchestration.domain.product;

import java.math.BigDecimal;

public record ReservedItem(String productId, int quantity, BigDecimal unitPrice) {}