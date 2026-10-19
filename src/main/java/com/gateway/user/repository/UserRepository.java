package com.gateway.user.repository;

import java.util.List;
import java.util.Optional;

import com.gateway.user.model.User;
import com.gateway.user.model.UserEntity;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserRepository(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public List<User> findAll() {
        return jpaRepository.findAll(Sort.by("id"))
                .stream()
                .map(this::toUser)
                .toList();
    }

    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id)
                .map(this::toUser);
    }

    @Transactional
    public User create(String name, String email) {
        UserEntity entity = new UserEntity(name, email);
        UserEntity saved = jpaRepository.save(entity);
        return toUser(saved);
    }

    @Transactional
    public Optional<User> update(Long id, String name, String email) {
        return jpaRepository.findByIdForUpdate(id)
                .map(entity -> {
                    entity.updateDetails(name, email);
                    return toUser(jpaRepository.save(entity));
                });
    }

    @Transactional
    public boolean deleteById(Long id) {
        Optional<UserEntity> existing =
                jpaRepository.findByIdForUpdate(id);

        if (existing.isEmpty()) {
            return false;
        }

        jpaRepository.delete(existing.get());
        return true;
    }

    private User toUser(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getName(),
                entity.getEmail()
        );
    }
}