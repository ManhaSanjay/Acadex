
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Mainframe extends JFrame {

    private static final String FORUM_SERVER = "http://192.168.1.14:8080";

    private final Color bg = new Color(245, 247, 250);
    private final Color navy = new Color(32, 43, 65);
    private final Color blue = new Color(79, 110, 220);
    private final Color ink = new Color(45, 52, 66);
    private final JPanel content = new JPanel(new BorderLayout());
    private final User currentUser;

    private record Subject(int id, String code, String name, int credits) {
        @Override
        public String toString() {
            return name;
        }
    }

    private record Assessment(
            int id, int subjectId, String title,
            double max, Double marks, String due) {
        @Override
        public String toString() {
            return title;
        }
    }

    private static final String[] GRADES =
            {"S", "A+", "A", "B+", "B", "C", "D", "E", "F"};

    private static final int[] MIN_PERCENT =
            {90, 80, 70, 60, 50, 40, 30, 20, 0};

    private static final int[] GRADE_POINTS =
            {10, 9, 8, 7, 6, 5, 4, 3, 0};

    public Mainframe() {
        this(null);
    }

    public Mainframe(User user) {
    if (user == null) {
        throw new IllegalArgumentException(
                "A logged-in user is required.");
    }

    currentUser = user;

    try {
        Database.setActiveUserId(user.getId());
        Database.initialize();
        ensureTimetableSchema();
        ensureDefaultAssessmentsForExistingSubjects();
    } catch (Exception e) {
        JOptionPane.showMessageDialog(
                this,
                "Could not initialize your academic data:\n"
                        + e.getMessage(),
                "Acadex",
                JOptionPane.ERROR_MESSAGE
        );
    }

        setTitle("Acadex");
        setSize(1200, 760);
        setMinimumSize(new Dimension(950, 620));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(bg);



        setLayout(new BorderLayout());
        add(createSidebar(), BorderLayout.WEST);

        content.setBackground(bg);
        content.setBorder(new EmptyBorder(24, 28, 24, 28));
        add(content, BorderLayout.CENTER);

        showDashboard();
    }

    private JPanel createSidebar() {
        JPanel p = new JPanel();
        p.setBackground(navy);
        p.setPreferredSize(new Dimension(200, 0));
        p.setBorder(new EmptyBorder(24, 14, 20, 14));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("acadex.");
        logo.setFont(new Font("SansSerif", Font.BOLD, 29));
        logo.setForeground(Color.WHITE);
        logo.setBorder(new EmptyBorder(0, 10, 32, 0));
        p.add(logo);

        nav(p, "Dashboard", this::showDashboard);
        nav(p, "Timetable", this::showTimetable);
        nav(p, "Grades", this::showGrades);
        nav(p, "Settings", this::showSettings);
        nav(p, "Community", this::showCommunity);

        if (currentUser != null && currentUser.isAdmin()) {
    nav(p, "Admin", this::showAdmin);
}


        // Logout / switch account
        nav(p, "Log out / Switch account", () -> {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Do you want to log out and switch accounts?",
                    "Log out",
                    JOptionPane.YES_NO_OPTION
            );

            if (choice == JOptionPane.YES_OPTION) {
                new UserDAO().logoutRemote(currentUser);
                dispose();
                new LoginFrame().setVisible(true);
            }
        });




p.add(Box.createVerticalGlue());

        JLabel footer = new JLabel("✨Student Management System✨");
        footer.setForeground(new Color(174, 185, 207));
        footer.setBorder(new EmptyBorder(0, 10, 0, 0));
        p.add(footer);

        return p;
    }

    private void nav(JPanel p, String title, Runnable action) {
        JButton b = new JButton(title);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        b.setForeground(Color.WHITE);
        b.setBackground(navy);
        b.setBorder(new EmptyBorder(8, 12, 8, 8));
        b.setFocusPainted(false);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.addActionListener(e -> action.run());
        p.add(b);
    }

    private void render(String title, JComponent panel) {
        content.removeAll();

        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setBackground(bg);

        JLabel heading = new JLabel(title);
        heading.setFont(new Font("SansSerif", Font.BOLD, 27));
        heading.setForeground(ink);

        page.add(heading, BorderLayout.NORTH);
        page.add(panel, BorderLayout.CENTER);

        content.add(page);
        content.revalidate();
        content.repaint();
    }

    private JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(
                new LineBorder(new Color(230, 233, 240), 1, true),
                new EmptyBorder(16, 18, 16, 18)));
        return p;
    }

    private JLabel label(String s, int size, boolean bold) {
        JLabel l = new JLabel(s);
        l.setFont(new Font(
                "SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(ink);
        return l;
    }

    private String displayName() {
        return currentUser == null
                ? "Student"
                : currentUser.getDisplayName();
    }

    private List<Subject> subjects() throws SQLException {
        List<Subject> out = new ArrayList<>();

        String sql =
                "SELECT id,code,name,credits FROM subjects ORDER BY name";

        try (Connection c = Database.getConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) {

            while (r.next()) {
                out.add(new Subject(
                        r.getInt("id"),
                        r.getString("code"),
                        r.getString("name"),
                        r.getInt("credits")));
            }
        }

        return out;
    }

    private List<Assessment> assessments(int subjectId)
            throws SQLException {

        List<Assessment> out = new ArrayList<>();

        String sql =
                "SELECT id,subject_id,title,max_marks," +
                "marks_obtained,due_date FROM assessments " +
                "WHERE subject_id=? ORDER BY id";

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, subjectId);

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    double raw = r.getDouble("marks_obtained");
                    Double marks = r.wasNull() ? null : raw;

                    out.add(new Assessment(
                            r.getInt("id"),
                            r.getInt("subject_id"),
                            r.getString("title"),
                            r.getDouble("max_marks"),
                            marks,
                            r.getString("due_date")));
                }
            }
        }

        return out;
    }

    private double[] markSplit(int subjectId) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "SELECT cie_max, ese_max FROM subjects WHERE id=?")) {

            p.setInt(1, subjectId);

            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    return new double[]{
                            r.getDouble("cie_max"),
                            r.getDouble("ese_max")
                    };
                }
            }
        }

        throw new SQLException("Subject mark split not found.");
    }

    private void error(Exception e) {
        JOptionPane.showMessageDialog(
                this, e.getMessage(), "Acadex",
                JOptionPane.ERROR_MESSAGE);
    }

    // ---------------- TIMETABLE / DEFAULT ASSESSMENTS ----------------

    private void ensureTimetableSchema() throws SQLException {
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement()) {

            boolean exists = false;

            try (ResultSet rs = st.executeQuery(
                    "SELECT 1 FROM sqlite_master " +
                    "WHERE type='table' AND name='timetable'")) {
                exists = rs.next();
            }

            if (exists) {
                Set<String> columns = new HashSet<>();

                try (ResultSet rs =
                             st.executeQuery("PRAGMA table_info(timetable)")) {
                    while (rs.next()) {
                        columns.add(
                                rs.getString("name").toLowerCase(Locale.ROOT));
                    }
                }

                boolean compatible =
                        columns.contains("day_of_week")
                        && columns.contains("hour")
                        && columns.contains("subject_id");

                if (!compatible) {
                    String legacyName =
                            "timetable_legacy_" + System.currentTimeMillis();

                    st.executeUpdate(
                            "ALTER TABLE timetable RENAME TO " + legacyName);
                }
            }

            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS timetable (" +
                    "day_of_week INTEGER NOT NULL, " +
                    "hour INTEGER NOT NULL, " +
                    "subject_id INTEGER NULL, " +
                    "PRIMARY KEY(day_of_week,hour), " +
                    "FOREIGN KEY(subject_id) REFERENCES subjects(id) " +
                    "ON DELETE SET NULL)");

            try (PreparedStatement q = c.prepareStatement(
                    "INSERT OR IGNORE INTO timetable " +
                    "(day_of_week,hour,subject_id) VALUES(?,?,NULL)")) {

                for (int day = 1; day <= 5; day++) {
                    for (int hour = 1; hour <= 6; hour++) {
                        q.setInt(1, day);
                        q.setInt(2, hour);
                        q.addBatch();
                    }
                }

                q.executeBatch();
            }
        }
    }

    private void ensureDefaultAssessmentsForExistingSubjects()
            throws SQLException {

        String[][] defaults = {
                {"Series 1", "10"},
                {"Series 2", "10"},
                {"Attendance", "5"},
                {"Assignment", "15"}
        };

        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);

            try {
                List<Integer> ids = new ArrayList<>();

                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT id FROM subjects")) {
                    while (rs.next()) {
                        ids.add(rs.getInt(1));
                    }
                }

                for (int id : ids) {
                    for (String[] d : defaults) {
                        boolean found = false;

                        try (PreparedStatement q = c.prepareStatement(
                                "SELECT 1 FROM assessments " +
                                "WHERE subject_id=? " +
                                "AND lower(title)=lower(?) LIMIT 1")) {

                            q.setInt(1, id);
                            q.setString(2, d[0]);

                            try (ResultSet rs = q.executeQuery()) {
                                found = rs.next();
                            }
                        }

                        if (found) {
                            continue;
                        }

                        double maxMarks = Double.parseDouble(d[1]);

                        try (PreparedStatement ins = c.prepareStatement(
                                "INSERT INTO assessments " +
                                "(subject_id,title,max_marks,marks_obtained) " +
                                "VALUES (?,?,?,?)")) {

                            ins.setInt(1, id);
                            ins.setString(2, d[0]);
                            ins.setDouble(3, maxMarks);
                            ins.setDouble(4, maxMarks);
                            ins.executeUpdate();
                        }
                    }
                }

                c.commit();

            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    private static final String[] DAY_NAMES = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"
    };

    private Subject scheduledSubject(int day, int hour)
            throws SQLException {

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "SELECT s.id,s.code,s.name,s.credits " +
                     "FROM timetable t LEFT JOIN subjects s " +
                     "ON s.id=t.subject_id " +
                     "WHERE t.day_of_week=? AND t.hour=?")) {

            p.setInt(1, day);
            p.setInt(2, hour);

            try (ResultSet r = p.executeQuery()) {
                if (r.next() && r.getObject("id") != null) {
                    return new Subject(
                            r.getInt("id"),
                            r.getString("code"),
                            r.getString("name"),
                            r.getInt("credits"));
                }
            }
        }

        return null;
    }

    // ---------------- TIMETABLE ----------------

    private void showTimetable() {
        try {
            List<Subject> all = subjects();

            String[] columns = {
                    "HOUR", "MONDAY", "TUESDAY",
                    "WEDNESDAY", "THURSDAY", "FRIDAY"
            };

            DefaultTableModel model = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return col > 0;
                }
            };

            for (int hour = 1; hour <= 6; hour++) {
                Object[] row = new Object[6];
                row[0] = "Hour " + hour;

                for (int day = 1; day <= 5; day++) {
                    Subject chosen = scheduledSubject(day, hour);
                    row[day] = chosen == null ? "Free Session" : chosen;
                }

                model.addRow(row);
            }

            JTable table = new JTable(model);
            table.setRowHeight(58);
            table.setFont(new Font("SansSerif", Font.PLAIN, 13));
            table.setForeground(ink);
            table.setBackground(Color.WHITE);
            table.setGridColor(new Color(232, 235, 243));
            table.setShowGrid(true);
            table.setIntercellSpacing(new Dimension(2, 2));
            table.setFillsViewportHeight(true);
            table.setSelectionBackground(new Color(226, 233, 255));
            table.setSelectionForeground(ink);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

            JTableHeader header = table.getTableHeader();
            header.setPreferredSize(new Dimension(0, 44));
            header.setFont(new Font("SansSerif", Font.BOLD, 12));
            header.setBackground(navy);
            header.setForeground(Color.WHITE);
            header.setReorderingAllowed(false);

            table.getColumnModel().getColumn(0).setPreferredWidth(90);
            table.getColumnModel().getColumn(0).setCellRenderer(
                    new DefaultTableCellRenderer() {
                        @Override
                        public Component getTableCellRendererComponent(
                                JTable t, Object value, boolean selected,
                                boolean focus, int row, int col) {

                            JLabel cell = (JLabel)
                                    super.getTableCellRendererComponent(
                                            t, value, selected, focus, row, col);

                            cell.setHorizontalAlignment(SwingConstants.CENTER);
                            cell.setFont(new Font("SansSerif", Font.BOLD, 12));
                            cell.setBackground(new Color(239, 242, 250));
                            cell.setForeground(navy);
                            cell.setBorder(new EmptyBorder(4, 4, 4, 4));

                            return cell;
                        }
                    });

            JComboBox<Object> choices = new JComboBox<>();
            choices.addItem("Free Session");

            for (Subject subject : all) {
                choices.addItem(subject);
            }

            choices.setFont(new Font("SansSerif", Font.PLAIN, 12));
            choices.setBackground(Color.WHITE);

            DefaultCellEditor editor = new DefaultCellEditor(choices);

            for (int col = 1; col <= 5; col++) {
                table.getColumnModel().getColumn(col).setCellEditor(editor);

                table.getColumnModel().getColumn(col)
                        .setCellRenderer(new DefaultTableCellRenderer() {
                            @Override
                            public Component getTableCellRendererComponent(
                                    JTable t, Object value, boolean selected,
                                    boolean focus, int row, int col) {

                                JLabel cell = (JLabel)
                                        super.getTableCellRendererComponent(
                                                t, value, selected, focus,
                                                row, col);

                                String name = value instanceof Subject s
                                        ? s.name()
                                        : String.valueOf(value);

                                cell.setText("  " + name);
                                cell.setHorizontalAlignment(
                                        SwingConstants.CENTER);
                                cell.setFont(new Font(
                                        "SansSerif", Font.PLAIN, 12));
                                cell.setBorder(
                                        new EmptyBorder(6, 4, 6, 4));

                                if (!selected) {
                                    cell.setBackground(Color.WHITE);
                                    cell.setForeground(
                                            name.equals("Free Session")
                                                    ? new Color(140, 145, 158)
                                                    : ink);
                                }

                                return cell;
                            }
                        });
            }

            JPanel panel = new JPanel(new BorderLayout(0, 16));
            panel.setBackground(bg);
            panel.setBorder(new EmptyBorder(4, 0, 0, 0));

            JPanel intro = new JPanel();
            intro.setOpaque(false);
            intro.setLayout(new BoxLayout(intro, BoxLayout.Y_AXIS));
            intro.add(label("Timetable ✨", 17, true));
            intro.add(Box.createVerticalStrut(6));
            intro.add(label("Enter your timetable.", 12, false));

            panel.add(intro, BorderLayout.NORTH);

            JScrollPane scroll = new JScrollPane(table);
            scroll.setBorder(new LineBorder(
                    new Color(225, 229, 239), 1, true));
            scroll.getViewport().setBackground(Color.WHITE);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            panel.add(scroll, BorderLayout.CENTER);

            JButton save = new JButton("Save timetable  ✓");
            save.setFont(new Font("SansSerif", Font.BOLD, 13));
            save.setBackground(blue);
            save.setForeground(Color.WHITE);
            save.setFocusPainted(false);
            save.setBorder(new EmptyBorder(12, 22, 12, 22));

            save.addActionListener(e -> {
                if (table.isEditing()
                        && !table.getCellEditor().stopCellEditing()) {
                    return;
                }

                try (Connection c = Database.getConnection()) {
                    c.setAutoCommit(false);

                    try (PreparedStatement up = c.prepareStatement(
                            "INSERT INTO timetable " +
                            "(day_of_week,hour,subject_id) VALUES (?,?,?) " +
                            "ON CONFLICT(day_of_week,hour) " +
                            "DO UPDATE SET subject_id=excluded.subject_id")) {

                        for (int row = 0; row < 6; row++) {
                            int hour = row + 1;

                            for (int col = 1; col <= 5; col++) {
                                Object value = model.getValueAt(row, col);

                                up.setInt(1, col);
                                up.setInt(2, hour);

                                if (value instanceof Subject subject) {
                                    up.setInt(3, subject.id());
                                } else {
                                    up.setNull(3, Types.INTEGER);
                                }

                                up.addBatch();
                            }
                        }

                        up.executeBatch();
                        c.commit();

                    } catch (Exception ex) {
                        c.rollback();
                        throw ex;
                    }

                    JOptionPane.showMessageDialog(
                            this, "Your weekly timetable has been saved! ✨");
                    showTimetable();

                } catch (Exception ex) {
                    error(ex);
                }
            });

            JPanel footer = new JPanel(
                    new FlowLayout(FlowLayout.RIGHT));
            footer.setOpaque(false);
            footer.add(save);
            panel.add(footer, BorderLayout.SOUTH);

            render("Timetable", panel);

        } catch (Exception e) {
            error(e);
        }
    }

    // ---------------- DASHBOARD ----------------

    private void showDashboard() {
        JPanel page = new JPanel();
        page.setBackground(bg);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));

        JPanel welcome = card();
        welcome.setLayout(new BorderLayout(0, 8));

        welcome.add(
                label("Welcome, " + displayName() + "!✨", 22, true),
                BorderLayout.NORTH);

        welcome.add(
                label(LocalDate.now().format(
                        DateTimeFormatter.ofPattern("dd MMM yyyy, EEEE")),
                        13, false),
                BorderLayout.CENTER);

        page.add(welcome);
        page.add(Box.createVerticalStrut(16));

        JPanel schedule = card();
        schedule.setLayout(new BorderLayout(0, 14));
        schedule.add(label("Today's schedule", 18, true),
                BorderLayout.NORTH);

        JPanel hours = new JPanel(new GridLayout(2, 3, 12, 12));
        hours.setOpaque(false);

        Subject[] scheduled = new Subject[6];
        JCheckBox[] present = new JCheckBox[6];
        JPanel[] hourCards = new JPanel[6];
        JLabel[] subjectLabels = new JLabel[6];

        @SuppressWarnings("unchecked")
        JComboBox<Object>[] subjectChoices = new JComboBox[6];

        JCheckBox holiday = new JCheckBox("🎄Holiday mode");
        holiday.setOpaque(false);

        JButton submit = new JButton("Submit attendance");
        submit.setBackground(blue);
        submit.setForeground(Color.WHITE);

        int today = LocalDate.now().getDayOfWeek().getValue();

        try {
            List<Subject> allSubjects = subjects();

            for (int i = 0; i < 6; i++) {
                final int index = i;

                if (today >= 1 && today <= 5) {
                    scheduled[i] = scheduledSubject(today, i + 1);
                }

                JPanel h = new JPanel(new GridLayout(0, 1, 0, 8));
                h.setBackground(new Color(247, 248, 252));
                h.setBorder(new EmptyBorder(12, 12, 12, 12));
                h.add(label("HOUR " + (i + 1), 11, true));

                subjectLabels[i] = label(
                        scheduled[i] == null
                                ? "Free Session"
                                : scheduled[i].name(),
                        14, true);

                h.add(subjectLabels[i]);

                subjectChoices[i] = new JComboBox<>();
                subjectChoices[i].addItem("Free Session");

                for (Subject s : allSubjects) {
                    subjectChoices[i].addItem(s);
                }

                if (scheduled[i] != null) {
                    subjectChoices[i].setSelectedItem(scheduled[i]);
                } else {
                    subjectChoices[i].setSelectedIndex(0);
                }

                subjectChoices[i].setRenderer(
                        new DefaultListCellRenderer() {
                            @Override
                            public Component getListCellRendererComponent(
                                    JList<?> list, Object value, int row,
                                    boolean selected, boolean focus) {

                                Object display = value instanceof Subject
                                        ? ((Subject) value).name()
                                        : value;

                                return super.getListCellRendererComponent(
                                        list, display, row, selected, focus);
                            }
                        });

                subjectChoices[i].addActionListener(e -> {
                    Object selected =
                            subjectChoices[index].getSelectedItem();

                    scheduled[index] = selected instanceof Subject
                            ? (Subject) selected
                            : null;

                    subjectLabels[index].setText(
                            scheduled[index] == null
                                    ? "Free Session"
                                    : scheduled[index].name());

                    present[index].setEnabled(
                            !holiday.isSelected()
                                    && scheduled[index] != null);
                });

                h.add(subjectChoices[i]);

                present[i] = new JCheckBox("Present");
                present[i].setOpaque(false);
                present[i].setEnabled(scheduled[i] != null);
                h.add(present[i]);

                hourCards[i] = h;
                hours.add(h);
            }

            // Restore attendance already saved for today.
            String todayDate = LocalDate.now().toString();

            String sql =
                    "SELECT hour,subject_id,present FROM attendance " +
                    "WHERE attendance_date=?";

            try (Connection c = Database.getConnection();
                 PreparedStatement q = c.prepareStatement(sql)) {

                q.setString(1, todayDate);

                try (ResultSet r = q.executeQuery()) {
                    while (r.next()) {
                        int hourIndex = r.getInt("hour") - 1;
                        int savedSubjectId = r.getInt("subject_id");

                        if (hourIndex < 0 || hourIndex >= 6) {
                            continue;
                        }

                        for (Subject s : allSubjects) {
                            if (s.id() == savedSubjectId) {
                                subjectChoices[hourIndex].setSelectedItem(s);
                                break;
                            }
                        }

                        present[hourIndex].setSelected(
                                r.getInt("present") == 1);
                    }
                }
            }

            schedule.add(hours, BorderLayout.CENTER);

            holiday.addActionListener(e -> {
                boolean enabled = !holiday.isSelected();

                for (int i = 0; i < 6; i++) {
                    subjectChoices[i].setEnabled(enabled);

                    present[i].setEnabled(
                            enabled && scheduled[i] != null);

                    hourCards[i].setBackground(
                            enabled
                                    ? new Color(247, 248, 252)
                                    : new Color(225, 228, 234));
                }

                submit.setEnabled(enabled);
                hours.repaint();
            });

            submit.addActionListener(e -> {
                if (holiday.isSelected()) {
                    return;
                }

                try {
                    saveTodayAttendance(scheduled, present);

                    JOptionPane.showMessageDialog(
                            this, "Today's attendance has been saved.");

                    showDashboard();

                } catch (Exception ex) {
                    error(ex);
                }
            });

            JPanel actions = new JPanel(
                    new FlowLayout(FlowLayout.RIGHT));
            actions.setOpaque(false);
            actions.add(holiday);
            actions.add(submit);
            schedule.add(actions, BorderLayout.SOUTH);

            page.add(schedule);
            page.add(Box.createVerticalStrut(16));

            JPanel overview = card();
            overview.setLayout(
                    new BoxLayout(overview, BoxLayout.Y_AXIS));
            overview.add(label("Attendance overview", 18, true));
            overview.add(Box.createVerticalStrut(12));

            for (Subject s : allSubjects) {
                String overviewSql =
                        "SELECT classes_attended,classes_held " +
                        "FROM attendance " +
                        "WHERE subject_id=? AND attendance_date IS NULL";

                try (Connection c = Database.getConnection();
                     PreparedStatement p = c.prepareStatement(overviewSql)) {

                    p.setInt(1, s.id());

                    try (ResultSet r = p.executeQuery()) {
                        if (r.next()) {
                            int attended = r.getInt(1);
                            int held = r.getInt(2);

                            double pct = held == 0
                                    ? 0
                                    : 100.0 * attended / held;

                            overview.add(label(
                                    String.format(
                                            Locale.US,
                                            "%s  |  %.1f%%  (%d/%d)",
                                            s.name(), pct, attended, held),
                                    13, false));

                            overview.add(Box.createVerticalStrut(8));
                        }
                    }
                }
            }

            page.add(overview);

            JScrollPane scroll = new JScrollPane(page);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(bg);

            render("Dashboard", scroll);

        } catch (Exception e) {
            error(e);
        }
    }

    private void saveTodayAttendance(
            Subject[] scheduled, JCheckBox[] present)
            throws SQLException {

        int day = LocalDate.now().getDayOfWeek().getValue();

        if (day < 1 || day > 5) {
            throw new IllegalArgumentException(
                    "There is no timetable configured for weekends.");
        }

        String date = LocalDate.now().toString();

        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);

            try {
                for (int i = 0; i < 6; i++) {
                    Subject subject = scheduled[i];

                    if (subject == null) {
                        continue;
                    }

                    int newPresent = present[i].isSelected() ? 1 : 0;
                    int oldSubjectId = -1;
                    int oldPresent = -1;
                    int dailyId = -1;

                    try (PreparedStatement q = c.prepareStatement(
                            "SELECT id,subject_id,present FROM attendance " +
                            "WHERE attendance_date=? AND hour=?")) {

                        q.setString(1, date);
                        q.setInt(2, i + 1);

                        try (ResultSet r = q.executeQuery()) {
                            if (r.next()) {
                                dailyId = r.getInt("id");
                                oldSubjectId = r.getInt("subject_id");
                                oldPresent = r.getInt("present");
                            }
                        }
                    }

                    if (dailyId >= 0) {
                        try (PreparedStatement u = c.prepareStatement(
                                "UPDATE attendance SET subject_id=?,present=? " +
                                "WHERE id=?")) {

                            u.setInt(1, subject.id());
                            u.setInt(2, newPresent);
                            u.setInt(3, dailyId);
                            u.executeUpdate();
                        }

                        if (oldSubjectId == subject.id()) {
                            adjustAggregate(
                                    c, subject.id(), 0,
                                    newPresent - oldPresent);
                        } else {
                            if (oldSubjectId >= 0) {
                                adjustAggregate(
                                        c, oldSubjectId, -1,
                                        -Math.max(0, oldPresent));
                            }

                            adjustAggregate(
                                    c, subject.id(), 1, newPresent);
                        }

                    } else {
                        try (PreparedStatement ins = c.prepareStatement(
                                "INSERT INTO attendance " +
                                "(subject_id,attendance_date,hour,present) " +
                                "VALUES (?,?,?,?)")) {

                            ins.setInt(1, subject.id());
                            ins.setString(2, date);
                            ins.setInt(3, i + 1);
                            ins.setInt(4, newPresent);
                            ins.executeUpdate();
                        }

                        adjustAggregate(
                                c, subject.id(), 1, newPresent);
                    }
                }

                c.commit();

            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    private void adjustAggregate(
            Connection c, int subjectId,
            int heldDelta, int attendedDelta) throws SQLException {

        try (PreparedStatement p = c.prepareStatement(
                "UPDATE attendance SET " +
                "classes_held=MAX(0,classes_held+?), " +
                "classes_attended=MAX(0,classes_attended+?) " +
                "WHERE subject_id=? AND attendance_date IS NULL")) {

            p.setInt(1, heldDelta);
            p.setInt(2, attendedDelta);
            p.setInt(3, subjectId);
            p.executeUpdate();
        }
    }

    // ---------------- GRADES ----------------

    private void showGrades() {
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setBackground(bg);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(
                label("Assessment marks and targets", 18, true),
                BorderLayout.WEST);

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> showGrades());
        header.add(refresh, BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        JPanel list = new JPanel(new GridLayout(0, 2, 14, 14));
        list.setBackground(bg);

        try {
            for (Subject s : subjects()) {
                list.add(gradeCard(s));
            }
        } catch (Exception e) {
            error(e);
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(bg);
        page.add(scroll, BorderLayout.CENTER);

        JButton add = new JButton("+ Add subject");
        add.addActionListener(e -> addSubject());
        page.add(add, BorderLayout.SOUTH);

        render("Grades", page);
    }

    private JPanel gradeCard(Subject subject) throws SQLException {
        JPanel p = card();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        p.add(label(
                subject.name() + "  |  " + subject.credits() + " credits",
                17, true));
        p.add(Box.createVerticalStrut(10));

        List<Assessment> assessmentList = assessments(subject.id());
        List<JTextField> markFields = new ArrayList<>();
        List<Assessment> records = new ArrayList<>();

        for (Assessment a : assessmentList) {
            JPanel row = new JPanel(new GridLayout(1, 0, 6, 0));
            row.setOpaque(false);
            row.add(new JLabel(a.title()));

            boolean attendanceAssessment =
                    a.title().equalsIgnoreCase("Attendance");

            JTextField marks = new JTextField(
                    a.marks() == null
                            ? format(a.max())
                            : format(a.marks()));

            marks.setToolTipText(
                    attendanceAssessment
                            ? "Calculated automatically from attendance"
                            : "Leave blank if not entered");

            if (attendanceAssessment) {
                marks.setEditable(false);
                marks.setBackground(new Color(238, 241, 247));
                marks.setText(format(attendanceMark(subject.id())));
            }

            JTextField max = new JTextField(format(a.max()));
            max.setEditable(false);
            max.setBackground(new Color(238, 241, 247));

            row.add(marks);
            row.add(new JLabel("/"));
            row.add(max);

            p.add(row);
            p.add(Box.createVerticalStrut(5));

            markFields.add(marks);
            records.add(a);
        }

        double[] split = markSplit(subject.id());
        double cieMax = split[0];
        double eseMax = split[1];

        JTextField cie = new JTextField();
        cie.setEditable(false);
        cie.setBackground(new Color(238, 241, 247));
        cie.setToolTipText(
                "Automatically calculated from all assessment marks");

        JTextField ese = new JTextField();

        JComboBox<String> target = new JComboBox<>(
                Arrays.copyOf(GRADES, GRADES.length - 1));

        JLabel total = label("Total: — / 100", 12, true);
        JLabel grade = label("Calculated grade: S", 12, true);
        JLabel points = label("Grade points: 10", 12, false);
        JLabel needed = label("Minimum ESE needed: —", 12, true);

        try (Connection c = Database.getConnection();
             PreparedStatement q = c.prepareStatement(
                     "SELECT cie,ese,target_grade FROM grades WHERE subject_id=?")) {

            q.setInt(1, subject.id());

            try (ResultSet r = q.executeQuery()) {
                if (r.next()) {
                    ese.setText(format(r.getDouble("ese")));
                    target.setSelectedItem(r.getString("target_grade"));
                }
            }
        }

        JPanel cieRow = new JPanel(new GridLayout(1, 0, 8, 0));
        cieRow.setOpaque(false);
        cieRow.add(new JLabel("CIE /" + format(cieMax)));
        cieRow.add(cie);
        cieRow.add(new JLabel("ESE /" + format(eseMax)));
        cieRow.add(ese);

        p.add(Box.createVerticalStrut(8));
        p.add(cieRow);

        JPanel targetRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        targetRow.setOpaque(false);
        targetRow.add(new JLabel("Target grade"));
        targetRow.add(target);
        p.add(targetRow);

        p.add(total);
        p.add(grade);
        p.add(points);
        p.add(needed);

        Runnable calculate = () -> {
            try {
                double cv = 0;

                for (JTextField field : markFields) {
                    cv += numberOrZero(field.getText());
                }

                cie.setText(format(cv));

                double ev = numberOrZero(ese.getText());
                int idx = targetIndex((String) target.getSelectedItem());
                double sum = cv + ev;
                int actual = gradeIndex(sum);

                total.setText("Total: " + format(sum) + " / 100");
                grade.setText("Calculated grade: " + GRADES[actual]);
                points.setText("Grade points: " + GRADE_POINTS[actual]);

                double minESE = MIN_PERCENT[idx] - cv;

                String needText = minESE <= 0
                        ? "0 / " + format(eseMax)
                        : minESE > eseMax
                        ? format(minESE) + " / " + format(eseMax)
                            + " (not achievable)"
                        : format(minESE) + " / " + format(eseMax);

                needed.setText("Minimum ESE needed: " + needText);

            } catch (Exception ignored) {
                // Wait until the current field contains a valid number.
            }
        };

        DocumentListener marksListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                calculate.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                calculate.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                calculate.run();
            }
        };

        for (JTextField field : markFields) {
            field.getDocument().addDocumentListener(marksListener);
        }

        ese.getDocument().addDocumentListener(marksListener);
        target.addActionListener(e -> calculate.run());

        calculate.run();

        JButton save = new JButton("Save marks and target");
        save.setBackground(blue);
        save.setForeground(Color.WHITE);

        save.addActionListener(e -> {
            try (Connection c = Database.getConnection()) {
                c.setAutoCommit(false);

                try {
                    for (int i = 0; i < records.size(); i++) {
                        Assessment a = records.get(i);

                        if (a.title().equalsIgnoreCase("Attendance")) {
                            double autoMark = attendanceMark(subject.id());

                            try (PreparedStatement u = c.prepareStatement(
                                    "UPDATE assessments SET marks_obtained=? " +
                                    "WHERE id=?")) {
                                u.setDouble(1, autoMark);
                                u.setInt(2, a.id());
                                u.executeUpdate();
                            }

                            continue;
                        }

                        String markText =
                                markFields.get(i).getText().trim();

                        double max = a.max();
                        double mark = markText.isEmpty()
                                ? 0
                                : Double.parseDouble(markText);

                        if (mark < 0 || mark > max) {
                            throw new IllegalArgumentException(
                                    "Marks must be between 0 and the maximum ("
                                    + format(max) + ").");
                        }

                        try (PreparedStatement u = c.prepareStatement(
                                "UPDATE assessments SET marks_obtained=? " +
                                "WHERE id=?")) {
                            u.setDouble(1, mark);
                            u.setInt(2, a.id());
                            u.executeUpdate();
                        }
                    }

                    double cv = 0;

                    for (JTextField field : markFields) {
                        cv += numberOrZero(field.getText());
                    }

                    double ev = ese.getText().trim().isEmpty()
                            ? 0
                            : Double.parseDouble(ese.getText().trim());

                    cie.setText(format(cv));

                    if (cv < 0 || cv > cieMax || ev < 0 || ev > eseMax) {
                        throw new IllegalArgumentException(
                                "CIE must be between 0 and " + format(cieMax)
                                + ", and ESE must be between 0 and "
                                + format(eseMax) + ".");
                    }

                    try (PreparedStatement u = c.prepareStatement(
                            "UPDATE grades SET cie=?,ese=?,target_grade=? " +
                            "WHERE subject_id=?")) {
                        u.setDouble(1, cv);
                        u.setDouble(2, ev);
                        u.setString(3, (String) target.getSelectedItem());
                        u.setInt(4, subject.id());
                        u.executeUpdate();
                    }

                    c.commit();

                    JOptionPane.showMessageDialog(this, "Saved.");
                    showGrades();

                } catch (Exception ex) {
                    c.rollback();
                    throw ex;
                }

            } catch (Exception ex) {
                error(ex);
            }
        });

        p.add(Box.createVerticalStrut(10));
        p.add(save);

        return p;
    }

    private double attendanceMark(int subjectId) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "SELECT classes_attended,classes_held FROM attendance " +
                     "WHERE subject_id=? AND attendance_date IS NULL")) {

            p.setInt(1, subjectId);

            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) {
                    return 0;
                }

                int attended = r.getInt(1);
                int held = r.getInt(2);

                double pct = held <= 0 ? 0 : 100.0 * attended / held;

                if (pct >= 85) return 5;
                if (pct >= 75) return 4;
                if (pct >= 65) return 3;
                if (pct >= 55) return 2;
                if (pct >= 45) return 1;

                return 0;
            }
        }
    }

    private String format(double n) {
        if (n == (long) n) {
            return String.valueOf((long) n);
        }

        return String.format(Locale.US, "%.2f", n);
    }

    private double numberOrZero(String s) {
        return s.trim().isEmpty()
                ? 0
                : Double.parseDouble(s.trim());
    }

    private int targetIndex(String grade) {
        for (int i = 0; i < GRADES.length; i++) {
            if (GRADES[i].equals(grade)) {
                return i;
            }
        }

        return 0;
    }

    private int gradeIndex(double total) {
        for (int i = 0; i < MIN_PERCENT.length; i++) {
            if (total >= MIN_PERCENT[i]) {
                return i;
            }
        }

        return GRADES.length - 1;
    }

    // ---------------- SETTINGS ----------------

    private void showSettings() {
        JPanel page = new JPanel();
        page.setBackground(bg);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));

        JPanel subjectCard = card();
        subjectCard.setLayout(
                new BoxLayout(subjectCard, BoxLayout.Y_AXIS));

        subjectCard.add(label("Select a subject", 18, true));
        subjectCard.add(label(
                "Choose a subject to manage its mark split and assessments.",
                12, false));
        subjectCard.add(Box.createVerticalStrut(12));

        try {
            List<Subject> allSubjects = subjects();

            if (allSubjects.isEmpty()) {
                subjectCard.add(label("No subjects added yet.", 13, false));
            }

            for (Subject s : allSubjects) {
                JPanel subjectOption = new JPanel(
                        new BorderLayout(12, 0));

                subjectOption.setBackground(Color.WHITE);
                subjectOption.setBorder(new CompoundBorder(
                        new MatteBorder(
                                0, 0, 1, 0, new Color(232, 234, 240)),
                        new EmptyBorder(12, 8, 12, 8)));

                JPanel details = new JPanel();
                details.setOpaque(false);
                details.setLayout(
                        new BoxLayout(details, BoxLayout.Y_AXIS));

                details.add(label(s.name(), 14, true));
                details.add(Box.createVerticalStrut(4));

                double[] split = markSplit(s.id());

                details.add(label(
                        "Credits: " + s.credits()
                        + "   |   CIE: " + format(split[0])
                        + "   |   ESE: " + format(split[1]),
                        12, false));

                JButton open = new JButton("Manage  →");
                open.addActionListener(e -> showSubjectSettings(s));

                JButton delete = new JButton("🗑 Delete");
                delete.setForeground(new Color(190, 45, 45));

                delete.addActionListener(e -> {
                    int choice = JOptionPane.showConfirmDialog(
                            this,
                            "Delete " + s.name() + "?\n\n"
                            + "This may affect its grades, attendance, "
                            + "assessments and timetable records.",
                            "Confirm subject deletion",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);

                    if (choice != JOptionPane.YES_OPTION) {
                        return;
                    }

                    try {
                        new SubjectDAO().delete(s.id());

                        JOptionPane.showMessageDialog(
                                this, "Subject deleted successfully! ♡");

                        showSubjectSettings(s);

                    } catch (Exception ex) {
                        error(ex);
                    }
                });

                JPanel actions = new JPanel(
                        new FlowLayout(FlowLayout.RIGHT, 8, 0));
                actions.setOpaque(false);
                actions.add(open);
                actions.add(delete);

                subjectOption.add(details, BorderLayout.CENTER);
                subjectOption.add(actions, BorderLayout.EAST);
                subjectCard.add(subjectOption);
            }

        } catch (Exception e) {
            error(e);
        }

        JButton add = new JButton("+ Add subject");
        add.addActionListener(e -> addSubject());
        subjectCard.add(add);

        page.add(subjectCard);
        page.add(Box.createVerticalStrut(16));

        JPanel attendance = card();
        attendance.setLayout(
                new BoxLayout(attendance, BoxLayout.Y_AXIS));
        attendance.add(label("Attendance history", 18, true));
        attendance.add(Box.createVerticalStrut(10));

        SpinnerDateModel model = new SpinnerDateModel(
                new java.util.Date(), null, null, Calendar.DAY_OF_MONTH);

        JSpinner dateSpinner = new JSpinner(model);
        dateSpinner.setEditor(
                new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy"));

        JButton search = new JButton("Show records");

        JPanel dateRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dateRow.setOpaque(false);
        dateRow.add(new JLabel("Select date:"));
        dateRow.add(dateSpinner);
        dateRow.add(search);
        attendance.add(dateRow);

        JPanel records = new JPanel();
        records.setBackground(Color.WHITE);
        records.setLayout(
                new BoxLayout(records, BoxLayout.Y_AXIS));
        attendance.add(records);

        Runnable loadRecords = () -> {
            records.removeAll();

            String date = new java.text.SimpleDateFormat("yyyy-MM-dd")
                    .format((java.util.Date) dateSpinner.getValue());

            try (Connection c = Database.getConnection();
                 PreparedStatement q = c.prepareStatement(
                         "SELECT a.hour,s.name,a.present FROM attendance a " +
                         "JOIN subjects s ON s.id=a.subject_id " +
                         "WHERE a.attendance_date=? ORDER BY a.hour")) {

                q.setString(1, date);

                try (ResultSet r = q.executeQuery()) {
                    boolean any = false;

                    while (r.next()) {
                        any = true;

                        records.add(label(
                                "Hour " + r.getInt("hour")
                                + "  |  " + r.getString("name")
                                + "  |  "
                                + (r.getInt("present") == 1
                                        ? "Present" : "Absent"),
                                13, false));
                    }

                    if (!any) {
                        records.add(label(
                                "No attendance records saved for this date.",
                                13, false));
                    }
                }

            } catch (Exception ex) {
                error(ex);
            }

            records.revalidate();
            records.repaint();
        };

        search.addActionListener(e -> loadRecords.run());
        loadRecords.run();
        page.add(attendance);

        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        render("Settings", scroll);
    }

    private void showSubjectSettings(Subject s) {
        JPanel page = new JPanel();
        page.setBackground(bg);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));

        JButton back = new JButton("← Back to subjects");
        back.setAlignmentX(Component.LEFT_ALIGNMENT);
        back.addActionListener(e -> showSettings());

        page.add(back);
        page.add(Box.createVerticalStrut(12));

        JPanel subjectCard = card();
        subjectCard.setLayout(
                new BoxLayout(subjectCard, BoxLayout.Y_AXIS));

        subjectCard.add(label(s.name(), 20, true));
        subjectCard.add(label("Credits: " + s.credits(), 13, false));
        subjectCard.add(Box.createVerticalStrut(12));

        try {
            subjectCard.add(subjectSettingsRow(s));
        } catch (SQLException ex) {
            error(ex);
        }

        page.add(subjectCard);

        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        render("Settings", scroll);
    }

    private JPanel subjectSettingsRow(Subject s) throws SQLException {
        JPanel row = new JPanel();
        row.setBackground(Color.WHITE);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBorder(new CompoundBorder(
                new MatteBorder(
                        0, 0, 1, 0, new Color(232, 234, 240)),
                new EmptyBorder(10, 0, 10, 0)));

        row.add(label(
                s.name() + "  |  Credits: " + s.credits(),
                14, true));

        double[] split = markSplit(s.id());

        JTextField cieMaxField = new JTextField(format(split[0]), 5);
        JTextField eseMaxField = new JTextField(format(split[1]), 5);

        JPanel splitRow = new JPanel(
                new FlowLayout(FlowLayout.LEFT));
        splitRow.setOpaque(false);
        splitRow.add(new JLabel("CIE maximum:"));
        splitRow.add(cieMaxField);
        splitRow.add(new JLabel("ESE maximum:"));
        splitRow.add(eseMaxField);

        JButton saveSplit = new JButton("Save mark split");

        saveSplit.addActionListener(e -> {
            try {
                double cieMax =
                        Double.parseDouble(cieMaxField.getText().trim());
                double eseMax =
                        Double.parseDouble(eseMaxField.getText().trim());

                if (cieMax <= 0 || eseMax <= 0
                        || Math.abs(cieMax + eseMax - 100.0) > 0.000001) {
                    throw new IllegalArgumentException(
                            "CIE and ESE must both be positive "
                            + "and add up to exactly 100.");
                }

                try (Connection c = Database.getConnection();
                     PreparedStatement u = c.prepareStatement(
                             "UPDATE subjects SET cie_max=?,ese_max=? " +
                             "WHERE id=?")) {

                    u.setDouble(1, cieMax);
                    u.setDouble(2, eseMax);
                    u.setInt(3, s.id());
                    u.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "Mark split saved!");
                showSettings();

            } catch (Exception ex) {
                error(ex);
            }
        });

        splitRow.add(saveSplit);
        row.add(splitRow);

        JComboBox<Assessment> choice = new JComboBox<>();
        List<Assessment> items = assessments(s.id());

        for (Assessment a : items) {
            choice.addItem(a);
        }

        row.add(Box.createVerticalStrut(6));
        row.add(choice);

        JLabel assessmentTotal = label("", 13, true);
        row.add(assessmentTotal);

        Runnable updateAssessmentTotal = () -> {
            try {
                double totalMax = 0;

                for (Assessment a : assessments(s.id())) {
                    totalMax += a.max();
                }

                double cieMax = markSplit(s.id())[0];
                double difference = totalMax - cieMax;

                if (Math.abs(difference) < 0.000001) {
                    assessmentTotal.setText(
                            "Assessment maxima: " + format(totalMax)
                            + " / " + format(cieMax)
                            + " - Perfect match!");
                    assessmentTotal.setForeground(
                            new Color(30, 130, 70));

                } else if (difference < 0) {
                    assessmentTotal.setText(
                            "Assessment maxima: " + format(totalMax)
                            + " / " + format(cieMax)
                            + " — " + format(-difference) + " BELOW CIE!");
                    assessmentTotal.setForeground(
                            new Color(190, 125, 0));

                } else {
                    assessmentTotal.setText(
                            "Assessment maxima: " + format(totalMax)
                            + " / " + format(cieMax)
                            + " — " + format(difference) + " ABOVE CIE!");
                    assessmentTotal.setForeground(
                            new Color(200, 45, 45));
                }

            } catch (SQLException ex) {
                assessmentTotal.setText(
                        "Unable to calculate assessment total.");
                assessmentTotal.setForeground(Color.RED);
            }
        };

        updateAssessmentTotal.run();

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);

        JButton edit = new JButton("Edit assessment");
        JButton delete = new JButton("Delete assessment");
        JButton add = new JButton("+ Add assessment");

        edit.addActionListener(e -> {
            Assessment a = (Assessment) choice.getSelectedItem();

            if (a == null) {
                JOptionPane.showMessageDialog(
                        this, "Add an assessment first.");
                return;
            }

            if (a.title().equalsIgnoreCase("Attendance")) {
                JOptionPane.showMessageDialog(
                        this,
                        "The Attendance assessment is linked to attendance "
                        + "records and cannot be renamed.");
                return;
            }

            JTextField title = new JTextField(a.title());
            JTextField max = new JTextField(format(a.max()));

            Object[] form = {
                    "Assessment name", title,
                    "Maximum marks", max
            };

            if (JOptionPane.showConfirmDialog(
                    this, form, "Edit assessment",
                    JOptionPane.OK_CANCEL_OPTION)
                    != JOptionPane.OK_OPTION) {
                return;
            }

            try (Connection c = Database.getConnection();
                 PreparedStatement u = c.prepareStatement(
                         "UPDATE assessments SET title=?,max_marks=? " +
                         "WHERE id=?")) {

                double m = Double.parseDouble(max.getText().trim());

                if (title.getText().isBlank() || m <= 0) {
                    throw new IllegalArgumentException(
                            "Enter a title and a positive maximum mark.");
                }

                u.setString(1, title.getText().trim());
                u.setDouble(2, m);
                u.setInt(3, a.id());
                u.executeUpdate();

                showSubjectSettings(s);

            } catch (Exception ex) {
                error(ex);
            }
        });

        delete.addActionListener(e -> {
            Assessment a = (Assessment) choice.getSelectedItem();

            if (a == null) {
                return;
            }

            if (a.title().equalsIgnoreCase("Attendance")) {
                JOptionPane.showMessageDialog(
                        this,
                        "The Attendance assessment is linked to attendance "
                        + "records and cannot be deleted.");
                return;
            }

            if (JOptionPane.showConfirmDialog(
                    this, "Delete " + a.title() + "?",
                    "Confirm deletion", JOptionPane.YES_NO_OPTION)
                    != JOptionPane.YES_OPTION) {
                return;
            }

            try (Connection c = Database.getConnection();
                 PreparedStatement d = c.prepareStatement(
                         "DELETE FROM assessments WHERE id=?")) {

                d.setInt(1, a.id());
                d.executeUpdate();
                showSubjectSettings(s);

            } catch (Exception ex) {
                error(ex);
            }
        });

        add.addActionListener(e -> addAssessment(s));

        buttons.add(edit);
        buttons.add(delete);
        buttons.add(add);
        row.add(buttons);

        return row;
    }

    private void addSubject() {
        JTextField code = new JTextField();
        JTextField name = new JTextField();
        JTextField credits = new JTextField("3");

        Object[] form = {
                "Subject code", code,
                "Subject name", name,
                "Credits", credits
        };

        if (JOptionPane.showConfirmDialog(
                this, form, "Add subject",
                JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            int cr = Integer.parseInt(credits.getText().trim());

            if (code.getText().isBlank()
                    || name.getText().isBlank() || cr <= 0) {
                throw new IllegalArgumentException(
                        "Fill all fields correctly.");
            }

            new SubjectDAO().add(
                    code.getText().trim(),
                    name.getText().trim(), cr);

            ensureDefaultAssessmentsForExistingSubjects();
            showSettings();

        } catch (Exception e) {
            error(e);
        }
    }

    private void addAssessment(Subject s) {
        JTextField title = new JTextField();
        JTextField max = new JTextField("10");

        Object[] form = {
                "Assessment name", title,
                "Maximum marks", max
        };

        if (JOptionPane.showConfirmDialog(
                this, form, "Add assessment",
                JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "INSERT INTO assessments " +
                     "(subject_id,title,max_marks,marks_obtained) " +
                     "VALUES (?,?,?,?)")) {

            double m = Double.parseDouble(max.getText().trim());

            if (title.getText().isBlank() || m <= 0) {
                throw new IllegalArgumentException(
                        "Enter a title and positive maximum mark.");
            }

            p.setInt(1, s.id());
            p.setString(2, title.getText().trim());
            p.setDouble(3, m);
            p.setDouble(4, m);
            p.executeUpdate();

            showSubjectSettings(s);

        } catch (Exception e) {
            error(e);
        }
    }

    // ---------------- ADMIN ----------------



private void showAdmin() {
    JPanel p = card();
    p.setLayout(new BorderLayout(0, 16));

    JPanel header = new JPanel();
    header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
    header.setOpaque(false);

    header.add(label("Admin tools", 22, true));
    header.add(Box.createVerticalStrut(6));
    header.add(label("Signed in as " + displayName(), 13, false));

    JPanel tools = new JPanel(new GridLayout(0, 2, 12, 12));
    tools.setOpaque(false);

    JButton usersButton = new JButton("Manage users");
    JButton backupsButton = new JButton("Database backups");
    JButton monitoringButton = new JButton("System monitoring");
    JButton recordsButton = new JButton("Academic support");

    usersButton.addActionListener(e -> showAdminUsers());

    backupsButton.addActionListener(e -> showAdminBackups());

    monitoringButton.addActionListener(
        e -> showAdminMonitoring());

    recordsButton.addActionListener(
        e -> showAdminAcademicSupport());

    tools.add(usersButton);
    tools.add(backupsButton);
    tools.add(monitoringButton);
    tools.add(recordsButton);

    p.add(header, BorderLayout.NORTH);
    p.add(tools, BorderLayout.CENTER);

    render("Admin", p);
}


    // ---------------- COMMUNITY ----------------

    private void showCommunity() {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setBackground(bg);
        page.setBorder(
                BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel header = new JPanel(new BorderLayout(12, 6));
        header.setBackground(navy);
        header.setBorder(
                BorderFactory.createEmptyBorder(20, 22, 20, 22));

        JPanel headerText = new JPanel();
        headerText.setLayout(
                new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setOpaque(false);

        JLabel title = new JLabel("Community✨");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Your space!!.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(new Color(220, 228, 245));

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(5));
        headerText.add(subtitle);

        JButton refreshButton = new JButton("Refresh feed");
        refreshButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        refreshButton.setBackground(blue);
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.setBorder(
                BorderFactory.createEmptyBorder(10, 14, 10, 14));

        header.add(headerText, BorderLayout.CENTER);
        header.add(refreshButton, BorderLayout.EAST);

        JPanel composer = new JPanel(new BorderLayout(0, 12));
        composer.setBackground(Color.WHITE);
        composer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(225, 230, 238)),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel composerTitle = label("Create a post", 17, true);

        JTextField titleField = new JTextField();
        titleField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        titleField.setToolTipText("Give your post a title");
        titleField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(215, 222, 233)),
                BorderFactory.createEmptyBorder(9, 10, 9, 10)));

        JTextArea messageField = new JTextArea(3, 30);
        messageField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        messageField.setLineWrap(true);
        messageField.setWrapStyleWord(true);
        messageField.setBorder(
                BorderFactory.createEmptyBorder(8, 10, 8, 10));
        messageField.setToolTipText("What would you like to share?");

        JScrollPane messageScroll = new JScrollPane(messageField);
        messageScroll.setBorder(
                BorderFactory.createLineBorder(new Color(215, 222, 233)));
        messageScroll.setPreferredSize(new Dimension(400, 85));

        JComboBox<String> categoryField = new JComboBox<>(
                new String[]{
                        "General", "Academic Help", "Announcements",
                        "Projects", "Events"
                });
        categoryField.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JPanel titleAndCategory =
                new JPanel(new GridLayout(1, 2, 12, 0));
        titleAndCategory.setOpaque(false);

        JPanel titleGroup = new JPanel(new BorderLayout(0, 5));
        titleGroup.setOpaque(false);
        titleGroup.add(label("Title", 12, true), BorderLayout.NORTH);
        titleGroup.add(titleField, BorderLayout.CENTER);

        JPanel categoryGroup = new JPanel(new BorderLayout(0, 5));
        categoryGroup.setOpaque(false);
        categoryGroup.add(label("Category", 12, true), BorderLayout.NORTH);
        categoryGroup.add(categoryField, BorderLayout.CENTER);

        titleAndCategory.add(titleGroup);
        titleAndCategory.add(categoryGroup);

        JPanel messageGroup = new JPanel(new BorderLayout(0, 5));
        messageGroup.setOpaque(false);
        messageGroup.add(label("Message", 12, true), BorderLayout.NORTH);
        messageGroup.add(messageScroll, BorderLayout.CENTER);

        JPanel formFields = new JPanel(new BorderLayout(0, 12));
        formFields.setOpaque(false);
        formFields.add(titleAndCategory, BorderLayout.NORTH);
        formFields.add(messageGroup, BorderLayout.CENTER);

        JButton postButton = new JButton("Publish post");
        postButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        postButton.setBackground(blue);
        postButton.setForeground(Color.WHITE);
        postButton.setFocusPainted(false);
        postButton.setBorder(
                BorderFactory.createEmptyBorder(10, 18, 10, 18));

        JLabel hint = label(
                "Share questions, ideas and updates with other students.",
                11, false);

        JPanel composerFooter = new JPanel(new BorderLayout(8, 0));
        composerFooter.setOpaque(false);
        composerFooter.add(hint, BorderLayout.CENTER);
        composerFooter.add(postButton, BorderLayout.EAST);

        composer.add(composerTitle, BorderLayout.NORTH);
        composer.add(formFields, BorderLayout.CENTER);
        composer.add(composerFooter, BorderLayout.SOUTH);

        JPanel feed = new JPanel();
        feed.setLayout(new BoxLayout(feed, BoxLayout.Y_AXIS));
        feed.setBackground(bg);
        feed.setBorder(
                BorderFactory.createEmptyBorder(2, 2, 8, 2));

        JScrollPane feedScroll = new JScrollPane(feed);
        feedScroll.setBorder(BorderFactory.createEmptyBorder());
        feedScroll.getViewport().setBackground(bg);
        feedScroll.getVerticalScrollBar().setUnitIncrement(16);

        JLabel feedTitle = label("Latest discussions", 18, true);

        JPanel feedSection = new JPanel(new BorderLayout(0, 12));
        feedSection.setBackground(bg);
        feedSection.add(feedTitle, BorderLayout.NORTH);
        feedSection.add(feedScroll, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setBackground(bg);
        center.add(composer, BorderLayout.NORTH);
        center.add(feedSection, BorderLayout.CENTER);

        page.add(header, BorderLayout.NORTH);
        page.add(center, BorderLayout.CENTER);

        refreshButton.addActionListener(e -> loadCommunityPosts(feed));

        postButton.addActionListener(e -> {
            String postTitle = titleField.getText().trim();
            String message = messageField.getText().trim();
            String category = (String) categoryField.getSelectedItem();

            if (postTitle.isEmpty() || message.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please enter a title and message.",
                        "Missing information",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            postButton.setEnabled(false);

            try {
                publishCommunityPost(
                        postTitle, message, category);

                titleField.setText("");
                messageField.setText("");

                loadCommunityPosts(feed);

                JOptionPane.showMessageDialog(
                        this, "Your post is live!",
                        "Post published",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Could not publish your post.\n" + ex.getMessage(),
                        "Connection error",
                        JOptionPane.ERROR_MESSAGE);

            } finally {
                postButton.setEnabled(true);
            }
        });

        render("Community", page);
        loadCommunityPosts(feed);
    }


private void loadCommunityPosts(JPanel feed) {
    feed.removeAll();

    if (currentUser == null ||
            currentUser.getSessionToken().isEmpty()) {
        addCommunityEmptyState(feed);
        return;
    }

    try {
        URL url = new URL(FORUM_SERVER + "/api/forum/posts");
        HttpURLConnection con =
                (HttpURLConnection) url.openConnection();

        con.setRequestMethod("GET");
        con.setConnectTimeout(4000);
        con.setReadTimeout(4000);
        con.setRequestProperty(
                "Authorization",
                "Bearer " + currentUser.getSessionToken());

        try {
            int status = con.getResponseCode();

            if (status != 200) {
                throw new IOException(
                        "HTTP " + status +
                        (status == 401 ? " — please sign in again." : ""));
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            con.getInputStream(),
                            StandardCharsets.UTF_8))) {

                String line;
                boolean foundPost = false;

                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    String[] parts = line.split("\t", -1);

                    // New server format: ID + five encoded fields + owner ID.
                    if (parts.length != 7) continue;

                    String postId = parts[0];
                    String postTitle = decodeForumField(parts[1]);
                    String message = decodeForumField(parts[2]);
                    String category = decodeForumField(parts[3]);
                    String author = decodeForumField(parts[4]);
                    String date = decodeForumField(parts[5]);

                    Integer ownerId = null;
                    if (!parts[6].isEmpty()) {
                        try {
                            ownerId = Integer.valueOf(parts[6]);
                        } catch (NumberFormatException ignored) {
                            // Treat invalid ownership data as unowned.
                        }
                    }

                    boolean canDelete = currentUser.isAdmin()
                            || (ownerId != null
                            && ownerId.intValue() == currentUser.getId());

                    JPanel post = new JPanel(new BorderLayout(0, 10));
                    post.setBackground(Color.WHITE);
                    post.setAlignmentX(Component.LEFT_ALIGNMENT);
                    post.setBorder(
                            BorderFactory.createCompoundBorder(
                                    BorderFactory.createLineBorder(
                                            new Color(225, 230, 238)),
                                    BorderFactory.createEmptyBorder(
                                            16, 18, 16, 18)));

                    JPanel postHeader = new JPanel(new BorderLayout(8, 8));
                    postHeader.setOpaque(false);

                    JLabel heading = label(postTitle, 16, true);
                    heading.setForeground(navy);

                    JLabel categoryLabel = new JLabel(category);
                    categoryLabel.setFont(
                            new Font("SansSerif", Font.BOLD, 11));
                    categoryLabel.setForeground(blue);
                    categoryLabel.setOpaque(true);
                    categoryLabel.setBackground(new Color(235, 240, 255));
                    categoryLabel.setBorder(
                            BorderFactory.createEmptyBorder(5, 9, 5, 9));

                    postHeader.add(heading, BorderLayout.CENTER);
                    postHeader.add(categoryLabel, BorderLayout.EAST);

                    JTextArea body = new JTextArea(message);
                    body.setEditable(false);
                    body.setLineWrap(true);
                    body.setWrapStyleWord(true);
                    body.setOpaque(false);
                    body.setForeground(ink);
                    body.setFont(
                            new Font("SansSerif", Font.PLAIN, 14));
                    body.setColumns(30);
                    body.setRows(Math.min(8,
                            Math.max(2, (message.length() / 65) + 1)));
                    body.setBorder(BorderFactory.createEmptyBorder());

                    JLabel metadata = new JLabel(
                            "Posted by " + author + "   •   " + date);
                    metadata.setFont(
                            new Font("SansSerif", Font.PLAIN, 11));
                    metadata.setForeground(new Color(115, 124, 140));

                    JPanel postFooter = new JPanel(new BorderLayout(8, 0));
                    postFooter.setOpaque(false);
                    postFooter.add(metadata, BorderLayout.CENTER);


if (canDelete) {
    JButton deleteButton = new JButton("Delete");
    deleteButton.setForeground(new Color(190, 45, 45));
    deleteButton.setFocusPainted(false);

    deleteButton.addActionListener(e -> {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this post?",
                "Confirm deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            deleteForumPost(postId);
        }
    });

    postFooter.add(deleteButton, BorderLayout.EAST);
}

                    post.add(postHeader, BorderLayout.NORTH);
                    post.add(body, BorderLayout.CENTER);
                    post.add(postFooter, BorderLayout.SOUTH);

                    feed.add(post);
                    feed.add(Box.createVerticalStrut(12));
                    foundPost = true;
                }

                if (!foundPost) {
                    addCommunityEmptyState(feed);
                }
            }
        } finally {
            con.disconnect();
        }

    } catch (Exception ex) {
        JPanel errorPanel = new JPanel();
        errorPanel.setLayout(
                new BoxLayout(errorPanel, BoxLayout.Y_AXIS));
        errorPanel.setBackground(Color.WHITE);
        errorPanel.setBorder(
                BorderFactory.createEmptyBorder(22, 20, 22, 20));

        errorPanel.add(label("Couldn't load discussions", 15, true));
        errorPanel.add(Box.createVerticalStrut(6));
        errorPanel.add(label(
                "Check the server connection. If your session expired, "
                + "sign in again. Details: " + ex.getMessage(),
                12, false));
        errorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        feed.add(errorPanel);
    }

    feed.revalidate();
    feed.repaint();
}

    private void addCommunityEmptyState(JPanel feed) {
        JPanel empty = new JPanel();
        empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
        empty.setBackground(Color.WHITE);
        empty.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(225, 230, 238)),
                        BorderFactory.createEmptyBorder(
                                28, 20, 28, 20)));

        JLabel title = label("It's quiet in here!", 18, true);
        title.setForeground(navy);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel message = label(
                "Start a discussion and make the first post.",
                13, false);
        message.setAlignmentX(Component.CENTER_ALIGNMENT);

        empty.add(title);
        empty.add(Box.createVerticalStrut(8));
        empty.add(message);
        empty.setAlignmentX(Component.LEFT_ALIGNMENT);

        feed.add(empty);
    }


