package com.ceyloncrown.service;

import com.ceyloncrown.model.*;
import com.ceyloncrown.repository.*;
import java.time.LocalDate;
import java.util.*;

public class ReservationService {
    public record Quote(long nights, long total, boolean available, String reason) {}

    private final ReservationRepository reservations;
    private final RoomRepository rooms;

    public ReservationService(ReservationRepository reservations, RoomRepository rooms) {
        this.reservations = reservations; this.rooms = rooms;
    }

    /** True when no non-cancelled reservation overlaps [in, out). */
    public boolean isFree(String roomId, LocalDate in, LocalDate out) {
        return reservations.findByRoom(roomId).stream().noneMatch(r -> r.overlaps(in, out));
    }

    /** Price + availability, all computed on the backend. Never throws for rule violations. */
    public Quote quote(String roomId, LocalDate in, LocalDate out, int adults, int children) {
        Room room = rooms.findById(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found"));
        if (!room.isActive()) return new Quote(0, 0, false, "This room is not currently bookable");
        Reservation trial;
        try { trial = Reservation.create("preview", room, in, out, adults, children, LocalDate.now()); }
        catch (IllegalArgumentException e) { return new Quote(0, 0, false, e.getMessage()); }
        if (!isFree(roomId, in, out)) return new Quote(trial.nights(), trial.getTotalPrice(), false, "Not available for the selected dates");
        return new Quote(trial.nights(), trial.getTotalPrice(), true, "Available");
    }

    /** synchronized: check + save happen as one step, so two bookings cannot both pass the overlap check. */
    public synchronized Reservation book(String customerId, String roomId, LocalDate in, LocalDate out, int adults, int children) {
        Room room = rooms.findById(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found"));
        if (!room.isActive()) throw new IllegalArgumentException("This room is not currently bookable");
        Reservation r = Reservation.create(customerId, room, in, out, adults, children, LocalDate.now());
        if (!isFree(roomId, in, out)) throw new IllegalArgumentException("Room is not available for the selected dates");
        return reservations.save(r);
    }

    public List<Reservation> forCustomer(String customerId) {
        List<Reservation> l = new ArrayList<>(reservations.findByCustomer(customerId));
        l.sort(Comparator.comparing(Reservation::getCreatedAt).reversed());
        return l;
    }

    /** Only the owner may cancel, and only upcoming, not-yet-cancelled reservations. */
    public synchronized Reservation cancel(String customerId, String reservationId) {
        Reservation r = reservations.findById(reservationId).filter(x -> x.getCustomerId().equals(customerId))
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        if (r.getStatus() == ReservationStatus.CANCELLED) throw new IllegalArgumentException("Reservation is already cancelled");
        if (!r.getCheckIn().isAfter(LocalDate.now())) throw new IllegalArgumentException("Only upcoming reservations can be cancelled");
        r.setStatus(ReservationStatus.CANCELLED);
        return reservations.save(r);
    }
}