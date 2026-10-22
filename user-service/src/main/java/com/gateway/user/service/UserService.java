package com.gateway.user.service;

import java.util.List;
import com.gateway.shared.security.CallerIdentity;
import com.gateway.user.exception.ProfileAlreadyExistsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;

import com.gateway.user.dto.UserRequest;
import com.gateway.user.dto.UserResponse;
import com.gateway.user.exception.UserNotFoundException;
import com.gateway.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> findAll() {
        return repository.findAll().stream().map(UserResponse::from).toList();
    }

    public UserResponse findById(Long id) {
        return UserResponse.from((CallerIdentity.isAdmin() ? repository.findById(id)
                : repository.findOwnedById(id, CallerIdentity.subject()))
                .orElseThrow(() -> new UserNotFoundException(id)));
    }

    public UserResponse create(UserRequest request) {
        if (CallerIdentity.isAdmin()) {
            return UserResponse.from(repository.create(request.name(), request.email()));
        }
        String subject = CallerIdentity.subject();
        if (repository.hasProfile(subject)) {
            throw new ProfileAlreadyExistsException();
        }
        try {
            return UserResponse.from(repository.createOwned(request.name(), request.email(), subject));
        } catch (DataIntegrityViolationException ex) {
            // The database uniqueness constraint also protects simultaneous requests.
            if (repository.hasProfile(subject)) {
                throw new ProfileAlreadyExistsException();
            }
            throw ex;
        }
    }

    public UserResponse update(Long id, UserRequest request) {
        return UserResponse.from((CallerIdentity.isAdmin() ? repository.update(id, request.name(), request.email())
                : repository.updateOwned(id, request.name(), request.email(), CallerIdentity.subject()))
                .orElseThrow(() -> new UserNotFoundException(id)));
    }

    public void delete(Long id) {
        if (!(CallerIdentity.isAdmin() ? repository.deleteById(id)
                : repository.deleteOwned(id, CallerIdentity.subject()))) {
            throw new UserNotFoundException(id);
        }
    }
}
