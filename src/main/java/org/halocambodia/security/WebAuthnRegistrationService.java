package org.halocambodia.security;

import java.util.Base64;

import org.springframework.stereotype.Service;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.RegistrationRequest;

@Service
public class WebAuthnRegistrationService {

    public RegistrationData parse(String clientDataJsonBase64,String attestationObjectBase64) {

        byte[] clientDataJSON =Base64.getDecoder().decode(clientDataJsonBase64);

        byte[] attestationObject =Base64.getDecoder().decode(attestationObjectBase64);

        RegistrationRequest request = new RegistrationRequest(attestationObject,clientDataJSON);

        WebAuthnManager manager = WebAuthnManager.createNonStrictWebAuthnManager();

        return manager.parse(request);
    }
}