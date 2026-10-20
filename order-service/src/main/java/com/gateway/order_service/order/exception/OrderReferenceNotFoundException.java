package com.gateway.order_service.order.exception;

public class OrderReferenceNotFoundException extends RuntimeException{

    public OrderReferenceNotFoundException(String resource, Long id){
        super(resource + "with id" + id + "was not found");
    }
}
