package org.fincore.wealth.application.service;

import org.fincore.wealth.profile.application.command.CreateInvestorProfileCommand;
import org.fincore.wealth.profile.application.exception.InvestorProfileAlreadyExistsException;
import org.fincore.wealth.profile.application.service.CreateInvestorProfileService;
import org.fincore.wealth.profile.domain.InvestorProfile;
import org.fincore.wealth.profile.domain.InvestorProfileRepository;
import org.fincore.wealth.profile.domain.RiskProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateInvestorProfileServiceTest {

    @Mock
    InvestorProfileRepository repository;

    private CreateInvestorProfileService service;

    private final UUID userId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-29T10:00:00Z");

    @BeforeEach
    void setUp() {
        service = new CreateInvestorProfileService(
                repository,
                Clock.fixed(now, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldCreateProfile() {
        when(repository.findByUserId(userId))
                .thenReturn(Optional.empty());

        when(repository.save(any(InvestorProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InvestorProfile result = service.create(
                new CreateInvestorProfileCommand(
                        userId,
                        "Ada",
                        "Lovelace",
                        "GB",
                        RiskProfile.GROWTH
                )
        );

        assertEquals(userId, result.getUserId());
        assertEquals(now, result.getCreatedAt());
        verify(repository).save(any(InvestorProfile.class));
    }

    @Test
    void shouldRejectDuplicateProfile() {
        when(repository.findByUserId(userId))
                .thenReturn(Optional.of(mock(InvestorProfile.class)));

        assertThrows(
                InvestorProfileAlreadyExistsException.class,
                () -> service.create(new CreateInvestorProfileCommand(
                        userId,
                        "Ada",
                        "Lovelace",
                        "GB",
                        RiskProfile.GROWTH
                ))
        );

        verify(repository, never()).save(any());
    }
}