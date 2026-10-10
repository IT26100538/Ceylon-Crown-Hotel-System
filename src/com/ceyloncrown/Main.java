package com.ceyloncrown;

import com.ceyloncrown.repository.*;
import com.ceyloncrown.service.*;
import com.ceyloncrown.web.WebServer;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path data = Path.of("data");
        var customers = new CustomerRepository(data.resolve("customers.txt"));
        var admins = new AdminRepository(data.resolve("admins.txt"));
        var roomRepo = new RoomRepository(data.resolve("rooms.txt"));
        var reservationRepo = new ReservationRepository(data.resolve("reservations.txt"));

        var auth = new AuthenticationService(customers, admins);
        var roomService = new RoomService(roomRepo);
        var reservationService = new ReservationService(reservationRepo, roomRepo);
        auth.seedDefaultAdmin();
        roomService.seedIfEmpty();

        new WebServer(auth, roomService, reservationService, customers, reservationRepo, Path.of("web")).start(port);
        System.out.println("Ceylon Crown running at http://localhost:" + port);
        System.out.println("Demo admin login: admin@ceyloncrown.lk / Admin@12345  (sample credential)");
    }
}