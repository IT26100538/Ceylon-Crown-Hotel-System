package com.ceyloncrown.model;

import com.ceyloncrown.util.RecordCodec;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class Reservation implements Storable {
    private final String id, reference, customerId, roomId;
    private final LocalDate checkIn, checkOut;
    private final int adults, children;
    private final long totalPrice;
    private ReservationStatus status;
    private final LocalDateTime createdAt;

    public Reservation(String id, String reference, String customerId, String roomId, LocalDate checkIn, LocalDate checkOut,
                       int adults, int children, long totalPrice, ReservationStatus status, LocalDateTime createdAt) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn))
            throw new IllegalArgumentException("Check-out must be after check-in");
        if (adults < 1 || children < 0) throw new IllegalArgumentException("At least one adult is required");
        this.id = id; this.reference = reference; this.customerId = customerId; this.roomId = roomId;
        this.checkIn = checkIn; this.checkOut = checkOut; this.adults = adults; this.children = children;
        this.totalPrice = totalPrice; this.status = status; this.createdAt = createdAt;
    }

    /** Factory: validates against the room, computes the price on the backend, generates the reference. */
    public static Reservation create(String customerId, Room room, LocalDate in, LocalDate out, int adults, int children, LocalDate today) {
        if (in == null || out == null) throw new IllegalArgumentException("Dates are required");
        if (in.isBefore(today)) throw new IllegalArgumentException("Check-in cannot be in the past");
        if (adults + children > room.getMaxOccupancy())
            throw new IllegalArgumentException("Guests exceed room capacity of " + room.getMaxOccupancy());
        long nights = ChronoUnit.DAYS.between(in, out);
        String ref = "CC-" + today.getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return new Reservation(UUID.randomUUID().toString().substring(0, 8), ref, customerId, room.getId(), in, out,
                adults, children, room.totalFor(nights), ReservationStatus.PENDING, LocalDateTime.now());
    }

    public long nights() { return ChronoUnit.DAYS.between(checkIn, checkOut); }

    /** Cancelled reservations never block a room. */
    public boolean blocksAvailability() { return status != ReservationStatus.CANCELLED; }

    /** NewCheckIn < ExistingCheckOut AND NewCheckOut > ExistingCheckIn */
    public boolean overlaps(LocalDate newIn, LocalDate newOut) {
        return blocksAvailability() && newIn.isBefore(checkOut) && newOut.isAfter(checkIn);
    }

    @Override public String getId() { return id; }
    public String getReference() { return reference; }
    public String getCustomerId() { return customerId; }
    public String getRoomId() { return roomId; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getAdults() { return adults; }
    public int getChildren() { return children; }
    public long getTotalPrice() { return totalPrice; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus s) { this.status = s; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override public String toRecord() {
        return RecordCodec.join(id, reference, customerId, roomId, checkIn, checkOut, adults, children, totalPrice, status, createdAt);
    }
    public static Reservation fromRecord(String[] f) {
        return new Reservation(f[0], f[1], f[2], f[3], LocalDate.parse(f[4]), LocalDate.parse(f[5]),
                Integer.parseInt(f[6]), Integer.parseInt(f[7]), Long.parseLong(f[8]),
                ReservationStatus.valueOf(f[9]), LocalDateTime.parse(f[10]));
    }
}