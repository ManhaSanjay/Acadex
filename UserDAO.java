import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDAO {

    public boolean addUser(User user) {

        String sql =
                "INSERT INTO users " +
                "(username, password_hash, role, semester) " +
                "VALUES (?, ?, ?, ?)";

        try (
                Connection connection =
                        DatabaseManager.connect();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    user.getUsername()
            );

            statement.setString(
                    2,
                    user.getPasswordHash()
            );

            statement.setString(
                    3,
                    user.getRole()
            );

            statement.setString(
                    4,
                    user.getSemester()
            );

            statement.executeUpdate();

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    public User getUser(String username) {

        String sql =
                "SELECT user_id, username, password_hash, " +
                "role, semester " +
                "FROM users WHERE username = ?";

        try (
                Connection connection =
                        DatabaseManager.connect();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, username);

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {

                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("role"),
                            rs.getString("semester")
                    );
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return null;
    }

    public boolean deleteUser(int userId) {

        String sql =
                "DELETE FROM users WHERE user_id = ?";

        try (
                Connection connection =
                        DatabaseManager.connect();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    userId
            );

            statement.executeUpdate();

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }
}