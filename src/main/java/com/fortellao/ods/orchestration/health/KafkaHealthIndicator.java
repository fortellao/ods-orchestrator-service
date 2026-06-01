package com.fortellao.ods.orchestration.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
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
    private final int timeoutMs;

    public KafkaHealthIndicator(
            KafkaAdmin kafkaAdmin,
            @Value("${management.health.kafka.timeout-ms:1000}") int timeoutMs) {
        this.timeoutMs = timeoutMs;
        Map<String, Object> config = new HashMap<>(kafkaAdmin.getConfigurationProperties());
        config.put(AdminClientConfig.CLIENT_ID_CONFIG, "health-indicator");
        // Leave 20% headroom so the Kafka request fails before Future.get() cuts the thread
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, timeoutMs * 4 / 5);
        // Pace reconnect retries; honoured because metadata.recovery.strategy=none
        // preserves the backoff counter (the default "rebootstrap" strategy resets it on every cycle)
        config.put(AdminClientConfig.RECONNECT_BACKOFF_MS_CONFIG, 100);
        // Cap max backoff so a single backoff cycle always fits within the probe timeout budget
        config.put(AdminClientConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, timeoutMs / 2);
        // Prevents rebootstrap from resetting the backoff counter, which would cause rapid reconnect storms
        config.put("metadata.recovery.strategy", "none");
        this.adminClient = AdminClient.create(config);
    }

    @Override
    public Health health() {
        try {
            DescribeClusterResult cluster = adminClient.describeCluster();
            String clusterId = cluster.clusterId().get(timeoutMs, TimeUnit.MILLISECONDS);
            int nodeCount = cluster.nodes().get(timeoutMs, TimeUnit.MILLISECONDS).size();
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