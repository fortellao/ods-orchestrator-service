package com.fortellao.ods.orchestration.messaging.inbound;

import com.fortellao.ods.orchestration.domain.product.ProductEvent;
import com.fortellao.ods.orchestration.domain.order.OrderEvent;
import com.fortellao.ods.orchestration.domain.payment.PaymentEvent;
import com.fortellao.ods.orchestration.saga.OrchestratorEventHandler;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorKafkaListener.class);

    private final OrchestratorEventHandler eventHandler;
    private final ObjectMapper objectMapper;

    public OrchestratorKafkaListener(OrchestratorEventHandler eventHandler,
                                     ObjectMapper objectMapper) {
        this.eventHandler = eventHandler;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${orchestration.topics.order-event}")
    public void onOrderEvent(String value) {
        try {
            eventHandler.onOrderReceived(objectMapper.readValue(value, OrderEvent.class));
        } catch (Exception e) {
            log.error("Failed to process order event", e);
        }
    }

    @KafkaListener(topics = "${orchestration.topics.product-event}")
    public void onProductEvent(String value) {
        try {
            eventHandler.onInventoryEvent(objectMapper.readValue(value, ProductEvent.class));
        } catch (Exception e) {
            log.error("Failed to process inventory event", e);
        }
    }

    @KafkaListener(topics = "${orchestration.topics.payment-event}")
    public void onPaymentEvent(String value) {
        try {
            eventHandler.onPaymentEvent(objectMapper.readValue(value, PaymentEvent.class));
        } catch (Exception e) {
            log.error("Failed to process payment event", e);
        }
    }
}