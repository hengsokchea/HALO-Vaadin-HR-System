package org.halocambodia.views;

import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.halocambodia.data.User;
import org.halocambodia.data.UserRepository;
import org.halocambodia.data.UserWebAuthn;
import org.halocambodia.data.UserWebAuthnRepository;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.security.CustomUserPrincipal;
import org.halocambodia.security.UserDetailsServiceImpl;
import org.halocambodia.security.WebAuthnAuditService;
import org.halocambodia.security.WebAuthnRegistrationService;
import org.halocambodia.security.WebAuthnRegistrationVerificationService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.RegistrationData;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.halocambodia.security.WebAuthnAuthenticationVerificationService;

@UIScope
@SpringComponent
@Slf4j
public class WebAuthnBridge extends Div {
    
    private final UserWebAuthnRepository userWebAuthnRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUser authenticatedUser;
    private final UserDetailsServiceImpl userDetailsService;
    private final WebAuthnRegistrationService webAuthnRegistrationService;
    private final WebAuthnRegistrationVerificationService registrationVerificationService;
    private final WebAuthnAuthenticationVerificationService authenticationVerificationService;
    private final WebAuthnAuditService auditService;
    
    private Dialog registrationDialog;

    public void setRegistrationDialog(Dialog dialog) {
        this.registrationDialog = dialog;
    }

    public WebAuthnBridge(
            UserWebAuthnRepository userWebAuthnRepository,
            UserRepository userRepository,
            AuthenticatedUser authenticatedUser,
            UserDetailsServiceImpl userDetailsService,
            WebAuthnRegistrationService webAuthnRegistrationService,
            WebAuthnRegistrationVerificationService registrationVerificationService,
            WebAuthnAuthenticationVerificationService authenticationVerificationService,WebAuthnAuditService auditService) {

        this.userWebAuthnRepository = userWebAuthnRepository;
        this.userRepository = userRepository;
        this.authenticatedUser = authenticatedUser;
        this.userDetailsService = userDetailsService;
        this.webAuthnRegistrationService = webAuthnRegistrationService;
        this.registrationVerificationService = registrationVerificationService;
        this.authenticationVerificationService = authenticationVerificationService;
        this.auditService = auditService;
    }
    
    @ClientCallable
    public String saveCredential(String json) {

        try {

            log.info( json);

            ObjectMapper mapper = new ObjectMapper();

            JsonNode node = mapper.readTree(json);

            //String credentialId =node.get("credentialId").asText();

            //String userHandle =node.path("userHandle").asText(null);
            

            String deviceName =node.path("deviceName").asText("Unknown Device");
            String userAgent =node.path("userAgent").asText(null);

            String credentialType =node.path("credentialType").asText("public-key");
            
            
            String rawId =node.get("rawId").asText();
            String clientDataJSON =node.get("clientDataJSON").asText();
            String attestationObject =node.get("attestationObject").asText();
            
            //RegistrationData registrationData =webAuthnRegistrationService.parse(clientDataJSON, attestationObject);
            String challenge =(String) VaadinSession.getCurrent().getAttribute("webauthn-register-challenge");
            if (challenge == null) {
                return "CHALLENGE_NOT_FOUND";
            }

            RegistrationData registrationData = registrationVerificationService.verifyRegistration(clientDataJSON,attestationObject,challenge);
            
            byte[] credentialIdBytes =registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData().getCredentialId();

           String verifiedCredentialId =Base64.getEncoder().encodeToString(credentialIdBytes);
            
            VaadinSession.getCurrent().setAttribute("webauthn-register-challenge", null);
            
            
            String publicKey =
            	    Base64.getEncoder()
            	          .encodeToString(
            	              registrationData
            	                  .getAttestationObject()
            	                  .getAuthenticatorData()
            	                  .getAttestedCredentialData()
            	                  .getCOSEKey()
            	                  .getPublicKey()
            	                  .getEncoded()
            	          );


            
          	    
            long signCount =registrationData.getAttestationObject().getAuthenticatorData().getSignCount();
                        
            Optional<User> maybeUser =authenticatedUser.get();

            if (maybeUser.isEmpty()) {
                return "USER_NOT_FOUND";
            }

            User user = maybeUser.get();
            
            Optional<UserWebAuthn> existing = userWebAuthnRepository.findByCredentialId(verifiedCredentialId);

            if (existing.isPresent()) {
                return "ALREADY_REGISTERED";
            }

        
            
            UserWebAuthn credential =  new UserWebAuthn();

            //credential.setDeviceName(registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData().getAaguid().toString());
            credential.setDeviceName(deviceName);
            credential.setUser(user);
            credential.setCredentialId(verifiedCredentialId);
            credential.setRawId(rawId);
            credential.setClientDataJson(clientDataJSON);
            credential.setAttestationObject(attestationObject);
            
            credential.setPublicKey(publicKey);
            credential.setCredentialPublicKey(publicKey);
            
            //credential.setUserHandle(userHandle);
            credential.setUserHandle( Base64.getEncoder().encodeToString(String.valueOf(user.getId()).getBytes()));
            credential.setUserAgent(userAgent);
            credential.setCredentialType(credentialType);
            
            
            credential.setSignCount(signCount);
            credential.setRegisteredAt(LocalDateTime.now());
            
            credential.setAaguid(registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData().getAaguid().toString());
            
            
            JsonNode transportsNode = node.get("transports");
            
            credential.setTransports(transportsNode.toString());
            

            userWebAuthnRepository.save(credential);
            
            auditService.log(user,credential,"REGISTER",true,"Passkey registered");

            user.setFingerprintEnabled(true);

            userRepository.save(user);
          
            return "SUCCESS";

        } catch (Exception ex) {
            log.error("Save credential failed", ex);
            auditService.log(null,null,"REGISTER_FAILED",false,ExceptionUtils.getStackTrace(ex));
            
            Throwable root = ex;
            while (root.getCause() != null) {
                root = root.getCause();
            }

            return root.getClass().getSimpleName() + ": " + root.getMessage();
            //return ex.getMessage();
        }
    }
    
