package com.fortellao.ods.orchestration.messaging.outbound;

import com.fortellao.ods.orchestration.config.KafkaTopicsConfiguration;
import com.fortellao.ods.orchestration.domain.inventory.InventoryCommand;
import com.fortellao.ods.orchestration.domain.order.OrderCommand;
import com.fortellao.ods.orchestration.domain.payment.PaymentCommand;
import com.fortellao.ods.orchestration.saga.OrchestratorCommandPublisher;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorKafkaPublisher implements OrchestratorCommandPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsConfiguration topics;
    private final ObjectMapper objectMapper;

    public OrchestratorKafkaPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                      KafkaTopicsConfiguration topics,
                                      ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendInventoryCommand(InventoryCommand command) {
        send(topics.getInventoryCommand(), command.getOrderId(), command);
    }

    @Override
    public void sendPaymentCommand(PaymentCommand command) {
        send(topics.getPaymentCommand(), command.getOrderId(), command);
    }

    @Override
    public void sendOrderCommand(OrderCommand command) {
        send(topics.getOrderCommand(), command.getOrderId(), command);
    }

    private void send(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("Failed to publish to topic {}: orderId={}", topic, key, e);
        }
    }
}