package org.fincore.wealth.portfolio.infrastructure.persistence;

import org.fincore.wealth.portfolio.domain.Portfolio;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PortfolioMapper {

    PortfolioJpaEntity toEntity(Portfolio portfolio);

    Portfolio toDomain(PortfolioJpaEntity entity);
}