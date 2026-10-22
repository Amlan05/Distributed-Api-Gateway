package com.gateway.auth_service.auth.repository;

import com.gateway.auth_service.auth.model.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, Long> {

    Optional<AccountEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}
