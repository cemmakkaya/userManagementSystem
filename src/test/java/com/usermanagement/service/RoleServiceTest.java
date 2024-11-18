package com.usermanagement.service;

import com.usermanagement.model.Role;
import com.usermanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.List;

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

    // Name Check Mutation Test
    @ParameterizedTest(name = "Mutation: Ungültiger Name - {0}")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  \t  "})
    void CheckInvalidRoleName(String invalidName) {
        assertThrows(IllegalArgumentException.class,
                () -> roleService.addRole(invalidName),
                "Sollte ungültige Rollennamen ablehnen");

        verify(repository, never()).saveRole(any());
    }

    // Create Role Check Mutation Test
    @Test
    @DisplayName("Mutation: Rolle wird korrekt erstellt und gespeichert")
    void CreateAndSaveRole() {
        String roleName = "ADMIN";
        Role expectedRole = new Role(1L, roleName);
        when(repository.saveRole(any())).thenReturn(expectedRole);

        Role result = roleService.addRole(roleName);

        assertNotNull(result, "Rolle sollte nicht null sein");
        assertEquals(roleName, result.getName(), "Rollenname sollte korrekt gesetzt sein");
        assertEquals(1L, result.getId(), "ID sollte korrekt gesetzt sein");
        verify(repository, times(1)).saveRole(argThat(role ->
                role.getName().equals(roleName) && role.getId() == null
        ));
    }

    // Check ID on Role Mutation Test
    @Test
    @DisplayName("Mutation: Rolle mit Berechtigungen wird korrekt geladen")
    void getRoleWithPermissions_withValidId_shouldSucceed() {
        Long roleId = 1L;
        Role expectedRole = new Role(roleId, "ADMIN");
        when(repository.getRoleWithPermissions(roleId)).thenReturn(expectedRole);

        Role result = roleService.getRoleWithPermissions(roleId);

        assertNotNull(result, "Rolle sollte nicht null sein");
        assertEquals("ADMIN", result.getName(), "Rollenname sollte korrekt sein");
        assertEquals(roleId, result.getId(), "ID sollte korrekt sein");
        verify(repository).getRoleWithPermissions(roleId);
    }

    // Check Null ID Mutation Test
    @Test
    @DisplayName("Mutation: Null-ID wird korrekt behandelt")
    void getRoleWithPermissions_withNullId_shouldThrowException() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> roleService.getRoleWithPermissions(null));

        assertEquals("Role ID darf nicht null sein", exception.getMessage());
        verify(repository, never()).getRoleWithPermissions(any());
    }

    // Check all Roles Mutation Test
    @Test
    @DisplayName("Mutation: Rollenliste wird korrekt zurückgegeben")
    void getAllRoles_shouldReturnCorrectList() {
        List<Role> expectedRoles = Arrays.asList(
                new Role(1L, "ADMIN"),
                new Role(2L, "USER")
        );
        when(repository.getAllRoles()).thenReturn(expectedRoles);

        List<Role> result = roleService.getAllRoles();

        assertNotNull(result, "Liste darf nicht null sein");
        assertEquals(2, result.size(), "Liste muss korrekte Größe haben");
        assertEquals(expectedRoles, result, "Listen muss identisch sein");
        verify(repository, times(1)).getAllRoles();
    }
}