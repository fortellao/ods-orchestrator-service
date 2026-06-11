package com.fortellao.ods.orchestration.saga;

import com.fortellao.ods.orchestration.domain.product.ProductCommand;
import com.fortellao.ods.orchestration.domain.order.OrderCommand;
import com.fortellao.ods.orchestration.domain.payment.PaymentCommand;

public interface OrchestratorCommandPublisher {
    void sendProductCommand(ProductCommand command);
    void sendPaymentCommand(PaymentCommand command);
    void sendOrderCommand(OrderCommand command);
}