
import java.sql.*;
import java.util.*;

/**
 * DAO for assessment records.
 * Class name preserves the original project spelling.
 */
public class AssesmentsDAO {

    /**
     * Returns all assessments with their subject details.
     */
    public List<String[]> all() throws SQLException {
        List<String[]> out = new ArrayList<>();

        String sql = """
            SELECT a.id, s.code, s.name, a.title,
                   a.max_marks, a.marks_obtained, a.due_date
            FROM assessments a
            JOIN subjects s ON s.id = a.subject_id
            ORDER BY a.id DESC
            """;

        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet r = st.executeQuery(sql)) {

            while (r.next()) {
                out.add(new String[]{
                    r.getString(1),
                    r.getString(2),
                    r.getString(3),
                    r.getString(4),
                    r.getString(5),
                    r.getString(6),
                    r.getString(7)
                });
            }
        }

        return out;
    }

    /**
     * Adds an assessment.
     * If marks are invalid or non-finite, defaults to maximum marks.
     */
    public void add(
            int subjectId,
            String title,
            double max,
            double marks,
            String due) throws SQLException {

        if (!Double.isFinite(max) || max <= 0) {
            throw new IllegalArgumentException(
                "Maximum marks must be a positive number."
            );
        }

        // Default to full marks if the supplied value is invalid.
        if (!Double.isFinite(marks) || marks < 0 || marks > max) {
            marks = max;
        }

        String sql = """
            INSERT INTO assessments
                (subject_id, title, max_marks, marks_obtained, due_date)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, subjectId);
            p.setString(2, title);
            p.setDouble(3, max);
            p.setDouble(4, marks);

            if (due == null || due.isBlank()) {
                p.setNull(5, Types.VARCHAR);
            } else {
                p.setString(5, due.trim());
            }

            p.executeUpdate();
        }
    }

    /**
     * Deletes an assessment by ID.
     */
    public void delete(int id) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                 "DELETE FROM assessments WHERE id = ?")) {

            p.setInt(1, id);
            p.executeUpdate();
        }
    }
}

