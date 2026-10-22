package com.gateway.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;

@Configuration(proxyBeanMethods = false)
public class GatewaySecurityConfiguration {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http) {

        return http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .securityContextRepository(
                        NoOpServerSecurityContextRepository.getInstance())
                .requestCache(cache -> cache.requestCache(
                        NoOpServerRequestCache.getInstance()))
                .authorizeExchange(authorize -> authorize
                        .pathMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/users", "/api/users/").hasRole("ADMIN")
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/products",
                                "/api/products/**"
                        ).hasAnyRole("USER", "ADMIN")
                        .pathMatchers(
                                "/api/products",
                                "/api/products/**"
                        ).hasRole("ADMIN")
                        .pathMatchers(
                                "/api/users",
                                "/api/users/**"
                        ).hasAnyRole("USER", "ADMIN")
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/orders",
                                "/api/orders/**"
                        ).hasAnyRole("USER", "ADMIN")
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/orders"
                        ).hasAnyRole("USER", "ADMIN")
                        .anyExchange().denyAll())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                jwtAuthenticationConverter())))
                .build();
    }

    private ReactiveJwtAuthenticationConverterAdapter
    jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter roles =
                new JwtGrantedAuthoritiesConverter();

        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(roles);

        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}