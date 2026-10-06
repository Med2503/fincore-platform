package org.fincore.wealth.position.messaging;


import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.fincore.wealth.position.application.event.PositionChangedEvent;
import org.fincore.wealth.position.application.service.PositionChangedProjectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest(
        properties = {
                "spring.cloud.stream.bindings.positionChangedInput-in-0.consumer.maxAttempts=3",
                "spring.cloud.stream.bindings.positionChangedInput-in-0.consumer.backOffInitialInterval=100",
                "spring.cloud.stream.bindings.positionChangedInput-in-0.consumer.backOffMultiplier=1.0",
                "spring.cloud.stream.bindings.positionChangedInput-in-0.consumer.backOffMaxInterval=100",
                "spring.cloud.stream.kafka.bindings.positionChangedInput-in-0.consumer.enableDlq=true",
                "spring.cloud.stream.kafka.bindings.positionChangedInput-in-0.consumer.dlqName=wealth.position.changed.dlq",
                "spring.cloud.stream.bindings.positionChangedInput-in-0.consumer.startOffset=earliest"
        }
)
@Import(PositionChangedConsumerKafkaIT.TestConfiguration.class)
class PositionChangedConsumerKafkaIT {

    private static final String INPUT_TOPIC =
            "wealth.position.changed";

    private static final String DLQ_TOPIC =
            "wealth.position.changed.dlq";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16");

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(
                    "apache/kafka-native:3.8.0"
            );

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FailingProjectionService failingProjectionService;

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );

        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );

        registry.add(
                "spring.flyway.url",
                POSTGRES::getJdbcUrl
        );

        registry.add(
                "spring.flyway.user",
                POSTGRES::getUsername
        );

        registry.add(
                "spring.flyway.password",
                POSTGRES::getPassword
        );

        registry.add(
                "spring.cloud.stream.kafka.binder.brokers",
                KAFKA::getBootstrapServers
        );

        registry.add(
                "spring.kafka.bootstrap-servers",
                KAFKA::getBootstrapServers
        );
    }

    @Test
    void shouldRetryThreeTimesAndSendMessageToDlq()
            throws Exception {

        UUID eventId = UUID.randomUUID();

        PositionChangedEvent event =
                createEvent(eventId);

        String payload =
                objectMapper.writeValueAsString(event);

        try (
                Consumer<String, String> dlqConsumer =
                        createDlqConsumer()
        ) {
            dlqConsumer.subscribe(
                    Collections.singleton(DLQ_TOPIC)
            );

            kafkaTemplate.send(
                    INPUT_TOPIC,
                    eventId.toString(),
                    payload
            ).get(10, TimeUnit.SECONDS);

            ConsumerRecords<String, String> records =
                    waitForDlqRecord(
                            dlqConsumer
                    );

            assertEquals(
                    1,
                    records.count()
            );

            assertEquals(
                    3,
                    failingProjectionService
                            .attempts()
            );

            assertProcessedEventCount(
                    eventId,
                    0
            );
        }
    }

    private ConsumerRecords<String, String> waitForDlqRecord(
            Consumer<String, String> consumer
    ) {
        long deadline =
                System.nanoTime()
                        + TimeUnit.SECONDS.toNanos(15);

        while (System.nanoTime() < deadline) {

            ConsumerRecords<String, String> records =
                    consumer.poll(
                            Duration.ofMillis(500)
                    );

            if (!records.isEmpty()) {
                return records;
            }
        }

        throw new AssertionError(
                "Message was not sent to DLQ"
        );
    }

    private Consumer<String, String> createDlqConsumer() {

        Map<String, Object> properties =
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        KAFKA.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG,
                        "wealth-dlq-test-" + UUID.randomUUID(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                        "earliest",
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        StringDeserializer.class,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        StringDeserializer.class
                );

        ConsumerFactory<String, String> factory =
                new DefaultKafkaConsumerFactory<>(
                        properties
                );

        return factory.createConsumer();
    }

    private void assertProcessedEventCount(
            UUID eventId,
            int expected
    ) {
        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM processed_position_changed_events
                        WHERE event_id = ?
                        """,
                        Integer.class,
                        eventId
                );

        assertEquals(
                expected,
                count
        );
    }

    private PositionChangedEvent createEvent(
            UUID eventId
    ) {
        return new PositionChangedEvent(
                eventId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("10"),
                new BigDecimal("100"),
                "EUR",
                1,
                Instant.parse(
                        "2026-09-30T10:00:00Z"
                )
        );
    }

    @TestConfiguration
    static class TestConfiguration {

        @Bean
        FailingProjectionService
        failingProjectionService() {
            return new FailingProjectionService();
        }

        @Bean
        @Primary
        PositionChangedProjectionService
        positionChangedProjectionService(
                FailingProjectionService service
        ) {
            return service;
        }
    }

    static class FailingProjectionService
            extends PositionChangedProjectionService {

        private final AtomicInteger attempts =
                new AtomicInteger();

        FailingProjectionService() {
            super(null);
        }

        @Override
        public void apply(
                PositionChangedEvent event
        ) {
            attempts.incrementAndGet();

            throw new IllegalStateException(
                    "Simulated Kafka processing failure"
            );
        }

        int attempts() {
            return attempts.get();
        }
    }
}
