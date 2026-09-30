package org.fincore.wealth.portfolio.application.service;

import org.fincore.wealth.portfolio.domain.Portfolio;
import org.fincore.wealth.portfolio.domain.PortfolioRepository;
import org.fincore.wealth.portfolio.domain.PortfolioStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatePortfolioServiceTest {

    @Mock
    private PortfolioRepository repository;

    private CreatePortfolioService service;

    @BeforeEach
    void setUp() {
        service = new CreatePortfolioService(repository);
    }

    @Test
    void shouldCreatePortfolio() {
        UUID userId = UUID.randomUUID();

        when(repository.save(any(Portfolio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Portfolio result = service.execute(
                userId,
                "Long Term Portfolio",
                "eur"
        );

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getName()).isEqualTo("Long Term Portfolio");
        assertThat(result.getBaseCurrency()).isEqualTo("EUR");
        assertThat(result.getStatus()).isEqualTo(PortfolioStatus.ACTIVE);
        assertThat(result.getId()).isNotNull();

        verify(repository).save(any(Portfolio.class));
    }

    @Test
    void shouldNormalizeCurrencyBeforeSaving() {
        UUID userId = UUID.randomUUID();

        when(repository.save(any(Portfolio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(
                userId,
                "Investment Portfolio",
                "usd"
        );

        ArgumentCaptor<Portfolio> captor =
                ArgumentCaptor.forClass(Portfolio.class);

        verify(repository).save(captor.capture());

        Portfolio saved = captor.getValue();

        assertThat(saved.getBaseCurrency()).isEqualTo("USD");
    }
}