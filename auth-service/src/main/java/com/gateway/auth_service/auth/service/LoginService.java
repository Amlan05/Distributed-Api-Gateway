package com.gateway.auth_service.auth.service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.gateway.auth_service.auth.dto.LoginRequest;
import com.gateway.auth_service.auth.dto.TokenResponse;
import com.gateway.auth_service.auth.model.AccountEntity;
import com.gateway.auth_service.auth.repository.AccountJpaRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    private final AccountJpaRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final String dummyPasswordHash;

    public LoginService(
            AccountJpaRepository repository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService) {

        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyPasswordHash =
                passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public TokenResponse login(LoginRequest request) {
        String password = request.password();

        if (password == null
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {

            throw invalidCredentials();
        }

        AccountEntity account = repository
                .findByUsername(request.username())
                .orElse(null);

        String storedHash = account == null
                ? dummyPasswordHash
                : account.getPasswordHash();

        boolean passwordMatches =
                passwordEncoder.matches(password, storedHash);

        if (account == null
                || !passwordMatches
                || !account.isEnabled()) {

            throw invalidCredentials();
        }

        return tokenService.issueFor(account);
    }

    private BadCredentialsException invalidCredentials() {
        return new BadCredentialsException(
                "Invalid username or password"
        );
    }
}