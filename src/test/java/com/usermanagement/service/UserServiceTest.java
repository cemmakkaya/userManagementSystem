package com.usermanagement.service;

import com.usermanagement.model.Role;
import com.usermanagement.model.User;
import com.usermanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository repository;
    private UserService userService;
    private Role testRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        userService = new UserService(repository);
        testRole = new Role(1L, "USER");
        testUser = new User(1L, "testUser", "test@test.com", testRole);
    }

    @Test
    void createUser_withValidData_shouldSucceed() {
        when(repository.findByUsername(testUser.getUsername())).thenReturn(null);
        when(repository.saveUser(any(User.class))).thenReturn(testUser);

        User result = userService.createUser(
                testUser.getUsername(),
                testUser.getEmail(),
                testRole
        );

        assertNotNull(result);
        assertEquals(testUser.getUsername(), result.getUsername());
        verify(repository).saveUser(any(User.class));
    }

    @Test
    void createUser_withExistingUsername_shouldThrowException() {
        when(repository.findByUsername(testUser.getUsername())).thenReturn(testUser);

        assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(testUser.getUsername(), testUser.getEmail(), testRole));
        verify(repository, never()).saveUser(any());
    }

    @Test
    void createUser_withInvalidEmail_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(testUser.getUsername(), "invalid-email", testRole));
        verify(repository, never()).saveUser(any());
    }

    @Test
    void createUser_withNullRole_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(testUser.getUsername(), testUser.getEmail(), null));
        verify(repository, never()).saveUser(any());
    }

    @Test
    void createUser_withEmptyUsername_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.createUser("", testUser.getEmail(), testRole));
        verify(repository, never()).saveUser(any());
    }
}