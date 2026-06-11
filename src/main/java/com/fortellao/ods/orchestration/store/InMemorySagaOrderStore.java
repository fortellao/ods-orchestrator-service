package com.fortellao.ods.orchestration.store;

import com.fortellao.ods.orchestration.domain.order.Order;
import com.fortellao.ods.orchestration.saga.SagaOrderStore;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemorySagaOrderStore implements SagaOrderStore {

    private final ConcurrentHashMap<String, Order> store = new ConcurrentHashMap<>();

    @Override
    public void save(Order order) {
        store.put(order.orderId(), order);
    }

    @Override
    public Order find(String orderId) {
        return store.get(orderId);
    }

    @Override
    public Order remove(String orderId) {
        return store.remove(orderId);
    }
}