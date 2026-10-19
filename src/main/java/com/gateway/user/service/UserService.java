package com.gateway.user.service;

import java.util.List;

import com.gateway.user.dto.UserRequest;
import com.gateway.user.dto.UserResponse;
import com.gateway.user.exception.UserNotFoundException;
import com.gateway.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public List<UserResponse> findAll() {
        return repository.findAll().stream().map(UserResponse::from).toList();
    }

    public UserResponse findById(Long id) {
        return UserResponse.from(repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id)));
    }

    public UserResponse create(UserRequest request) {
        return UserResponse.from(repository.create(request.name(), request.email()));
    }

    public UserResponse update(Long id, UserRequest request) {
        return UserResponse.from(repository.update(id, request.name(), request.email())
                .orElseThrow(() -> new UserNotFoundException(id)));
    }

    public void delete(Long id) {
        if (!repository.deleteById(id)) {
            throw new UserNotFoundException(id);
        }
    }
}
