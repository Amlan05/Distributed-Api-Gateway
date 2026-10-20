package com.gateway.user.service;

import java.util.List;
import java.util.Optional;

import com.gateway.user.dto.UserRequest;
import com.gateway.user.dto.UserResponse;
import com.gateway.user.exception.UserNotFoundException;
import com.gateway.user.model.User;
import com.gateway.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {
    @Mock
    private UserRepository repository;
    private UserService service;
    private final User user = new User(1L, "Asha", "asha@example.com");
    private final UserRequest request = new UserRequest("Asha", "asha@example.com");

    @BeforeEach
    void setUp() {
        service = new UserService(repository);
    }

    @Test
    void listsUsersAsResponseDtos() {
        when(repository.findAll()).thenReturn(List.of(user));
        assertThat(service.findAll()).containsExactly(UserResponse.from(user));
    }

    @Test
    void listsNoUsersAsAnEmptyList() {
        when(repository.findAll()).thenReturn(List.of());
        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void findsExistingUser() {
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        assertThat(service.findById(1L)).isEqualTo(UserResponse.from(user));
    }

    @Test
    void rejectsMissingUserOnRead() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(1L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with id 1 was not found");
    }

    @Test
    void createsUserWithRepositoryAssignedId() {
        when(repository.create(request.name(), request.email())).thenReturn(user);
        assertThat(service.create(request)).isEqualTo(UserResponse.from(user));
    }

    @Test
    void updatesExistingUser() {
        when(repository.update(1L, request.name(), request.email())).thenReturn(Optional.of(user));
        assertThat(service.update(1L, request)).isEqualTo(UserResponse.from(user));
    }

    @Test
    void rejectsMissingUserOnUpdate() {
        when(repository.update(1L, request.name(), request.email())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(1L, request)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void deletesExistingUser() {
        when(repository.deleteById(1L)).thenReturn(true);
        service.delete(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void rejectsMissingUserOnDelete() {
        when(repository.deleteById(1L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(UserNotFoundException.class);
    }
}
