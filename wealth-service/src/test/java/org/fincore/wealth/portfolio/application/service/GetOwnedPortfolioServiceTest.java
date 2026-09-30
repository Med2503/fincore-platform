package org.fincore.wealth.portfolio.application.service;


import org.fincore.wealth.portfolio.application.exception.PortfolioNotFoundException;
import org.fincore.wealth.portfolio.domain.Portfolio;
import org.fincore.wealth.portfolio.domain.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetPortfolioServiceTest {

    @Mock
    private PortfolioRepository repository;

    private GetPortfolioService service;

    private Clock clock;

    @BeforeEach
    void setUp() {
        service = new GetPortfolioService(repository);
    }

    @Test
    void shouldReturnPortfolioOwnedByUser() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        Portfolio portfolio = Portfolio.create(
                portfolioId,
                userId,
                "Retirement",
                "EUR",
                Instant.now()
        );

        when(repository.findOwnedById(portfolioId, userId))
                .thenReturn(Optional.of(portfolio));

        Portfolio result = service.getOwnedPortfolio(portfolioId, userId);

        assertThat(result).isSameAs(portfolio);

        verify(repository)
                .findOwnedById(portfolioId, userId);
    }

    @Test
    void shouldThrowWhenPortfolioDoesNotBelongToUser() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        when(repository.findOwnedById(portfolioId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getOwnedPortfolio(portfolioId, userId)
        )
                .isInstanceOf(PortfolioNotFoundException.class);

        verify(repository)
                .findOwnedById(portfolioId, userId);
    }
}