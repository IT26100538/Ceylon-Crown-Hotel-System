package com.ceyloncrown.model;

import java.util.regex.Pattern;

public class Customer extends User {
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ]{9,15}$");
    private String phone;

    public Customer(String id, String fullName, String email, String passwordHash, String phone) {
        super(id, fullName, email, passwordHash);
        setPhone(phone);
    }

    public String getPhone() { return phone; }
    public final void setPhone(String p) {
        if (p == null || !PHONE.matcher(p.trim()).matches()) throw new IllegalArgumentException("Invalid phone number");
        this.phone = p.trim();
    }

    @Override public String getRole() { return "CUSTOMER"; }
    @Override public String getHomePath() { return "/dashboard.html"; }
    @Override public String toRecord() { return baseRecord(phone); }

    public static Customer fromRecord(String[] f) { return new Customer(f[0], f[1], f[2], f[3], f[4]); }
}