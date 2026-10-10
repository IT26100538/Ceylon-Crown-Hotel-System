package com.ceyloncrown.repository;

import com.ceyloncrown.model.Admin;
import java.nio.file.Path;
import java.util.*;

public class AdminRepository extends FileRepository<Admin> {
    public AdminRepository(Path file) { super(file); }
    @Override protected Admin fromRecord(String[] f) { return Admin.fromRecord(f); }

    public Optional<Admin> findByEmail(String email) {
        String e = email == null ? "" : email.trim().toLowerCase();
        return findAll().stream().filter(a -> a.getEmail().equals(e)).findFirst();
    }
}