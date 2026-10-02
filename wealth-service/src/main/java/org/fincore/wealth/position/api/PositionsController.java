package org.fincore.wealth.position.api;


import org.fincore.wealth.position.api.dto.PositionPageResponse;
import org.fincore.wealth.position.service.ListPortfolioPositions;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wealth/portfolios/{portfolioId}/positions")
public class PositionsController {
    private final ListPortfolioPositions listPositions;

    public PositionsController(ListPortfolioPositions listPositions) {
        this.listPositions = listPositions;
    }

    @GetMapping
    public PositionPageResponse list(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID portfolioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return listPositions.execute(userId, portfolioId, page, size);
    }
}