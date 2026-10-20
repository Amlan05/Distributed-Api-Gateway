package com.gateway.order_service.order.exception;

public class DownStreamServiceException extends RuntimeException {
    public DownStreamServiceException(String service, Throwable cause) {
        super(service + " service could not completed the request", cause);
    }
}