private void publishCommunityPost(
        String title, String message, String category)
        throws IOException {

    if (currentUser == null ||
            currentUser.getSessionToken().isEmpty()) {
        throw new IOException("Please sign in again.");
    }

    URL url = new URL(FORUM_SERVER + "/api/forum/posts");
    HttpURLConnection con =
            (HttpURLConnection) url.openConnection();

    con.setRequestMethod("POST");
    con.setConnectTimeout(4000);
    con.setReadTimeout(4000);
    con.setDoOutput(true);
    con.setRequestProperty(
            "Authorization",
            "Bearer " + currentUser.getSessionToken());
    con.setRequestProperty(
            "Content-Type",
            "application/x-www-form-urlencoded; charset=UTF-8");

    String body =
            "title=" + encodeForumField(title)
            + "&message=" + encodeForumField(message)
            + "&category=" + encodeForumField(
                    category == null ? "General" : category);

    try {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        con.setFixedLengthStreamingMode(bytes.length);

        try (OutputStream out = con.getOutputStream()) {
            out.write(bytes);
        }

        int status = con.getResponseCode();

        if (status != 201) {
            InputStream errorStream = con.getErrorStream();
            String detail = "";

            if (errorStream != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                errorStream, StandardCharsets.UTF_8))) {
                    detail = reader.lines().collect(
                            java.util.stream.Collectors.joining("\n"));
                }
            }

            throw new IOException("HTTP " + status + ": " + detail);
        }
    } finally {
        con.disconnect();
    }
}

    private String encodeForumField(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String decodeForumField(String value) {
        return new String(
                Base64.getUrlDecoder().decode(value),
                StandardCharsets.UTF_8);
    }



private void deleteForumPost(String postId) {
    if (currentUser == null ||
            currentUser.getSessionToken().isEmpty()) {
        JOptionPane.showMessageDialog(
                this, "Please sign in again.");
        return;
    }

    HttpURLConnection conn = null;

    try {
        URL url = new URL(
                FORUM_SERVER + "/api/forum/posts/"
                + URLEncoder.encode(postId, "UTF-8"));

        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        conn.setRequestProperty(
                "Authorization",
                "Bearer " + currentUser.getSessionToken());

        int responseCode = conn.getResponseCode();

        if (responseCode == 200 || responseCode == 204) {
            JOptionPane.showMessageDialog(
                    this, "Post deleted successfully!");
            showCommunity();
        } else {
            String detail = "";
            InputStream stream = conn.getErrorStream();

            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                stream, StandardCharsets.UTF_8))) {
                    detail = reader.lines().collect(
                            java.util.stream.Collectors.joining("\n"));
                }
            }

            if (responseCode == 401) {
                detail = "Your session expired. Please sign in again.";
            } else if (responseCode == 403 && detail.isEmpty()) {
                detail = "You don't have permission to delete this post.";
            }

            JOptionPane.showMessageDialog(
                    this,
                    "Could not delete post (HTTP "
                    + responseCode + ").\n" + detail,
                    "Deletion failed",
                    JOptionPane.WARNING_MESSAGE);
        }
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(
                this, "Could not delete post: " + ex.getMessage());
    } finally {
        if (conn != null) conn.disconnect();
    }
}

