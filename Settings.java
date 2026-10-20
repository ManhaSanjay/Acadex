
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;

public class Settings extends JPanel {

    private final SubjectDAO subjectDAO = new SubjectDAO();

    private final DefaultTableModel subjectModel =
        new DefaultTableModel(
            new String[]{"ID", "Code", "Subject Name", "Credits"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

    private final JTable subjectTable = new JTable(subjectModel);

    public Settings(User user) {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));

        // Account information
        JPanel info = new JPanel(new GridLayout(0, 1, 8, 8));
        info.setBorder(BorderFactory.createTitledBorder("♡ Account"));

        info.add(new JLabel("Name: " + user.getDisplayName()));
        info.add(new JLabel("Username: " + user.getUsername()));
        info.add(new JLabel("Role: " + user.getRole()));
        info.add(new JLabel(
            "Database: " + Paths.get("acadex.db").toAbsolutePath()
        ));

        // Subject management
        JPanel subjects = new JPanel(new BorderLayout(10, 10));
        subjects.setBorder(
            BorderFactory.createTitledBorder("📚 Subject Management")
        );

        subjectTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );
        subjectTable.setRowHeight(28);
        subjectTable.setFillsViewportHeight(true);

        // Hide the internal database ID column
        subjectTable.removeColumn(
            subjectTable.getColumnModel().getColumn(0)
        );

        subjects.add(new JScrollPane(subjectTable), BorderLayout.CENTER);

        JPanel subjectButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton refresh = new JButton("↻ Refresh");
        JButton delete = new JButton("🗑 Delete Subject");

        delete.setForeground(new Color(180, 55, 65));

        refresh.addActionListener(e -> loadSubjects());
        delete.addActionListener(e -> deleteSelectedSubject());

        subjectButtons.add(refresh);
        subjectButtons.add(delete);

        subjects.add(subjectButtons, BorderLayout.SOUTH);

        // Database backup
        JPanel backupPanel = new JPanel();
        backupPanel.setLayout(new BoxLayout(backupPanel, BoxLayout.Y_AXIS));
        backupPanel.setBorder(
            BorderFactory.createTitledBorder("Database Backup")
        );

        JButton backup = new JButton("Create database backup");
        backup.setAlignmentX(Component.LEFT_ALIGNMENT);

        backup.addActionListener(e -> {
            try {
                JOptionPane.showMessageDialog(
                    this,
                    "Backup created:\n" + BackupService.backup(),
                    "Backup successful",
                    JOptionPane.INFORMATION_MESSAGE
                );
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Backup failed",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });

        backupPanel.add(backup);
        backupPanel.add(Box.createVerticalStrut(8));

        JLabel note = new JLabel(
            "Tip: close Acadex before manually copying acadex.db."
        );
        note.setForeground(new Color(90, 90, 90));
        backupPanel.add(note);

        add(info, BorderLayout.NORTH);
        add(subjects, BorderLayout.CENTER);
        add(backupPanel, BorderLayout.SOUTH);

        loadSubjects();
    }

    private void loadSubjects() {
        subjectModel.setRowCount(0);

        try {
            List<String[]> subjects = subjectDAO.all();

            for (String[] subject : subjects) {
                subjectModel.addRow(subject);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Could not load subjects:\n" + ex.getMessage(),
                "Database error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void deleteSelectedSubject() {
        int selectedRow = subjectTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a subject first! ♡",
                "No subject selected",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Column 0 in the model contains the hidden database ID.
        int id = Integer.parseInt(
            subjectModel.getValueAt(selectedRow, 0).toString()
        );

        String code = subjectModel.getValueAt(selectedRow, 1).toString();
        String name = subjectModel.getValueAt(selectedRow, 2).toString();

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Delete " + code + " — " + name + "?\n\n"
                + "This may affect associated grades, attendance "
                + "and timetable records.\n\n"
                + "This action may not be reversible.",
            "Confirm subject deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            subjectDAO.delete(id);

            JOptionPane.showMessageDialog(
                this,
                "Subject deleted successfully! ♡",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            );

            loadSubjects();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Could not delete the subject.\n\n"
                    + "Related records may prevent deletion.\n\n"
                    + ex.getMessage(),
                "Deletion failed",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}

