package org.fincore.identity.refresh.api.dto;

import jakarta.validation.Valid;
import org.fincore.identity.refresh.usecase.RefreshTokenUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class RefreshController {

    private final RefreshTokenUseCase refreshTokenUseCase;

    public RefreshController(RefreshTokenUseCase refreshTokenUseCase) {
        this.refreshTokenUseCase = refreshTokenUseCase;
    }

    @PostMapping("/refresh")
    public RefreshResponse refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshSession session =
                refreshTokenUseCase.refresh(request.refreshToken());

        return new RefreshResponse(
                session.accessToken(),
                session.refreshToken(),
                "Bearer",
                session.expiresIn()
        );
    }
}
