package sqlite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * 用户管理 REST API + 前端静态资源服务。
 * 线上部署后，访问同一端口即可打开前端并调用 /api 接口。
 */
public class UserApiServer {

    private static final int DEFAULT_PORT = 3000;
    private static final String ALLOWED_ORIGINS = resolveAllowedOrigins();
    private final UserDao userDao = new UserDao();
    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final Path webRoot;

    private static String resolveAllowedOrigins() {
        String origins = System.getenv("ALLOWED_ORIGINS");
        if (origins == null || origins.isBlank()) {
            return "*";
        }
        return origins;
    }

    public UserApiServer(Path webRoot) {
        this.webRoot = webRoot;
    }

    public static void main(String[] args) throws Exception {
        int port = resolvePort(args);
        Path webRoot = resolveWebRoot();
        new UserApiServer(webRoot).start(port);
    }

    private static int resolvePort(String[] args) {
        if (args.length > 0) {
            return Integer.parseInt(args[0]);
        }
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isBlank()) {
            return Integer.parseInt(envPort);
        }
        return DEFAULT_PORT;
    }

    private static Path resolveWebRoot() {
        String webRoot = System.getenv("WEB_ROOT");
        if (webRoot == null || webRoot.isBlank()) {
            webRoot = System.getProperty("web.root", "web");
        }
        Path path = Path.of(webRoot).toAbsolutePath().normalize();
        return Files.isDirectory(path) ? path : null;
    }

    public void start(int port) throws Exception {
        userDao.initTable();

        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (BindException e) {
            System.err.println("端口 " + port + " 已被占用，请更换端口或停止旧进程。");
            throw e;
        }

        server.createContext("/api/health", this::handleHealth);
        server.createContext("/api/getUser", this::handleGetUsers);
        server.createContext("/api/addUser", this::handleAddUser);
        server.createContext("/api/editUser", this::handleEditUser);
        server.createContext("/api/delUser", this::handleDeleteUser);
        if (webRoot != null) {
            server.createContext("/", this::handleStatic);
        }
        server.setExecutor(null);
        server.start();

        System.out.println("服务已启动: http://0.0.0.0:" + port);
        System.out.println("  模式:     " + (webRoot != null ? "API + 前端" : "仅 API"));
        System.out.println("  CORS:     " + ALLOWED_ORIGINS);
        System.out.println("  数据库:   " + System.getenv().getOrDefault("DB_PATH", "data/demo.db"));
        System.out.println("  GET    /api/getUser");
        System.out.println("  POST   /api/addUser");
        System.out.println("  PUT    /api/editUser");
        System.out.println("  DELETE /api/delUser/{id}");
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        sendJson(exchange, 200, ApiResponse.success("ok"));
    }

    /** 静态资源与 SPA 入口 */
    private void handleStatic(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, ApiResponse.error("仅支持 GET 请求"));
            return;
        }

        String requestPath = exchange.getRequestURI().getPath();
        Path target = webRoot.resolve(requestPath.startsWith("/") ? requestPath.substring(1) : requestPath).normalize();

        if (!target.startsWith(webRoot)) {
            sendJson(exchange, 403, ApiResponse.error("禁止访问"));
            return;
        }

        if (Files.isDirectory(target)) {
            target = target.resolve("index.html");
        }
        if (!Files.exists(target)) {
            target = webRoot.resolve("index.html");
        }
        if (!Files.exists(target)) {
            sendJson(exchange, 404, ApiResponse.error("页面不存在"));
            return;
        }

        serveFile(exchange, target);
    }

    private void serveFile(HttpExchange exchange, Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        String contentType = detectContentType(file);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        addCorsHeaders(exchange);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String detectContentType(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return switch (name) {
            case "index.html", "html" -> "text/html; charset=utf-8";
            default -> {
                if (name.endsWith(".js")) yield "application/javascript; charset=utf-8";
                if (name.endsWith(".css")) yield "text/css; charset=utf-8";
                if (name.endsWith(".svg")) yield "image/svg+xml";
                if (name.endsWith(".png")) yield "image/png";
                if (name.endsWith(".jpg") || name.endsWith(".jpeg")) yield "image/jpeg";
                if (name.endsWith(".ico")) yield "image/x-icon";
                if (name.endsWith(".json")) yield "application/json; charset=utf-8";
                yield "application/octet-stream";
            }
        };
    }

    /** 查询全部用户 */
    private void handleGetUsers(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, ApiResponse.error("仅支持 GET 请求"));
            return;
        }

        try {
            List<User> users = userDao.findAll();
            sendJson(exchange, 200, ApiResponse.success(users));
        } catch (SQLException e) {
            sendJson(exchange, 500, ApiResponse.error("获取用户列表失败"));
        }
    }

    /** 新增用户 */
    private void handleAddUser(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, ApiResponse.error("仅支持 POST 请求"));
            return;
        }

        try {
            UserRequest request = parseBody(exchange, UserRequest.class);
            if (request == null || isBlank(request.name()) || request.age() <= 0) {
                sendJson(exchange, 400, ApiResponse.badRequest("请填写完整信息"));
                return;
            }

            userDao.insert(new User(request.name(), request.age()));
            sendJson(exchange, 200, ApiResponse.success("新增成功"));
        } catch (SQLException e) {
            sendJson(exchange, 500, ApiResponse.error("新增失败"));
        }
    }

    /** 修改用户 */
    private void handleEditUser(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        if (!"PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, ApiResponse.error("仅支持 PUT 请求"));
            return;
        }

        try {
            UserRequest request = parseBody(exchange, UserRequest.class);
            if (request == null || request.id() == null || request.id() <= 0
                    || isBlank(request.name()) || request.age() <= 0) {
                sendJson(exchange, 400, ApiResponse.badRequest("请填写完整信息"));
                return;
            }

            boolean updated = userDao.update(new User(request.id(), request.name(), request.age()));
            if (updated) {
                sendJson(exchange, 200, ApiResponse.success("修改成功"));
            } else {
                sendJson(exchange, 400, ApiResponse.badRequest("用户不存在"));
            }
        } catch (SQLException e) {
            sendJson(exchange, 500, ApiResponse.error("修改失败"));
        }
    }

    /** 删除用户 */
    private void handleDeleteUser(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, ApiResponse.error("仅支持 DELETE 请求"));
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();
            String idPart = path.substring(path.lastIndexOf('/') + 1);
            long id = Long.parseLong(idPart);

            boolean deleted = userDao.deleteById(id);
            if (deleted) {
                sendJson(exchange, 200, ApiResponse.success("删除成功"));
            } else {
                sendJson(exchange, 400, ApiResponse.badRequest("用户不存在"));
            }
        } catch (NumberFormatException e) {
            sendJson(exchange, 400, ApiResponse.badRequest("无效的用户 id"));
        } catch (SQLException e) {
            sendJson(exchange, 500, ApiResponse.error("删除失败"));
        }
    }

    private record UserRequest(Long id, String name, int age) {}

    private <T> T parseBody(HttpExchange exchange, Class<T> type) {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (body.isBlank()) {
                return null;
            }
            return gson.fromJson(body, type);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean handleOptions(HttpExchange exchange) throws IOException {
        if (!"OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            return false;
        }
        addCorsHeaders(exchange);
        exchange.sendResponseHeaders(204, -1);
        return true;
    }

    private void addCorsHeaders(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        String allowOrigin = resolveAllowOrigin(origin);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", allowOrigin);
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private String resolveAllowOrigin(String origin) {
        if ("*".equals(ALLOWED_ORIGINS)) {
            return origin != null && !origin.isBlank() ? origin : "*";
        }
        if (origin == null || origin.isBlank()) {
            return ALLOWED_ORIGINS.split(",")[0].trim();
        }
        boolean allowed = Arrays.stream(ALLOWED_ORIGINS.split(","))
                .map(String::trim)
                .anyMatch(origin::equals);
        return allowed ? origin : ALLOWED_ORIGINS.split(",")[0].trim();
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] bytes = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        addCorsHeaders(exchange);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
