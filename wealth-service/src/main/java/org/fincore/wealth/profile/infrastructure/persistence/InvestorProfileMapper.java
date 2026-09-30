package org.fincore.wealth.profile.infrastructure.persistence;

import org.fincore.wealth.profile.domain.InvestorProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvestorProfileMapper {

    InvestorProfileJpaEntity toEntity(InvestorProfile profile);

    InvestorProfile toDomain(InvestorProfileJpaEntity entity);
}