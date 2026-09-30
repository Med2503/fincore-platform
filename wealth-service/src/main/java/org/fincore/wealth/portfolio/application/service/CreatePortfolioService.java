package org.fincore.wealth.portfolio.application.service;

import org.fincore.wealth.portfolio.application.command.CreatePortfolioCommand;
import org.fincore.wealth.portfolio.domain.Portfolio;
import org.fincore.wealth.portfolio.domain.PortfolioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class CreatePortfolioService {

    private final PortfolioRepository repository;
    private final Clock clock;

    public CreatePortfolioService(
            PortfolioRepository repository,
            Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Portfolio create(CreatePortfolioCommand command) {
        Portfolio portfolio = Portfolio.create(
                UUID.randomUUID(),
                command.userId(),
                command.name(),
                command.baseCurrency(),
                clock.instant()
        );

        return repository.save(portfolio);
    }
}