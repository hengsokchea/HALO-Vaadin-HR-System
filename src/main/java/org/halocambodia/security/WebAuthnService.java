package org.halocambodia.security;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Service;

@Service
public class WebAuthnService {

    public String generateChallenge() {

        byte[] challenge = new byte[32];

        new SecureRandom().nextBytes(challenge);

        return Base64.getEncoder().withoutPadding().encodeToString(challenge);
    }
}