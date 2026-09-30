package org.fincore.wealth.portfolio.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PortfolioTest {

    private final UUID portfolioId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-30T10:00:00Z");

    @Test
    void shouldNormalizeCurrency() {
        Portfolio portfolio = Portfolio.create(
                portfolioId,
                userId,
                "Long Term",
                "usd",
                now
        );

        assertEquals("USD", portfolio.getBaseCurrency());
        assertEquals(PortfolioStatus.ACTIVE, portfolio.getStatus());
    }

    @Test
    void shouldArchiveActivePortfolio() {
        Portfolio portfolio = Portfolio.create(
                portfolioId,
                userId,
                "Long Term",
                "USD",
                now
        );

        portfolio.archive(now.plusSeconds(60));

        assertEquals(PortfolioStatus.ARCHIVED, portfolio.getStatus());
        assertEquals(now.plusSeconds(60), portfolio.getUpdatedAt());
    }

    @Test
    void shouldRejectArchivingTwice() {
        Portfolio portfolio = Portfolio.create(
                portfolioId,
                userId,
                "Long Term",
                "USD",
                now
        );

        portfolio.archive(now.plusSeconds(60));

        assertThrows(
                PortfolioNotActiveException.class,
                () -> portfolio.archive(now.plusSeconds(120))
        );
    }
}