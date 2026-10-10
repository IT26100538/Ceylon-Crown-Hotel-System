package com.ceyloncrown.model;

import com.ceyloncrown.util.RecordCodec;
import java.util.UUID;
import java.util.regex.Pattern;

/** Abstract base for all accounts. Fields are private (information hiding); the hash is never exposed by a getter. */
public abstract class User implements Storable {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final String id;
    private String fullName;
    private String email;
    private final String passwordHash;

    protected User(String id, String fullName, String email, String passwordHash) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString().substring(0, 8) : id;
        setFullName(fullName);
        setEmail(email);
        if (passwordHash == null || passwordHash.isBlank()) throw new IllegalArgumentException("Password hash required");
        this.passwordHash = passwordHash;
    }

    /** Polymorphism: each subclass defines its own role and landing page. */
    public abstract String getRole();
    public abstract String getHomePath();

    public boolean passwordMatches(String plain, java.util.function.BiPredicate<String, String> verifier) {
        return verifier.test(plain, passwordHash);
    }

    protected String passwordHashForStorage() { return passwordHash; }

    @Override public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }

    public final void setFullName(String n) {
        if (n == null || n.trim().length() < 2) throw new IllegalArgumentException("Full name must be at least 2 characters");
        this.fullName = n.trim();
    }
    public final void setEmail(String e) {
        if (e == null || !EMAIL.matcher(e.trim()).matches()) throw new IllegalArgumentException("Invalid email address");
        this.email = e.trim().toLowerCase();
    }

    protected String baseRecord(Object... extra) {
        Object[] all = new Object[4 + extra.length];
        all[0] = id; all[1] = fullName; all[2] = email; all[3] = passwordHash;
        System.arraycopy(extra, 0, all, 4, extra.length);
        return RecordCodec.join(all);
    }
}