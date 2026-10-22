package com.gateway.auth_service.auth.controller;


import com.gateway.auth_service.auth.dto.AccountResponse;
import com.gateway.auth_service.auth.dto.RegisterRequest;
import com.gateway.auth_service.auth.service.LoginService;
import com.gateway.auth_service.auth.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gateway.auth_service.auth.dto.LoginRequest;
import com.gateway.auth_service.auth.dto.TokenResponse;
import org.springframework.http.CacheControl;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final LoginService loginService;

    public AuthController(
            RegistrationService registrationService,
            LoginService loginService) {

        this.registrationService = registrationService;
        this.loginService = loginService;
    }
    @PostMapping("/register")
    public ResponseEntity<AccountResponse> register(
            @Valid @RequestBody RegisterRequest request
    ){
        AccountResponse created = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse>  login(
            @Valid @RequestBody LoginRequest request
    ){
        TokenResponse token = loginService.login(request);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(token);
    }

}
