package com.fortellao.ods.orchestration.status;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/status")
public class ServiceStatusController {

    private final ServiceStatusService statusService;

    public ServiceStatusController(ServiceStatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping
    public ServiceStatus getStatus() {
        return statusService.getServiceStatus();
    }
}
