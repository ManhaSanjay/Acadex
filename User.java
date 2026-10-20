
public class User {
    private final int id;
    private final String username;
    private final String displayName;
    private final String role;
    private final String sessionToken;

    public User(int id, String username, String displayName, String role) {
        this(id, username, displayName, role, "");
    }

    public User(int id, String username, String displayName,
                String role, String sessionToken) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
        this.sessionToken = sessionToken;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
    public String getSessionToken() { return sessionToken; }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}

