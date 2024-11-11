package com.usermanagement;

import com.usermanagement.model.Role;
import com.usermanagement.model.User;
import com.usermanagement.model.Permission;
import com.usermanagement.repository.UserRepository;
import com.usermanagement.service.UserService;

import java.util.List;
import java.util.Scanner;
import java.io.File;

public class Main {
    private static UserRepository repository;
    private static UserService service;
    private static Scanner scanner;

    public static void main(String[] args) {
        try {
            repository = new UserRepository("jdbc:sqlite:users.db");
            service = new UserService(repository);
            scanner = new Scanner(System.in);

            boolean running = true;
            while (running) {
                showMainMenu();
                String choice = scanner.nextLine();

                switch (choice) {
                    case "1" -> showAllUsers();
                    case "2" -> showDatabaseDetails();
                    case "3" -> createNewUser();
                    case "4" -> manageUsers();
                    case "5" -> managePermissions();
                    case "6" -> resetDatabase();
                    case "7" -> running = false;
                    default -> System.out.println("Ungültige Eingabe. Bitte versuchen Sie es erneut.");
                }

                if (running) {
                    System.out.println("\nDrücken Sie Enter um fortzufahren...");
                    scanner.nextLine();
                }
            }

            System.out.println("Programm wird beendet.");
            scanner.close();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void showMainMenu() {
        clearScreen();
        System.out.println("=== User Management System ===");
        System.out.println("1. Alle Benutzer anzeigen");
        System.out.println("2. Datenbank-Details anzeigen");
        System.out.println("3. Neuen Benutzer erstellen");
        System.out.println("4. Benutzer verwalten");
        System.out.println("5. Berechtigungen verwalten");
        System.out.println("6. Datenbank zurücksetzen");
        System.out.println("7. Programm beenden");
        System.out.print("\nIhre Wahl (1-7): ");
    }

    private static void manageUsers() {
        while (true) {
            clearScreen();
            System.out.println("=== Benutzer Verwaltung ===");
            System.out.println("1. Benutzer bearbeiten");
            System.out.println("2. Benutzer löschen");
            System.out.println("3. Benutzerrolle ändern");
            System.out.println("4. Zurück zum Hauptmenü");
            System.out.print("\nIhre Wahl (1-4): ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1" -> editUser();
                case "2" -> deleteUser();
                case "3" -> changeUserRole();
                case "4" -> {
                    return;
                }
                default -> System.out.println("Ungültige Eingabe!");
            }

            System.out.println("\nDrücken Sie Enter um fortzufahren...");
            scanner.nextLine();
        }
    }

    private static void managePermissions() {
        while (true) {
            clearScreen();
            System.out.println("=== Berechtigungen Verwaltung ===");
            System.out.println("1. Alle Berechtigungen anzeigen");
            System.out.println("2. Berechtigungen einer Rolle anzeigen");
            System.out.println("3. Berechtigung zu Rolle hinzufügen");
            System.out.println("4. Berechtigung von Rolle entfernen");
            System.out.println("5. Zurück zum Hauptmenü");
            System.out.print("\nIhre Wahl (1-5): ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1" -> showAllPermissions();
                case "2" -> showRolePermissions();
                case "3" -> addPermissionToRole();
                case "4" -> removePermissionFromRole();
                case "5" -> {
                    return;
                }
                default -> System.out.println("Ungültige Eingabe!");
            }

            System.out.println("\nDrücken Sie Enter um fortzufahren...");
            scanner.nextLine();
        }
    }

    private static void showAllPermissions() {
        System.out.println("\n=== Alle verfügbaren Berechtigungen ===");
        List<Permission> permissions = repository.getAllPermissions();
        for (Permission permission : permissions) {
            System.out.println("\nBerechtigung:");
            System.out.println("ID: " + permission.getId());
            System.out.println("Name: " + permission.getName());
            System.out.println("Beschreibung: " + permission.getDescription());
        }
    }

    private static void showRolePermissions() {
        System.out.println("\n=== Rollen auswählen ===");
        List<Role> roles = repository.getAllRoles();
        for (Role role : roles) {
            System.out.println(role.getId() + ". " + role.getName());
        }

        System.out.print("\nRollen-ID eingeben: ");
        try {
            Long roleId = Long.parseLong(scanner.nextLine());
            Role role = repository.getRoleWithPermissions(roleId);
            if (role != null) {
                System.out.println("\nBerechtigungen für Rolle '" + role.getName() + "':");
                for (Permission permission : role.getPermissions()) {
                    System.out.println("- " + permission.getName() + ": " + permission.getDescription());
                }
            } else {
                System.out.println("Rolle nicht gefunden!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Ungültige Eingabe!");
        }
    }

    private static void addPermissionToRole() {
        System.out.println("\n=== Berechtigung zu Rolle hinzufügen ===");

        // Zeige verfügbare Rollen
        List<Role> roles = repository.getAllRoles();
        System.out.println("\nVerfügbare Rollen:");
        for (Role role : roles) {
            System.out.println(role.getId() + ". " + role.getName());
        }

        System.out.print("\nRollen-ID eingeben: ");
        try {
            Long roleId = Long.parseLong(scanner.nextLine());

            // Zeige verfügbare Berechtigungen
            List<Permission> permissions = repository.getAllPermissions();
            System.out.println("\nVerfügbare Berechtigungen:");
            for (Permission permission : permissions) {
                System.out.println(permission.getId() + ". " + permission.getName());
            }

            System.out.print("\nBerechtigungs-ID eingeben: ");
            Long permissionId = Long.parseLong(scanner.nextLine());

            repository.addPermissionToRole(roleId, permissionId);
            System.out.println("Berechtigung erfolgreich hinzugefügt!");

        } catch (NumberFormatException e) {
            System.out.println("Ungültige Eingabe!");
        } catch (Exception e) {
            System.out.println("Fehler: " + e.getMessage());
        }
    }

    private static void removePermissionFromRole() {
        System.out.println("\n=== Berechtigung von Rolle entfernen ===");

        // Zeige verfügbare Rollen
        List<Role> roles = repository.getAllRoles();
        System.out.println("\nVerfügbare Rollen:");
        for (Role role : roles) {
            System.out.println(role.getId() + ". " + role.getName());
        }

        System.out.print("\nRollen-ID eingeben: ");
        try {
            Long roleId = Long.parseLong(scanner.nextLine());
            Role role = repository.getRoleWithPermissions(roleId);

            if (role != null && !role.getPermissions().isEmpty()) {
                System.out.println("\nAktuelle Berechtigungen für '" + role.getName() + "':");
                for (Permission permission : role.getPermissions()) {
                    System.out.println(permission.getId() + ". " + permission.getName());
                }

                System.out.print("\nBerechtigungs-ID zum Entfernen eingeben: ");
                Long permissionId = Long.parseLong(scanner.nextLine());

                repository.removePermissionFromRole(roleId, permissionId);
                System.out.println("Berechtigung erfolgreich entfernt!");
            } else {
                System.out.println("Rolle nicht gefunden oder keine Berechtigungen vorhanden!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Ungültige Eingabe!");
        } catch (Exception e) {
            System.out.println("Fehler: " + e.getMessage());
        }
    }

    private static void showAllUsers() {
        clearScreen();
        System.out.println("=== Alle Benutzer ===");
        List<User> users = repository.findAllUsers();

        if (users.isEmpty()) {
            System.out.println("Keine Benutzer in der Datenbank gefunden.");
        } else {
            for (User user : users) {
                System.out.println("\nBenutzer Details:");
                System.out.println("ID: " + user.getId());
                System.out.println("Username: " + user.getUsername());
                System.out.println("Email: " + user.getEmail());
                System.out.println("Role: " + user.getRole().getName());
                System.out.println("------------------------");
            }
        }
    }

    private static void showDatabaseDetails() {
        clearScreen();
        System.out.println("=== Datenbank Details ===");
        repository.checkDatabase();
    }

    private static void createNewUser() {
        clearScreen();
        System.out.println("=== Neuen Benutzer erstellen ===");

        try {
            System.out.print("Username eingeben: ");
            String username = scanner.nextLine();

            System.out.print("Email eingeben: ");
            String email = scanner.nextLine();

            System.out.println("\nRolle auswählen:");
            System.out.println("1. Admin");
            System.out.println("2. User");
            System.out.print("Ihre Wahl (1-2): ");
            String roleChoice = scanner.nextLine();

            Role role;
            if (roleChoice.equals("1")) {
                role = new Role(1L, "ADMIN");
            } else {
                role = new Role(2L, "USER");
            }

            User newUser = service.createUser(username, email, role);
            System.out.println("\nBenutzer erfolgreich erstellt:");
            System.out.println("Username: " + newUser.getUsername());
            System.out.println("Email: " + newUser.getEmail());
            System.out.println("Role: " + newUser.getRole().getName());

        } catch (IllegalArgumentException e) {
            System.out.println("\nFehler beim Erstellen des Benutzers: " + e.getMessage());
        }
    }


    private static void clearScreen() {
        // Fügt Leerzeilen ein, um den Bildschirm "zu leeren"
        System.out.println("\n".repeat(50));
    }

    private static void resetDatabase() {
        clearScreen();
        System.out.println("=== Datenbank zurücksetzen ===");
        System.out.print("Sind Sie sicher? Alle Daten werden gelöscht! (j/n): ");
        String confirm = scanner.nextLine();

        if (confirm.equalsIgnoreCase("j")) {
            try {
                // Schließe zuerst alle Datenbankverbindungen
                if (repository != null) {
                    repository.closeConnections();
                }

                // Lösche die Datenbankdatei
                File dbFile = new File("users.db");
                if (dbFile.exists()) {
                    if (!dbFile.delete()) {
                        throw new RuntimeException("Konnte Datenbankdatei nicht löschen");
                    }
                }

                // Erstelle neue Repository-Instanz
                repository = new UserRepository("jdbc:sqlite:users.db");
                service = new UserService(repository);

                // Erstelle Standardberechtigungen
                Permission readPerm = repository.createPermission("READ", "Daten lesen");
                Permission writePerm = repository.createPermission("WRITE", "Daten schreiben");
                Permission deletePerm = repository.createPermission("DELETE", "Daten löschen");
                Permission adminPerm = repository.createPermission("ADMIN", "Administrative Rechte");

                // Erstelle Standardrollen
                Role adminRole = new Role(1L, "ADMIN");
                repository.saveRole(adminRole);

                Role userRole = new Role(2L, "USER");
                repository.saveRole(userRole);

                // Füge Berechtigungen zu Rollen hinzu
                // Admin bekommt alle Rechte
                repository.addPermissionToRole(adminRole.getId(), readPerm.getId());
                repository.addPermissionToRole(adminRole.getId(), writePerm.getId());
                repository.addPermissionToRole(adminRole.getId(), deletePerm.getId());
                repository.addPermissionToRole(adminRole.getId(), adminPerm.getId());

                // User bekommt Basis-Rechte
                repository.addPermissionToRole(userRole.getId(), readPerm.getId());
                repository.addPermissionToRole(userRole.getId(), writePerm.getId());

                // Erstelle Standardbenutzer
                service.createUser("admin", "admin@test.com", adminRole);
                service.createUser("user", "user@test.com", userRole);

                System.out.println("Datenbank wurde erfolgreich zurückgesetzt und initialisiert.");
                System.out.println("\nStandardbenutzer wurden erstellt:");
                System.out.println("1. admin/admin@test.com (ADMIN)");
                System.out.println("2. user/user@test.com (USER)");

            } catch (Exception e) {
                System.out.println("Fehler beim Zurücksetzen der Datenbank: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Vorgang abgebrochen.");
        }
    }

    private static void editUser() {
        clearScreen();
        System.out.println("=== Benutzer bearbeiten ===");

        // Zeige alle Benutzer
        List<User> users = repository.findAllUsers();
        if (users.isEmpty()) {
            System.out.println("Keine Benutzer vorhanden!");
            return;
        }

        System.out.println("\nVerfügbare Benutzer:");
        for (User user : users) {
            System.out.println(user.getId() + ". " + user.getUsername() + " (" + user.getEmail() + ")");
        }

        System.out.print("\nBenutzer-ID zum Bearbeiten eingeben: ");
        try {
            Long userId = Long.parseLong(scanner.nextLine());
            User user = repository.findById(userId);

            if (user != null) {
                System.out.println("\nAktuelle Werte:");
                System.out.println("Username: " + user.getUsername());
                System.out.println("Email: " + user.getEmail());

                System.out.print("\nNeuen Username eingeben (oder Enter für keine Änderung): ");
                String newUsername = scanner.nextLine();
                if (!newUsername.trim().isEmpty()) {
                    user.setUsername(newUsername);
                }

                System.out.print("Neue Email eingeben (oder Enter für keine Änderung): ");
                String newEmail = scanner.nextLine();
                if (!newEmail.trim().isEmpty()) {
                    user.setEmail(newEmail);
                }

                repository.updateUser(user);
                System.out.println("\nBenutzer erfolgreich aktualisiert!");
            } else {
                System.out.println("Benutzer nicht gefunden!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Ungültige ID!");
        } catch (Exception e) {
            System.out.println("Fehler: " + e.getMessage());
        }
    }

    private static void deleteUser() {
        clearScreen();
        System.out.println("=== Benutzer löschen ===");

        // Zeige alle Benutzer
        List<User> users = repository.findAllUsers();
        if (users.isEmpty()) {
            System.out.println("Keine Benutzer vorhanden!");
            return;
        }

        System.out.println("\nVerfügbare Benutzer:");
        for (User user : users) {
            System.out.println(user.getId() + ". " + user.getUsername() + " (" + user.getEmail() + ")");
        }

        System.out.print("\nBenutzer-ID zum Löschen eingeben: ");
        try {
            Long userId = Long.parseLong(scanner.nextLine());
            User user = repository.findById(userId);

            if (user != null) {
                System.out.println("\nSind Sie sicher, dass Sie den Benutzer '" + user.getUsername() + "' löschen möchten? (j/n)");
                String confirm = scanner.nextLine();

                if (confirm.equalsIgnoreCase("j")) {
                    repository.deleteUser(userId);
                    System.out.println("Benutzer erfolgreich gelöscht!");
                } else {
                    System.out.println("Löschvorgang abgebrochen.");
                }
            } else {
                System.out.println("Benutzer nicht gefunden!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Ungültige ID!");
        } catch (Exception e) {
            System.out.println("Fehler: " + e.getMessage());
        }
    }

    private static void changeUserRole() {
        clearScreen();
        System.out.println("=== Benutzerrolle ändern ===");

        // Zeige alle Benutzer
        List<User> users = repository.findAllUsers();
        if (users.isEmpty()) {
            System.out.println("Keine Benutzer vorhanden!");
            return;
        }

        System.out.println("\nVerfügbare Benutzer:");
        for (User user : users) {
            System.out.println(user.getId() + ". " + user.getUsername() + " (Aktuelle Rolle: " + user.getRole().getName() + ")");
        }

        System.out.print("\nBenutzer-ID eingeben: ");
        try {
            Long userId = Long.parseLong(scanner.nextLine());
            User user = repository.findById(userId);

            if (user != null) {
                System.out.println("\nVerfügbare Rollen:");
                List<Role> roles = repository.getAllRoles();
                for (Role role : roles) {
                    System.out.println(role.getId() + ". " + role.getName());
                }

                System.out.print("\nNeue Rollen-ID eingeben: ");
                Long roleId = Long.parseLong(scanner.nextLine());
                Role newRole = roles.stream()
                        .filter(r -> r.getId().equals(roleId))
                        .findFirst()
                        .orElse(null);

                if (newRole != null) {
                    user.setRole(newRole);
                    repository.updateUser(user);
                    System.out.println("\nBenutzerrolle erfolgreich aktualisiert!");
                } else {
                    System.out.println("Rolle nicht gefunden!");
                }
            } else {
                System.out.println("Benutzer nicht gefunden!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Ungültige ID!");
        } catch (Exception e) {
            System.out.println("Fehler: " + e.getMessage());
        }
    }
}