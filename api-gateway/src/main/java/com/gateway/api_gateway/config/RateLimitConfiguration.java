package com.gateway.api_gateway.config;


import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration(proxyBeanMethods = false)
public class RateLimitConfiguration {

    @Bean
    public KeyResolver accountKeyResolver(){
        return exchange -> exchange.getPrincipal()
                .ofType(JwtAuthenticationToken.class)
                .filter(JwtAuthenticationToken::isAuthenticated)
                .map(authentication ->
                        "account:" + authentication.getToken().getSubject());
    }
}
