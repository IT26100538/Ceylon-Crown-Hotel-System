package com.ceyloncrown.model;

import com.ceyloncrown.util.RecordCodec;
import java.time.LocalDateTime;
import java.util.UUID;

/** Demonstration only: stores method + status, never card numbers, CVVs or bank passwords. */
public class Payment implements Storable {
    private final String id, reservationId;
    private final PaymentMethod method;
    private final long amount;
    private PaymentStatus status;
    private final LocalDateTime createdAt;

    public Payment(String id, String reservationId, PaymentMethod method, long amount, PaymentStatus status, LocalDateTime createdAt) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        this.id = (id == null || id.isBlank()) ? "P" + UUID.randomUUID().toString().substring(0, 7) : id;
        this.reservationId = reservationId; this.method = method; this.amount = amount;
        this.status = status; this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }
    @Override public String getId() { return id; }
    public String getReservationId() { return reservationId; }
    public PaymentMethod getMethod() { return method; }
    public long getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus s) { status = s; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override public String toRecord() { return RecordCodec.join(id, reservationId, method, amount, status, createdAt); }
    public static Payment fromRecord(String[] f) {
        return new Payment(f[0], f[1], PaymentMethod.valueOf(f[2]), Long.parseLong(f[3]), PaymentStatus.valueOf(f[4]), LocalDateTime.parse(f[5]));
    }
}