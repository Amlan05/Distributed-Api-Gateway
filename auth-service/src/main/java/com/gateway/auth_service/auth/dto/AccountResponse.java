package com.gateway.auth_service.auth.dto;

import com.gateway.auth_service.auth.model.Role;

public record AccountResponse(
        Long id,
        String username,
        Role role
) {
}

