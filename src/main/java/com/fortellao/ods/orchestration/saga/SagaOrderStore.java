package com.fortellao.ods.orchestration.saga;

import com.fortellao.ods.orchestration.domain.order.Order;

public interface SagaOrderStore {
    void save(Order order);
    Order find(String orderId);
    Order remove(String orderId);
}