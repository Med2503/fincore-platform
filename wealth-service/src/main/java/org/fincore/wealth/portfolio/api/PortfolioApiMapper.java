package org.fincore.wealth.portfolio.api;

import org.fincore.wealth.portfolio.domain.Portfolio;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PortfolioApiMapper {

    PortfolioResponse toResponse(Portfolio portfolio);
}