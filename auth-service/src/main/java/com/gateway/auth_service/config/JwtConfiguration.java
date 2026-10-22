package com.gateway.auth_service.config;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration(proxyBeanMethods = false)
public class JwtConfiguration {

    @Bean
    public RSAKey signingKey(
            @Value("${auth.jwt.key-store}") Resource keyStoreResource,
            @Value("${auth.jwt.key-store-password}") String password,
            @Value("${auth.jwt.key-alias}") String alias) throws Exception {

        char[] passwordChars = password.toCharArray();

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");

            try (InputStream input = keyStoreResource.getInputStream()) {
                keyStore.load(input, passwordChars);
            }

            RSAPrivateKey privateKey =
                    (RSAPrivateKey) keyStore.getKey(alias, passwordChars);

            RSAPublicKey publicKey =
                    (RSAPublicKey) keyStore.getCertificate(alias)
                            .getPublicKey();

            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(alias)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .build();

        } finally {
            Arrays.fill(passwordChars, '\0');
        }
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAKey signingKey) {
        ImmutableJWKSet<SecurityContext> keySource =
                new ImmutableJWKSet<>(new JWKSet(signingKey));

        return new NimbusJwtEncoder(keySource);
    }
}