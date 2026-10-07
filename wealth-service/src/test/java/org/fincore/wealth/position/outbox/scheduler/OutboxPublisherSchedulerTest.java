package org.fincore.wealth.position.outbox.scheduler;

import org.fincore.wealth.position.outbox.application.service.PublishOutboxEvents;
import org.fincore.wealth.position.outbox.infrastructure.scheduler.OutboxPublisherScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OutboxPublisherSchedulerTest {

    private PublishOutboxEvents publisher;
    private OutboxPublisherScheduler scheduler;

    @BeforeEach
    void setUp() {
        publisher =
                mock(PublishOutboxEvents.class);

        scheduler =
                new OutboxPublisherScheduler(
                        publisher
                );
    }

    @Test
    void shouldTriggerOutboxPublication() {
        scheduler.publish();

        verify(publisher).publishBatch();
    }
}