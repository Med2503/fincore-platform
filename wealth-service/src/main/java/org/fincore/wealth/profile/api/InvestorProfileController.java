package org.fincore.wealth.profile.api;

import jakarta.validation.Valid;
import org.fincore.wealth.profile.api.dto.CreateInvestorProfileRequest;
import org.fincore.wealth.profile.api.dto.InvestorProfileResponse;
import org.fincore.wealth.profile.application.command.CreateInvestorProfileCommand;
import org.fincore.wealth.profile.application.service.CreateInvestorProfileService;
import org.fincore.wealth.profile.domain.InvestorProfile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wealth/profile")
public class InvestorProfileController {

    private final CreateInvestorProfileService createService;

    public InvestorProfileController(
            CreateInvestorProfileService createService
    ) {
        this.createService = createService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvestorProfileResponse create(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateInvestorProfileRequest request
    ) {
        InvestorProfile profile = createService.create(
                new CreateInvestorProfileCommand(
                        userId,
                        request.firstName(),
                        request.lastName(),
                        request.countryCode(),
                        request.riskProfile()
                )
        );

        return toResponse(profile);
    }

    private InvestorProfileResponse toResponse(InvestorProfile profile) {
        return new InvestorProfileResponse(
                profile.getId(),
                profile.getUserId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getCountryCode(),
                profile.getRiskProfile(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}