
package org.fincore.wealth.portfolio.application.service;

import org.fincore.wealth.portfolio.application.command.CreatePortfolioCommand;
import org.fincore.wealth.portfolio.domain.Portfolio;
import org.fincore.wealth.portfolio.domain.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatePortfolioServiceTest {

    @Mock
    private PortfolioRepository repository;

    private Clock clock;

    private CreatePortfolioService service;

    private static final Instant FIXED_INSTANT =
            Instant.parse("2026-10-02T10:15:30Z");

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(
                FIXED_INSTANT,
                ZoneOffset.UTC
        );

        service = new CreatePortfolioService(
                repository,
                clock
        );
    }

    @Test
    void shouldCreatePortfolioSuccessfully() {

        // Given
        UUID userId = UUID.randomUUID();

        CreatePortfolioCommand command =
                new CreatePortfolioCommand(
                        userId,
                        "Long Term Investment",
                        "EUR"
                );

        when(repository.save(any(Portfolio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Portfolio result = service.create(command);

        // Then
        assertNotNull(result);

        ArgumentCaptor<Portfolio> captor =
                ArgumentCaptor.forClass(Portfolio.class);

        verify(repository, times(1))
                .save(captor.capture());

        Portfolio savedPortfolio = captor.getValue();

        assertNotNull(savedPortfolio);

        assertEquals(userId, savedPortfolio.getUserId());
        assertEquals("Long Term Investment", savedPortfolio.getName());
        assertEquals("EUR", savedPortfolio.getBaseCurrency());
        assertEquals(FIXED_INSTANT, savedPortfolio.getCreatedAt());

        assertNotNull(savedPortfolio.getId());

        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldGenerateUniquePortfolioId() {

        // Given
        UUID userId = UUID.randomUUID();

        CreatePortfolioCommand command =
                new CreatePortfolioCommand(
                        userId,
                        "Retirement Portfolio",
                        "USD"
                );

        when(repository.save(any(Portfolio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Portfolio firstPortfolio = service.create(command);
        Portfolio secondPortfolio = service.create(command);

        // Then
        assertNotNull(firstPortfolio.getId());
        assertNotNull(secondPortfolio.getId());

        assertNotEquals(
                firstPortfolio.getId(),
                secondPortfolio.getId()
        );

        verify(repository, times(2))
                .save(any(Portfolio.class));
    }

    @Test
    void shouldReturnPortfolioSavedByRepository() {

        // Given
        UUID userId = UUID.randomUUID();

        CreatePortfolioCommand command =
                new CreatePortfolioCommand(
                        userId,
                        "Growth Portfolio",
                        "USD"
                );

        Portfolio expectedPortfolio = Portfolio.create(
                UUID.randomUUID(),
                userId,
                "Growth Portfolio",
                "USD",
                FIXED_INSTANT
        );

        when(repository.save(any(Portfolio.class)))
                .thenReturn(expectedPortfolio);

        // When
        Portfolio result = service.create(command);

        // Then
        assertSame(expectedPortfolio, result);

        verify(repository).save(any(Portfolio.class));
    }

    @Test
    void shouldUseClockForPortfolioCreationDate() {

        // Given
        UUID userId = UUID.randomUUID();

        CreatePortfolioCommand command =
                new CreatePortfolioCommand(
                        userId,
                        "Clock Test Portfolio",
                        "TND"
                );

        when(repository.save(any(Portfolio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Portfolio result = service.create(command);

        // Then
        assertEquals(FIXED_INSTANT, result.getCreatedAt());
    }
}
