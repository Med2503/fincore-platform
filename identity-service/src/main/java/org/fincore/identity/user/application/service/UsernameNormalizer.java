package org.fincore.identity.user.application.service;


import org.springframework.stereotype.Component;

@Component
public class UsernameNormalizer {

    public String normalize(String username) {
        if (username == null) {
            return null;
        }

        return username.trim().toLowerCase();
    }
}