private void showAdminUsers() {
    if (!currentUser.isAdmin()) {
        JOptionPane.showMessageDialog(
                this, "Administrator access required.");
        return;
    }

    JPanel p = card();
    p.setLayout(new BorderLayout(0, 12));

    JPanel top = new JPanel(new BorderLayout(8, 8));
    top.setOpaque(false);

    JButton backButton = new JButton("← Admin tools");
    backButton.addActionListener(e -> showAdmin());

    JTextField searchField =
            new JTextField(20);
    searchField.setToolTipText(
            "Search username, display name, or role");

    top.add(backButton, BorderLayout.WEST);
    top.add(searchField, BorderLayout.CENTER);

    String[] columns = {
    "ID", "Username", "Display name", "Role", "Status"
};

    javax.swing.table.DefaultTableModel model =
            new javax.swing.table.DefaultTableModel(
                    columns, 0) {
                @Override
                public boolean isCellEditable(
                        int row, int column) {
                    return false;
                }
            };

    JTable table = new JTable(model);
    table.setRowHeight(28);
    table.setFillsViewportHeight(true);
    table.setAutoCreateRowSorter(true);

    javax.swing.table.TableRowSorter<
            javax.swing.table.DefaultTableModel> sorter =
            new javax.swing.table.TableRowSorter<>(model);

    table.setRowSorter(sorter);

    searchField.getDocument().addDocumentListener(
            new DocumentListener() {
                private void filter() {
                    String query =
                            searchField.getText().trim();

                    if (query.isEmpty()) {
                        sorter.setRowFilter(null);
                    } else {
                        sorter.setRowFilter(
                                javax.swing.RowFilter.regexFilter(
                                        "(?i)" +
                                        java.util.regex.Pattern.quote(query)));
                    }
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    filter();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    filter();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    filter();
                }
            });

    JLabel statusLabel = label(
            "Loading users...", 13, false);


JButton toggleButton = new JButton("Deactivate / Reactivate");

toggleButton.addActionListener(e -> {
    int selectedRow = table.getSelectedRow();

    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(
                this, "Please select a user first.");
        return;
    }


int modelRow = table.convertRowIndexToModel(selectedRow);

Object idValue = model.getValueAt(modelRow, 0);
Object usernameValue = model.getValueAt(modelRow, 1);
Object statusValue = model.getValueAt(modelRow, 4);

if (idValue == null || usernameValue == null || statusValue == null) {
    JOptionPane.showMessageDialog(
            this,
            "This user row has incomplete data. Click Refresh and try again.",
            "Incomplete user data",
            JOptionPane.WARNING_MESSAGE);
    return;
}

int userId;

try {
    userId = Integer.parseInt(idValue.toString());
} catch (NumberFormatException ex) {
    JOptionPane.showMessageDialog(this, "Invalid user ID.");
    return;
}

String username = usernameValue.toString();
String status = statusValue.toString();

if (!"Active".equals(status) && !"Inactive".equals(status)) {
    JOptionPane.showMessageDialog(
            this,
            "Unknown account status: " + status +
            "\nClick Refresh and check the server response.",
            "Invalid status",
            JOptionPane.WARNING_MESSAGE);
    return;
}

boolean activate = "Inactive".equals(status);




    if (!activate && userId == currentUser.getId()) {
        JOptionPane.showMessageDialog(
                this, "You cannot deactivate your own account.");
        return;
    }

    String action = activate ? "reactivate" : "deactivate";

    int choice = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to " + action +
            " the account '" + username + "'?",
            "Confirm account change",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

    if (choice == JOptionPane.YES_OPTION) {
        toggleAdminUser(userId, activate, model, statusLabel);
    }
});

