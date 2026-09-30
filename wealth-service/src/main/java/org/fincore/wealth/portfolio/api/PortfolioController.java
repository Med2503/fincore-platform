package org.fincore.wealth.portfolio.api;

import jakarta.validation.Valid;
import org.fincore.wealth.portfolio.application.*;
import org.fincore.wealth.portfolio.application.command.CreatePortfolioCommand;
import org.fincore.wealth.portfolio.application.service.ArchivePortfolioService;
import org.fincore.wealth.portfolio.application.service.CreatePortfolioService;
import org.fincore.wealth.portfolio.application.service.ListPortfoliosService;
import org.fincore.wealth.portfolio.application.service.GetPortfolioService;
import org.fincore.wealth.portfolio.domain.Portfolio;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wealth/portfolios")
public class PortfolioController {

    private final CreatePortfolioService createService;
    private final GetPortfolioService getService;
    private final ListPortfoliosService listService;
    private final ArchivePortfolioService archiveService;
    private final PortfolioApiMapper mapper;

    public PortfolioController(
            CreatePortfolioService createService,
            GetPortfolioService getService,
            ListPortfoliosService listService,
            ArchivePortfolioService archiveService,
            PortfolioApiMapper mapper
    ) {
        this.createService = createService;
        this.getService = getService;
        this.listService = listService;
        this.archiveService = archiveService;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioResponse create(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreatePortfolioRequest request
    ) {
        Portfolio portfolio = createService.create(
                new CreatePortfolioCommand(
                        userId,
                        request.name(),
                        request.baseCurrency()
                )
        );

        return mapper.toResponse(portfolio);
    }

    @GetMapping("/{portfolioId}")
    public PortfolioResponse get(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID portfolioId
    ) {
        return mapper.toResponse(
                getService.getOwnedPortfolio(portfolioId, userId)
        );
    }

    @GetMapping
    public Page<PortfolioResponse> list(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return listService.list(userId, page, size)
                .map(mapper::toResponse);
    }

    @PostMapping("/{portfolioId}/archive")
    public PortfolioResponse archive(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID portfolioId
    ) {
        return mapper.toResponse(
                archiveService.archive(portfolioId, userId)
        );
    }
}