public class User {

    private int userId;
    private String username;
    private String passwordHash;
    private String role;
    private String semester;

    public User(
            int userId,
            String username,
            String passwordHash,
            String role,
            String semester) {

        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.semester = semester;
    }

    public User(
            String username,
            String passwordHash,
            String role,
            String semester) {

        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.semester = semester;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    public String getSemester() {
        return semester;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }
}