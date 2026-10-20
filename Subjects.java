import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.*;

public class Subjects extends JPanel {
    private final boolean editable; private final JTable table=new JTable();
    public Subjects(boolean editable){this.editable=editable;setLayout(new BorderLayout(8,8));setBorder(BorderFactory.createEmptyBorder(12,12,12,12));JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT));JButton refresh=new JButton("Refresh");refresh.addActionListener(e->load());buttons.add(refresh);if(editable){JButton add=new JButton("Add subject");add.addActionListener(e->add());JButton del=new JButton("Delete selected");del.addActionListener(e->delete());buttons.add(add);buttons.add(del);}add(buttons,BorderLayout.NORTH);add(new JScrollPane(table),BorderLayout.CENTER);load();}
    private void load(){try{java.util.List<String[]> rows=new ArrayList<>();try(Connection c=Database.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT id,code,name,credits FROM subjects ORDER BY code")){while(r.next())rows.add(new String[]{r.getString(1),r.getString(2),r.getString(3),r.getString(4)});}replace(new String[]{"ID","Code","Subject","Credits"},rows);}catch(Exception e){error(e);}}
    private void replace(String[] cols,java.util.List<String[]> rows){JTable n=TableRender.table(cols,rows);remove(1);add(new JScrollPane(n),BorderLayout.CENTER);revalidate();repaint();}
    private void add(){JTextField code=new JTextField(),name=new JTextField(),credits=new JTextField("3");Object[] f={"Subject code",code,"Subject name",name,"Credits",credits};if(JOptionPane.showConfirmDialog(this,f,"Add subject",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;try{int cr=Integer.parseInt(credits.getText().trim());if(code.getText().isBlank()||name.getText().isBlank())throw new IllegalArgumentException("Code and name are required.");if(cr<1||cr>10)throw new IllegalArgumentException("Credits must be between 1 and 10.");new SubjectDAO().add(code.getText().trim(),name.getText().trim(),cr);load();}catch(Exception e){error(e);}}
    private void delete(){JTable t=(JTable)((JScrollPane)getComponent(1)).getViewport().getView();String id=TableRender.selectedId(t);if(id==null){JOptionPane.showMessageDialog(this,"Select a subject first.");return;}if(JOptionPane.showConfirmDialog(this,"Delete this subject and its related records?","Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("DELETE FROM subjects WHERE id=?")){p.setInt(1,Integer.parseInt(id));p.executeUpdate();load();}catch(Exception e){error(e);}}
    private void error(Exception e){JOptionPane.showMessageDialog(this,e.getMessage(),"Subjects",JOptionPane.ERROR_MESSAGE);}
}
