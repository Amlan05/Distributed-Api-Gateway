package com.gateway.product_service.product.service;
import org.springframework.security.access.prepost.PreAuthorize;
import com.gateway.product_service.product.dto.ProductRequest;
import com.gateway.product_service.product.dto.ProductResponse;
import com.gateway.product_service.product.exception.ProductNotFoundException;
import com.gateway.product_service.product.model.Product;
import com.gateway.product_service.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository){
        this.repository = repository;
    }

    public List<ProductResponse> findAll(){
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse findById(Long id){
        Product product = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return toResponse(product);

    }

    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse create(ProductRequest request){
        Product product = repository.create(
                request.name(),
                request.price()
        );

        return toResponse(product);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse update(Long id, ProductRequest request){
        Product product = repository.update(
                id,
                request.name(),
                request.price()
        ).orElseThrow(() -> new ProductNotFoundException(id));

        return toResponse(product);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteById(Long id){
        if(!repository.deleteById(id)){
            throw new ProductNotFoundException(id);
        }
    }

    private ProductResponse toResponse(Product product){
        return new ProductResponse(
                product.id(),
                product.name(),
                product.price()
        );
    }
}
