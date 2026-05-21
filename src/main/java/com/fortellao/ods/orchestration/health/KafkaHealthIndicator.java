package com.fortellao.ods.orchestration.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class KafkaHealthIndicator implements HealthIndicator, DisposableBean {

    private final AdminClient adminClient;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        Map<String, Object> config = new HashMap<>(kafkaAdmin.getConfigurationProperties());
        config.put(AdminClientConfig.CLIENT_ID_CONFIG, "health-indicator");
        // Fail the in-flight describeCluster() request quickly so the health probe doesn't stall
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 2500);
        // Pace reconnect retries; honoured because metadata.recovery.strategy=none
        // preserves the backoff counter (the default "rebootstrap" strategy resets it on every cycle)
        config.put(AdminClientConfig.RECONNECT_BACKOFF_MS_CONFIG, 1000);
        config.put(AdminClientConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 30_000);
        // Prevents rebootstrap from resetting the backoff counter, which would cause rapid reconnect storms
        config.put("metadata.recovery.strategy", "none");
        this.adminClient = AdminClient.create(config);
    }

    @Override
    public Health health() {
        try {
            DescribeClusterResult cluster = adminClient.describeCluster();
            String clusterId = cluster.clusterId().get(3, TimeUnit.SECONDS);
            int nodeCount = cluster.nodes().get(3, TimeUnit.SECONDS).size();
            return Health.up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodeCount", nodeCount)
                    .build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }

    @Override
    public void destroy() {
        adminClient.close(Duration.ofSeconds(5));
    }
}