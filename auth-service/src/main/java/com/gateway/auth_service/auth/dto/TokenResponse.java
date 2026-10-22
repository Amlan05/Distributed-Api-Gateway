package com.gateway.auth_service.auth.dto;

public record TokenResponse (
        String accessToken,
        String tokenType,
        long expiresIn
){

}
