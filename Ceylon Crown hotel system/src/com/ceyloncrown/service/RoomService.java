package com.ceyloncrown.service;

import com.ceyloncrown.model.Room;
import com.ceyloncrown.repository.RoomRepository;
import java.util.*;

public class RoomService {
    private final RoomRepository rooms;
    public RoomService(RoomRepository rooms) { this.rooms = rooms; }
    public List<Room> findAll() { return rooms.findAll(); }
    public Optional<Room> findById(String id) { return rooms.findById(id); }

    /** SAMPLE DATA - demonstration prices only. */
    public void seedIfEmpty() {
        if (rooms.count() > 0) return;
        rooms.save(new Room("R-DLX", "Deluxe Room", "Elegant 38 sqm room with garden views and handcrafted Sri Lankan decor.", 45000, 2,
                List.of("Free Wi-Fi", "Air conditioning", "Tea & coffee", "Garden view"), true, ""));
        rooms.save(new Room("R-PDX", "Premium Deluxe Room", "Spacious 48 sqm room with a private balcony overlooking the tropical grounds.", 62000, 3,
                List.of("Free Wi-Fi", "Air conditioning", "Private balcony", "Minibar"), true, ""));
        rooms.save(new Room("R-LUX", "Luxury Suite", "72 sqm suite with separate living area, soaking tub and ocean-facing terrace.", 95000, 4,
                List.of("Free Wi-Fi", "Living area", "Soaking tub", "Ocean view", "Butler on request"), true, ""));
        rooms.save(new Room("R-ROY", "Royal Suite", "Our signature 140 sqm suite with private dining room, plunge pool and personal butler.", 150000, 5,
                List.of("Private plunge pool", "Butler service", "Dining room", "Ocean view", "Airport transfer"), true, ""));
    }
}