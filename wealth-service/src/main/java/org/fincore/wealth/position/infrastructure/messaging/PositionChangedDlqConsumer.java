package org.fincore.wealth.position.infrastructure.messaging;

import org.fincore.wealth.position.application.service.DlqEventReplayService;
import org.fincore.wealth.position.application.service.DlqReplayResult;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PositionChangedDlqConsumer {

    private final DlqEventReplayService replayService;
    private final DlqReplayProperties properties;
    private final PositionChangedEventIdExtractor eventIdExtractor;

    public PositionChangedDlqConsumer(
            DlqEventReplayService replayService,
            DlqReplayProperties properties,
            PositionChangedEventIdExtractor eventIdExtractor
    ) {
        this.replayService = replayService;
        this.properties = properties;
        this.eventIdExtractor = eventIdExtractor;
    }

    public void consume(Message<String> message) {

        String payload = message.getPayload();

        UUID eventId =
                eventIdExtractor.extract(payload);

        DlqReplayResult result =
                replayService.replay(
                        eventId,
                        payload,
                        properties.maxReplays()
                );

        if (result == DlqReplayResult.REJECTED) {
            throw new IllegalStateException(
                    "Maximum DLQ replay count reached for event "
                            + eventId
            );
        }
    }
}