package com.bank.model;

import java.util.Map;

/**
 * Admin user representation extending User.
 */
public class Admin extends User {
    private static final long serialVersionUID = 1L;
    private String department;
    private int accessLevel; // 1 to 5

    public Admin() {
        super();
        this.role = "ADMIN";
        this.department = "System Administration";
        this.accessLevel = 5;
    }

    public Admin(String id, String username, String passwordHash, String fullName, String email, String phone, String department, int accessLevel) {
        super(id, username, passwordHash, fullName, email, phone, "ADMIN");
        this.department = department;
        this.accessLevel = accessLevel;
    }

    @Override
    public String getDashboardType() {
        return "ADMIN_DASHBOARD";
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = getBaseMap();
        map.put("department", department);
        map.put("accessLevel", accessLevel);
        return map;
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getAccessLevel() { return accessLevel; }
    public void setAccessLevel(int accessLevel) { this.accessLevel = accessLevel; }
}
