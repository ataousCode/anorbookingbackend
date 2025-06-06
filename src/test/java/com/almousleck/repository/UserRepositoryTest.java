package com.almousleck.repository;

import com.almousleck.model.Role;
import com.almousleck.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        // Create and persist a role
        userRole = Role.builder()
                .name(Role.RoleName.ROLE_USER)
                .build();
        entityManager.persistAndFlush(userRole);

        // Create test user
        testUser = User.builder()
                .name("John Doe")
                .username("johndoe")
                .email("john@example.com")
                .password("password123")
                .phoneNumber("+1234567890")
                .enabled(true)
                .roles(Set.of(userRole))
                .build();
    }

    @Test
    void whenFindByUsername_thenReturnUser() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Optional<User> found = userRepository.findByUsername("johndoe");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Doe");
        assertThat(found.get().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void whenFindByEmail_thenReturnUser() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Optional<User> found = userRepository.findByEmail("john@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("johndoe");
    }

    @Test
    void whenFindByUsernameOrEmail_withUsername_thenReturnUser() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Optional<User> found = userRepository.findByUsernameOrEmail("johndoe", "wrong@email.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("johndoe");
    }

    @Test
    void whenFindByUsernameOrEmail_withEmail_thenReturnUser() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Optional<User> found = userRepository.findByUsernameOrEmail("wrongusername", "john@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void whenExistsByUsername_thenReturnTrue() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        boolean exists = userRepository.existsByUsername("johndoe");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void whenExistsByEmail_thenReturnTrue() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        boolean exists = userRepository.existsByEmail("john@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void whenUserNotExists_thenReturnFalse() {
        // When
        boolean existsByUsername = userRepository.existsByUsername("nonexistent");
        boolean existsByEmail = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(existsByUsername).isFalse();
        assertThat(existsByEmail).isFalse();
    }
}