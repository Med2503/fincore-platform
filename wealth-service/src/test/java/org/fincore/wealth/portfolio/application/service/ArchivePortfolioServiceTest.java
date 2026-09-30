package org.fincore.wealth.portfolio.application.service;


import org.fincore.wealth.portfolio.application.exception.PortfolioNotFoundException;
import org.fincore.wealth.portfolio.application.service.ArchivePortfolioService;
import org.fincore.wealth.portfolio.domain.Portfolio;
import org.fincore.wealth.portfolio.domain.PortfolioNotActiveException;
import org.fincore.wealth.portfolio.domain.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchivePortfolioServiceTest {

    @Mock
    private PortfolioRepository repository;

    private ArchivePortfolioService service;
    private Clock clock;

    @BeforeEach
    void setUp() {
        service = new ArchivePortfolioService(repository, clock);
    }

    @Test
    void shouldArchiveOwnedPortfolio() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        Portfolio portfolio = Portfolio.create(
                portfolioId,
                userId,
                "Trading",
                "EUR",
                Instant.now()
        );

        when(repository.findOwnedById(portfolioId, userId))
                .thenReturn(java.util.Optional.of(portfolio));

        when(repository.save(any(Portfolio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Portfolio result =
                service.archive(portfolioId, userId);

        assertThat(result.getStatus())
                .isEqualTo(org.fincore.wealth.portfolio.domain.PortfolioStatus.ARCHIVED);

        verify(repository).findOwnedById(portfolioId, userId);
        verify(repository).save(portfolio);
    }

    @Test
    void shouldFailWhenPortfolioDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        when(repository.findOwnedById(portfolioId, userId))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() ->
                service.archive(portfolioId, userId)
        )
                .isInstanceOf(PortfolioNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldNotArchiveAlreadyArchivedPortfolio() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        Portfolio portfolio = Portfolio.create(
                portfolioId,
                userId,
                "Trading",
                "EUR",
                Instant.now()
        );

        portfolio.archive(Instant.now());

        when(repository.findOwnedById(portfolioId, userId))
                .thenReturn(java.util.Optional.of(portfolio));

        assertThatThrownBy(() ->
                service.archive(portfolioId, userId)
        )
                .isInstanceOf(PortfolioNotActiveException.class);

        verify(repository, never()).save(any());
    }
}