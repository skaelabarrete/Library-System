import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LibraryApiServer {
    private  AdminService adminService;
    private  BookService bookService;
    private  StudentService studentService;

    public LibraryApiServer(AdminService adminService, BookService bookService, StudentService studentService) {
        this.adminService = adminService;
        this.bookService = bookService;
        this.studentService = studentService;
    }

    public void start(int port) throws IOException {
        Path base = Paths.get("").toAbsolutePath();
        Path frontendDir = null;

        java.util.List<Path> candidates = new java.util.ArrayList<>();
        for (int up = 0; up <= 3; up++) {
            Path current = base;
            for (int i = 0; i < up; i++) {
                if (current == null) break;
                current = current.getParent();
            }
            if (current == null) continue;
            candidates.add(current.resolve("frontend").normalize());
            candidates.add(current.resolve("Library-System").resolve("frontend").normalize());
        }

        for (Path candidate : candidates) {
            if (candidate != null && Files.exists(candidate) && Files.isDirectory(candidate)) {
                frontendDir = candidate;
                break;
            }
        }

        if (frontendDir == null) {
            throw new IOException("Frontend directory not found. Checked: " +
                candidates.stream().map(Path::toString).collect(Collectors.joining(", ")));
        }

        // Use a final canonical frontend path for inner handler usage
        final Path realFrontend = frontendDir.toRealPath();

        System.out.println("Serving frontend files from: " + realFrontend);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/books", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                addCors(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 204, "");
                    return;
                }
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                    return;
                }
                List<Book> books = adminService.viewAllBooks();
                String body = booksToJson(books);
                sendJson(exchange, 200, body);
            }
        });

        server.createContext("/issue", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                addCors(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 204, "");
                    return;
                }
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                    return;
                }
                String body = readBody(exchange.getRequestBody());
                Map<String, String> data = parseJson(body);
                int studentId = parseIntSafe(data.get("studentId"));
                int bookId = parseIntSafe(data.get("bookId"));
                boolean ok = bookService.issueBook(studentId, bookId);
                if (ok) {
                    sendJson(exchange, 200, "{\"success\":true,\"message\":\"Book issued\"}");
                } else {
                    sendJson(exchange, 400, "{\"success\":false,\"message\":\"Unable to issue book\"}");
                }
            }
        });

        server.createContext("/return", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                addCors(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 204, "");
                    return;
                }
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                    return;
                }
                String body = readBody(exchange.getRequestBody());
                Map<String, String> data = parseJson(body);
                int studentId = parseIntSafe(data.get("studentId"));
                int bookId = parseIntSafe(data.get("bookId"));
                boolean ok = bookService.returnBook(studentId, bookId);
                if (ok) {
                    sendJson(exchange, 200, "{\"success\":true,\"message\":\"Book returned\"}");
                } else {
                    sendJson(exchange, 400, "{\"success\":false,\"message\":\"Unable to return book\"}");
                }
            }
        });

        server.createContext("/users/login", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                addCors(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 204, "");
                    return;
                }
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                    return;
                }
                String body = readBody(exchange.getRequestBody());
                Map<String, String> data = parseJson(body);
                String role = data.getOrDefault("role", "");
                if ("admin".equalsIgnoreCase(role) || "student".equalsIgnoreCase(role)) {
                    sendJson(exchange, 200, "{\"success\":true,\"message\":\"Logged in\",\"token\":\"dummy-token\"}");
                } else {
                    sendJson(exchange, 400, "{\"success\":false,\"message\":\"Unknown role\"}");
                }
            }
        });

        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                addCors(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 204, "");
                    return;
                }
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/")) {
                    path = "/index.html";
                }

                String requested = path.startsWith("/") ? path.substring(1) : path;
                Path filePath = realFrontend.resolve(requested).normalize();

                // Secure canonical resolution to avoid path traversal and Windows case-sensitivity issues
                Path realPath;
                try {
                    realPath = filePath.toRealPath();
                } catch (java.nio.file.NoSuchFileException e) {
                    sendJson(exchange, 404, "{\"error\":\"File not found\"}");
                    return;
                }

                if (!realPath.startsWith(realFrontend)) {
                    sendJson(exchange, 403, "{\"error\":\"Forbidden\"}");
                    return;
                }
                if (Files.isDirectory(realPath)) {
                    sendJson(exchange, 404, "{\"error\":\"File not found\"}");
                    return;
                }

                byte[] content = Files.readAllBytes(realPath);
                String contentType = getContentType(filePath.toString());
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, content.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(content);
                    os.flush();
                }
            }
        });

        server.setExecutor(null);
        System.out.println("Starting backend API server at http://localhost:" + port);
        server.start();
    }

    private String getContentType(String filename) {
        if (filename.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filename.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filename.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filename.endsWith(".json")) return "application/json; charset=UTF-8";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }

    private static String booksToJson(List<Book> books) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < books.size(); i++) {
            Book b = books.get(i);
            sb.append("{")
              .append(jsonEscape("id")).append(":").append(b.getId()).append(",")
              .append(jsonField("title", b.getTitle())).append(",")
              .append(jsonField("author", b.getAuthor())).append(",")
              .append(jsonField("category", b.getCategory())).append(",")
              .append(jsonField("isbn", b.getIsbn())).append(",")
              .append(jsonField("available", b.isAvailable())).append("}");
            if (i < books.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String jsonField(String key, String value) {
        return jsonEscape(key) + ":" + jsonEscape(value == null ? "" : value);
    }

    private static String jsonField(String key, boolean value) {
        return jsonEscape(key) + ":" + (value ? "true" : "false");
    }

    private static String jsonField(String key, int value) {
        return jsonEscape(key) + ":" + value;
    }

    private static String jsonEscape(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static void addCors(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static void sendJson(HttpExchange exchange, int status, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
            os.flush();
        }
    }

    private static String readBody(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) return map;

        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        List<String> pairs = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
                continue;
            }
            if (c == ',' && !inQuotes) {
                pairs.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (current.length() > 0) {
            pairs.add(current.toString());
        }

        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length != 2) continue;

            String key = keyValue[0].trim().replaceAll("^\"|\"$", "");
            String value = keyValue[1].trim();
            if (value.startsWith("\"")) value = value.substring(1);
            if (value.endsWith("\"")) value = value.substring(0, value.length() - 1);
            value = value.replace("\\\"", "\"").replace("\\\\", "\\");

            map.put(key, value);
        }

        return map;
    }

    private static int parseIntSafe(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
