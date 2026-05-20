package com.fortellao.ods.orchestration.saga;

import com.fortellao.ods.orchestration.domain.inventory.InventoryEvent;
import com.fortellao.ods.orchestration.domain.order.OrderEvent;
import com.fortellao.ods.orchestration.domain.payment.PaymentEvent;

public interface OrchestratorEventHandler {
    void onOrderReceived(OrderEvent event);
    void onInventoryEvent(InventoryEvent event);
    void onPaymentEvent(PaymentEvent event);
}