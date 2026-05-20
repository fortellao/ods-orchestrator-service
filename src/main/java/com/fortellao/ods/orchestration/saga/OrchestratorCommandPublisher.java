package com.fortellao.ods.orchestration.saga;

import com.fortellao.ods.orchestration.domain.inventory.InventoryCommand;
import com.fortellao.ods.orchestration.domain.order.OrderCommand;
import com.fortellao.ods.orchestration.domain.payment.PaymentCommand;

public interface OrchestratorCommandPublisher {
    void sendInventoryCommand(InventoryCommand command);
    void sendPaymentCommand(PaymentCommand command);
    void sendOrderCommand(OrderCommand command);
}