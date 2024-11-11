package com.usermanagement.service;

import com.usermanagement.model.Role;
import com.usermanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {
    @Mock
    private UserRepository repository;
    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleService(repository);
    }

    @Test
    void addRole_withValidName_shouldSucceed() {
        Role expectedRole = new Role(1L, "MANAGER");
        when(repository.saveRole(any(Role.class))).thenReturn(expectedRole);

        Role result = roleService.addRole("MANAGER");

        assertNotNull(result);
        assertEquals("MANAGER", result.getName());
        verify(repository).saveRole(any(Role.class));
    }

    @Test
    void addRole_withEmptyName_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> roleService.addRole(""));
        verify(repository, never()).saveRole(any());
    }

    @Test
    void getRoleWithPermissions_withValidId_shouldSucceed() {
        Role expectedRole = new Role(1L, "ADMIN");
        when(repository.getRoleWithPermissions(1L)).thenReturn(expectedRole);

        Role result = roleService.getRoleWithPermissions(1L);

        assertEquals("ADMIN", result.getName());
        verify(repository).getRoleWithPermissions(1L);
    }

    @Test
    void getRoleWithPermissions_withNullId_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> roleService.getRoleWithPermissions(null));
        verify(repository, never()).getRoleWithPermissions(any());
    }

    @Test
    void getAllRoles_shouldReturnList() {
        when(repository.getAllRoles()).thenReturn(Arrays.asList(
                new Role(1L, "ADMIN"),
                new Role(2L, "USER")
        ));

        assertEquals(2, roleService.getAllRoles().size());
        verify(repository).getAllRoles();
    }
}