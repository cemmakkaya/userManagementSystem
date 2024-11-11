package com.usermanagement.service;

import com.usermanagement.model.Permission;
import com.usermanagement.repository.UserRepository;
import java.util.List;

public class PermissionService {
    private final UserRepository repository;

    public PermissionService(UserRepository repository) {
        this.repository = repository;
    }

    public Permission createPermission(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Berechtigungsname darf nicht leer sein");
        }

        return repository.createPermission(name, description);
    }

    public void assignPermissionToRole(Long roleId, Long permissionId) {
        if (roleId == null || permissionId == null) {
            throw new IllegalArgumentException("Role ID und Permission ID dürfen nicht null sein");
        }

        repository.addPermissionToRole(roleId, permissionId);
    }

    public List<Permission> getAllPermissions() {
        return repository.getAllPermissions();
    }
}