JButton refreshButton = new JButton("Refresh");
refreshButton.addActionListener(e ->
        loadAdminUsers(model, statusLabel));

JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
actions.setOpaque(false);
actions.add(toggleButton);
actions.add(refreshButton);

JPanel bottom = new JPanel(new BorderLayout());
bottom.setOpaque(false);
bottom.add(statusLabel, BorderLayout.CENTER);
bottom.add(actions, BorderLayout.EAST);


    p.add(top, BorderLayout.NORTH);
    p.add(new JScrollPane(table), BorderLayout.CENTER);
    p.add(bottom, BorderLayout.SOUTH);

    render("Manage users", p);
    loadAdminUsers(model, statusLabel);
}

private void showAdminBackups() {
    if (!currentUser.isAdmin()) {
        JOptionPane.showMessageDialog(
                this, "Administrator access required.");
        return;
    }

    JPanel p = card();
    p.setLayout(new BorderLayout(0, 12));

    JPanel top = new JPanel(new BorderLayout());
    top.setOpaque(false);

    JButton backButton = new JButton("← Admin tools");
    backButton.addActionListener(e -> showAdmin());

    JLabel description = label(
            "Create and review database backups.", 13, false);

    top.add(backButton, BorderLayout.WEST);
    top.add(description, BorderLayout.CENTER);

    String[] columns = {
        "Backup file", "Created", "Size"
    };

    javax.swing.table.DefaultTableModel model =
            new javax.swing.table.DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    JTable table = new JTable(model);
    table.setRowHeight(28);
    table.setFillsViewportHeight(true);
    table.setAutoCreateRowSorter(true);

    JLabel statusLabel = label("Ready.", 13, false);

    JButton createButton = new JButton("Create Backup");
    createButton.addActionListener(e ->
            createAdminBackup(model, statusLabel, createButton));

    JButton refreshButton = new JButton("Refresh");
    refreshButton.addActionListener(e ->
            loadAdminBackups(model, statusLabel));

    JPanel actions = new JPanel(
            new FlowLayout(FlowLayout.RIGHT));
    actions.setOpaque(false);
    actions.add(createButton);
    actions.add(refreshButton);

    JPanel bottom = new JPanel(new BorderLayout());
    bottom.setOpaque(false);
    bottom.add(statusLabel, BorderLayout.CENTER);
    bottom.add(actions, BorderLayout.EAST);

    p.add(top, BorderLayout.NORTH);
    p.add(new JScrollPane(table), BorderLayout.CENTER);
    p.add(bottom, BorderLayout.SOUTH);

    render("Database backups", p);
    loadAdminBackups(model, statusLabel);
}

