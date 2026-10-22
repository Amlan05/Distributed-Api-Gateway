package com.gateway.auth_service.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.gateway.auth_service.auth.dto.TokenResponse;
import com.gateway.auth_service.auth.model.AccountEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final String audience;
    private final String keyId;
    private final Duration accessTokenTtl;

    public TokenService(
            JwtEncoder jwtEncoder,
            @Value("${auth.jwt.issuer}") String issuer,
            @Value("${auth.jwt.audience}") String audience,
            @Value("${auth.jwt.key-alias}") String keyId,
            @Value("${auth.jwt.access-token-ttl}") Duration accessTokenTtl) {

        if (accessTokenTtl.getSeconds() <= 0) {
            throw new IllegalArgumentException(
                    "Access token lifetime must be positive"
            );
        }

        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.audience = audience;
        this.keyId = keyId;
        this.accessTokenTtl = accessTokenTtl;
    }

    public TokenResponse issueFor(AccountEntity authenticatedAccount) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(authenticatedAccount.getId().toString())
                .audience(List.of(audience))
                .issuedAt(now)
                .notBefore(now)
                .expiresAt(now.plus(accessTokenTtl))
                .id(UUID.randomUUID().toString())
                .claim(
                        "roles",
                        List.of(authenticatedAccount.getRole().name())
                )
                .build();

        JwsHeader header = JwsHeader
                .with(SignatureAlgorithm.RS256)
                .keyId(keyId)
                .type("JWT")
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        return new TokenResponse(
                token,
                "Bearer",
                accessTokenTtl.getSeconds()
        );
    }
}