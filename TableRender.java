import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class TableRender {
    private TableRender() {}
    public static JTable table(String[] columns, List<String[]> rows) {
        DefaultTableModel model=new DefaultTableModel(columns,0){@Override public boolean isCellEditable(int r,int c){return false;}};
        for(String[] row:rows)model.addRow(row);
        JTable table=new JTable(model);table.setRowHeight(25);table.setAutoCreateRowSorter(true);return table;
    }
    public static String selectedId(JTable table){int row=table.getSelectedRow();if(row<0)return null;return String.valueOf(table.getValueAt(table.convertRowIndexToModel(row),0));}
}
