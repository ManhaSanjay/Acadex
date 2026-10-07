import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SubjectDAO {

    private static final String URL = "jdbc:sqlite:grades.db";
  
    public void addSubject(Subject subject) {

        String sql = "INSERT INTO subjects " +
                     "(subId, subjectName, series1, series2, semesterExam) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DriverManager.getConnection(URL);
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, subject.getSubId());
            ps.setString(2, subject.getSubjectName());
            ps.setDouble(3, subject.getSeries1());
            ps.setDouble(4, subject.getSeries2());
            ps.setDouble(5, subject.getSemesterExam());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Get all subjects
    public List<Subject> getSubjects() {

        List<Subject> subjects = new ArrayList<>();

        String sql = "SELECT * FROM subjects";

        try (Connection con = DriverManager.getConnection(URL);
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {

                Subject subject = new Subject();

                subject.setSubId(rs.getInt("subId"));
                subject.setSubjectName(rs.getString("subjectName"));
                subject.setSeries1(rs.getDouble("series1"));
                subject.setSeries2(rs.getDouble("series2"));
                subject.setSemesterExam(rs.getDouble("semesterExam"));

                subjects.add(subject);
            }

        } catch (SQLException e) {
            e.printStackTrace();}
        return subjects;}

    public void updateSubject(Subject subject) {

        String sql = "UPDATE subjects SET " +
                     "subjectName = ?, series1 = ?, series2 = ?, semesterExam = ? " +
                     "WHERE subId = ?";

        try (Connection con = DriverManager.getConnection(URL);
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, subject.getSubjectName());
            ps.setDouble(2, subject.getSeries1());
            ps.setDouble(3, subject.getSeries2());
            ps.setDouble(4, subject.getSemesterExam());
            ps.setInt(5, subject.getSubId());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();}}
  
    public void deleteSubject(int subId) {

        String sql = "DELETE FROM subjects WHERE subId = ?";

        try (Connection con = DriverManager.getConnection(URL);
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, subId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();}}}
