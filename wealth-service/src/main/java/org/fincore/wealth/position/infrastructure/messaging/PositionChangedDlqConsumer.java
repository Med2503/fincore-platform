package org.fincore.wealth.position.infrastructure.messaging;

import org.fincore.wealth.position.application.service.DlqEventReplayService;
import org.fincore.wealth.position.application.service.DlqReplayResult;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PositionChangedDlqConsumer {

    private static final String REPLAY_COUNT_HEADER =
            "x-dlq-replay-count";

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

        int replayCount =
                extractReplayCount(message);

        DlqReplayResult result =
                replayService.replay(
                        eventId,
                        payload,
                        replayCount,
                        properties.maxReplays()
                );

        if (result == DlqReplayResult.REJECTED) {
            throw new IllegalStateException(
                    "Maximum DLQ replay count reached for event "
                            + eventId
            );
        }
    }

    private int extractReplayCount(
            Message<String> message
    ) {
        Object value =
                message.getHeaders()
                        .get(REPLAY_COUNT_HEADER);

        if (value == null) {
            return 0;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        return Integer.parseInt(
                value.toString()
        );
    }
}