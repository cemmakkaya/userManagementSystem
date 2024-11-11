package com.usermanagement.model;

import java.util.ArrayList;
import java.util.List;

public class Role {
    private Long id;
    private String name;  // USER, ADMIN
    private List<Permission> permissions = new ArrayList<>();

    public Role(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Permission> getPermissions() { return permissions; }
    public void setPermissions(List<Permission> permissions) { this.permissions = permissions; }
}
