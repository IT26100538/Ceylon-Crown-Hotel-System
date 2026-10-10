package com.ceyloncrown.repository;

import com.ceyloncrown.model.Review;
import java.nio.file.Path;

public class ReviewRepository extends FileRepository<Review> {
    public ReviewRepository(Path file) { super(file); }
    @Override protected Review fromRecord(String[] f) { return Review.fromRecord(f); }
}