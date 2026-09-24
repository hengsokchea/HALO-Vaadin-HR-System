package org.halocambodia.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import org.halocambodia.data.EmployeeLeave;
import org.halocambodia.data.LeaveActionToken;
import org.halocambodia.data.LeaveActionTokenRepository;
import org.springframework.stereotype.Service;

@Service
public class LeaveTokenService {

    private final LeaveActionTokenRepository repo;

    public LeaveTokenService(LeaveActionTokenRepository repo) {
        this.repo = repo;
    }

    public String createToken(
            EmployeeLeave leave,
            String actionType
    ) {

        try {

            // RAW token sent to email
            String rawToken =
                    UUID.randomUUID().toString()
                    + UUID.randomUUID();

            // HASH stored in DB
            String tokenHash = sha256(rawToken);

            LeaveActionToken token =
                    new LeaveActionToken();

            token.setTokenHash(tokenHash);

            token.setActionType(actionType);

            token.setEmployeeLeave(leave);

            token.setUsed(false);

            token.setExpiresAt(LocalDateTime.now().plusDays(3));

            repo.save(token);

            return rawToken;

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    public String sha256(String input) {

        try {

            MessageDigest digest =MessageDigest.getInstance("SHA-256");

            byte[] hash =digest.digest( input.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }
}