    @ClientCallable
    public void registrationSuccess() {

        if (registrationDialog != null) {
            registrationDialog.close();
        }

        Notification.show("Fingerprint registered successfully",6000, Notification.Position.MIDDLE ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
    
    
   
    @ClientCallable
    public String verifyLogin(String json) {

        try {
        	HttpServletRequest request = null;

        	if (VaadinServletRequest.getCurrent() != null) {
        	    request =VaadinServletRequest.getCurrent().getHttpServletRequest();
        	}
        	
            ObjectMapper mapper = new ObjectMapper();

            JsonNode node = mapper.readTree(json);

            String credentialId = node.get("credentialId").asText();
            log.info("LOGIN credentialId = {}", credentialId);
            
            String rawId = node.get("rawId").asText();
            
            log.info("LOGIN rawId = {}", rawId);
            
            userWebAuthnRepository.findAll()
            .forEach(c -> {
                log.info("DB credentialId={}", c.getCredentialId());
                log.info("DB rawId={}", c.getRawId());
            });
            
            Optional<UserWebAuthn> credential = userWebAuthnRepository.findByRawIdAndActiveTrue(rawId);
           // Optional<UserWebAuthn> credential =userWebAuthnRepository.findByCredentialIdAndActiveTrue(credentialId);
            if (credential.isEmpty()) {
                return "FINGERPRINT_NOT_FOUND";
            }
            
            String authenticatorData =node.get("authenticatorData").asText();

            String clientDataJSON =node.get("clientDataJSON").asText();

            String signature =node.get("signature").asText();
            String challenge =(String) VaadinSession.getCurrent().getAttribute("webauthn-login-challenge");

            log.info("challenge={}", challenge);
            log.info("rawId={}", credential.get().getRawId());
            log.info("attestation={}", credential.get().getAttestationObject() != null);
            log.info("clientData={}", credential.get().getClientDataJson() != null);
            	
            if (challenge == null) {
            	throw new RuntimeException("Login challenge missing");
            }
            
            
            if (credential.get().getAttestationObject() == null) {
                throw new RuntimeException("AttestationObject missing");
            }
            
            if (credential.get().getClientDataJson() == null) {
                throw new RuntimeException("ClientDataJson missing");
            }
            
            AuthenticationData authData =authenticationVerificationService.verifyAuthentication(
            	        credential.get(),
            	        authenticatorData,
            	        clientDataJSON,
            	        signature,
            	        challenge
            	    );
            
            VaadinSession.getCurrent().setAttribute("webauthn-login-challenge", null);
            
            long newCounter =authData.getAuthenticatorData().getSignCount();

            long storedCounter =credential.get().getSignCount();
            
            log.info( "Stored Counter={}, New Counter={}", storedCounter,newCounter);

            if (newCounter != 0 && storedCounter != 0) {

                if (newCounter <= storedCounter) {

                    UserWebAuthn webAuthn = credential.get();

                    webAuthn.setActive(false);

                    auditService.log(null,webAuthn,"CLONED_DEVICE",false,"Sign counter validation failed");
                    
                    userWebAuthnRepository.save(webAuthn);

                    log.error(
                        "AUTHENTICATOR_DISABLED: credentialId={}, storedCounter={}, newCounter={}",
                        webAuthn.getCredentialId(),
                        storedCounter,
                        newCounter
                    );


                    return "CLONED_DEVICE";
                }
            }


            User user =credential.get().getUser();

            CustomUserPrincipal principal =(CustomUserPrincipal) userDetailsService.loadUserByUsername(user.getUsername());

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

 
            SecurityContext context =SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            
            HttpServletRequest httpRequest =VaadinServletRequest.getCurrent().getHttpServletRequest();

            HttpSession session =httpRequest.getSession(true);

            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,context);
            

            user.setLastFingerprintLogin(LocalDateTime.now());

            userRepository.save(user);
            
            UserWebAuthn webAuthn = credential.get();
            webAuthn.setLastUsedAt(LocalDateTime.now());
            webAuthn.setSignCount(newCounter);
            webAuthn.setLastIpAddress(request != null ? request.getRemoteAddr(): null);
            
            String loginUserAgent =request.getHeader("User-Agent");

            if (loginUserAgent != null) {
                webAuthn.setUserAgent(loginUserAgent);
            }
            
            userWebAuthnRepository.save(webAuthn);
            
            auditService.log(user,webAuthn,"LOGIN_SUCCESS",true,"WebAuthn login successful");
            
            
            Authentication current = SecurityContextHolder.getContext().getAuthentication();

            log.info("AUTH = {}", current);
            log.info("SESSION ID = {}", session.getId());
            log.info("Returning SUCCESS");
            return "SUCCESS";

        } catch (Exception ex) {
            log.error("Fingerprint login failed",ex);
            auditService.log(null,null,"LOGIN_FAILED",false,ExceptionUtils.getStackTrace(ex));
            
            return ex.getMessage();
        }
        
    }
    
    @ClientCallable
    public void loginSuccess() {
        UI.getCurrent().navigate("");
    }
}