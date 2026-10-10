package com.ceyloncrown.model;

public class Admin extends User {
    public Admin(String id, String fullName, String email, String passwordHash) {
        super(id, fullName, email, passwordHash);
    }
    @Override public String getRole() { return "ADMIN"; }
    @Override public String getHomePath() { return "/admin/dashboard.html"; }
    @Override public String toRecord() { return baseRecord(); }

    public static Admin fromRecord(String[] f) { return new Admin(f[0], f[1], f[2], f[3]); }
}