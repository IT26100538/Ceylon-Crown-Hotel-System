package com.ceyloncrown.repository;

import com.ceyloncrown.model.Room;
import java.nio.file.Path;

public class RoomRepository extends FileRepository<Room> {
    public RoomRepository(Path file) { super(file); }
    @Override protected Room fromRecord(String[] f) { return Room.fromRecord(f); }
}