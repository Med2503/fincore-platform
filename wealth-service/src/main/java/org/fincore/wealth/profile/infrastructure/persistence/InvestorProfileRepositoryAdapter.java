package org.fincore.wealth.profile.infrastructure.persistence;

import org.fincore.wealth.profile.domain.InvestorProfile;
import org.fincore.wealth.profile.domain.InvestorProfileRepository;

import java.util.Optional;
import java.util.UUID;

public class InvestorProfileRepositoryAdapter implements InvestorProfileRepository {

    private final InvestorProfileJpaRepository jpaRepository;
    private final InvestorProfileMapper mapper;

    public InvestorProfileRepositoryAdapter(InvestorProfileJpaRepository jpaRepository, InvestorProfileMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }


    @Override
    public InvestorProfile save(InvestorProfile profile) {
        var saved = jpaRepository.save(mapper.toEntity(profile));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<InvestorProfile> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId)
                .map(mapper::toDomain);
    }
}
