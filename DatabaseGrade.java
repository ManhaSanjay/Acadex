
CREATE TABLE grades (
    subject TEXT PRIMARY KEY,
    series1 REAL,
    series2 REAL,
    semester_exam REAL
);



import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private static final String URL = "jdbc:sqlite:grades.db";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);}}


import java.sql.Connection;
import java.sql.Statement;

public class CreateTable {

    public static void createTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS grades (
                subject TEXT PRIMARY KEY,
                series1 REAL,
                series2 REAL,
                semester_exam REAL
            );
            """;

        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);
            System.out.println("Table created successfully.");

        } catch (Exception e) {
            e.printStackTrace();}}}


import java.sql.Connection;
import java.sql.PreparedStatement;

public class GradeDAO {

    public static void saveGrade(String subject,
                                 double series1,
                                 double series2,
                                 double semesterExam) {

        String sql = """
            INSERT OR REPLACE INTO grades
            (subject, series1, series2, semester_exam)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, subject);
            pstmt.setDouble(2, series1);
            pstmt.setDouble(3, series2);
            pstmt.setDouble(4, semesterExam);

            pstmt.executeUpdate();

            System.out.println("Grade saved successfully.");

        } catch (Exception e) {
            e.printStackTrace();}}}
