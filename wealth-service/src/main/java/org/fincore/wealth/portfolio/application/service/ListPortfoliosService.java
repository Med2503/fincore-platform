package org.fincore.wealth.portfolio.application.service;

import org.fincore.wealth.portfolio.domain.Portfolio;
import org.fincore.wealth.portfolio.domain.PortfolioRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ListPortfoliosService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PortfolioRepository repository;

    public ListPortfoliosService(PortfolioRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<Portfolio> list(UUID userId, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be non-negative");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.asc("id")
                )
        );

        return repository.findAllByUserId(userId, pageable);
    }
}