package org.fincore.identity.auth.api;

import jakarta.servlet.http.HttpServletRequest;
import org.fincore.identity.audit.application.SecurityAuditService;
import org.fincore.identity.refresh.application.RevokeAllSessionsUseCase;

import org.fincore.identity.shared.security.dto.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class SessionController {

    private final RevokeAllSessionsUseCase revokeAllSessions;
    private final SecurityAuditService auditService;

    public SessionController(RevokeAllSessionsUseCase revokeAllSessions, SecurityAuditService auditService) {
        this.revokeAllSessions = revokeAllSessions;
        this.auditService = auditService;
    }

    @PostMapping("/logout-all")
    public void logoutAll(
            @AuthenticationPrincipal AuthenticatedUser principal,
            HttpServletRequest request
    ) {
        revokeAllSessions.revokeAll(principal.userId());
        auditService.record(
                principal.userId(),
                "LOGOUT_ALL_SUCCEEDED",
                request.getHeader("X-Correlation-Id"),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );
    }
}
