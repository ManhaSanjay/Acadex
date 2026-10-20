
import java.sql.*;

public class FixDatabase {
    public static void main(String[] args) throws Exception {
        try (Connection c = Database.getConnection();
             Statement s = c.createStatement()) {

            addColumnIfMissing(c, "attendance",
                "attendance_date", "TEXT");

            addColumnIfMissing(c, "attendance",
                "hour", "INTEGER");

            addColumnIfMissing(c, "attendance",
                "present", "INTEGER");

            System.out.println(
                "Attendance database columns checked successfully."
            );
        }
    }

    private static void addColumnIfMissing(
            Connection c, String table, String column, String type)
            throws SQLException {

        boolean exists = false;

        try (Statement s = c.createStatement();
             ResultSet r = s.executeQuery(
                 "PRAGMA table_info(" + table + ")")) {

            while (r.next()) {
                if (column.equalsIgnoreCase(r.getString("name"))) {
                    exists = true;
                    break;
                }
            }
        }

        if (!exists) {
            try (Statement s = c.createStatement()) {
                s.execute("ALTER TABLE " + table
                    + " ADD COLUMN " + column + " " + type);
                System.out.println("Added column: " + column);
            }
        }
    }
}

