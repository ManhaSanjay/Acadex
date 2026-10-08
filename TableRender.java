import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class TableRender extends JFrame {

    JTable table;

    public TableRender() {

        setTitle("Grade Report");
        setSize(700, 400);
        setDefaultCloseOperation(
                JFrame.DISPOSE_ON_CLOSE
        );
        setLocationRelativeTo(null);

        String[] columns = {
                "Subject ID",
                "Internal Marks",
                "Grade",
                "External Required"
        };

        DefaultTableModel model =
                new DefaultTableModel(
                        columns,
                        0
                );

        table = new JTable(model);

        JScrollPane scrollPane =
                new JScrollPane(table);

        add(
                scrollPane,
                BorderLayout.CENTER
        );
    }

    public TableRender(
            String[] subjectId,
            double[] internalMarks,
            String[] grades,
            double[] externalRequired) {

        setTitle("Grade Report");
        setSize(700, 400);
        setDefaultCloseOperation(
                JFrame.DISPOSE_ON_CLOSE
        );
        setLocationRelativeTo(null);

        String[] columns = {
                "Subject ID",
                "Internal Marks",
                "Grade",
                "External Required"
        };

        DefaultTableModel model =
                new DefaultTableModel(
                        columns,
                        0
                );

        for (int i = 0;
             i < subjectId.length;
             i++) {

            Object[] row = {
                    subjectId[i],
                    internalMarks[i],
                    grades[i],
                    externalRequired[i]
            };

            model.addRow(row);
        }

        table = new JTable(model);

        JScrollPane scrollPane =
                new JScrollPane(table);

        add(
                scrollPane,
                BorderLayout.CENTER
        );

        setVisible(true);
    }
}