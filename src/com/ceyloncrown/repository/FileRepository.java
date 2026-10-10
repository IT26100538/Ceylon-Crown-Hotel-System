package com.ceyloncrown.repository;

import com.ceyloncrown.model.Storable;
import com.ceyloncrown.util.RecordCodec;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

/**
 * Generic CRUD over a text file (one record per line). Subclasses only say how to rebuild an object from a record
 * (Template Method). Writes go to a temp file then an atomic move, so a crash cannot leave a half-written file.
 */
public abstract class FileRepository<T extends Storable> {
    private final Path file;

    protected FileRepository(Path file) {
        this.file = file;
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            if (!Files.exists(file)) Files.createFile(file);
        } catch (IOException e) { throw new UncheckedIOException("Cannot prepare " + file, e); }
    }

    protected abstract T fromRecord(String[] fields);

    public synchronized List<T> findAll() {
        List<T> out = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank()) continue;
                try { out.add(fromRecord(RecordCodec.split(line))); }
                catch (RuntimeException bad) { System.err.println("Skipping corrupt record in " + file.getFileName() + ": " + bad.getMessage()); }
            }
        } catch (IOException e) { throw new UncheckedIOException("Cannot read " + file, e); }
        return out;
    }

    public synchronized Optional<T> findById(String id) {
        return findAll().stream().filter(t -> t.getId().equals(id)).findFirst();
    }

    public synchronized List<T> findWhere(Predicate<T> p) {
        return findAll().stream().filter(p).toList();
    }

    /** Create or update (matched by id). */
    public synchronized T save(T item) {
        List<T> all = findAll();
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++)
            if (all.get(i).getId().equals(item.getId())) { all.set(i, item); replaced = true; break; }
        if (!replaced) all.add(item);
        writeAll(all);
        return item;
    }

    public synchronized boolean delete(String id) {
        List<T> all = findAll();
        boolean removed = all.removeIf(t -> t.getId().equals(id));
        if (removed) writeAll(all);
        return removed;
    }

    public synchronized int count() { return findAll().size(); }

    private void writeAll(List<T> items) {
        try {
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(tmp, items.stream().map(Storable::toRecord).toList(), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) { throw new UncheckedIOException("Cannot write " + file, e); }
    }
}