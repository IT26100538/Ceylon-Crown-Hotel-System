package com.ceyloncrown.model;

import com.ceyloncrown.util.RecordCodec;
import java.util.*;

public class Room implements Storable {
    private final String id;
    private String type, description, imageUrl;
    private long pricePerNight; // LKR (demonstration price)
    private int maxOccupancy;
    private List<String> facilities;
    private boolean active;     // admin switch; real availability depends on dates

    public Room(String id, String type, String description, long pricePerNight, int maxOccupancy,
                List<String> facilities, boolean active, String imageUrl) {
        this.id = (id == null || id.isBlank()) ? "R" + UUID.randomUUID().toString().substring(0, 6) : id;
        setType(type); setDescription(description); setPricePerNight(pricePerNight);
        setMaxOccupancy(maxOccupancy); setFacilities(facilities);
        this.active = active; this.imageUrl = imageUrl == null ? "" : imageUrl;
    }

    @Override public String getId() { return id; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public long getPricePerNight() { return pricePerNight; }
    public int getMaxOccupancy() { return maxOccupancy; }
    public List<String> getFacilities() { return Collections.unmodifiableList(facilities); }
    public boolean isActive() { return active; }
    public String getImageUrl() { return imageUrl; }

    public final void setType(String t) { if (t == null || t.isBlank()) throw new IllegalArgumentException("Room type required"); type = t.trim(); }
    public final void setDescription(String d) { description = d == null ? "" : d.trim(); }
    public final void setPricePerNight(long p) { if (p <= 0) throw new IllegalArgumentException("Price must be positive"); pricePerNight = p; }
    public final void setMaxOccupancy(int m) { if (m < 1 || m > 10) throw new IllegalArgumentException("Occupancy must be 1-10"); maxOccupancy = m; }
    public final void setFacilities(List<String> f) { facilities = f == null ? new ArrayList<>() : new ArrayList<>(f); }
    public void setActive(boolean a) { active = a; }
    public void setImageUrl(String u) { imageUrl = u == null ? "" : u; }

    /** Business rule lives in the model so no controller can bypass it. */
    public long totalFor(long nights) { return pricePerNight * nights; }

    @Override public String toRecord() {
        return RecordCodec.join(id, type, description, pricePerNight, maxOccupancy, String.join(";", facilities), active, imageUrl);
    }
    public static Room fromRecord(String[] f) {
        List<String> fac = f[5].isBlank() ? List.of() : Arrays.asList(f[5].split(";"));
        return new Room(f[0], f[1], f[2], Long.parseLong(f[3]), Integer.parseInt(f[4]), fac, Boolean.parseBoolean(f[6]), f[7]);
    }
}