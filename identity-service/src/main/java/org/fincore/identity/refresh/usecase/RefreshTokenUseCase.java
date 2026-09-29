package org.fincore.identity.refresh.usecase;

import org.fincore.identity.auth.application.port.TokenIssuer;
import org.fincore.identity.refresh.api.dto.RefreshSession;
import org.fincore.identity.refresh.exceptions.InvalidRefreshTokenException;
import org.fincore.identity.refresh.exceptions.RefreshTokenReuseDetectedException;
import org.fincore.identity.refresh.infrastructure.persistence.RefreshTokenRotationService;
import org.fincore.identity.refresh.infrastructure.persistence.RotationResult;
import org.fincore.identity.refresh.infrastructure.persistence.RotationStatus;
import org.fincore.identity.user.domain.model.User;
import org.fincore.identity.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenUseCase {

    private final RefreshTokenRotationService rotationService;
    private final UserRepository userRepository;
    private final TokenIssuer tokenIssuer;

    public RefreshTokenUseCase(
            RefreshTokenRotationService rotationService,
            UserRepository userRepository,
            TokenIssuer tokenIssuer
    ) {
        this.rotationService = rotationService;
        this.userRepository = userRepository;
        this.tokenIssuer = tokenIssuer;
    }

    public RefreshSession refresh(String rawToken) {
        RotationResult result = rotationService.rotate(rawToken);

        if (result.status() == RotationStatus.REUSE_DETECTED) {
            throw new RefreshTokenReuseDetectedException("RefreshToken reused !");
        }

        if (result.status() != RotationStatus.SUCCESS) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(result.userId())
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!user.isActive()) {
            throw new InvalidRefreshTokenException();
        }

        String accessToken = tokenIssuer.issue(user);

        return new RefreshSession(
                accessToken,
                result.rawRefreshToken(),
                900
        );
    }
}