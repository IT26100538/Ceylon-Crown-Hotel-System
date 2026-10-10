package com.ceyloncrown.service;

import com.ceyloncrown.model.*;
import com.ceyloncrown.repository.*;
import com.ceyloncrown.util.PasswordHasher;
import java.util.Optional;

public class AuthenticationService {
    private final CustomerRepository customers;
    private final AdminRepository admins;

    public AuthenticationService(CustomerRepository customers, AdminRepository admins) {
        this.customers = customers; this.admins = admins;
    }

    /** Demo admin so the dashboard can be used. Change this password before any real use. */
    public void seedDefaultAdmin() {
        if (admins.count() == 0)
            admins.save(new Admin(null, "Hotel Administrator", "admin@ceyloncrown.lk", PasswordHasher.hash("Admin@12345")));
    }

    public synchronized Customer register(String name, String email, String password, String phone) {
        if (password == null || password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*"))
            throw new IllegalArgumentException("Password must be at least 8 characters with a letter and a number");
        if (customers.findByEmail(email).isPresent() || admins.findByEmail(email).isPresent())
            throw new IllegalArgumentException("An account with this email already exists");
        return customers.save(new Customer(null, name, email, PasswordHasher.hash(password), phone));
    }

    public Optional<User> login(String email, String password) {
        Optional<User> u = admins.findByEmail(email).map(a -> (User) a);
        if (u.isEmpty()) u = customers.findByEmail(email).map(c -> (User) c);
        return u.filter(x -> x.passwordMatches(password == null ? "" : password, PasswordHasher::verify));
    }

    public Optional<User> findUser(String id) {
        Optional<User> u = admins.findById(id).map(a -> (User) a);
        return u.isPresent() ? u : customers.findById(id).map(c -> (User) c);
    }
}