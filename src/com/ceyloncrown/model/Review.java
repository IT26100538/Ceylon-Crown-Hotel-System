package com.ceyloncrown.model;

import com.ceyloncrown.util.RecordCodec;
import java.time.LocalDateTime;
import java.util.UUID;

public class Review implements Storable {
    private final String id, customerId, customerName;
    private final int rating;
    private final String title, body;
    private boolean approved;
    private final LocalDateTime createdAt;

    public Review(String id, String customerId, String customerName, int rating, String title, String body,
                  boolean approved, LocalDateTime createdAt) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating must be 1-5");
        if (title == null || title.isBlank() || title.length() > 80) throw new IllegalArgumentException("Title required (max 80 chars)");
        if (body == null || body.trim().length() < 10 || body.length() > 1000) throw new IllegalArgumentException("Review must be 10-1000 characters");
        this.id = (id == null || id.isBlank()) ? "V" + UUID.randomUUID().toString().substring(0, 7) : id;
        this.customerId = customerId; this.customerName = customerName; this.rating = rating;
        this.title = title.trim(); this.body = body.trim(); this.approved = approved;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }
    @Override public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public int getRating() { return rating; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean a) { approved = a; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override public String toRecord() { return RecordCodec.join(id, customerId, customerName, rating, title, body, approved, createdAt); }
    public static Review fromRecord(String[] f) {
        return new Review(f[0], f[1], f[2], Integer.parseInt(f[3]), f[4], f[5], Boolean.parseBoolean(f[6]), LocalDateTime.parse(f[7]));
    }
}