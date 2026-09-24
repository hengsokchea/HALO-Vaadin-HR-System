package org.halocambodia.security;

import java.util.Base64;

import org.springframework.stereotype.Service;

import com.vaadin.flow.server.VaadinServletRequest;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.RegistrationRequest;

import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.verifier.exception.VerificationException;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class WebAuthnRegistrationVerificationService {

    public RegistrationData verifyRegistration(String clientDataJsonBase64,String attestationObjectBase64,String challenge) {

        byte[] clientDataJSON = Base64.getDecoder().decode(clientDataJsonBase64);
        byte[] attestationObject =Base64.getDecoder().decode(attestationObjectBase64);

        RegistrationRequest request =new RegistrationRequest(attestationObject,clientDataJSON);
        Challenge challengeObj =new DefaultChallenge(Base64.getDecoder().decode(challenge));
        
       // Origin origin =new Origin("http://localhost:8081");

        
        HttpServletRequest httpRequest =VaadinServletRequest.getCurrent().getHttpServletRequest();
        String origin =httpRequest.getScheme() + "://" + httpRequest.getServerName() + ( httpRequest.getServerPort() == 81 || httpRequest.getServerPort() == 443 ? ""  : ":" + httpRequest.getServerPort());

        ServerProperty serverProperty =new ServerProperty(new Origin(origin),httpRequest.getServerName(),challengeObj); 
        RegistrationParameters params =new RegistrationParameters(serverProperty,true,true);
        
        WebAuthnManager manager =WebAuthnManager.createNonStrictWebAuthnManager();
        RegistrationData registrationData =manager.parse(request);

        manager.verify(registrationData,params);


        return registrationData;
    }
}