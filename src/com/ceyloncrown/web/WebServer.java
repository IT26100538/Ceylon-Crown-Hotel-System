package com.ceyloncrown.web;

import com.ceyloncrown.model.*;
import com.ceyloncrown.repository.*;
import com.ceyloncrown.service.*;
import com.sun.net.httpserver.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.*;

/** Thin HTTP layer: parses requests, enforces login/role rules, delegates ALL logic to the services. */
public class WebServer {
    static class ApiException extends RuntimeException {
        final int status;
        ApiException(int status, String msg) { super(msg); this.status = status; }
    }

    private final AuthenticationService auth;
    private final RoomService rooms;
    private final ReservationService reservations;
    private final CustomerRepository customers;
    private final ReservationRepository reservationRepo;
    private final Path webRoot;
    private final Map<String, String> sessions = new ConcurrentHashMap<>(); // token -> userId
    private final SecureRandom random = new SecureRandom();

    public WebServer(AuthenticationService auth, RoomService rooms, ReservationService reservations,
                     CustomerRepository customers, ReservationRepository reservationRepo, Path webRoot) {
        this.auth = auth; this.rooms = rooms; this.reservations = reservations;
        this.customers = customers; this.reservationRepo = reservationRepo;
        this.webRoot = webRoot.toAbsolutePath().normalize();
    }

