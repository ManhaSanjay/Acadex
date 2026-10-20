
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Stream;
import java.io.File;
public class Server {
    private static final int PORT = 8080;
    private static final Map<String, Session> SESSIONS =
            new ConcurrentHashMap<>();

    private static final class Session {
        final int userId;
        final String username;
        final String displayName;
        final String role;

        Session(int userId, String username, String displayName, String role) {
            this.userId = userId;
            this.username = username;
            this.displayName = displayName;
            this.role = role;
        }

        boolean isAdmin() {
            return "ADMIN".equals(role);
        }
    }


public static void main(String[] args) throws Exception {
    Database.setActiveUserId(1);
    Database.initialize();
    ensureUserActiveColumn();
    ensureForumOwnerColumn();

    HttpServer server = HttpServer.create(
            new InetSocketAddress("0.0.0.0", PORT), 0);

    server.createContext("/api/auth/", Server::handleAuth);
    server.createContext("/api/forum/posts", Server::handlePosts);
    server.createContext("/api/admin/academic/",
        Server::handleLegacyAcademicExport);
        server.createContext("/api/admin/users", Server::handleAdminUsers);
    server.createContext(
        "/api/admin/backups", Server::handleAdminBackups);
    server.createContext(
        "/api/admin/monitoring", Server::handleAdminMonitoring);
        server.createContext(
        "/api/admin/academic-support",
        Server::handleAdminAcademicSupport);
    server.setExecutor(
            java.util.concurrent.Executors.newCachedThreadPool());
    server.start();

    System.out.println("Acadex server running on port " + PORT);
    System.out.println("Keep this terminal open while testing.");
}


    // Preserve all existing posts; old posts keep a NULL owner.
    private static void ensureForumOwnerColumn() throws SQLException {
        try (Connection c = Database.getServerConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("PRAGMA table_info(forum_posts)")) {
            while (r.next()) {
                if ("owner_user_id".equalsIgnoreCase(r.getString("name"))) {
                    return;
                }
            }
        }

        try (Connection c = Database.getServerConnection();
             Statement s = c.createStatement()) {
            s.execute("ALTER TABLE forum_posts ADD COLUMN owner_user_id INTEGER");
        }
    }

    // Read-only export of legacy academic records.
    // These records have not yet been assigned to individual accounts,
    // so only an authenticated administrator can access them.
    private static void handleLegacyAcademicExport(HttpExchange ex)
            throws IOException {
        try {
            if (!"GET".equals(ex.getRequestMethod())) {
                respond(ex, 405, "Method not allowed");
                return;
            }

            Session session = requireSession(ex);
            if (session == null) {
                return;
            }

            if (!session.isAdmin()) {
                respond(ex, 403,
                        "Administrator access required.");
                return;
            }

            StringBuilder out = new StringBuilder();
            out.append("# Acadex legacy academic export\n");
            out.append("# Read-only; records are not yet assigned to accounts.\n");

            try (Connection c = Database.getServerConnection()) {
                appendExportRows(out, c, "subjects",
                        "SELECT id,code,name,credits,cie_max,ese_max " +
                        "FROM subjects ORDER BY id");

                appendExportRows(out, c, "grades",
                        "SELECT id,subject_id,cie,ese,target_grade " +
                        "FROM grades ORDER BY id");

                appendExportRows(out, c, "assessments",
                        "SELECT id,subject_id,title,max_marks," +
                        "marks_obtained,due_date " +
                        "FROM assessments ORDER BY id");

                appendExportRows(out, c, "attendance",
                        "SELECT id,subject_id,classes_held,classes_attended," +
                        "attendance_date,hour,present " +
                        "FROM attendance ORDER BY id");

                appendExportRows(out, c, "timetable",
                        "SELECT day_of_week,hour,subject_id " +
                        "FROM timetable ORDER BY day_of_week,hour");

                appendExportRows(out, c, "holidays",
                        "SELECT holiday_date,description " +
                        "FROM holidays ORDER BY holiday_date");
            }

            respond(ex, 200, out.toString());

        } catch (Exception e) {
            e.printStackTrace();
            safeRespond(ex, 500, "Academic export failed.");
        } finally {
            ex.close();
        }
    }

