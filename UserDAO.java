
import java.sql.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class UserDAO {
    private static final String SERVER = "http://192.168.1.14:8080";

    // Existing local authentication method retained for compatibility.
    public User authenticate(String username, String password)
            throws SQLException {
        String sql = "SELECT id,username,display_name,role FROM users " +
                     "WHERE username=? AND password_hash=?";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, username.trim());
            p.setString(2, PasswordUtil.hash(password));
            try (ResultSet r = p.executeQuery()) {
                return r.next()
                        ? new User(r.getInt("id"), r.getString("username"),
                            r.getString("display_name"), r.getString("role"))
                        : null;
            }
        }
    }

    public User loginRemote(String username, String password)
            throws IOException {
        String response = sendForm(
                "/api/auth/login",
                "username", username,
                "password", password
        );

        String[] fields = response.split("\t", -1);
        if (fields.length != 6 || !"OK".equals(fields[0])) {
            throw new IOException("Unexpected server login response.");
        }

        try {
            int id = Integer.parseInt(fields[1]);
            String actualUsername = decode(fields[2]);
            String displayName = decode(fields[3]);
            String role = decode(fields[4]);
            String token = fields[5];

            return new User(id, actualUsername, displayName, role, token);
        } catch (IllegalArgumentException e) {
            throw new IOException("Could not read the server login response.");
        }
    }

    public String registerRemote(String username, String displayName,
                                 String password) throws IOException {
        return sendForm(
                "/api/auth/register",
                "username", username,
                "displayName", displayName,
                "password", password
        );
    }

    public void logoutRemote(User user) {
        if (user == null || user.getSessionToken().isEmpty()) return;
        try {
            HttpURLConnection conn = open("/api/auth/logout", "POST", user);
            conn.getResponseCode();
            conn.disconnect();
        } catch (IOException ignored) {
            // The server may already have restarted or the connection may be gone.
        }
    }

    private String sendForm(String endpoint, String... fields)
            throws IOException {
        URL url = new URL(SERVER + endpoint);
        HttpURLConnection conn =
                (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setDoOutput(true);
        conn.setRequestProperty(
                "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        StringBuilder body = new StringBuilder();
        for (int i = 0; i < fields.length; i += 2) {
            if (i > 0) body.append('&');
            body.append(URLEncoder.encode(fields[i], StandardCharsets.UTF_8));
            body.append('=');
            body.append(URLEncoder.encode(fields[i + 1], StandardCharsets.UTF_8));
        }

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream stream = status >= 400
                ? conn.getErrorStream() : conn.getInputStream();

        String response = "";
        if (stream != null) {
            try (InputStream in = stream) {
                response = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        conn.disconnect();

        if (status < 200 || status >= 300) {
            throw new IOException(response.isBlank()
                    ? "Server request failed (HTTP " + status + ")."
                    : response);
        }
        return response;
    }

    private HttpURLConnection open(String endpoint, String method, User user)
            throws IOException {
        HttpURLConnection conn = (HttpURLConnection)
                new URL(SERVER + endpoint).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty(
                "Authorization", "Bearer " + user.getSessionToken());
        return conn;
    }

    private String decode(String value) {
        return new String(
                Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    public List<String[]> allUsers() throws SQLException {
        List<String[]> out = new ArrayList<>();
        try (Connection c = Database.getConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(
                     "SELECT id,username,display_name,role FROM users ORDER BY id")) {
            while (r.next()) {
                out.add(new String[]{
                    r.getString(1), r.getString(2),
                    r.getString(3), r.getString(4)
                });
            }
        }
        return out;
    }

    public void addUser(String username, String password,
                        String name, String role) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "INSERT INTO users(username,password_hash,display_name,role) " +
                     "VALUES(?,?,?,?)")) {
            p.setString(1, username.trim());
            p.setString(2, PasswordUtil.hash(password));
            p.setString(3, name.trim());
            p.setString(4, role);
            p.executeUpdate();
        }
    }

    public void deleteUser(int id) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "DELETE FROM users WHERE id=? AND username<>'admin'")) {
            p.setInt(1, id);
            p.executeUpdate();
        }
    }
}

