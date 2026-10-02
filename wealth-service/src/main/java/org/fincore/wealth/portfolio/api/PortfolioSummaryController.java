package org.fincore.wealth.portfolio.api;


import org.fincore.wealth.portfolio.application.service.GetPortfolioSummary;
import org.fincore.wealth.portfolio.domain.PortfolioSummary;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wealth/portfolios/{portfolioId}/summary")
public class PortfolioSummaryController {

    private final GetPortfolioSummary getSummary;

    public PortfolioSummaryController(GetPortfolioSummary getSummary) {
        this.getSummary = getSummary;
    }

    @GetMapping
    public PortfolioSummary get(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID portfolioId
    ) {
        return getSummary.execute(userId, portfolioId);
    }
}