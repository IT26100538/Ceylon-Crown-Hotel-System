package com.ceyloncrown;

import com.ceyloncrown.repository.*;
import com.ceyloncrown.service.*;
import com.ceyloncrown.web.WebServer;

import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {

        // Server port
        int port = args.length > 0
                ? Integer.parseInt(args[0])
                : 8080;

        // Get the project working directory
        Path workingDirectory = Path.of("").toAbsolutePath().normalize();

        System.out.println("======================================");
        System.out.println("       CEYLON CROWN HOTEL SYSTEM");
        System.out.println("======================================");

        System.out.println("Working directory: " + workingDirectory);

        // Find the web folder
        Path web = workingDirectory.resolve("web");

        // If web folder is not found, check the nested project folder
        if (!Files.isRegularFile(web.resolve("index.html"))) {
            Path nestedWeb = workingDirectory
                    .resolve("Ceylon Crown hotel system")
                    .resolve("web");

            if (Files.isRegularFile(nestedWeb.resolve("index.html"))) {
                web = nestedWeb;
            }
        }

        // Display web folder information
        System.out.println("Web folder: " + web);
        System.out.println("Web folder exists: " + Files.isDirectory(web));

        Path indexFile = web.resolve("index.html");

        System.out.println("Index file: " + indexFile);
        System.out.println("index.html exists: " + Files.isRegularFile(indexFile));

        // Stop if index.html cannot be found
        if (!Files.isRegularFile(indexFile)) {
            System.out.println();
            System.out.println("ERROR: index.html was not found!");
            System.out.println("Please check the location of your web folder.");
            return;
        }

        // Data folder
        Path data = workingDirectory.resolve("data");

        System.out.println("Data folder: " + data);
        System.out.println("Data folder exists: " + Files.isDirectory(data));

        // Create data folder if necessary
        Files.createDirectories(data);

        System.out.println("--------------------------------------");

        // Create repositories
        var customers = new CustomerRepository(
                data.resolve("customers.txt")
        );

        var admins = new AdminRepository(
                data.resolve("admins.txt")
        );

        var roomRepo = new RoomRepository(
                data.resolve("rooms.txt")
        );

        var reservationRepo = new ReservationRepository(
                data.resolve("reservations.txt")
        );

        // Create services
        var auth = new AuthenticationService(
                customers,
                admins
        );

        var roomService = new RoomService(
                roomRepo
        );

        var reservationService = new ReservationService(
                reservationRepo,
                roomRepo
        );

        // Create default admin if required
        auth.seedDefaultAdmin();

        // Add sample rooms if required
        roomService.seedIfEmpty();

        // Start web server
        WebServer webServer = new WebServer(
                auth,
                roomService,
                reservationService,
                customers,
                reservationRepo,
                web
        );

        webServer.start(port);

        System.out.println("--------------------------------------");
        System.out.println("Ceylon Crown Hotel System started!");
        System.out.println("Website: http://localhost:" + port + "/");
        System.out.println("Rooms API: http://localhost:" + port + "/api/rooms");
        System.out.println("--------------------------------------");
        System.out.println(
                "Demo admin login: admin@ceyloncrown.lk / Admin@12345"
        );
        System.out.println("======================================");
    }
}