package org.fincore.wealth.position.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fincore.wealth.position.application.port.DlqReplayAttemptRepository;
import org.fincore.wealth.position.application.port.DlqReplayOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class DlqEventReplayService {

    private final DlqReplayAttemptRepository attempts;
    private final DlqReplayOutboxRepository outbox;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public DlqEventReplayService(
            DlqReplayAttemptRepository attempts,
            DlqReplayOutboxRepository outbox,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.attempts = attempts;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public DlqReplayResult replay(
            UUID eventId,
            String payload,
            int maxReplays
    ) {
        validate(
                eventId,
                payload,
                maxReplays
        );

        UUID portfolioId =
                extractPortfolioId(payload);

        Instant now = clock.instant();

        int claimed =
                attempts.claimNextReplay(
                        eventId,
                        now,
                        maxReplays
                );

        if (claimed == 0) {
            return DlqReplayResult.REJECTED;
        }

        outbox.save(
                eventId,
                portfolioId,
                payload,
                now
        );

        return DlqReplayResult.REPLAYED;
    }

    private UUID extractPortfolioId(
            String payload
    ) {
        try {
            JsonNode root =
                    objectMapper.readTree(payload);

            JsonNode portfolioId =
                    root.get("portfolioId");

            if (portfolioId == null
                    || portfolioId.isNull()
                    || portfolioId.asText().isBlank()) {
                throw new IllegalArgumentException(
                        "Missing portfolioId in payload"
                );
            }

            return UUID.fromString(
                    portfolioId.asText()
            );

        } catch (IOException
                 | IllegalArgumentException exception) {

            throw new IllegalArgumentException(
                    "Invalid PositionChanged payload",
                    exception
            );
        }
    }

    private void validate(
            UUID eventId,
            String payload,
            int maxReplays
    ) {
        Objects.requireNonNull(
                eventId,
                "Event ID is required"
        );

        Objects.requireNonNull(
                payload,
                "Payload is required"
        );

        if (payload.isBlank()) {
            throw new IllegalArgumentException(
                    "Payload cannot be blank"
            );
        }

        if (maxReplays <= 0) {
            throw new IllegalArgumentException(
                    "Maximum replays must be positive"
            );
        }
    }
}