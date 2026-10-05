package org.fincore.wealth.position.messaging;

import org.fincore.wealth.position.application.event.PositionChangedEvent;
import org.fincore.wealth.position.infrastructure.messaging.PositionChangedConsumer;
import org.fincore.wealth.position.infrastructure.messaging.PositionChangedConsumerConfiguration;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PositionChangedConsumerConfigurationTest {

    @Test
    void shouldDelegateEventToConsumer() {
        PositionChangedConsumer consumer =
                mock(PositionChangedConsumer.class);

        PositionChangedConsumerConfiguration configuration =
                new PositionChangedConsumerConfiguration();

        Consumer<PositionChangedEvent> input =
                configuration.positionChangedInput(consumer);

        PositionChangedEvent event =
                new PositionChangedEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        BigDecimal.TEN,
                        BigDecimal.ONE,
                        "EUR",
                        1,
                        Instant.parse(
                                "2026-10-05T10:00:00Z"
                        )
                );

        input.accept(event);

        verify(consumer).consume(event);
    }
}