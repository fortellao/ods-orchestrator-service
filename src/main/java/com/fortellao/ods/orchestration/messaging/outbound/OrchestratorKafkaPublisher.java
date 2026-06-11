package com.fortellao.ods.orchestration.messaging.outbound;

import com.fortellao.ods.orchestration.domain.product.ProductCommand;
import com.fortellao.ods.orchestration.domain.order.OrderCommand;
import com.fortellao.ods.orchestration.domain.payment.PaymentCommand;
import com.fortellao.ods.orchestration.saga.OrchestratorCommandPublisher;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorKafkaPublisher implements OrchestratorCommandPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String productCommandTopic;
    private final String paymentCommandTopic;
    private final String orderCommandTopic;

    public OrchestratorKafkaPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${orchestration.topics.order-command}") String orderCommandTopic,
            @Value("${orchestration.topics.product-command}") String productCommandTopic,
            @Value("${orchestration.topics.payment-command}") String paymentCommandTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.productCommandTopic = productCommandTopic;
        this.paymentCommandTopic = paymentCommandTopic;
        this.orderCommandTopic = orderCommandTopic;
    }

    @Override
    public void sendProductCommand(ProductCommand command) {
        send(this.productCommandTopic, command.orderId(), command);
    }

    @Override
    public void sendPaymentCommand(PaymentCommand command) {
        send(this.paymentCommandTopic, command.orderId(), command);
    }

    @Override
    public void sendOrderCommand(OrderCommand command) {
        send(this.orderCommandTopic, command.orderId(), command);
    }

    private void send(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("Failed to publish to topic {}: orderId={}", topic, key, e);
        }
    }
}