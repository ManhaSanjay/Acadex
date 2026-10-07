import java.sql.*;

public class DatabaseAtt {

    // Database details
    private static final String URL =
            "jdbc:mysql://localhost:3306/attendance_db";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    // Connect to database
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // --------------------------------------------------
    // 1. ADD SUBJECT
    // --------------------------------------------------
    public static void addSubject(String subId, String subName, int credits) {

        String sql = "INSERT INTO Subjects(sub_id, sub_name, credits) VALUES (?, ?, ?)";

        try (Connection con = getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, subId);
            pst.setString(2, subName);
            pst.setInt(3, credits);

            pst.executeUpdate();

            System.out.println("Subject added successfully.");

        } catch (SQLException e) {
            System.out.println("Error adding subject: " + e.getMessage());
        }
    }

    // --------------------------------------------------
    // 2. SAVE ATTENDANCE
    // --------------------------------------------------
    public static void saveAttendance(
            String subId, String date, int hour, String status) {

        String sql = "INSERT INTO Attendance(sub_id, date, hour, status) "
                   + "VALUES (?, ?, ?, ?)";

        try (Connection con = getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, subId);
            pst.setDate(2, Date.valueOf(date));
            pst.setInt(3, hour);
            pst.setString(4, status);

            pst.executeUpdate();

            System.out.println("Attendance saved.");

        } catch (SQLException e) {
            System.out.println("Error saving attendance: "
                    + e.getMessage());
        }
    }

    // --------------------------------------------------
    // 3. CALCULATE ATTENDANCE PERCENTAGE
    // --------------------------------------------------
    public static double calcAttPerc(String subId) {

        String sql = "SELECT COUNT(*) AS total, "
                   + "SUM(CASE WHEN status='Present' THEN 1 ELSE 0 END) "
                   + "AS present "
                   + "FROM Attendance WHERE sub_id=?";

        try (Connection con = getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, subId);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {

                int total = rs.getInt("total");
                int present = rs.getInt("present");

                if (total == 0) {
                    return 0;
                }

                return ((double) present / total) * 100;
            }

        } catch (SQLException e) {
            System.out.println("Error calculating attendance: "
                    + e.getMessage());
        }

        return 0;
    }

    // --------------------------------------------------
    // 4. CALCULATE CLASSES NEEDED TO REACH 75%
    // --------------------------------------------------
    public static int calcClassNeeded(String subId) {

        String sql = "SELECT COUNT(*) AS total, "
                   + "SUM(CASE WHEN status='Present' THEN 1 ELSE 0 END) "
                   + "AS present "
                   + "FROM Attendance WHERE sub_id=?";

        try (Connection con = getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, subId);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {

                int total = rs.getInt("total");
                int present = rs.getInt("present");

                // Already satisfies 75%
                if (total == 0 || ((double) present / total) * 100 >= 75) {
                    return 0;
                }

                // Find number of consecutive classes needed
                int needed = 0;

                while (((double) (present + needed)
                        / (total + needed)) * 100 < 75) {

                    needed++;
                }

                return needed;
            }

        } catch (SQLException e) {
            System.out.println("Error calculating classes needed: "
                    + e.getMessage());
        }

        return 0;
    }

    // --------------------------------------------------
    // 5. GET SUBJECT DETAILS
    // --------------------------------------------------
    public static void getSubjects() {

        String sql = "SELECT * FROM Subjects";

        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {

                System.out.println(
                        "ID: " + rs.getString("sub_id"));

                System.out.println(
                        "Subject: " + rs.getString("sub_name"));

                System.out.println(
                        "Credits: " + rs.getInt("credits"));

                System.out.println(
                        "Attendance: "
                        + calcAttPerc(rs.getString("sub_id"))
                        + "%");

                System.out.println("----------------------");
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving subjects: "
                    + e.getMessage());
        }
    }
}
