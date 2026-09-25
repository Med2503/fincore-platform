package org.fincore.identity.auth.infrastructure.security;

import org.fincore.identity.auth.application.port.TokenIssuer;
import org.fincore.identity.user.domain.model.User;
import org.springframework.stereotype.Component;


// for dev grade
/*@Component
public class UnsupportedTokenIssuer implements TokenIssuer {

    @Override
    public String issue(User user) {
        throw new UnsupportedOperationException(
                "JWT token issuer is not implemented yet"
        );
    }
}*/
