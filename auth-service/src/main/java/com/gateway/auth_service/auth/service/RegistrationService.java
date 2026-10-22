package com.gateway.auth_service.auth.service;


import com.gateway.auth_service.auth.dto.AccountResponse;
import com.gateway.auth_service.auth.dto.RegisterRequest;
import com.gateway.auth_service.auth.exception.InvalidPasswordException;
import com.gateway.auth_service.auth.exception.UsernameAlreadyExistsException;
import com.gateway.auth_service.auth.model.AccountEntity;
import com.gateway.auth_service.auth.repository.AccountJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class RegistrationService {

    private final AccountJpaRepository repository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(AccountJpaRepository repository, PasswordEncoder passwordEncoder){
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public AccountResponse register(RegisterRequest request){
        validatePassword(request.password());

        if(repository.existsByUsername(request.username())){
            throw new UsernameAlreadyExistsException();
        }

        String passwordHash = passwordEncoder.encode(request.password());

        AccountEntity account = new AccountEntity(
                request.username(),
                passwordHash
        );

        AccountEntity saved;

        try{
            saved = repository.save(account);
        }catch(DataIntegrityViolationException ex){
            if(repository.existsByUsername(request.username())){
                throw new UsernameAlreadyExistsException();
            }

            throw ex;
        }

        return new AccountResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getRole()
        );
    }

    private void validatePassword(String password){
        if (password == null || password.isBlank() || password.length() < 12 || password.length() > 72
        || password.getBytes(StandardCharsets.UTF_8).length > 72){
            throw new InvalidPasswordException();
        }
    }
}
