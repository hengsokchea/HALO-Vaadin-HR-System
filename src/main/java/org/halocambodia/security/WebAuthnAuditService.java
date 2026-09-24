package org.halocambodia.security;

import java.time.LocalDateTime;

import org.halocambodia.data.User;
import org.halocambodia.data.UserWebAuthn;
import org.halocambodia.data.UserWebAuthnAudit;
import org.halocambodia.data.UserWebAuthnAuditRepository;
import org.springframework.stereotype.Service;

import com.vaadin.flow.server.VaadinServletRequest;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebAuthnAuditService {
	private final UserWebAuthnAuditRepository repository;
	
    public void log(User user,UserWebAuthn credential,String eventType,boolean success,String details) {

    	HttpServletRequest request = null;

    	if (VaadinServletRequest.getCurrent() != null) {
    	    request =VaadinServletRequest.getCurrent().getHttpServletRequest();
    	}

        UserWebAuthnAudit audit =new UserWebAuthnAudit();

        audit.setUser(user);
        audit.setCredential(credential);

        audit.setEventType(eventType);
        audit.setSuccess(success);

        audit.setIpAddress(request != null ? request.getRemoteAddr(): null);

        audit.setUserAgent(request != null? request.getHeader("User-Agent") : null);

        audit.setDetails(details);

        audit.setCreatedAt(LocalDateTime.now()
        );

        repository.save(audit);
    }
}
