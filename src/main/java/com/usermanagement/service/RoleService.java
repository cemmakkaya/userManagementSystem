package com.usermanagement.service;

import com.usermanagement.model.Role;
import com.usermanagement.repository.UserRepository;
import java.util.List;

public class RoleService {
    private final UserRepository repository;

    public RoleService(UserRepository repository) {
        this.repository = repository;
    }

    public Role addRole(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Rollenname darf nicht leer sein");
        }

        Role role = new Role(null, name);
        return repository.saveRole(role);
    }

    public Role getRoleWithPermissions(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID darf nicht null sein");
        }

        Role role = repository.getRoleWithPermissions(roleId);
        if (role == null) {
            throw new IllegalArgumentException("Rolle nicht gefunden");
        }
        return role;
    }

    public List<Role> getAllRoles() {
        return repository.getAllRoles();
    }
}
