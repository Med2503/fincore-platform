package org.fincore.identity.user.application.port;

public interface PasswordHasher {
    String hash(String password);

    boolean matches(String password, String passwordHash);
}
