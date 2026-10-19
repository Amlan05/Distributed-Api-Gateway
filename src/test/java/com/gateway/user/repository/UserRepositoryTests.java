package com.gateway.user.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.gateway.user.model.User;
import com.gateway.support.PostgresTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class UserRepositoryTests {
    @Autowired
    private UserRepository repository;

    @Autowired
    private UserJpaRepository jpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void clearDatabase() {
        jpaRepository.deleteAllInBatch();
    }

    @Test
    void storesUpdatesAndDeletesImmutableUsers() {
        User original = repository.create("Asha", "asha@example.com");
        User updated = repository.update(original.id(), "Asha Rao", "rao@example.com").orElseThrow();

        assertThat(original.name()).isEqualTo("Asha");
        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(repository.findById(original.id())).contains(updated);
        assertThat(repository.deleteById(original.id())).isTrue();
        assertThat(repository.findById(original.id())).isEmpty();
        assertThat(repository.update(original.id(), "Again", "again@example.com")).isEmpty();
        assertThat(repository.deleteById(original.id())).isFalse();
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void concurrentCreatesProduceUniqueIdsWithoutLosingUsers() throws Exception {
        var executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<User>> tasks = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                tasks.add(() -> repository.create("Concurrent", "concurrent@example.com"));
            }
            List<Future<User>> results = executor.invokeAll(tasks, 10, TimeUnit.SECONDS);
            List<Long> ids = new ArrayList<>();
            for (Future<User> result : results) {
                ids.add(result.get().id());
            }
            assertThat(ids).hasSize(200).doesNotHaveDuplicates();
            assertThat(repository.findAll()).hasSize(200);
            assertThat(repository.findAll().stream().map(User::id).toList()).isSorted();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void racingUpdateAndDeleteNeverResurrectAUser() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 100; i++) {
                User user = repository.create("Original", "original@example.com");
                var start = new java.util.concurrent.CountDownLatch(1);
                Future<?> update = executor.submit(() -> {
                    await(start);
                    repository.update(user.id(), "Updated", "updated@example.com");
                });
                Future<?> delete = executor.submit(() -> {
                    await(start);
                    repository.deleteById(user.id());
                });
                start.countDown();
                update.get(5, TimeUnit.SECONDS);
                delete.get(5, TimeUnit.SECONDS);
                assertThat(repository.findById(user.id())).isEmpty();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void failedTransactionRollsBackCreatedUser() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            repository.create("Rollback", "rollback@example.com");
            throw new IllegalStateException("Intentional transaction failure");
        })).isInstanceOf(IllegalStateException.class)
                .hasMessage("Intentional transaction failure");

        assertThat(repository.findAll()).isEmpty();
    }

    private static void await(java.util.concurrent.CountDownLatch start) {
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent test did not start in time");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}
