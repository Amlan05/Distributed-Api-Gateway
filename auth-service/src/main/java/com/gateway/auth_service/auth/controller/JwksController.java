package com.gateway.auth_service.auth.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JwksController {

    private final RSAKey signingKey;

    public JwksController(RSAKey signingKey) {
        this.signingKey = signingKey;
    }

    @GetMapping(
            value = "/.well-known/jwks.json",
            produces = "application/json"
    )
    public Map<String, Object> publicKeys(){
        RSAKey publicKey = signingKey.toPublicJWK();

        return new JWKSet(publicKey).toJSONObject();
    }
}
