package org.fincore.identity.auth.application.port;

import java.util.List;
import java.util.UUID;

public interface UserAuthoritiesProvider {

    List<String> getAuthorities(UUID userId);
}