package com.gateway.order_service.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class ServiceClientConfiguration {

    @Bean
    public RestClient userRestClient(
            RestClient.Builder builder,
            @Value("${clients.user.base-url}") String baseUrl
    ){
        return createClient(builder, baseUrl);
    }

    @Bean
    public RestClient productRestClient(
            RestClient.Builder builder,
            @Value("${clients.product.base-url}") String baseUrl
    ){
        return createClient(builder, baseUrl);
    }

    private RestClient createClient(
            RestClient.Builder builder,
            String baseUrl
    ){
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        return builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
