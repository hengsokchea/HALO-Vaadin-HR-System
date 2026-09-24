package org.halocambodia.security;

import java.util.Base64;
import java.util.List;

import org.halocambodia.data.UserWebAuthn;
import org.springframework.stereotype.Service;

import com.vaadin.flow.server.VaadinServletRequest;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.verifier.exception.VerificationException;

import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WebAuthnAuthenticationVerificationService {

    public AuthenticationData verifyAuthentication( UserWebAuthn credential,String authenticatorDataBase64, String clientDataJSONBase64, String signatureBase64, String challenge ) {
    	try {
            WebAuthnManager manager =WebAuthnManager.createNonStrictWebAuthnManager();
            RegistrationRequest registrationRequest =new RegistrationRequest(Base64.getDecoder().decode(credential.getAttestationObject()), Base64.getDecoder().decode(credential.getClientDataJson()));
            RegistrationData registrationData = manager.parse(registrationRequest);
            Authenticator authenticator =new AuthenticatorImpl( registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData(), registrationData.getAttestationObject().getAttestationStatement(),credential.getSignCount());

            AuthenticationRequest authenticationRequest =new AuthenticationRequest(
                            Base64.getDecoder().decode(credential.getRawId()),Base64.getDecoder().decode(authenticatorDataBase64), Base64.getDecoder().decode(clientDataJSONBase64),
                            Base64.getDecoder().decode(signatureBase64));

            AuthenticationData authenticationData =manager.parse(authenticationRequest);

            Challenge challengeObj =new DefaultChallenge(Base64.getDecoder().decode(challenge));

           /* ServerProperty serverProperty =
                    new ServerProperty(
                            new Origin("http://localhost:8081"),
                            "localhost",
                            challengeObj
                    );
                    */
            
            HttpServletRequest request =VaadinServletRequest.getCurrent().getHttpServletRequest();
            String origin =request.getScheme() + "://" + request.getServerName() + ( request.getServerPort() == 81 || request.getServerPort() == 443 ? ""  : ":" + request.getServerPort());

            ServerProperty serverProperty =new ServerProperty(new Origin(origin),request.getServerName(),challengeObj); 
            AuthenticationParameters params =new AuthenticationParameters(serverProperty,authenticator,true,true);


            manager.verify(authenticationData,params);


            return authenticationData;

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getClass().getName()+ " : " + ex.getMessage(),ex);
        }
    }
    
    private List<String> getTransports(UserWebAuthn credential) {

        try {

            if (credential.getTransports() == null
                    || credential.getTransports().isBlank()) {
                return List.of();
            }

            ObjectMapper mapper = new ObjectMapper();

            return mapper.readValue(credential.getTransports(),new TypeReference<List<String>>() { });

        } catch (Exception ex) {

            return List.of();
        }
    }
}