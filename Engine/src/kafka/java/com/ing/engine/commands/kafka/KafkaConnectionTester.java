package com.ing.engine.commands.kafka;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.SslConfigs;

/**
 * Bounded Kafka connectivity probe used by the Settings "Test Connection"
 * button. Builds an {@link Admin} client from a producer/consumer config and
 * runs a single {@code describeCluster()} call &mdash; no produce/consume.
 *
 * <p>Invoked reflectively from the IDE ({@code DriverSettings.testKafkaConnection})
 * so the IDE has no compile-time dependency on {@code kafka-clients}; this source
 * is compiled only under the {@code -P kafka} profile.
 *
 * <p>Return contract: empty string on success; {@code "WARN:<msg>"} when the
 * broker is reachable but a referenced topic/group is missing (amber); any other
 * non-empty string is a hard failure (red).
 */
public final class KafkaConnectionTester {

    private static final int TIMEOUT_MS = 5000;

    private KafkaConnectionTester() {}

    public static String test(Properties cfg) {
        String servers = cfg.getProperty("bootstrap.servers", "").trim();
        if (servers.isEmpty()) {
            return "bootstrap.servers is not set";
        }
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, TIMEOUT_MS);
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, TIMEOUT_MS);
        applySsl(props, cfg);

        try (Admin admin = Admin.create(props)) {
            admin.describeCluster().nodes().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);

            String topic = cfg.getProperty(
                "producer.topic",
                cfg.getProperty("consumer.topic", "")
            );
            if (topic != null && !topic.trim().isEmpty()) {
                try {
                    admin
                        .describeTopics(Collections.singletonList(topic.trim()))
                        .allTopicNames()
                        .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } catch (Exception topicEx) {
                    return "WARN:Broker reachable but topic '" + topic + "' was not found.";
                }
            }
            return "";
        } catch (Exception ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            return cause.getMessage() == null ? cause.toString() : cause.getMessage();
        }
    }

    private static void applySsl(Properties props, Properties cfg) {
        if (Boolean.parseBoolean(cfg.getProperty("ssl.enabled", "false"))) {
            props.put("security.protocol", "SSL");
            put(props, SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, cfg.getProperty("ssl.keystore.location"));
            put(props, SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, cfg.getProperty("ssl.keystore.password"));
            put(props, SslConfigs.SSL_KEY_PASSWORD_CONFIG, cfg.getProperty("ssl.key.password"));
        }
    }

    private static void put(Properties props, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            props.put(key, value);
        }
    }
}
