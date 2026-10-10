package com.ceyloncrown.repository;

import com.ceyloncrown.model.Reservation;
import java.nio.file.Path;
import java.util.*;

public class ReservationRepository extends FileRepository<Reservation> {
    public ReservationRepository(Path file) { super(file); }
    @Override protected Reservation fromRecord(String[] f) { return Reservation.fromRecord(f); }

    public List<Reservation> findByCustomer(String customerId) { return findWhere(r -> r.getCustomerId().equals(customerId)); }
    public List<Reservation> findByRoom(String roomId) { return findWhere(r -> r.getRoomId().equals(roomId)); }
}