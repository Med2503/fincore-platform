package org.fincore.wealth.position.service;

import org.fincore.wealth.portfolio.application.exception.PortfolioNotFoundException;
import org.fincore.wealth.portfolio.domain.Portfolio;
import org.fincore.wealth.portfolio.domain.PortfolioRepository;
import org.fincore.wealth.position.api.dto.PositionPageResponse;
import org.fincore.wealth.position.application.port.MarketPriceProvider;
import org.fincore.wealth.position.application.port.PositionRepository;
import org.fincore.wealth.position.domain.MarketPrice;
import org.fincore.wealth.position.domain.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListPortfolioPositionsTest {

    @Mock
    private PortfolioRepository portfolios;

    @Mock
    private PositionRepository positions;

    @Mock
    private MarketPriceProvider prices;

    private ListPortfolioPositions service;

    private UUID userId;
    private UUID portfolioId;
    private UUID assetId1;
    private UUID assetId2;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-10-04T10:00:00Z"),
                ZoneOffset.UTC
        );

        MarketPricePolicy pricePolicy =
                new MarketPricePolicy(
                        clock,
                        new MarketPriceProperties(
                                Duration.ofSeconds(30)
                        )
                );

        service = new ListPortfolioPositions(
                portfolios,
                positions,
                prices,
                pricePolicy
        );

        userId = UUID.randomUUID();
        portfolioId = UUID.randomUUID();
        assetId1 = UUID.randomUUID();
        assetId2 = UUID.randomUUID();

        when(portfolios.findOwnedById(
                portfolioId,
                userId
        )).thenReturn(
                Optional.of(mock(Portfolio.class))
        );
    }

    @Test
    void shouldReturnValuatedPositions() {
        Position position = position(
                assetId1,
                "10",
                "90"
        );

        Page<Position> page = new PageImpl<>(
                List.of(position),
                PageRequest.of(0, 20),
                1
        );

        when(positions.findAllByPortfolioId(
                eq(portfolioId),
                any(Pageable.class)
        )).thenReturn(page);

        MarketPrice marketPrice = marketPrice(
                assetId1,
                "100",
                "2026-10-04T09:59:50Z"
        );

        when(prices.findLatestPrices(
                Set.of(assetId1)
        )).thenReturn(
                Map.of(assetId1, marketPrice)
        );

        PositionPageResponse response =
                service.execute(
                        userId,
                        portfolioId,
                        0,
                        20
                );

        assertThat(response.items())
                .hasSize(1);

        assertThat(response.items().get(0).priceAvailable())
                .isTrue();

        assertThat(response.items().get(0).marketPrice())
                .isEqualByComparingTo("100");

        assertThat(response.items().get(0).marketValue())
                .isEqualByComparingTo("1000");

        assertThat(response.items().get(0).unrealizedPnl())
                .isEqualByComparingTo("100");

        assertThat(response.hasNext())
                .isFalse();
    }

    @Test
    void shouldLoadPricesInOneBatch() {
        Position first = position(
                assetId1,
                "10",
                "90"
        );

        Position second = position(
                assetId2,
                "20",
                "200"
        );

        Page<Position> page = new PageImpl<>(
                List.of(first, second),
                PageRequest.of(0, 20),
                2
        );

        when(positions.findAllByPortfolioId(
                eq(portfolioId),
                any(Pageable.class)
        )).thenReturn(page);

        when(prices.findLatestPrices(
                anySet()
        )).thenReturn(
                Map.of(
                        assetId1,
                        marketPrice(
                                assetId1,
                                "100",
                                "2026-10-04T09:59:50Z"
                        ),
                        assetId2,
                        marketPrice(
                                assetId2,
                                "210",
                                "2026-10-04T09:59:50Z"
                        )
                )
        );

        service.execute(
                userId,
                portfolioId,
                0,
                20
        );

        ArgumentCaptor<Set<UUID>> captor =
                ArgumentCaptor.forClass(Set.class);

        verify(prices)
                .findLatestPrices(captor.capture());

        assertThat(captor.getValue())
                .containsExactlyInAnyOrder(
                        assetId1,
                        assetId2
                );
    }

    @Test
    void shouldNotCallPriceProviderWhenPageIsEmpty() {
        Page<Position> emptyPage =
                new PageImpl<>(
                        List.of(),
                        PageRequest.of(0, 20),
                        0
                );

        when(positions.findAllByPortfolioId(
                eq(portfolioId),
                any(Pageable.class)
        )).thenReturn(emptyPage);

        PositionPageResponse response =
                service.execute(
                        userId,
                        portfolioId,
                        0,
                        20
                );

        assertThat(response.items())
                .isEmpty();

        verifyNoInteractions(prices);
    }

    @Test
    void shouldMarkPositionUnavailableWhenPriceIsMissing() {
        Position position = position(
                assetId1,
                "10",
                "90"
        );

        Page<Position> page =
                new PageImpl<>(
                        List.of(position),
                        PageRequest.of(0, 20),
                        1
                );

        when(positions.findAllByPortfolioId(
                eq(portfolioId),
                any(Pageable.class)
        )).thenReturn(page);

        when(prices.findLatestPrices(
                Set.of(assetId1)
        )).thenReturn(Map.of());

        PositionPageResponse response =
                service.execute(
                        userId,
                        portfolioId,
                        0,
                        20
                );

        assertThat(
                response.items().get(0).priceAvailable()
        ).isFalse();

        assertThat(
                response.items().get(0).marketValue()
        ).isNull();
    }

    @Test
    void shouldMarkPositionUnavailableWhenPriceIsExpired() {
        Position position = position(
                assetId1,
                "10",
                "90"
        );

        Page<Position> page =
                new PageImpl<>(
                        List.of(position),
                        PageRequest.of(0, 20),
                        1
                );

        when(positions.findAllByPortfolioId(
                eq(portfolioId),
                any(Pageable.class)
        )).thenReturn(page);

        when(prices.findLatestPrices(
                Set.of(assetId1)
        )).thenReturn(
                Map.of(
                        assetId1,
                        marketPrice(
                                assetId1,
                                "100",
                                "2026-10-04T09:58:00Z"
                        )
                )
        );

        PositionPageResponse response =
                service.execute(
                        userId,
                        portfolioId,
                        0,
                        20
                );

        assertThat(
                response.items().get(0).priceAvailable()
        ).isFalse();
    }

    @Test
    void shouldMarkPositionUnavailableWhenCurrencyDoesNotMatch() {
        Position position = position(
                assetId1,
                "10",
                "90"
        );

        Page<Position> page =
                new PageImpl<>(
                        List.of(position),
                        PageRequest.of(0, 20),
                        1
                );

        when(positions.findAllByPortfolioId(
                eq(portfolioId),
                any(Pageable.class)
        )).thenReturn(page);

        when(prices.findLatestPrices(
                Set.of(assetId1)
        )).thenReturn(
                Map.of(
                        assetId1,
                        new MarketPrice(
                                assetId1,
                                new BigDecimal("100"),
                                "EUR",
                                Instant.parse(
                                        "2026-10-04T09:59:50Z"
                                )
                        )
                )
        );

        PositionPageResponse response =
                service.execute(
                        userId,
                        portfolioId,
                        0,
                        20
                );

        assertThat(
                response.items().get(0).priceAvailable()
        ).isFalse();
    }

    @Test
    void shouldRejectInvalidPagination() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.execute(
                                userId,
                                portfolioId,
                                -1,
                                20
                        )
                )
                .isInstanceOf(
                        IllegalArgumentException.class
                );

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.execute(
                                userId,
                                portfolioId,
                                0,
                                0
                        )
                )
                .isInstanceOf(
                        IllegalArgumentException.class
                );

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.execute(
                                userId,
                                portfolioId,
                                0,
                                101
                        )
                )
                .isInstanceOf(
                        IllegalArgumentException.class
                );
    }

    @Test
    void shouldRejectAccessToAnotherUsersPortfolio() {
        when(portfolios.findOwnedById(
                portfolioId,
                userId
        )).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.execute(
                                userId,
                                portfolioId,
                                0,
                                20
                        )
                )
                .isInstanceOf(
                        PortfolioNotFoundException.class
                );

        verifyNoInteractions(
                positions,
                prices
        );
    }

    @Test
    void shouldExposeHasNextFromDatabasePage() {
        Position position = position(
                assetId1,
                "10",
                "90"
        );

        Page<Position> page =
                new PageImpl<>(
                        List.of(position),
                        PageRequest.of(0, 1),
                        2
                );

        when(positions.findAllByPortfolioId(
                eq(portfolioId),
                any(Pageable.class)
        )).thenReturn(page);

        when(prices.findLatestPrices(
                Set.of(assetId1)
        )).thenReturn(
                Map.of(
                        assetId1,
                        marketPrice(
                                assetId1,
                                "100",
                                "2026-10-04T09:59:50Z"
                        )
                )
        );

        PositionPageResponse response =
                service.execute(
                        userId,
                        portfolioId,
                        0,
                        1
                );

        assertThat(response.hasNext())
                .isTrue();

        assertThat(response.page())
                .isZero();

        assertThat(response.size())
                .isEqualTo(1);
    }

    private Position position(
            UUID assetId,
            String quantity,
            String averageCost
    ) {
        return new Position(
                UUID.randomUUID(),
                portfolioId,
                assetId,
                new BigDecimal(quantity),
                new BigDecimal(averageCost),
                "USD",
                Instant.parse(
                        "2026-10-04T09:59:00Z"
                ),
                100L
        );
    }

    private MarketPrice marketPrice(
            UUID assetId,
            String price,
            String observedAt
    ) {
        return new MarketPrice(
                assetId,
                new BigDecimal(price),
                "USD",
                Instant.parse(observedAt)
        );
    }
}