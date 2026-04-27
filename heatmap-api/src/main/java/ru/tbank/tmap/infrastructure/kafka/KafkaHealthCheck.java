package ru.tbank.tmap.infrastructure.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component("kafkaHealthIndicator")
public class KafkaHealthCheck implements HealthIndicator {

    private static final int DESCRIBE_TIMEOUT_MS = 2000;
    private static final int GET_TIMEOUT_SEC = 2;

    private static final String DETAIL_KEY_SERVICE = "service";
    private static final String DETAIL_VALUE_KAFKA = "Kafka";

    private final AdminClient adminClient;

    public KafkaHealthCheck(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        this.adminClient = AdminClient.create(configs);
    }

    @Override
    public Health health() {
        try {
            adminClient.describeCluster(new DescribeClusterOptions().timeoutMs(DESCRIBE_TIMEOUT_MS))
                    .clusterId()
                    .get(GET_TIMEOUT_SEC, TimeUnit.SECONDS);

            return Health.up().withDetail(DETAIL_KEY_SERVICE, DETAIL_VALUE_KAFKA).build();
        } catch (Exception e) {
            return Health.down().withDetail(DETAIL_KEY_SERVICE, DETAIL_VALUE_KAFKA).withException(e).build();
        }
    }
}
