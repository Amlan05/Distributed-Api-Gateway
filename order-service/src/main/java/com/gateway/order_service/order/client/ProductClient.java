package com.gateway.order_service.order.client;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import java.math.BigDecimal;

import com.gateway.order_service.order.exception.DownStreamServiceException;
import com.gateway.order_service.order.exception.OrderReferenceNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ProductClient {

    private static final BigDecimal MAX_PRICE =
            new BigDecimal("9999999999.99");

    private final RestClient restClient;

    public ProductClient(
            @Qualifier("productRestClient") RestClient restClient) {

        this.restClient = restClient;
    }

    public ProductDetails findById(Long productId) {
        try {
            ProductDetails product = restClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .body(ProductDetails.class);

            if (!isValid(product, productId)) {
                throw new DownStreamServiceException(
                        "Product",
                        new IllegalStateException("Invalid product response")
                );
            }

            return product;

        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401) {
                throw new BadCredentialsException("Downstream authentication failed");
            }
            if (ex.getStatusCode().value() == 403) {
                throw new AccessDeniedException("Access denied");
            }
            if (ex.getStatusCode().value() == 404) {
                throw new OrderReferenceNotFoundException(
                        "Product",
                        productId
                );
            }

            throw new DownStreamServiceException("Product", ex);

        } catch (RestClientException ex) {
            throw new DownStreamServiceException("Product", ex);
        }
    }

    private boolean isValid(ProductDetails product, Long requestedId) {
        if (product == null
                || !requestedId.equals(product.id())
                || product.price() == null) {
            return false;
        }

        BigDecimal price = product.price();

        return price.signum() > 0
                && price.compareTo(MAX_PRICE) <= 0
                && price.stripTrailingZeros().scale() <= 2;
    }
}