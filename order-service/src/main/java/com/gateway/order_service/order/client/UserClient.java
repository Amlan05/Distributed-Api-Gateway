package com.gateway.order_service.order.client;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;

import com.gateway.order_service.order.exception.DownStreamServiceException;
import com.gateway.order_service.order.exception.OrderReferenceNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient(
            @Qualifier("userRestClient") RestClient restClient){
                this.restClient = restClient;
    }

    public void requireUser(Long userId){
        try{
            restClient.get()
                    .uri("/api/users/{id}", userId)
                    .retrieve()
                    .toBodilessEntity();
        }catch(RestClientResponseException ex){
            if (ex.getStatusCode().value() == 401) {
                throw new BadCredentialsException("Downstream authentication failed");
            }
            if (ex.getStatusCode().value() == 403) {
                throw new AccessDeniedException("Access denied");
            }
            if(ex.getStatusCode().value() == 404){
                throw new OrderReferenceNotFoundException("User", userId);
            }

            throw new DownStreamServiceException("User", ex);
        }catch(RestClientException ex){
            throw new DownStreamServiceException("User", ex);
        }
    }
}