    public void start(int port) throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0); // localhost only
        s.createContext("/", this::handle);
        s.setExecutor(Executors.newFixedThreadPool(8));
        s.start();
    }

    private void handle(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            if (path.startsWith("/api/")) send(ex, 200, Json.toJson(route(ex, ex.getRequestMethod(), path)), "application/json; charset=utf-8");
            else serveStatic(ex, path);
        } catch (ApiException e) { error(ex, e.status, e.getMessage());
        } catch (IllegalArgumentException e) { error(ex, 400, e.getMessage());
        } catch (Exception e) { e.printStackTrace(); error(ex, 500, "Server error"); }
    }

    private Object route(HttpExchange ex, String method, String path) throws IOException {
        Map<String, String> q = params(ex.getRequestURI().getRawQuery());
        Map<String, String> p = method.equals("POST") ? params(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)) : q;
        switch (method + " " + path) {
            case "GET /api/rooms": return roomList(p);
            case "GET /api/quote": {
                var qt = reservations.quote(p.getOrDefault("roomId", ""), date(p.get("in")), date(p.get("out")), intOf(p, "adults", 1), intOf(p, "children", 0));
                return map("nights", qt.nights(), "total", qt.total(), "available", qt.available(), "reason", qt.reason());
            }
            case "POST /api/register": {
                auth.register(p.get("name"), p.get("email"), p.get("password"), p.get("phone"));
                return map("ok", true);
            }
            case "POST /api/login": {
                User u = auth.login(p.get("email"), p.get("password")).orElseThrow(() -> new ApiException(401, "Invalid email or password"));
                byte[] b = new byte[32]; random.nextBytes(b);
                String token = HexFormat.of().formatHex(b);
                sessions.put(token, u.getId());
                ex.getResponseHeaders().add("Set-Cookie", "CC_SESSION=" + token + "; Path=/; HttpOnly; SameSite=Strict");
                return userMap(u);
            }
            case "POST /api/logout": {
                String t = cookie(ex); if (t != null) sessions.remove(t);
                ex.getResponseHeaders().add("Set-Cookie", "CC_SESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict");
                return map("ok", true);
            }
            case "GET /api/me": {
                User u = currentUser(ex);
                Map<String, Object> m = new LinkedHashMap<>(); m.put("user", u == null ? null : userMap(u)); return m;
            }
            case "POST /api/reservations": {
                User u = requireRole(ex, "CUSTOMER");
                return resMap(reservations.book(u.getId(), p.getOrDefault("roomId", ""), date(p.get("in")), date(p.get("out")),
                        intOf(p, "adults", 1), intOf(p, "children", 0)));
            }
            case "GET /api/reservations/mine": {
                User u = requireRole(ex, "CUSTOMER");
                List<Object> out = new ArrayList<>();
                for (Reservation r : reservations.forCustomer(u.getId())) out.add(resMap(r));
                return out;
            }
            case "POST /api/reservations/cancel": {
                User u = requireRole(ex, "CUSTOMER");
                return resMap(reservations.cancel(u.getId(), p.getOrDefault("id", "")));
            }
            case "GET /api/admin/stats": {
                requireRole(ex, "ADMIN");
                List<Reservation> all = reservationRepo.findAll();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("totalRooms", rooms.findAll().size());
                m.put("activeRooms", rooms.findAll().stream().filter(Room::isActive).count());
                m.put("totalCustomers", customers.count());
                m.put("totalReservations", all.size());
                for (ReservationStatus s : ReservationStatus.values())
                    m.put(s.name().toLowerCase(), all.stream().filter(r -> r.getStatus() == s).count());
                return m;
            }
            default: throw new ApiException(404, "Not found");
        }
    }

    // ---------- helpers ----------
    private List<Object> roomList(Map<String, String> p) {
        boolean dated = p.get("in") != null && !p.get("in").isBlank() && p.get("out") != null && !p.get("out").isBlank();
        List<Object> out = new ArrayList<>();
        for (Room r : rooms.findAll()) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("id", r.getId()); d.put("type", r.getType()); d.put("description", r.getDescription());
            d.put("pricePerNight", r.getPricePerNight()); d.put("maxOccupancy", r.getMaxOccupancy()); d.put("facilities", r.getFacilities());
            boolean ok = r.isActive(); String note = ok ? "Available" : "Currently unavailable";
            if (ok && dated) {
                var qt = reservations.quote(r.getId(), date(p.get("in")), date(p.get("out")), intOf(p, "adults", 1), intOf(p, "children", 0));
                ok = qt.available(); note = ok ? "Available for your dates" : qt.reason();
            }
            d.put("available", ok); d.put("note", note); out.add(d);
        }
        return out;
    }

    private Map<String, Object> resMap(Reservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId()); m.put("reference", r.getReference());
        m.put("roomType", rooms.findById(r.getRoomId()).map(Room::getType).orElse("Room"));
        m.put("checkIn", r.getCheckIn()); m.put("checkOut", r.getCheckOut()); m.put("nights", r.nights());
        m.put("adults", r.getAdults()); m.put("children", r.getChildren());
        m.put("totalPrice", r.getTotalPrice()); m.put("status", r.getStatus());
        return m;
    }

    private Map<String, Object> userMap(User u) { // never includes the password hash
        return map("name", u.getFullName(), "email", u.getEmail(), "role", u.getRole(), "home", u.getHomePath());
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private String cookie(HttpExchange ex) {
        String h = ex.getRequestHeaders().getFirst("Cookie");
        if (h == null) return null;
        for (String part : h.split(";")) { String[] kv = part.trim().split("=", 2); if (kv.length == 2 && kv[0].equals("CC_SESSION")) return kv[1]; }
        return null;
    }

    private User currentUser(HttpExchange ex) {
        String t = cookie(ex); String id = t == null ? null : sessions.get(t);
        return id == null ? null : auth.findUser(id).orElse(null);
    }

    /** Authorization is enforced here on the server, not just hidden in the UI. */
    private User requireRole(HttpExchange ex, String role) {
        User u = currentUser(ex);
        if (u == null) throw new ApiException(401, "Please log in");
        if (!u.getRole().equals(role)) throw new ApiException(403, "You do not have access to this");
        return u;
    }

    private static LocalDate date(String s) {
        try { return LocalDate.parse(s == null ? "" : s.trim()); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException("Please choose valid dates"); }
    }
    private static int intOf(Map<String, String> p, String k, int def) {
        String v = p.get(k); return v == null || v.isBlank() ? def : Integer.parseInt(v.trim());
    }
    private static Map<String, String> params(String s) {
        Map<String, String> m = new HashMap<>();
        if (s == null || s.isBlank()) return m;
        for (String pair : s.split("&")) {
            String[] kv = pair.split("=", 2);
            m.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return m;
    }

    private void serveStatic(HttpExchange ex, String path) throws IOException {
        Path file = webRoot.resolve(path.equals("/") ? "index.html" : path.substring(1)).normalize();
        if (!file.startsWith(webRoot) || !Files.isRegularFile(file)) { error(ex, 404, "Not found"); return; }
        String n = file.getFileName().toString();
        String type = n.endsWith(".html") ? "text/html; charset=utf-8" : n.endsWith(".css") ? "text/css; charset=utf-8"
                : n.endsWith(".js") ? "text/javascript; charset=utf-8" : n.endsWith(".png") ? "image/png"
                : n.endsWith(".jpg") || n.endsWith(".jpeg") ? "image/jpeg" : n.endsWith(".svg") ? "image/svg+xml" : "application/octet-stream";
        ex.getResponseHeaders().add("X-Content-Type-Options", "nosniff");
        send(ex, 200, Files.readAllBytes(file), type);
    }
    private void error(HttpExchange ex, int status, String msg) throws IOException {
        send(ex, status, Json.toJson(map("error", msg)), "application/json; charset=utf-8");
    }
    private void send(HttpExchange ex, int status, String body, String type) throws IOException {
        send(ex, status, body.getBytes(StandardCharsets.UTF_8), type);
    }
    private void send(HttpExchange ex, int status, byte[] body, String type) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(status, body.length);
        try (var os = ex.getResponseBody()) { os.write(body); }
    }
}