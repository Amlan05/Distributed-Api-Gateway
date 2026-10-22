package com.gateway.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class RateLimitHeadersFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        exchange.getResponse().beforeCommit(() ->
                Mono.fromRunnable(() -> {
                    HttpHeaders headers =
                            exchange.getResponse().getHeaders();

                    if (HttpStatus.TOO_MANY_REQUESTS.equals(
                            exchange.getResponse().getStatusCode())
                            && headers.containsHeader("X-RateLimit-Remaining")
                            && !headers.containsHeader(HttpHeaders.RETRY_AFTER)) {

                        headers.set(
                                HttpHeaders.RETRY_AFTER,
                                retryAfterSeconds(headers));
                    }
                }));

        return chain.filter(exchange);
    }

    private String retryAfterSeconds(HttpHeaders headers) {
        try {
            long remaining = Long.parseLong(
                    headers.getFirst("X-RateLimit-Remaining"));
            long requested = Long.parseLong(
                    headers.getFirst("X-RateLimit-Requested-Tokens"));
            long replenishRate = Long.parseLong(
                    headers.getFirst("X-RateLimit-Replenish-Rate"));

            if (remaining < 0 || requested <= 0 || replenishRate <= 0) {
                return "1";
            }

            long missingTokens = Math.max(0, requested - remaining);
            long seconds =
                    (missingTokens + replenishRate - 1) / replenishRate;

            return Long.toString(Math.max(1, seconds));
        } catch (NumberFormatException ex) {
            return "1";
        }
    }
}