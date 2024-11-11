package com.usermanagement.service;

import com.usermanagement.model.User;
import com.usermanagement.model.Role;
import com.usermanagement.repository.UserRepository;

public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    private void validateUserData(String username, String email, Role role) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username darf nicht leer sein");
        }
        if (email == null || !isValidEmail(email)) {
            throw new IllegalArgumentException("Ungültiges Email-Format");
        }
        if (role == null) {
            throw new IllegalArgumentException("Rolle darf nicht null sein");
        }
    }

    public User createUser(String username, String email, Role role) {
        validateUserData(username, email, role);

        if (repository.findByUsername(username) != null) {
            throw new IllegalArgumentException("Username existiert bereits");
        }

        User user = new User(null, username, email, role);
        return repository.saveUser(user);
    }

    public boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

}