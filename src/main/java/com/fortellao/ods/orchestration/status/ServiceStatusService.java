package com.fortellao.ods.orchestration.status;

import com.fortellao.ods.orchestration.config.KafkaTopicsConfiguration;
import org.springframework.stereotype.Service;

@Service
public class ServiceStatusService {

    private final KafkaTopicsConfiguration kafkaTopicsConfiguration;

    public ServiceStatusService(KafkaTopicsConfiguration kafkaTopicsConfiguration) {
        this.kafkaTopicsConfiguration = kafkaTopicsConfiguration;
    }

    public ServiceStatus getServiceStatus() {
        return new ServiceStatus(
                kafkaTopicsConfiguration.getOrderEvent(),
                kafkaTopicsConfiguration.getOrderCommand(),
                kafkaTopicsConfiguration.getInventoryEvent(),
                kafkaTopicsConfiguration.getInventoryCommand(),
                kafkaTopicsConfiguration.getPaymentEvent(),
                kafkaTopicsConfiguration.getPaymentCommand()
        );
    }
}
