package com.fortellao.ods.orchestration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("orchestration.topics")
public class KafkaTopicsConfiguration {

    @Name("order-event")
    private String orderEvent;

    @Name("order-command")
    private String orderCommand;

    @Name("product-event")
    private String productEvent;

    @Name("product-command")
    private String productCommand;

    @Name("payment-event")
    private String paymentEvent;

    @Name("payment-command")
    private String paymentCommand;

    public String getOrderEvent() {
        return orderEvent;
    }

    void setOrderEvent(String orderEvent) {
        this.orderEvent = orderEvent;
    }

    public String getOrderCommand() {
        return orderCommand;
    }

    void setOrderCommand(String orderCommand) {
        this.orderCommand = orderCommand;
    }

    public String getProductEvent() {
        return productEvent;
    }

    void setProductEvent(String productEvent) {
        this.productEvent = productEvent;
    }

    public String getProductCommand() {
        return productCommand;
    }

    void setProductCommand(String productCommand) {
        this.productCommand = productCommand;
    }

    public String getPaymentEvent() {
        return paymentEvent;
    }

    void setPaymentEvent(String paymentEvent) {
        this.paymentEvent = paymentEvent;
    }

    public String getPaymentCommand() {
        return paymentCommand;
    }

    void setPaymentCommand(String paymentCommand) {
        this.paymentCommand = paymentCommand;
    }
}