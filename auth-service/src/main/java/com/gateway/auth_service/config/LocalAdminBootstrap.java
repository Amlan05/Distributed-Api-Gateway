package com.gateway.auth_service.config;

import java.nio.charset.StandardCharsets;

import com.gateway.auth_service.auth.model.AccountEntity;
import com.gateway.auth_service.auth.model.Role;
import com.gateway.auth_service.auth.repository.AccountJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Explicit, local-only provisioning. Public registration always creates USER accounts. */
@Configuration(proxyBeanMethods = false)
@Profile("local")
@ConditionalOnProperty(name = "auth.bootstrap.admin.enabled", havingValue = "true")
public class LocalAdminBootstrap {
    @Bean
    ApplicationRunner provisionLocalAdministrator(
            AccountJpaRepository repository,
            PasswordEncoder encoder,
            @Value("${auth.bootstrap.admin.username}") String username,
            @Value("${auth.bootstrap.admin.password-file}") Resource passwordFile) {
        return arguments -> {
            if (!username.matches("[a-z][a-z0-9._-]{2,49}")) {
                throw new IllegalStateException("Invalid bootstrap administrator username");
            }
            var existing = repository.findByUsername(username);
            if (existing.isPresent()) {
                if (existing.get().getRole() != Role.ADMIN || !existing.get().isEnabled()) {
                    throw new IllegalStateException("Bootstrap account must already be an enabled administrator");
                }
                return;
            }
            String password = passwordFile.getContentAsString(StandardCharsets.UTF_8)
                    .replaceFirst("[\\r\\n]+$", "");
            if (password.isBlank() || password.length() < 12 || password.length() > 72
                    || password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException("Bootstrap password must be 12 to 72 characters and at most 72 UTF-8 bytes");
            }
            repository.save(AccountEntity.administrator(username, encoder.encode(password)));
        };
    }
}
