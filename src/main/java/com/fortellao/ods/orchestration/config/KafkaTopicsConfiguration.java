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

    @Name("inventory-event")
    private String inventoryEvent;

    @Name("inventory-command")
    private String inventoryCommand;

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

    public String getInventoryEvent() {
        return inventoryEvent;
    }

    void setInventoryEvent(String inventoryEvent) {
        this.inventoryEvent = inventoryEvent;
    }

    public String getInventoryCommand() {
        return inventoryCommand;
    }

    void setInventoryCommand(String inventoryCommand) {
        this.inventoryCommand = inventoryCommand;
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