private void loadAdminUsers(
        javax.swing.table.DefaultTableModel model,
        JLabel statusLabel) {

    model.setRowCount(0);
    statusLabel.setText("Loading users...");

    if (currentUser.getSessionToken() == null ||
            currentUser.getSessionToken().isEmpty()) {
        statusLabel.setText("No valid session token.");
        return;
    }

    new SwingWorker<java.util.List<Object[]>, Void>() {
        @Override
        protected java.util.List<Object[]> doInBackground()
                throws Exception {

            URL url = new URL(
                    FORUM_SERVER + "/api/admin/users");

            HttpURLConnection con =
                    (HttpURLConnection) url.openConnection();

            try {
                con.setRequestMethod("GET");
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);
                con.setRequestProperty(
                        "Authorization",
                        "Bearer " + currentUser.getSessionToken());

                int status = con.getResponseCode();

                InputStream stream = status >= 200 && status < 300
                        ? con.getInputStream()
                        : con.getErrorStream();

                StringBuilder response = new StringBuilder();

                if (stream != null) {
                    try (BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            stream,
                                            StandardCharsets.UTF_8))) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line).append('\n');
                        }
                    }
                }

                if (status != HttpURLConnection.HTTP_OK) {
                    throw new IOException(
                            "Server returned HTTP " + status +
                            (response.length() > 0
                                    ? ": " + response.toString().trim()
                                    : ""));
                }

                java.util.List<Object[]> users =
                        new java.util.ArrayList<>();

                for (String line :
                        response.toString().split("\\R")) {

                    if (line.trim().isEmpty()) continue;

                    String[] parts = line.split("\t", -1);

                    if (parts.length != 5) {
                        throw new IOException(
                                "Unexpected user data from server.");
                    }

                    try {
                        int id = Integer.parseInt(parts[0]);

                        String username =
                                decodeForumField(parts[1]);
                        String displayName =
                                decodeForumField(parts[2]);

String role = decodeForumField(parts[3]);

String accountStatus;
if ("1".equals(parts[4])) {
    accountStatus = "Active";
} else if ("0".equals(parts[4])) {
    accountStatus = "Inactive";
} else {
    throw new IOException("Invalid account status received.");
}

users.add(new Object[] {
    id, username, displayName, role, accountStatus
});

                    } catch (RuntimeException ex) {
                        throw new IOException(
                                "Could not parse user data.", ex);
                    }
                }

                return users;

            } finally {
                con.disconnect();
            }
        }

        @Override
        protected void done() {
            try {
                java.util.List<Object[]> users = get();

                for (Object[] user : users) {
                    model.addRow(user);
                }

                statusLabel.setText(
                        users.size() + " user(s) loaded.");

            } catch (Exception ex) {
                Throwable cause = ex.getCause();
                String message = cause != null
                        ? cause.getMessage()
                        : ex.getMessage();

                statusLabel.setText("Could not load users.");

                JOptionPane.showMessageDialog(
                        Mainframe.this,
                        "Unable to load users:\n" + message +
                        "\n\nCheck that the server is running " +
                        "and that you are signed in as an admin.",
                        "User management",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }.execute();
}

private void loadAdminBackups(
        javax.swing.table.DefaultTableModel model,
        JLabel statusLabel) {

    model.setRowCount(0);
    statusLabel.setText("Loading backups...");

    new SwingWorker<java.util.List<Object[]>, Void>() {
        @Override
        protected java.util.List<Object[]> doInBackground()
                throws Exception {

            URL url = new URL(
                    FORUM_SERVER + "/api/admin/backups");

            HttpURLConnection con =
                    (HttpURLConnection) url.openConnection();

            try {
                con.setRequestMethod("GET");
                con.setConnectTimeout(5000);
                con.setReadTimeout(10000);
                con.setRequestProperty(
                        "Authorization",
                        "Bearer " + currentUser.getSessionToken());

                int code = con.getResponseCode();

                InputStream stream = code >= 200 && code < 300
                        ? con.getInputStream()
                        : con.getErrorStream();

                StringBuilder response = new StringBuilder();

                if (stream != null) {
                    try (BufferedReader reader =
                            new BufferedReader(new InputStreamReader(
                                    stream, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line).append('\n');
                        }
                    }
                }

                if (code != HttpURLConnection.HTTP_OK) {
                    throw new IOException(
                            "Server returned HTTP " + code + ": " +
                            response.toString().trim());
                }

                java.util.List<Object[]> backups =
                        new java.util.ArrayList<>();

                for (String line : response.toString().split("\\R")) {
                    if (line.isBlank()) continue;

                    String[] parts = line.split("\t", -1);
                    if (parts.length != 3) {
                        throw new IOException(
                                "Unexpected backup data from server.");
                    }

                    String filename = decodeForumField(parts[0]);
                    long timestamp = Long.parseLong(parts[1]);
                    long bytes = Long.parseLong(parts[2]);

                    String created = java.time.Instant
                            .ofEpochMilli(timestamp)
                            .atZone(java.time.ZoneId.systemDefault())
                            .format(java.time.format.DateTimeFormatter
                                    .ofPattern("dd MMM yyyy, HH:mm"));

                    String size = bytes < 1024
                            ? bytes + " B"
                            : String.format(java.util.Locale.ROOT,
                                    "%.2f MB", bytes / (1024.0 * 1024.0));

                    backups.add(new Object[] {
                        filename, created, size
                    });
                }

                return backups;

            } finally {
                con.disconnect();
            }
        }

        @Override
        protected void done() {
            try {
                java.util.List<Object[]> backups = get();

                for (Object[] backup : backups) {
                    model.addRow(backup);
                }

                statusLabel.setText(
                        backups.size() + " backup(s) found.");

            } catch (Exception ex) {
                Throwable cause = ex.getCause();
                String message = cause != null
                        ? cause.getMessage() : ex.getMessage();

                statusLabel.setText("Could not load backups.");

                JOptionPane.showMessageDialog(
                        Mainframe.this,
                        "Unable to load backups:\n" + message,
                        "Database backups",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }.execute();
}

private void createAdminBackup(
        javax.swing.table.DefaultTableModel model,
        JLabel statusLabel,
        JButton createButton) {

    int choice = JOptionPane.showConfirmDialog(
            this,
            "Create a backup of the current Acadex database?",
            "Confirm database backup",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

    if (choice != JOptionPane.YES_OPTION) return;

    createButton.setEnabled(false);
    statusLabel.setText("Creating backup...");

    new SwingWorker<String, Void>() {
        @Override
        protected String doInBackground() throws Exception {
            URL url = new URL(
                    FORUM_SERVER + "/api/admin/backups");

            HttpURLConnection con =
                    (HttpURLConnection) url.openConnection();

            try {
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.setConnectTimeout(5000);
                con.setReadTimeout(30000);
                con.setRequestProperty(
                        "Authorization",
                        "Bearer " + currentUser.getSessionToken());

                int code = con.getResponseCode();

                InputStream stream = code >= 200 && code < 300
                        ? con.getInputStream()
                        : con.getErrorStream();

                StringBuilder response = new StringBuilder();

                if (stream != null) {
                    try (BufferedReader reader =
                            new BufferedReader(new InputStreamReader(
                                    stream, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line).append('\n');
                        }
                    }
                }

                if (code != HttpURLConnection.HTTP_OK) {
                    throw new IOException(
                            "Server returned HTTP " + code + ": " +
                            response.toString().trim());
                }

                return response.toString().trim();

            } finally {
                con.disconnect();
            }
        }

        @Override
        protected void done() {
            createButton.setEnabled(true);

            try {
                String result = get();

                JOptionPane.showMessageDialog(
                        Mainframe.this, result,
                        "Backup created",
                        JOptionPane.INFORMATION_MESSAGE);

                loadAdminBackups(model, statusLabel);

            } catch (Exception ex) {
                Throwable cause = ex.getCause();
                String message = cause != null
                        ? cause.getMessage() : ex.getMessage();

                statusLabel.setText("Backup failed.");

                JOptionPane.showMessageDialog(
                        Mainframe.this,
                        "Unable to create backup:\n" + message,
                        "Database backups",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }.execute();
}

private void toggleAdminUser(
        int userId,
        boolean activate,
        javax.swing.table.DefaultTableModel model,
        JLabel statusLabel) {

    statusLabel.setText("Updating account...");

    new SwingWorker<String, Void>() {
        @Override
        protected String doInBackground() throws Exception {
            URL url = new URL(
                    FORUM_SERVER + "/api/admin/users/" +
                    userId + "/status");

            HttpURLConnection con =
                    (HttpURLConnection) url.openConnection();

            try {
                con.setRequestMethod("POST");
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);
                con.setDoOutput(true);

                con.setRequestProperty(
                        "Authorization",
                        "Bearer " + currentUser.getSessionToken());

                con.setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded; charset=UTF-8");

                String body = "active=" + (activate ? "1" : "0");

                try (OutputStream os = con.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = con.getResponseCode();

                InputStream stream =
                        responseCode >= 200 && responseCode < 300
                                ? con.getInputStream()
                                : con.getErrorStream();

                StringBuilder response = new StringBuilder();

                if (stream != null) {
                    try (BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            stream,
                                            StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException(
                            "HTTP " + responseCode + ": " +
                            response.toString());
                }

                return response.toString();

            } finally {
                con.disconnect();
            }
        }

        @Override
        protected void done() {
            try {
                String message = get();

                JOptionPane.showMessageDialog(
                        Mainframe.this, message);

                loadAdminUsers(model, statusLabel);

            } catch (Exception ex) {
                Throwable cause = ex.getCause();
                String message = cause != null
                        ? cause.getMessage()
                        : ex.getMessage();

                statusLabel.setText("Account update failed.");

                JOptionPane.showMessageDialog(
                        Mainframe.this,
                        "Could not update account:\n" + message,
                        "User management",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }.execute();
}
private void showAdminMonitoring() {
    if (currentUser == null || !currentUser.isAdmin()) {
        JOptionPane.showMessageDialog(
                this, "Administrator access required.");
        return;
    }

    JPanel panel = new JPanel(new BorderLayout(10, 10));
    panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    JButton backButton = new JButton("Back to Admin");
    JButton refreshButton = new JButton("Refresh");

    JTextArea output = new JTextArea();
    output.setEditable(false);
    output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
    output.setText("Loading system monitoring...");

    JLabel statusLabel = new JLabel("Connecting to server...");

    JPanel top = new JPanel(new BorderLayout());
    top.add(backButton, BorderLayout.WEST);
    top.add(refreshButton, BorderLayout.EAST);

    panel.add(top, BorderLayout.NORTH);
    panel.add(new JScrollPane(output), BorderLayout.CENTER);
    panel.add(statusLabel, BorderLayout.SOUTH);

    backButton.addActionListener(e -> showAdmin());
    refreshButton.addActionListener(e ->
            loadAdminMonitoring(output, statusLabel, refreshButton));

    render("System Monitoring", panel);
    loadAdminMonitoring(output, statusLabel, refreshButton);
}
private void loadAdminMonitoring(
        JTextArea output,
        JLabel statusLabel,
        JButton refreshButton) {

    refreshButton.setEnabled(false);
    statusLabel.setText("Checking server and database...");

    new SwingWorker<String, Void>() {
        @Override
        protected String doInBackground() throws Exception {
            java.net.HttpURLConnection connection =
                    (java.net.HttpURLConnection)
                    new java.net.URL(
                            FORUM_SERVER + "/api/admin/monitoring")
                            .openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty(
                    "Authorization",
                    "Bearer " + currentUser.getSessionToken());

            try {
                int code = connection.getResponseCode();

                java.io.InputStream stream =
                        code >= 200 && code < 300
                                ? connection.getInputStream()
                                : connection.getErrorStream();

                String response;
                try (java.io.InputStream in = stream) {
                    response = new String(
                            in.readAllBytes(),
                            java.nio.charset.StandardCharsets.UTF_8);
                }

                if (code != 200) {
                    throw new java.io.IOException(
                            "HTTP " + code + ": " + response);
                }

                return response;
            } finally {
                connection.disconnect();
            }
        }

        @Override
        protected void done() {
            refreshButton.setEnabled(true);

            try {
                String response = get();
                StringBuilder formatted = new StringBuilder();

                for (String line : response.split("\\R")) {
                    String[] parts = line.split("\\t", 2);

                    if (parts.length != 2) {
                        continue;
                    }

                    String key = parts[0];
                    String value = parts[1];

                    if (key.equals("server_status") ||
                            key.equals("database_status") ||
                            key.equals("checked_at")) {
                        try {
                            value = new String(
                                    java.util.Base64.getUrlDecoder()
                                            .decode(value),
                                    java.nio.charset.StandardCharsets.UTF_8);
                        } catch (IllegalArgumentException ignored) {
                            // Keep the original value if it isn't encoded.
                        }
                    }

                    String label = switch (key) {
                        case "server_status" -> "Server status";
                        case "database_status" -> "Database status";
                        case "uptime_ms" -> "Server uptime";
                        case "users_total" -> "Total users";
                        case "users_active" -> "Active users";
                        case "users_inactive" -> "Inactive users";
                        case "users_admins" -> "Administrators";
                        case "records_subjects" -> "Subjects";
                        case "records_grades" -> "Grades";
                        case "records_assessments" -> "Assessments";
                        case "records_attendance" -> "Attendance records";
                        case "records_forum_posts" -> "Forum posts";
                        case "records_holidays" -> "Holidays";
                        case "checked_at" -> "Last checked";
                        default -> key;
                    };

                    if (key.equals("uptime_ms")) {
                        try {
                            long seconds = Long.parseLong(value) / 1000;
                            long hours = seconds / 3600;
                            long minutes = (seconds % 3600) / 60;
                            long remaining = seconds % 60;

                            value = hours + "h " + minutes +
                                    "m " + remaining + "s";
                        } catch (NumberFormatException ignored) {
                            // Keep the original value.
                        }
                    }

                    formatted.append(String.format(
                            "%-24s %s%n", label + ":", value));
                }

                output.setText(formatted.toString());
                output.setCaretPosition(0);
                statusLabel.setText("Monitoring data refreshed.");

            } catch (Exception ex) {
                output.setText(
                        "Unable to retrieve monitoring data.\n\n" +
                        ex.getMessage());
                statusLabel.setText("Monitoring request failed.");
            }
        }
    }.execute();
}

private void showAdminAcademicSupport() {
    JPanel panel = new JPanel(new BorderLayout(10, 10));
    panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
    top.add(new JLabel("Select student:"));

    JComboBox<String> studentBox = new JComboBox<>();
    studentBox.setPreferredSize(new Dimension(320, 30));
    top.add(studentBox);

    JButton loadButton = new JButton("Load records");
    top.add(loadButton);

    JTextArea output = new JTextArea();
    output.setEditable(false);
    output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
    output.setText("Select a student and click Load records.");

    panel.add(top, BorderLayout.NORTH);
    panel.add(new JScrollPane(output), BorderLayout.CENTER);

    // Store student IDs separately from the displayed names.
    java.util.Map<String, Integer> studentIds =
            new java.util.LinkedHashMap<>();

    try {
        java.net.HttpURLConnection connection =
                (java.net.HttpURLConnection)
                new java.net.URL(FORUM_SERVER + "/api/admin/users")
                        .openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty(
                "Authorization",
                "Bearer " + currentUser.getSessionToken());

        int status = connection.getResponseCode();

        if (status != 200) {
            throw new java.io.IOException(
                    "Could not load users. HTTP " + status);
        }

        try (java.io.BufferedReader reader =
                     new java.io.BufferedReader(
                             new java.io.InputStreamReader(
                                     connection.getInputStream(),
                                     java.nio.charset.StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t", -1);

                if (parts.length < 5) continue;

                String username = new String(
                        java.util.Base64.getUrlDecoder()
                                .decode(parts[1]),
                        java.nio.charset.StandardCharsets.UTF_8);

                String displayName = new String(
                        java.util.Base64.getUrlDecoder()
                                .decode(parts[2]),
                        java.nio.charset.StandardCharsets.UTF_8);

                String role = new String(
                        java.util.Base64.getUrlDecoder()
                                .decode(parts[3]),
                        java.nio.charset.StandardCharsets.UTF_8);

                if (!"STUDENT".equalsIgnoreCase(role)) continue;

                String label = parts[0] + " — "
                        + displayName + " (" + username + ")";

                studentIds.put(label, Integer.parseInt(parts[0]));
                studentBox.addItem(label);
            }
        } finally {
            connection.disconnect();
        }

    } catch (Exception ex) {
        JOptionPane.showMessageDialog(
                this,
                "Could not load student accounts:\n" + ex.getMessage(),
                "Academic Support",
                JOptionPane.ERROR_MESSAGE);
    }

    loadButton.addActionListener(e -> {
        String selected = (String) studentBox.getSelectedItem();

        if (selected == null) {
            JOptionPane.showMessageDialog(
                    this, "No student account is available.");
            return;
        }

        Integer userId = studentIds.get(selected);
        if (userId == null) return;

        output.setText("Loading academic records...");

        try {
            java.net.HttpURLConnection connection =
                    (java.net.HttpURLConnection)
                    new java.net.URL(
                            FORUM_SERVER
                                    + "/api/admin/academic-support?userId="
                                    + userId)
                            .openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty(
                    "Authorization",
                    "Bearer " + currentUser.getSessionToken());

            int status = connection.getResponseCode();

            java.io.InputStream stream = status >= 400
                    ? connection.getErrorStream()
                    : connection.getInputStream();

            StringBuilder result = new StringBuilder();

            if (stream != null) {
                try (java.io.BufferedReader reader =
                             new java.io.BufferedReader(
                                     new java.io.InputStreamReader(
                                             stream,
                                             java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line).append('\n');
                    }
                }
            }

            if (status != 200) {
                output.setText("Could not load records (HTTP "
                        + status + "):\n" + result);
                return;
            }

            // Decode the server's TABLE / COLUMNS / ROW response.
            StringBuilder formatted = new StringBuilder();
            String currentTable = "";

            for (String line : result.toString().split("\\R")) {
                String[] parts = line.split("\t", -1);

                if (parts.length == 0) continue;

                if ("TABLE".equals(parts[0]) && parts.length > 1) {
                    currentTable = parts[1].toUpperCase(
                            java.util.Locale.ROOT);
                    formatted.append("\n========== ")
                            .append(currentTable)
                            .append(" ==========\n");

                } else if ("COLUMNS".equals(parts[0])) {
                    for (int i = 1; i < parts.length; i++) {
                        if (i > 1) formatted.append(" | ");
                        formatted.append(new String(
                                java.util.Base64.getUrlDecoder()
                                        .decode(parts[i]),
                                java.nio.charset.StandardCharsets.UTF_8));
                    }
                    formatted.append('\n');

                } else if ("ROW".equals(parts[0])) {
                    for (int i = 1; i < parts.length; i++) {
                        if (i > 1) formatted.append(" | ");
                        formatted.append(new String(
                                java.util.Base64.getUrlDecoder()
                                        .decode(parts[i]),
                                java.nio.charset.StandardCharsets.UTF_8));
                    }
                    formatted.append('\n');

                } else if ("UNAVAILABLE".equals(parts[0])) {
                    String message = parts.length > 1
                            ? new String(
                                    java.util.Base64.getUrlDecoder()
                                            .decode(parts[1]),
                                    java.nio.charset.StandardCharsets.UTF_8)
                            : "Table unavailable";

                    formatted.append("Could not read ")
                            .append(currentTable)
                            .append(": ")
                            .append(message)
                            .append('\n');
                }
            }

            output.setText(formatted.length() == 0
                    ? "No academic records were returned."
                    : formatted.toString());
            output.setCaretPosition(0);
            connection.disconnect();

        } catch (Exception ex) {
            output.setText("Could not load academic records:\n"
                    + ex.getMessage());
        }
    });

    render("Academic Support", panel);
}

}
