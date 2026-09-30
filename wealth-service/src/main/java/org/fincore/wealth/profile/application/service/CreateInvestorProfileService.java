package org.fincore.wealth.profile.application.service;

import org.fincore.wealth.profile.application.command.CreateInvestorProfileCommand;
import org.fincore.wealth.profile.application.exception.InvestorProfileAlreadyExistsException;
import org.fincore.wealth.profile.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class CreateInvestorProfileService {

    private final InvestorProfileRepository repository;
    private final Clock clock;

    public CreateInvestorProfileService(
            InvestorProfileRepository repository,
            Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public InvestorProfile create(CreateInvestorProfileCommand command) {
        if (repository.findByUserId(command.userId()).isPresent()) {
            throw new InvestorProfileAlreadyExistsException("InvestorProfile with userId " + command.userId() + " already exists");
        }

        Instant now = clock.instant();


        InvestorProfile profile = new InvestorProfile(
                UUID.randomUUID(),
                command.userId(),
                command.firstName(),
                command.lastName(),
                command.countryCode(),
                command.riskProfile(),
                now,
                now
        );

        return repository.save(profile);
    }
}