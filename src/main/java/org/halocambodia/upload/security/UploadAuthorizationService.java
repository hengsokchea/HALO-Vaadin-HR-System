package org.halocambodia.upload.security;

import org.halocambodia.upload.domain.UploadSession;
import org.halocambodia.upload.exception.UploadException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UploadAuthorizationService {

    public void requireOwnerOrAdministrator(UploadSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UploadException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        boolean administrator = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "UPLOAD_ADMIN".equals(authority.getAuthority()));

        String owner = session.getOwnerUsername();
        boolean ownerMatches = owner == null || owner.isBlank()
                || owner.equals(authentication.getName())
                || ("anonymous".equals(owner) && "anonymousUser".equals(authentication.getName()));

        if (!administrator && !ownerMatches) {
            throw new UploadException(HttpStatus.FORBIDDEN,
                    "You are not allowed to access this uploaded file");
        }
    }
}
