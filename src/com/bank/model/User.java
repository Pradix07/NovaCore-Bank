package com.bank.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract Base class representing a System User.
 * Demonstrates Object-Oriented Principles: Encapsulation, Abstraction, and Inheritance.
 */
public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String id;
    protected String username;
    protected String passwordHash;
    protected String fullName;
    protected String email;
    protected String phone;
    protected String role; // "ADMIN" or "CUSTOMER"
    protected boolean active;
    protected String createdAt;
    protected String lastLogin;

    public User() {
        this.active = true;
        this.createdAt = LocalDateTime.now().toString();
    }

    public User(String id, String username, String passwordHash, String fullName, String email, String phone, String role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.active = true;
        this.createdAt = LocalDateTime.now().toString();
    }

    // Abstract method to be overridden by subclasses (Polymorphism)
    public abstract String getDashboardType();

    public abstract Map<String, Object> toMap();

    // Getters and Setters (Encapsulation)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }

    protected Map<String, Object> getBaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("username", username);
        map.put("fullName", fullName);
        map.put("email", email);
        map.put("phone", phone);
        map.put("role", role);
        map.put("active", active);
        map.put("createdAt", createdAt);
        map.put("lastLogin", lastLogin);
        map.put("dashboardType", getDashboardType());
        return map;
    }
}
