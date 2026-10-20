package com.gateway.product_service.product.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.gateway.product_service.product.model.Product;
import com.gateway.product_service.product.model.ProductEntity;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class ProductRepository {

    private final ProductJpaRepository jpaRepository;

    public ProductRepository(ProductJpaRepository jpaRepository){
        this.jpaRepository = jpaRepository;
    }

    public List<Product> findAll(){
        return jpaRepository.findAll(Sort.by("id"))
                .stream()
                .map(this::toProduct)
                .toList();
    }

    public Optional<Product> findById(Long id){
        return jpaRepository.findById(id)
                .map(this::toProduct);
    }

    @Transactional
    public Product create(String name, BigDecimal price){
        ProductEntity entity = new ProductEntity(name, price);
        ProductEntity saved = jpaRepository.save(entity);
        return toProduct(saved);
    }

    @Transactional
    public Optional<Product> update(Long id, String name, BigDecimal price){
        return jpaRepository.findByIdForUpdate(id)
                .map(entity -> {
                    entity.updateDetails(name, price);
                    return toProduct(jpaRepository.save(entity));
                });
    }

    @Transactional
    public boolean deleteById(Long id){
        Optional<ProductEntity> existing = jpaRepository.findByIdForUpdate(id);

        if(existing.isEmpty()){
            return false;
        }

        jpaRepository.delete(existing.get());
        return true;
    }

    private Product toProduct(ProductEntity entity){
        return new Product(
                entity.getId(),
                entity.getName(),
                entity.getPrice()
        );
    }
}
