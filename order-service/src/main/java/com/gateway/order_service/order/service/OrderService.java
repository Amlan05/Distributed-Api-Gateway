package com.gateway.order_service.order.service;


import com.gateway.order_service.order.client.ProductClient;
import com.gateway.order_service.order.client.ProductDetails;
import com.gateway.order_service.order.client.UserClient;
import com.gateway.order_service.order.dto.OrderRequest;
import com.gateway.order_service.order.dto.OrderResponse;
import com.gateway.order_service.order.exception.OrderNotFoundException;
import com.gateway.order_service.order.model.Order;
import com.gateway.order_service.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository repository;
    private final UserClient userClient;
    private final ProductClient productClient;

    public OrderService(OrderRepository repository, UserClient userClient, ProductClient productClient) {
        this.repository = repository;
        this.userClient = userClient;
        this.productClient = productClient;
    }

    public List<OrderResponse> findAll(){
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public OrderResponse findById(Long id){
        Order order = repository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return toResponse(order);
    }

    public OrderResponse create(OrderRequest request){
        userClient.requireUser(request.userId());

        ProductDetails product = productClient.findById(request.productId());

        Order order = repository.create(
                request.userId(),
                product.id(),
                request.quantity(),
                product.price()
        );

        return toResponse(order);
    }

    private OrderResponse toResponse(Order order){
        return new OrderResponse(
                order.id(),
                order.userId(),
                order.productId(),
                order.quantity(),
                order.unitPrice(),
                order.totalPrice()
        );
    }
}
