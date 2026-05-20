package com.fortellao.ods.orchestration.status;

public record ServiceStatus(
        String orderEvents,
        String orderCommands,
        String inventoryEvents,
        String inventoryCommands,
        String paymentEvents,
        String paymentCommands
) {}
