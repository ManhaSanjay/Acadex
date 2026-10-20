import java.sql.*;
import java.util.*;

public class DatabaseTimetable {
    public List<String[]> all() throws SQLException {
        List<String[]> out=new ArrayList<>();String sql="SELECT t.id,t.day,t.period,s.code,s.name,t.start_time,t.end_time,t.room FROM timetable t LEFT JOIN subjects s ON s.id=t.subject_id ORDER BY CASE t.day WHEN 'Monday' THEN 1 WHEN 'Tuesday' THEN 2 WHEN 'Wednesday' THEN 3 WHEN 'Thursday' THEN 4 WHEN 'Friday' THEN 5 WHEN 'Saturday' THEN 6 ELSE 7 END,t.period";
        try(Connection c=Database.getConnection();Statement st=c.createStatement();ResultSet r=st.executeQuery(sql)){while(r.next())out.add(new String[]{r.getString(1),r.getString(2),r.getString(3),r.getString(4)==null?"Free period":r.getString(4),r.getString(5)==null?"":r.getString(5),r.getString(6),r.getString(7),r.getString(8)});}return out;
    }
    public void add(String day,int period,Integer subjectId,String start,String end,String room) throws SQLException {
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO timetable(day,period,subject_id,start_time,end_time,room) VALUES(?,?,?,?,?,?)")){p.setString(1,day);p.setInt(2,period);if(subjectId==null)p.setNull(3,Types.INTEGER);else p.setInt(3,subjectId);p.setString(4,start);p.setString(5,end);p.setString(6,room);p.executeUpdate();}
    }
    public void delete(int id) throws SQLException {try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("DELETE FROM timetable WHERE id=?")){p.setInt(1,id);p.executeUpdate();}}
}