    // Export format:
    // ROW<TAB>table-name<TAB>base64-value<TAB>base64-value...
    // Each database value is encoded to preserve tabs and newlines.
    private static void appendExportRows(
            StringBuilder out,
            Connection c,
            String table,
            String sql) throws SQLException {

        try (Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) {

            ResultSetMetaData meta = r.getMetaData();
            int columns = meta.getColumnCount();

            out.append("TABLE\t").append(table);
            for (int i = 1; i <= columns; i++) {
                out.append('\t').append(meta.getColumnName(i));
            }
            out.append('\n');

            while (r.next()) {
                out.append("ROW\t").append(table);

                for (int i = 1; i <= columns; i++) {
                    String value = r.getString(i);
                    out.append('\t').append(
                            encode(value == null ? "" : value));
                }

                out.append('\n');
            }
        }
    }

    private static void handleAuth(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();

            if ("/api/auth/register".equals(path) && "POST".equals(method)) {
                register(ex);
            } else if ("/api/auth/login".equals(path) && "POST".equals(method)) {
                login(ex);
            } else if ("/api/auth/logout".equals(path) && "POST".equals(method)) {
                logout(ex);
            } else {
                respond(ex, 404, "Unknown authentication endpoint");
            }
        } catch (Exception e) {
            e.printStackTrace();
            safeRespond(ex, 500, "Server error");
        } finally {
            ex.close();
        }
    }

    private static void register(HttpExchange ex) throws Exception {
        Map<String, String> fields = readForm(ex);
        String username = fields.getOrDefault("username", "").trim();
        String displayName = fields.getOrDefault("displayName", "").trim();
        String password = fields.getOrDefault("password", "");

        if (!username.matches("[A-Za-z0-9_.]{3,24}")) {
            respond(ex, 400,
                    "Username must be 3-24 characters: letters, numbers, _ or .");
            return;
        }
        if (displayName.isEmpty() || displayName.length() > 60) {
            respond(ex, 400, "Display name must be 1-60 characters.");
            return;
        }
        if (password.length() < 8 || password.length() > 200) {
            respond(ex, 400, "Password must be at least 8 characters.");
            return;
        }

        try (Connection c = Database.getServerConnection();
             PreparedStatement p = c.prepareStatement(
                     "INSERT INTO users(username,password_hash,display_name,role) " +
                     "VALUES(?,?,?,'STUDENT')")) {
            p.setString(1, username);
            p.setString(2, PasswordUtil.hash(password));
            p.setString(3, displayName);
            p.executeUpdate();
            respond(ex, 201, "Registration successful. You can now sign in.");
        } catch (SQLException e) {
            if (e.getMessage() != null &&
                    e.getMessage().toLowerCase(Locale.ROOT).contains("unique")) {
                respond(ex, 409, "That username is already taken.");
            } else {
                throw e;
            }
        }
    }

    private static void login(HttpExchange ex) throws Exception {
        Map<String, String> fields = readForm(ex);
        String username = fields.getOrDefault("username", "").trim();
        String password = fields.getOrDefault("password", "");

        try (Connection c = Database.getServerConnection();
             PreparedStatement p = c.prepareStatement(
                     "SELECT id, username, display_name, role " +
"FROM users WHERE username=? AND password_hash=? " +
"AND is_active=1")) {
            p.setString(1, username);
            p.setString(2, PasswordUtil.hash(password));

            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) {
                    respond(ex, 401, "Incorrect username or password.");
                    return;
                }

                int id = r.getInt("id");
                String name = r.getString("username");
                String display = r.getString("display_name");
                String role = r.getString("role");

                String token = UUID.randomUUID().toString();
                SESSIONS.put(token, new Session(id, name, display, role));

                // Tab-separated response; text fields are Base64 URL encoded.
                String response = "OK\t" + id + "\t" +
                        encode(name) + "\t" + encode(display) + "\t" +
                        encode(role) + "\t" + token;
                respond(ex, 200, response);
            }
        }
    }

    private static void logout(HttpExchange ex) throws IOException {
        String token = getToken(ex);
        if (token != null) SESSIONS.remove(token);
        respond(ex, 200, "Signed out");
    }

    private static void handlePosts(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();

            if ("GET".equals(method) && "/api/forum/posts".equals(path)) {
                if (requireSession(ex) != null) listPosts(ex);
            } else if ("POST".equals(method) && "/api/forum/posts".equals(path)) {
                Session session = requireSession(ex);
                if (session != null) createPost(ex, session);
            } else if ("DELETE".equals(method)) {
                Session session = requireSession(ex);
                if (session != null) deletePost(ex, session);
            } else {
                respond(ex, 405, "Method not allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            safeRespond(ex, 500, "Server error");
        } finally {
            ex.close();
        }
    }

    private static Session requireSession(HttpExchange ex) throws IOException {
        String token = getToken(ex);
        Session session = token == null ? null : SESSIONS.get(token);
        if (session == null) {
            respond(ex, 401, "Please sign in again.");
        }
        return session;
    }

    private static String getToken(HttpExchange ex) {
        String header = ex.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        return header.substring(7).trim();
    }

    private static void listPosts(HttpExchange ex) throws Exception {
        StringBuilder out = new StringBuilder();

        try (Connection c = Database.getServerConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(
                     "SELECT id,title,message,category,author,created_at,owner_user_id " +
                     "FROM forum_posts ORDER BY id DESC")) {
            while (r.next()) {
                out.append(r.getInt("id"));
                for (String column : new String[]{
                        "title", "message", "category", "author", "created_at"}) {
                    String value = r.getString(column);
                    out.append('\t').append(encode(value == null ? "" : value));
                }

                Object owner = r.getObject("owner_user_id");
                out.append('\t').append(owner == null ? "" : owner.toString());
                out.append('\n');
            }
        }
        respond(ex, 200, out.toString());
    }

    private static void createPost(HttpExchange ex, Session session)
            throws Exception {
        Map<String, String> fields = readForm(ex);
        String title = fields.getOrDefault("title", "").trim();
        String message = fields.getOrDefault("message", "").trim();
        String category = fields.getOrDefault("category", "General").trim();

        if (title.isEmpty() || message.isEmpty()) {
            respond(ex, 400, "Title and message are required.");
            return;
        }
        if (title.length() > 150 || message.length() > 5000 ||
                category.length() > 50) {
            respond(ex, 400, "One or more fields are too long.");
            return;
        }

        try (Connection c = Database.getServerConnection();
             PreparedStatement p = c.prepareStatement(
                     "INSERT INTO forum_posts " +
                     "(title,message,category,author,owner_user_id) VALUES (?,?,?,?,?)")) {
            p.setString(1, title);
            p.setString(2, message);
            p.setString(3, category.isEmpty() ? "General" : category);
            p.setString(4, session.displayName);
            p.setInt(5, session.userId);
            p.executeUpdate();
        }
        respond(ex, 201, "Post published");
    }

    private static void deletePost(HttpExchange ex, Session session)
            throws Exception {
        String path = ex.getRequestURI().getPath();
        String prefix = "/api/forum/posts/";

        if (!path.startsWith(prefix)) {
            respond(ex, 400, "Missing post ID");
            return;
        }

        String idText = path.substring(prefix.length());
        if (idText.isEmpty() || idText.contains("/")) {
            respond(ex, 400, "Invalid post ID");
            return;
        }

        int postId;
        try {
            postId = Integer.parseInt(idText);
        } catch (NumberFormatException e) {
            respond(ex, 400, "Invalid post ID");
            return;
        }

        try (Connection c = Database.getServerConnection();
             PreparedStatement find = c.prepareStatement(
                     "SELECT owner_user_id FROM forum_posts WHERE id=?")) {
            find.setInt(1, postId);

            try (ResultSet r = find.executeQuery()) {
                if (!r.next()) {
                    respond(ex, 404, "Post not found");
                    return;
                }

                Object ownerObject = r.getObject("owner_user_id");
                Integer ownerId = ownerObject == null
                        ? null : ((Number) ownerObject).intValue();

                // Unowned legacy posts can only be deleted by an admin.
                if (!session.isAdmin() &&
                        (ownerId == null || ownerId != session.userId)) {
                    respond(ex, 403, "You can only delete your own posts.");
                    return;
                }
            }

            try (PreparedStatement delete = c.prepareStatement(
                    "DELETE FROM forum_posts WHERE id=?")) {
                delete.setInt(1, postId);
                delete.executeUpdate();
            }
        }

        respond(ex, 200, "Post deleted successfully");
    }

    private static Map<String, String> readForm(HttpExchange ex)
            throws IOException {
        Map<String, String> fields = new HashMap<>();
        String body = new String(
                ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        for (String pair : body.split("&")) {
            if (pair.isEmpty()) continue;
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1
                    ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            fields.put(key, value);
        }
        return fields;
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static void respond(HttpExchange ex, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void safeRespond(HttpExchange ex, int status, String body) {
        try {
            respond(ex, status, body);
        } catch (IOException ignored) {
        }
    }


private static void handleAdminUsers(HttpExchange ex)
        throws IOException {
    try {
        Session session = requireSession(ex);
        if (session == null) return;

        if (!session.isAdmin()) {
            respond(ex, 403, "Administrator access required.");
            return;
        }

        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();

        // List users, including their active status.
        if ("GET".equals(method) &&
                "/api/admin/users".equals(path)) {

            StringBuilder out = new StringBuilder();

            try (Connection c = Database.getServerConnection();
                 PreparedStatement p = c.prepareStatement(
                         "SELECT id, username, display_name, role, is_active " +
                         "FROM users ORDER BY id");
                 ResultSet r = p.executeQuery()) {

                while (r.next()) {
                    out.append(r.getInt("id")).append('\t')
                       .append(encode(r.getString("username"))).append('\t')
                       .append(encode(r.getString("display_name"))).append('\t')
                       .append(encode(r.getString("role"))).append('\t')
                       .append(r.getInt("is_active")).append('\n');
                }
            }

            respond(ex, 200, out.toString());
            return;
        }

        // Change account status:
        // POST /api/admin/users/{id}/status
        if ("POST".equals(method)) {
            String prefix = "/api/admin/users/";
            String suffix = "/status";

            if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
                respond(ex, 404, "Unknown user-management endpoint.");
                return;
            }

            String idText = path.substring(
                    prefix.length(), path.length() - suffix.length());

            if (idText.isEmpty() || idText.contains("/")) {
                respond(ex, 400, "Invalid user ID.");
                return;
            }

            int targetId;
            try {
                targetId = Integer.parseInt(idText);
                if (targetId < 1) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                respond(ex, 400, "Invalid user ID.");
                return;
            }

            Map<String, String> fields = readForm(ex);
            String activeValue = fields.get("active");

            if (!"0".equals(activeValue) &&
                    !"1".equals(activeValue)) {
                respond(ex, 400, "Active must be 0 or 1.");
                return;
            }

            boolean activate = "1".equals(activeValue);

            try (Connection c = Database.getServerConnection()) {
                c.setAutoCommit(false);

                try {
                    String targetRole;
                    int currentActive;

                    try (PreparedStatement p = c.prepareStatement(
                            "SELECT role, is_active FROM users WHERE id=?")) {
                        p.setInt(1, targetId);

                        try (ResultSet r = p.executeQuery()) {
                            if (!r.next()) {
                                c.rollback();
                                respond(ex, 404, "User not found.");
                                return;
                            }

                            targetRole = r.getString("role");
                            currentActive = r.getInt("is_active");
                        }
                    }

                    if (!activate && targetId == session.userId) {
                        c.rollback();
                        respond(ex, 400,
                                "You cannot deactivate your own account.");
                        return;
                    }

                    if (!activate && currentActive == 1 &&
                            "ADMIN".equals(targetRole)) {

                        try (PreparedStatement p = c.prepareStatement(
                                "SELECT COUNT(*) FROM users " +
                                "WHERE role='ADMIN' AND is_active=1")) {

                            try (ResultSet r = p.executeQuery()) {
                                if (r.next() && r.getInt(1) <= 1) {
                                    c.rollback();
                                    respond(ex, 400,
                                            "Cannot deactivate the last active admin.");
                                    return;
                                }
                            }
                        }
                    }

                    try (PreparedStatement p = c.prepareStatement(
                            "UPDATE users SET is_active=? WHERE id=?")) {
                        p.setInt(1, activate ? 1 : 0);
                        p.setInt(2, targetId);
                        p.executeUpdate();
                    }

                    c.commit();

                } catch (Exception e) {
                    try {
                        c.rollback();
                    } catch (SQLException ignored) {
                    }
                    throw e;
                } finally {
                    c.setAutoCommit(true);
                }
            }

            // Deactivation also revokes existing login sessions.
            if (!activate) {
                SESSIONS.entrySet().removeIf(entry ->
                        entry.getValue().userId == targetId);
            }

            respond(ex, 200,
                    activate ? "Account reactivated."
                             : "Account deactivated.");
            return;
        }

        respond(ex, 405, "Method not allowed.");

    } catch (Exception e) {
        e.printStackTrace();
        safeRespond(ex, 500, "User management failed.");
    } finally {
        ex.close();
    }
}


private static void ensureUserActiveColumn() throws SQLException {
    try (Connection c = Database.getServerConnection();
         Statement s = c.createStatement();
         ResultSet r = s.executeQuery("PRAGMA table_info(users)")) {

        while (r.next()) {
            if ("is_active".equalsIgnoreCase(r.getString("name"))) {
                return;
            }
        }
    }

    try (Connection c = Database.getServerConnection();
         Statement s = c.createStatement()) {
        s.executeUpdate(
                "ALTER TABLE users ADD COLUMN " +
                "is_active INTEGER NOT NULL DEFAULT 1");
    }
}
private static void handleAdminMonitoring(HttpExchange ex)
        throws IOException {
    try {
        if (!"GET".equals(ex.getRequestMethod())) {
            respond(ex, 405, "Method not allowed");
            return;
        }

        Session session = requireSession(ex);
        if (session == null) {
            return;
        }

        if (!session.isAdmin()) {
            respond(ex, 403, "Administrator access required.");
            return;
        }

        StringBuilder out = new StringBuilder();

        out.append("server_status\t")
                .append(encode("Running")).append('\n');

        out.append("uptime_ms\t")
                .append(System.currentTimeMillis() - SERVER_STARTED_AT)
                .append('\n');

        try (Connection c = Database.getServerConnection();
             Statement s = c.createStatement()) {

            out.append("database_status\t")
                    .append(encode("Connected")).append('\n');

            try (ResultSet r = s.executeQuery(
                    "SELECT COUNT(*) AS total, " +
                    "SUM(CASE WHEN is_active=1 THEN 1 ELSE 0 END) AS active, " +
                    "SUM(CASE WHEN is_active=0 THEN 1 ELSE 0 END) AS inactive, " +
                    "SUM(CASE WHEN role='ADMIN' THEN 1 ELSE 0 END) AS admins " +
                    "FROM users")) {

                if (r.next()) {
                    out.append("users_total\t")
                            .append(r.getInt("total")).append('\n');
                    out.append("users_active\t")
                            .append(r.getInt("active")).append('\n');
                    out.append("users_inactive\t")
                            .append(r.getInt("inactive")).append('\n');
                    out.append("users_admins\t")
                            .append(r.getInt("admins")).append('\n');
                }
            }

            String[] tables = {
                    "subjects", "grades", "assessments",
                    "attendance", "forum_posts", "holidays"
            };

            for (String table : tables) {
                try (Statement countStatement = c.createStatement();
                     ResultSet r = countStatement.executeQuery(
                             "SELECT COUNT(*) FROM \"" + table + "\"")) {

                    if (r.next()) {
                        out.append("records_")
                                .append(table).append('\t')
                                .append(r.getInt(1)).append('\n');
                    }
                } catch (SQLException ignored) {
                    out.append("records_")
                            .append(table).append("\tUnavailable\n");
                }
            }
        }

        out.append("checked_at\t")
                .append(encode(
                        java.time.LocalDateTime.now().toString()))
                .append('\n');

        respond(ex, 200, out.toString());

    } catch (Exception e) {
        e.printStackTrace();
        safeRespond(ex, 500, "Monitoring check failed.");
    } finally {
        ex.close();
    }
}
private static void handleAdminBackups(HttpExchange ex)
        throws IOException {
    try {
        Session session = requireSession(ex);
        if (session == null) return;

        if (!session.isAdmin()) {
            respond(ex, 403, "Administrator access required.");
            return;
        }

        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();

        Path backupDir;

        // Find the actual database file used by this connection.
        try (Connection c = Database.getServerConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("PRAGMA database_list")) {

            String dbFile = null;

            while (r.next()) {
                if ("main".equals(r.getString("name"))) {
                    dbFile = r.getString("file");
                    break;
                }
            }

            if (dbFile == null || dbFile.isBlank()) {
                respond(ex, 500, "Could not locate the database file.");
                return;
            }

            backupDir = Path.of(dbFile).toAbsolutePath()
                    .getParent().resolve("acadex_backups");
        }

        Files.createDirectories(backupDir);

        // GET /api/admin/backups
        if ("GET".equals(method) &&
                "/api/admin/backups".equals(path)) {

            StringBuilder out = new StringBuilder();

            try (Stream<Path> files = Files.list(backupDir)) {
                for (Path file : files
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString()
                                .endsWith(".db"))
                        .sorted(Comparator.reverseOrder())
                        .toList()) {

                    out.append(encode(file.getFileName().toString()))
                       .append('\t')
                       .append(Files.getLastModifiedTime(file)
                               .toMillis())
                       .append('\t')
                       .append(Files.size(file))
                       .append('\n');
                }
            }

            respond(ex, 200, out.toString());
            return;
        }

        // POST /api/admin/backups
        if ("POST".equals(method) &&
                "/api/admin/backups".equals(path)) {

            String stamp = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));

            Path destination = backupDir.resolve(
                    "acadex_backup_" + stamp + ".db");

            try (Connection c = Database.getServerConnection();
                 PreparedStatement p = c.prepareStatement(
                         "VACUUM INTO ?")) {

                p.setString(1, destination.toString());
                p.execute();
            } catch (Exception e) {
                Files.deleteIfExists(destination);
                throw e;
            }

            respond(ex, 200,
                    "Backup created: " + destination.getFileName());
            return;
        }

        respond(ex, 404, "Unknown backup endpoint.");

    } catch (Exception e) {
        e.printStackTrace();
        safeRespond(ex, 500, "Backup operation failed.");
    } finally {
        ex.close();
    }
}
private static final long SERVER_STARTED_AT =
        System.currentTimeMillis();

