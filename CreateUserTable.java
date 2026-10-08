import java.sql.Connection;
import java.sql.Statement;

public class CreateUserTable {

    public static void createTable() {

        String sql =
                "CREATE TABLE IF NOT EXISTS users (" +
                "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password_hash TEXT NOT NULL, " +
                "role TEXT NOT NULL, " +
                "semester TEXT)";

        try (
                Connection connection =
                        DatabaseManager.connect();

                Statement statement =
                        connection.createStatement()
        ) {

            statement.execute(sql);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}