package com.gateway.product_service.product.controller;


import com.gateway.product_service.product.dto.ProductRequest;
import com.gateway.product_service.product.dto.ProductResponse;
import com.gateway.product_service.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service){
        this.service = service;
    }

    @GetMapping
    public List<ProductResponse> findAll(){
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable @Positive Long id){
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request){

        ProductResponse created = service.create(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ProductRequest request){

        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(
            @PathVariable @Positive Long id
    ){
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
