import java.sql.*;
import java.util.*;

public class SubjectDAO {
    public List<String[]> all() throws SQLException {List<String[]> out=new ArrayList<>();try(Connection c=Database.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT id,code,name,credits FROM subjects ORDER BY code")){while(r.next())out.add(new String[]{r.getString(1),r.getString(2),r.getString(3),r.getString(4)});}return out;}
    public int add(String code,String name,int credits) throws SQLException {try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO subjects(code,name,credits) VALUES(?,?,?)")){p.setString(1,code);p.setString(2,name);p.setInt(3,credits);p.executeUpdate();}try(Connection c=Database.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT id FROM subjects WHERE code='"+code.replace("'","''")+"'")){if(!r.next())throw new SQLException("Subject was not created.");int id=r.getInt(1);try(PreparedStatement g=c.prepareStatement("INSERT INTO grades(subject_id) VALUES(?)")){g.setInt(1,id);g.executeUpdate();}try(PreparedStatement a=c.prepareStatement("INSERT INTO attendance(subject_id) VALUES(?)")){a.setInt(1,id);a.executeUpdate();}return id;}}
    public void delete(int id) throws SQLException {try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("DELETE FROM subjects WHERE id=?")){p.setInt(1,id);p.executeUpdate();}}
}
