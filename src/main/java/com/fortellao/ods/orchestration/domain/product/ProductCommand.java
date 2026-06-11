package com.fortellao.ods.orchestration.domain.product;

import com.fortellao.ods.orchestration.domain.order.Item;

import java.util.List;

public record ProductCommand(String orderId, ProductOperation operation, List<Item> items) {

    public ProductCommand(String orderId, ProductOperation operation) {
        this(orderId, operation, null);
    }
}