private static void handleAdminAcademicSupport(HttpExchange ex)
        throws IOException {
    try {
        if (!"GET".equals(ex.getRequestMethod())) {
            respond(ex, 405, "Method not allowed");
            return;
        }

        Session session = requireSession(ex);
        if (session == null) return;

        if (!session.isAdmin()) {
            respond(ex, 403, "Administrator access required.");
            return;
        }

        String query = ex.getRequestURI().getRawQuery();
        if (query == null || !query.startsWith("userId=")) {
            respond(ex, 400, "Missing userId parameter.");
            return;
        }

        String idText = URLDecoder.decode(
                query.substring("userId=".length()),
                StandardCharsets.UTF_8);

        int userId;
        try {
            userId = Integer.parseInt(idText);
            if (userId < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            respond(ex, 400, "Invalid user ID.");
            return;
        }

        // Step 1: Confirm that the selected account exists.
        try (Connection c = Database.getServerConnection();
             PreparedStatement p = c.prepareStatement(
                     "SELECT id FROM users WHERE id=?")) {
            p.setInt(1, userId);

            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) {
                    respond(ex, 404, "User not found.");
                    return;
                }
            }
        }

        // Step 2: Check that the student's database exists
        // BEFORE opening it. Opening a missing SQLite database
        // could otherwise create a new, empty file.
        if (userId != 1) {
            File studentDatabase =
                    new File("acadex_user_" + userId + ".db");

            if (!studentDatabase.isFile()) {
                respond(ex, 404,
                        "This student's academic database has not been initialized.");
                return;
            }
        }

        // Step 3: Read the student's academic records.
        StringBuilder out = new StringBuilder();

        try (Connection c = Database.getConnectionForUser(userId)) {
            String[] tables = {
                    "subjects", "grades", "assessments",
                    "attendance", "holidays"
            };

            for (String table : tables) {
                out.append("TABLE\t").append(table).append('\n');

                try (Statement s = c.createStatement();
                     ResultSet r = s.executeQuery(
                             "SELECT * FROM \"" + table + "\"")) {

                    ResultSetMetaData meta = r.getMetaData();
                    int columns = meta.getColumnCount();

                    out.append("COLUMNS");
                    for (int i = 1; i <= columns; i++) {
                        out.append('\t')
                                .append(encode(meta.getColumnName(i)));
                    }
                    out.append('\n');

                    while (r.next()) {
                        out.append("ROW");
                        for (int i = 1; i <= columns; i++) {
                            String value = r.getString(i);
                            out.append('\t')
                                    .append(encode(
                                            value == null ? "" : value));
                        }
                        out.append('\n');
                    }
                } catch (SQLException e) {
                    out.append("UNAVAILABLE\t")
                            .append(encode(e.getMessage() == null
                                    ? "Table unavailable"
                                    : e.getMessage()))
                            .append('\n');
                }
            }
        }

        respond(ex, 200, out.toString());

    } catch (Exception e) {
        e.printStackTrace();
        safeRespond(ex, 500, "Academic support request failed.");
    } finally {
        ex.close();
    }
}

}

