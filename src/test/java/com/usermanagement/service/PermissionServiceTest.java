package com.usermanagement.service;

import com.usermanagement.model.Permission;
import com.usermanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {
    @Mock
    private UserRepository repository;
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        permissionService = new PermissionService(repository);
    }

    @Test
    void createPermission_withValidData_shouldSucceed() {
        String name = "READ_ACCESS";
        String description = "Can read data";
        Permission expectedPermission = new Permission(1L, name, description);
        when(repository.createPermission(name, description)).thenReturn(expectedPermission);

        Permission result = permissionService.createPermission(name, description);

        assertNotNull(result);
        assertEquals(name, result.getName());
        verify(repository).createPermission(name, description);
    }

    @Test
    void createPermission_withEmptyName_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> permissionService.createPermission("", "description"));
        verify(repository, never()).createPermission(anyString(), anyString());
    }

    @Test
    void assignPermissionToRole_withValidIds_shouldSucceed() {
        permissionService.assignPermissionToRole(1L, 1L);
        verify(repository).addPermissionToRole(1L, 1L);
    }

    @Test
    void assignPermissionToRole_withNullRoleId_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> permissionService.assignPermissionToRole(null, 1L));
        verify(repository, never()).addPermissionToRole(any(), any());
    }

    @Test
    void getAllPermissions_shouldReturnList() {
        when(repository.getAllPermissions()).thenReturn(Arrays.asList(
                new Permission(1L, "READ", "Can read"),
                new Permission(2L, "WRITE", "Can write")
        ));

        assertEquals(2, permissionService.getAllPermissions().size());
        verify(repository).getAllPermissions();
    }
}