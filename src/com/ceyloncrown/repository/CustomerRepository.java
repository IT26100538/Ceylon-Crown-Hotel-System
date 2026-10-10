package com.ceyloncrown.repository;

import com.ceyloncrown.model.Customer;
import java.nio.file.Path;
import java.util.*;

public class CustomerRepository extends FileRepository<Customer> {
    public CustomerRepository(Path file) { super(file); }
    @Override protected Customer fromRecord(String[] f) { return Customer.fromRecord(f); }

    public Optional<Customer> findByEmail(String email) {
        String e = email == null ? "" : email.trim().toLowerCase();
        return findAll().stream().filter(c -> c.getEmail().equals(e)).findFirst();
    }
}