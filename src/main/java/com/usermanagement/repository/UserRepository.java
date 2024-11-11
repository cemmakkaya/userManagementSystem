package com.usermanagement.repository;

import com.usermanagement.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private final String dbUrl;

    public UserRepository(String dbUrl) {
        this.dbUrl = dbUrl;
        initializeDatabase();
    }

    private Connection currentConnection;

    public void closeConnections() {
        try {
            if (currentConnection != null && !currentConnection.isClosed()) {
                currentConnection.close();
            }
        } catch (SQLException e) {
            // Ignoriere Fehler beim Schließen
        }
    }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            Statement stmt = conn.createStatement();

            // Lösche existierende Tabellen in der richtigen Reihenfolge
            stmt.execute("DROP TABLE IF EXISTS role_permissions");
            stmt.execute("DROP TABLE IF EXISTS users");
            stmt.execute("DROP TABLE IF EXISTS permissions");
            stmt.execute("DROP TABLE IF EXISTS roles");

            // Erstelle Tabellen neu
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS roles (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS permissions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    description TEXT
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    email TEXT NOT NULL UNIQUE,
                    role_id INTEGER,
                    FOREIGN KEY (role_id) REFERENCES roles(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS role_permissions (
                    role_id INTEGER,
                    permission_id INTEGER,
                    PRIMARY KEY (role_id, permission_id),
                    FOREIGN KEY (role_id) REFERENCES roles(id),
                    FOREIGN KEY (permission_id) REFERENCES permissions(id)
                )
            """);

            // Füge Standard-Rollen hinzu
            stmt.execute("INSERT OR IGNORE INTO roles (name) VALUES ('ADMIN')");
            stmt.execute("INSERT OR IGNORE INTO roles (name) VALUES ('USER')");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
        }
    }

    public User findByUsername(String username) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT u.*, r.name as role_name FROM users u JOIN roles r ON u.role_id = r.id WHERE u.username = ?"
            );
            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Role role = new Role(rs.getLong("role_id"), rs.getString("role_name"));
                return new User(rs.getLong("id"), rs.getString("username"), rs.getString("email"), role);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
    }

    public User findById(Long id) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT u.*, r.name as role_name FROM users u JOIN roles r ON u.role_id = r.id WHERE u.id = ?"
            );
            stmt.setLong(1, id);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Role role = new Role(rs.getLong("role_id"), rs.getString("role_name"));
                return new User(rs.getLong("id"), rs.getString("username"), rs.getString("email"), role);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
    }

    public Role saveRole(Role role) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO roles (name) VALUES (?)"
            );

            stmt.setString(1, role.getName());
            stmt.executeUpdate();

            // ID abfragen
            Statement idStmt = conn.createStatement();
            ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()");

            if (rs.next()) {
                role.setId(rs.getLong(1));
            }
            return role;
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    public User saveUser(User user) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            // Prüfe ob Username bereits existiert
            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT id FROM users WHERE username = ?"
            );
            checkStmt.setString(1, user.getUsername());
            if (checkStmt.executeQuery().next()) {
                throw new IllegalArgumentException("Username existiert bereits");
            }

            // Prüfe ob Email bereits existiert
            checkStmt = conn.prepareStatement(
                    "SELECT id FROM users WHERE email = ?"
            );
            checkStmt.setString(1, user.getEmail());
            if (checkStmt.executeQuery().next()) {
                throw new IllegalArgumentException("Email existiert bereits");
            }

            // Füge den neuen User ein
            PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO users (username, email, role_id) 
                VALUES (?, ?, ?)
            """);

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setLong(3, user.getRole().getId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            // Hole die generierte ID
            try (Statement idStmt = conn.createStatement();
                 ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    user.setId(rs.getLong(1));
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }

            // Hole den kompletten User mit allen Daten
            try (PreparedStatement selectStmt = conn.prepareStatement("""
                SELECT u.*, r.name as role_name 
                FROM users u
                JOIN roles r ON u.role_id = r.id
                WHERE u.id = ?
            """)) {
                selectStmt.setLong(1, user.getId());
                ResultSet rs = selectStmt.executeQuery();

                if (rs.next()) {
                    Role role = new Role(
                            rs.getLong("role_id"),
                            rs.getString("role_name")
                    );

                    return new User(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            role
                    );
                }
            }

            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    public void deleteUser(Long userId) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            // Prüfen ob es der letzte Admin ist
            PreparedStatement checkStmt = conn.prepareStatement("""
                SELECT COUNT(*) as admin_count 
                FROM users u 
                JOIN roles r ON u.role_id = r.id 
                WHERE r.name = 'ADMIN'
            """);

            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                int adminCount = rs.getInt("admin_count");

                // Prüfen ob der zu löschende User ein Admin ist
                PreparedStatement userCheckStmt = conn.prepareStatement("""
                    SELECT r.name 
                    FROM users u 
                    JOIN roles r ON u.role_id = r.id 
                    WHERE u.id = ?
                """);
                userCheckStmt.setLong(1, userId);
                ResultSet userRs = userCheckStmt.executeQuery();

                if (userRs.next() && userRs.getString("name").equals("ADMIN") && adminCount <= 1) {
                    throw new IllegalStateException("Der letzte Admin-Benutzer kann nicht gelöscht werden!");
                }
            }

            // User löschen
            PreparedStatement deleteStmt = conn.prepareStatement(
                    "DELETE FROM users WHERE id = ?"
            );
            deleteStmt.setLong(1, userId);
            int affected = deleteStmt.executeUpdate();

            if (affected == 0) {
                throw new IllegalArgumentException("Benutzer mit ID " + userId + " nicht gefunden.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    public User updateUser(User user) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            // Prüfen ob der Username bereits existiert (außer bei diesem User)
            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT id FROM users WHERE username = ? AND id != ?"
            );
            checkStmt.setString(1, user.getUsername());
            checkStmt.setLong(2, user.getId());

            if (checkStmt.executeQuery().next()) {
                throw new IllegalArgumentException("Username existiert bereits!");
            }

            // Prüfen ob die Email bereits existiert (außer bei diesem User)
            checkStmt = conn.prepareStatement(
                    "SELECT id FROM users WHERE email = ? AND id != ?"
            );
            checkStmt.setString(1, user.getEmail());
            checkStmt.setLong(2, user.getId());

            if (checkStmt.executeQuery().next()) {
                throw new IllegalArgumentException("Email existiert bereits!");
            }

            // Prüfen ob es der letzte Admin ist und die Rolle geändert werden soll
            PreparedStatement roleCheckStmt = conn.prepareStatement("""
            SELECT r.name as current_role
            FROM users u 
            JOIN roles r ON u.role_id = r.id 
            WHERE u.id = ?
        """);
            roleCheckStmt.setLong(1, user.getId());
            ResultSet roleRs = roleCheckStmt.executeQuery();

            if (roleRs.next() && roleRs.getString("current_role").equals("ADMIN")) {
                PreparedStatement adminCountStmt = conn.prepareStatement("""
                SELECT COUNT(*) as admin_count 
                FROM users u 
                JOIN roles r ON u.role_id = r.id 
                WHERE r.name = 'ADMIN'
            """);
                ResultSet countRs = adminCountStmt.executeQuery();

                if (countRs.next() && countRs.getInt("admin_count") <= 1
                        && !user.getRole().getName().equals("ADMIN")) {
                    throw new IllegalStateException("Der letzte Admin-Benutzer kann nicht zu einem normalen Benutzer geändert werden!");
                }
            }

            // Update durchführen
            PreparedStatement updateStmt = conn.prepareStatement("""
            UPDATE users 
            SET username = ?, 
                email = ?, 
                role_id = ? 
            WHERE id = ?
        """);

            updateStmt.setString(1, user.getUsername());
            updateStmt.setString(2, user.getEmail());
            updateStmt.setLong(3, user.getRole().getId());
            updateStmt.setLong(4, user.getId());

            int affected = updateStmt.executeUpdate();

            if (affected == 0) {
                throw new IllegalArgumentException("Benutzer mit ID " + user.getId() + " nicht gefunden.");
            }

            // Den aktualisierten User zurückgeben
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    public List<User> findAllUsers() {
        List<User> users = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("""
                SELECT u.*, r.name as role_name 
                FROM users u 
                JOIN roles r ON u.role_id = r.id
                ORDER BY u.id
            """);

            while (rs.next()) {
                Role role = new Role(
                        rs.getLong("role_id"),
                        rs.getString("role_name")
                );

                User user = new User(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        role
                );

                users.add(user);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
        return users;
    }

    public void checkDatabase() {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            Statement stmt = conn.createStatement();

            // Prüfe Tabellen und deren Inhalte
            System.out.println("\n=== Datenbank Status ===");

            // Prüfe Roles Tabelle
            ResultSet rs = stmt.executeQuery("SELECT * FROM roles");
            System.out.println("\nRollen:");
            while (rs.next()) {
                System.out.println("ID: " + rs.getLong("id") +
                        ", Name: " + rs.getString("name"));
            }

            // Prüfe Users Tabelle
            rs = stmt.executeQuery("""
                SELECT u.*, r.name as role_name 
                FROM users u
                JOIN roles r ON u.role_id = r.id
            """);
            System.out.println("\nBenutzer:");
            while (rs.next()) {
                System.out.println("ID: " + rs.getLong("id") +
                        ", Username: " + rs.getString("username") +
                        ", Email: " + rs.getString("email") +
                        ", Role: " + rs.getString("role_name"));
            }

            // Prüfe Permissions Tabelle
            rs = stmt.executeQuery("SELECT * FROM permissions");
            System.out.println("\nBerechtigungen:");
            while (rs.next()) {
                System.out.println("ID: " + rs.getLong("id") +
                        ", Name: " + rs.getString("name") +
                        ", Description: " + rs.getString("description"));
            }

            // Prüfe Role-Permissions Verknüpfungen
            rs = stmt.executeQuery("""
                SELECT r.name as role_name, p.name as permission_name
                FROM role_permissions rp
                JOIN roles r ON r.id = rp.role_id
                JOIN permissions p ON p.id = rp.permission_id
            """);
            System.out.println("\nRollen-Berechtigungen:");
            while (rs.next()) {
                System.out.println("Role: " + rs.getString("role_name") +
                        ", Permission: " + rs.getString("permission_name"));
            }

        } catch (SQLException e) {
            System.out.println("Fehler beim Prüfen der Datenbank: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Permission createPermission(String name, String description) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            // Erst das Permission-Objekt einfügen
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO permissions (name, description) VALUES (?, ?)"
            );

            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.executeUpdate();

            // Dann die ID abfragen
            Statement idStmt = conn.createStatement();
            ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()");

            if (rs.next()) {
                Long id = rs.getLong(1);
                return new Permission(id, name, description);
            } else {
                throw new SQLException("Creating permission failed, no ID obtained.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    public List<Permission> getAllPermissions() {
        List<Permission> permissions = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM permissions");

            while (rs.next()) {
                permissions.add(new Permission(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
        return permissions;
    }

    public List<Role> getAllRoles() {
        List<Role> roles = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM roles ORDER BY id");

            while (rs.next()) {
                roles.add(new Role(
                        rs.getLong("id"),
                        rs.getString("name")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
        return roles;
    }

    public Role getRoleById(Long roleId) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM roles WHERE id = ?"
            );
            stmt.setLong(1, roleId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Role(
                        rs.getLong("id"),
                        rs.getString("name")
                );
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    public Role getRoleWithPermissions(Long roleId) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            // Get role
            PreparedStatement roleStmt = conn.prepareStatement(
                    "SELECT * FROM roles WHERE id = ?"
            );
            roleStmt.setLong(1, roleId);
            ResultSet roleRs = roleStmt.executeQuery();

            if (roleRs.next()) {
                Role role = new Role(
                        roleRs.getLong("id"),
                        roleRs.getString("name")
                );

                // Get permissions for role
                PreparedStatement permStmt = conn.prepareStatement("""
                    SELECT p.* FROM permissions p
                    JOIN role_permissions rp ON p.id = rp.permission_id
                    WHERE rp.role_id = ?
                """);
                permStmt.setLong(1, roleId);
                ResultSet permRs = permStmt.executeQuery();

                while (permRs.next()) {
                    role.getPermissions().add(new Permission(
                            permRs.getLong("id"),
                            permRs.getString("name"),
                            permRs.getString("description")
                    ));
                }

                return role;
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
    }

    public void addPermissionToRole(Long roleId, Long permissionId) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT OR IGNORE INTO role_permissions (role_id, permission_id) VALUES (?, ?)"
            );

            stmt.setLong(1, roleId);
            stmt.setLong(2, permissionId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
    }

    public void removePermissionFromRole(Long roleId, Long permissionId) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM role_permissions WHERE role_id = ? AND permission_id = ?"
            );

            stmt.setLong(1, roleId);
            stmt.setLong(2, permissionId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
    }
}