import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

public class Dashboard extends JFrame {

    // =========================================================
    // STUDENT INFORMATION
    // =========================================================

    private String studentName;
    private String semester;


    // =========================================================
    // UI COMPONENTS
    // =========================================================

    private JPanel contentPanel;

    private JTable timetableTable;
    private JTable attendanceTable;

    private DefaultTableModel timetableModel;
    private DefaultTableModel attendanceModel;


    // =========================================================
    // DATABASE
    // =========================================================

    private Database database;


    // =========================================================
    // CURRENT DAY
    // =========================================================

    private String selectedDay = "Monday";


    // =========================================================
    // EXISTING ATTENDANCE
    //
    // This represents attendance that already existed before
    // today's attendance was taken.
    //
    // Example:
    // OOP = 18 attended out of 20 classes
    // =========================================================

    private Map<String, int[]> subjectAttendance =
            new LinkedHashMap<>();


    // =========================================================
    // SAVED ATTENDANCE FOR CURRENT SESSION
    //
    // Each timetable slot gets one unique key:
    //
    // Date + Day + Hour
    //
    // Example:
    // 2026-09-07-Monday-1
    //
    // This prevents Monday attendance from disappearing when
    // Tuesday is opened.
    // =========================================================

    private Map<String, AttendanceRecord> savedAttendance =
            new HashMap<>();


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public Dashboard(
            String studentName,
            String semester) {

        this.studentName = studentName;
        this.semester = semester;

        database = new Database();

        initializeTemporaryAttendance();

        setTitle("Acadex - Dashboard");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        createUI();

        showHomePage();
    }


    // =========================================================
    // EXISTING ATTENDANCE
    // =========================================================

    private void initializeTemporaryAttendance() {

        subjectAttendance.put(
                "Mathematics",
                new int[]{18, 20}
        );

        subjectAttendance.put(
                "OOP",
                new int[]{18, 20}
        );

        subjectAttendance.put(
                "DSA",
                new int[]{14, 20}
        );

        subjectAttendance.put(
                "DSLD",
                new int[]{17, 20}
        );

        subjectAttendance.put(
                "DIG",
                new int[]{15, 20}
        );

        subjectAttendance.put(
                "DSA Lab",
                new int[]{8, 10}
        );

        subjectAttendance.put(
                "EESD",
                new int[]{17, 20}
        );

        subjectAttendance.put(
                "TOC",
                new int[]{13, 20}
        );
    }


    // =========================================================
    // MAIN UI
    // =========================================================

    private void createUI() {

        setLayout(new BorderLayout());


        // -----------------------------------------------------
        // TOP BAR
        // -----------------------------------------------------

        JPanel topBar =
                new JPanel(new BorderLayout());

        topBar.setBorder(
                BorderFactory.createEmptyBorder(
                        15, 20, 15, 20
                )
        );


        JLabel logo =
                new JLabel("ACADEX");

        logo.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        25
                )
        );

        topBar.add(
                logo,
                BorderLayout.WEST
        );


        // -----------------------------------------------------
        // NAVIGATION
        // -----------------------------------------------------

        JPanel navigation =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                10,
                                0
                        )
                );


        JButton homeButton =
                new JButton("Home");

        JButton attendanceButton =
                new JButton("Attendance");

        JButton gradesButton =
                new JButton("Grades");


        homeButton.addActionListener(
                e -> showHomePage()
        );

        attendanceButton.addActionListener(
                e -> showAttendancePage()
        );

        gradesButton.addActionListener(
                e -> showGradesPage()
        );


        navigation.add(homeButton);
        navigation.add(attendanceButton);
        navigation.add(gradesButton);


        topBar.add(
                navigation,
                BorderLayout.EAST
        );


        add(
                topBar,
                BorderLayout.NORTH
        );


        // -----------------------------------------------------
        // CONTENT PANEL
        // -----------------------------------------------------

        contentPanel =
                new JPanel(
                        new BorderLayout()
                );

        add(
                contentPanel,
                BorderLayout.CENTER
        );
    }


    // =========================================================
    // HOME PAGE
    // =========================================================

    private void showHomePage() {

        JLabel pageTitle =
                new JLabel(
                        "Welcome, " + studentName
                );

        pageTitle.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        28
                )
        );


        JLabel semesterLabel =
                new JLabel(
                        "Semester: " + semester
                );

        semesterLabel.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        15
                )
        );


        JPanel header =
                new JPanel(
                        new BorderLayout()
                );

        header.setBorder(
                BorderFactory.createEmptyBorder(
                        20, 30, 10, 30
                )
        );


        header.add(
                pageTitle,
                BorderLayout.NORTH
        );

        header.add(
                semesterLabel,
                BorderLayout.SOUTH
        );


        // -----------------------------------------------------
        // DATE
        // -----------------------------------------------------

        String today =
                new SimpleDateFormat(
                        "dd MMM yyyy"
                ).format(new Date());


        JLabel dateLabel =
                new JLabel(
                        "Today: " + today
                );

        dateLabel.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        14
                )
        );


        header.add(
                dateLabel,
                BorderLayout.EAST
        );


        // -----------------------------------------------------
        // SUMMARY
        // -----------------------------------------------------

        JPanel cards =
                new JPanel(
                        new GridLayout(
                                1,
                                3,
                                20,
                                20
                        )
                );


        cards.setBorder(
                BorderFactory.createEmptyBorder(
                        20, 30, 20, 30
                )
        );


        int safeSubjects = 0;

        int totalSubjects =
                subjectAttendance.size();


        double overallPercentage = 0;


        // Get the current attendance including
        // today's saved attendance.

        Map<String, int[]> currentAttendance =
                calculateCurrentAttendance();


        for (Map.Entry<String, int[]> entry :
                currentAttendance.entrySet()) {

            int attended =
                    entry.getValue()[0];

            int total =
                    entry.getValue()[1];


            double percentage =
                    AttendanceCalculator
                            .calculatePercentage(
                                    attended,
                                    total
                            );


            overallPercentage += percentage;


            if (percentage >= 75) {
                safeSubjects++;
            }
        }


        if (totalSubjects > 0) {

            overallPercentage =
                    overallPercentage /
                            totalSubjects;
        }


        cards.add(
                createCard(
                        "Attendance",
                        String.format(
                                "%.1f%%",
                                overallPercentage
                        )
                )
        );


        cards.add(
                createCard(
                        "Subjects Safe",
                        safeSubjects +
                                " / " +
                                totalSubjects
                )
        );


        cards.add(
                createCard(
                        "Semester",
                        semester
                )
        );


        JPanel center =
                new JPanel(
                        new BorderLayout()
                );


        center.add(
                cards,
                BorderLayout.NORTH
        );


        // -----------------------------------------------------
        // QUICK ACTION
        // -----------------------------------------------------

        JPanel quickPanel =
                new JPanel();


        quickPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        20, 30, 20, 30
                )
        );


        JButton attendanceButton =
                new JButton(
                        "Take Today's Attendance"
                );


        attendanceButton.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        15
                )
        );


        attendanceButton.addActionListener(
                e -> showAttendancePage()
        );


        quickPanel.add(
                attendanceButton
        );


        center.add(
                quickPanel,
                BorderLayout.CENTER
        );


        contentPanel.removeAll();


        contentPanel.add(
                header,
                BorderLayout.NORTH
        );


        contentPanel.add(
                center,
                BorderLayout.CENTER
        );


        contentPanel.revalidate();
        contentPanel.repaint();
    }


    // =========================================================
    // CREATE HOME CARD
    // =========================================================

    private JPanel createCard(
            String title,
            String value) {

        JPanel card =
                new JPanel(
                        new BorderLayout()
                );


        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                Color.LIGHT_GRAY
                        ),
                        BorderFactory.createEmptyBorder(
                                20, 20, 20, 20
                        )
                )
        );


        JLabel titleLabel =
                new JLabel(title);


        titleLabel.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        14
                )
        );


        JLabel valueLabel =
                new JLabel(value);


        valueLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        25
                )
        );


        card.add(
                titleLabel,
                BorderLayout.NORTH
        );


        card.add(
                valueLabel,
                BorderLayout.CENTER
        );


        return card;
    }


    // =========================================================
    // ATTENDANCE PAGE
    // =========================================================

    private void showAttendancePage() {

        JLabel pageTitle =
                new JLabel("Attendance");


        pageTitle.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        28
                )
        );


        // -----------------------------------------------------
        // DAY BUTTONS
        // -----------------------------------------------------

        JPanel dayPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT
                        )
                );


        String[] days = {
                "Monday",
                "Tuesday",
                "Wednesday",
                "Thursday",
                "Friday"
        };


        for (String day : days) {

            JButton dayButton =
                    new JButton(day);


            dayButton.addActionListener(
                    e -> {

                        /*
                         * If the user has edited the current
                         * table but hasn't clicked Save, those
                         * edits are intentionally not saved.
                         *
                         * Attendance is saved only when the
                         * Save Attendance button is clicked.
                         */

                        selectedDay = day;

                        loadTimetable(day);
                    }
            );


            dayPanel.add(dayButton);
        }


        // -----------------------------------------------------
        // HEADER
        // -----------------------------------------------------

        JPanel header =
                new JPanel(
                        new BorderLayout()
                );


        header.setBorder(
                BorderFactory.createEmptyBorder(
                        15, 20, 10, 20
                )
        );


        header.add(
                pageTitle,
                BorderLayout.NORTH
        );


        header.add(
                dayPanel,
                BorderLayout.SOUTH
        );


        // =====================================================
        // TIMETABLE TABLE
        // =====================================================

        timetableModel =
                new DefaultTableModel(
                        new Object[]{
                                "Hour",
                                "Original Subject",
                                "Actual Subject",
                                "Attendance"
                        },
                        0
                ) {

                    @Override
                    public boolean isCellEditable(
                            int row,
                            int column) {

                        /*
                         * Only:
                         *
                         * column 2 = Actual Subject
                         * column 3 = Attendance
                         */

                        return column == 2 ||
                                column == 3;
                    }


                    @Override
                    public Class<?> getColumnClass(
                            int column) {

                        if (column == 3) {
                            return Boolean.class;
                        }

                        return Object.class;
                    }
                };


        timetableTable =
                new JTable(
                        timetableModel
                );


        timetableTable.setRowHeight(40);


        timetableTable.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        14
                )
        );


        // -----------------------------------------------------
        // SUBJECT DROPDOWN
        // -----------------------------------------------------

        String[] subjects =
                TimetableHandler
                        .getSubjects(semester);


        JComboBox<String> subjectCombo =
                new JComboBox<>();


        for (String subject : subjects) {
            subjectCombo.addItem(subject);
        }


        subjectCombo.addItem(
                "Free Session"
        );


        timetableTable
                .getColumnModel()
                .getColumn(2)
                .setCellEditor(
                        new DefaultCellEditor(
                                subjectCombo
                        )
                );


        // -----------------------------------------------------
        // ATTENDANCE RENDERER
        // -----------------------------------------------------

        timetableTable
                .getColumnModel()
                .getColumn(3)
                .setCellRenderer(
                        new AttendanceRenderer()
                );


        JScrollPane timetableScroll =
                new JScrollPane(
                        timetableTable
                );


        // =====================================================
        // SAVE BUTTON
        // =====================================================

        JButton saveButton =
                new JButton(
                        "Save Attendance"
                );


        saveButton.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        14
                )
        );


        saveButton.addActionListener(
                e -> saveAttendance()
        );


        JPanel timetablePanel =
                new JPanel(
                        new BorderLayout()
                );


        timetablePanel.setBorder(
                BorderFactory.createEmptyBorder(
                        0, 20, 10, 20
                )
        );


        timetablePanel.add(
                timetableScroll,
                BorderLayout.CENTER
        );


        timetablePanel.add(
                saveButton,
                BorderLayout.SOUTH
        );


        // =====================================================
        // ATTENDANCE SUMMARY TABLE
        // =====================================================

        attendanceModel =
                new DefaultTableModel(
                        new Object[]{
                                "Subject",
                                "Attended",
                                "Total",
                                "Percentage",
                                "Status",
                                "Classes Needed"
                        },
                        0
                ) {

                    @Override
                    public boolean isCellEditable(
                            int row,
                            int column) {

                        return false;
                    }
                };


        attendanceTable =
                new JTable(
                        attendanceModel
                );


        attendanceTable.setRowHeight(35);


        JScrollPane attendanceScroll =
                new JScrollPane(
                        attendanceTable
                );


        attendanceScroll.setBorder(
                BorderFactory.createTitledBorder(
                        "Attendance Summary"
                )
        );


        // =====================================================
        // MAIN CONTENT
        // =====================================================

        JPanel mainPanel =
                new JPanel(
                        new GridLayout(
                                2,
                                1
                        )
                );


        mainPanel.add(
                timetablePanel
        );


        mainPanel.add(
                attendanceScroll
        );


        contentPanel.removeAll();


        contentPanel.add(
                header,
                BorderLayout.NORTH
        );


        contentPanel.add(
                mainPanel,
                BorderLayout.CENTER
        );


        // -----------------------------------------------------
        // LOAD CURRENT DAY
        // -----------------------------------------------------

        loadTimetable(selectedDay);


        // -----------------------------------------------------
        // LOAD ATTENDANCE SUMMARY
        // -----------------------------------------------------

        updateAttendanceTable();


        contentPanel.revalidate();
        contentPanel.repaint();
    }


    // =========================================================
    // LOAD TIMETABLE
    // =========================================================

    private void loadTimetable(String day) {

        if (timetableModel == null) {
            return;
        }


        // Remove old rows.

        timetableModel.setRowCount(0);


        // Get timetable from Shedha's program.

        List<String[]> timetable =
                TimetableHandler
                        .getTimetable(
                                semester,
                                day
                        );


        for (String[] row : timetable) {

            int hour =
                    Integer.parseInt(row[0]);


            String originalSubject =
                    row[1];


            // -------------------------------------------------
            // UNIQUE KEY
            //
            // Date + Day + Hour
            // -------------------------------------------------

            String key =
                    getAttendanceKey(
                            day,
                            hour
                    );


            // -------------------------------------------------
            // DEFAULT VALUES
            //
            // New attendance is automatically PRESENT.
            // -------------------------------------------------

            String actualSubject =
                    originalSubject;


            boolean present = true;


            // -------------------------------------------------
            // CHECK WHETHER THIS SLOT WAS ALREADY SAVED
            // -------------------------------------------------

            if (savedAttendance.containsKey(key)) {

                AttendanceRecord record =
                        savedAttendance.get(key);


                actualSubject =
                        record.getSubject();


                present =
                        record.isPresent();
            }


            // -------------------------------------------------
            // ADD ROW
            // -------------------------------------------------

            timetableModel.addRow(
                    new Object[]{
                            hour,
                            originalSubject,
                            actualSubject,
                            present
                    }
            );
        }
    }


    // =========================================================
    // CREATE UNIQUE ATTENDANCE KEY
    // =========================================================

    private String getAttendanceKey(
            String day,
            int hour) {

        String date =
                new SimpleDateFormat(
                        "yyyy-MM-dd"
                ).format(
                        new Date()
                );


        return date +
                "-" +
                day +
                "-" +
                hour;
    }


    // =========================================================
    // SAVE ATTENDANCE
    // =========================================================

    private void saveAttendance() {

        // -----------------------------------------------------
        // IMPORTANT:
        // If the user is currently editing a cell, commit
        // that edit before reading the table.
        // -----------------------------------------------------

        if (timetableTable.isEditing()) {

            timetableTable
                    .getCellEditor()
                    .stopCellEditing();
        }


        List<AttendanceRecord> records =
                new ArrayList<>();


        String date =
                new SimpleDateFormat(
                        "yyyy-MM-dd"
                ).format(
                        new Date()
                );


        // -----------------------------------------------------
        // READ EVERY TIMETABLE ROW
        // -----------------------------------------------------

        for (
                int i = 0;
                i < timetableModel.getRowCount();
                i++
        ) {

            int hour =
                    (int) timetableModel
                            .getValueAt(
                                    i,
                                    0
                            );


            String actualSubject =
                    timetableModel
                            .getValueAt(
                                    i,
                                    2
                            )
                            .toString();


            Object attendanceValue =
                    timetableModel
                            .getValueAt(
                                    i,
                                    3
                            );


            boolean present =
                    Boolean.TRUE.equals(
                            attendanceValue
                    );


            // -------------------------------------------------
            // UNIQUE KEY
            // -------------------------------------------------

            String key =
                    getAttendanceKey(
                            selectedDay,
                            hour
                    );


            // -------------------------------------------------
            // FREE SESSION
            // -------------------------------------------------

            if (
                    actualSubject.equals(
                            "Free Session"
                    )
            ) {

                /*
                 * If this slot had an old attendance record,
                 * remove it.
                 */

                savedAttendance.remove(key);

                continue;
            }


            // -------------------------------------------------
            // CREATE ATTENDANCE RECORD
            // -------------------------------------------------

            AttendanceRecord record =
                    new AttendanceRecord(
                            actualSubject,
                            date,
                            hour,
                            present
                    );


            // -------------------------------------------------
            // SAVE / UPDATE RECORD
            // -------------------------------------------------

            /*
             * If Monday-1 was already saved, this replaces
             * the old Monday-1 record.
             *
             * It does NOT create a duplicate.
             */

            savedAttendance.put(
                    key,
                    record
            );


            records.add(record);
        }


        // =====================================================
        // SEND TO DATABASE
        //
        // Abiya's Database.java will eventually store these
        // records in MySQL.
        // =====================================================

        database.saveAttendance(
                records
        );


        // =====================================================
        // UPDATE SUMMARY
        // =====================================================

        updateAttendanceTable();


        JOptionPane.showMessageDialog(
                this,
                selectedDay +
                        " attendance saved successfully!",
                "Acadex",
                JOptionPane.INFORMATION_MESSAGE
        );
    }


    // =========================================================
    // CALCULATE CURRENT ATTENDANCE
    // =========================================================

    private Map<String, int[]> calculateCurrentAttendance() {

        /*
         * Start with the old attendance.
         *
         * Example:
         *
         * OOP = 18/20
         */

        Map<String, int[]> current =
                new LinkedHashMap<>();


        for (
                Map.Entry<String, int[]> entry :
                subjectAttendance.entrySet()
        ) {

            current.put(
                    entry.getKey(),
                    new int[]{
                            entry.getValue()[0],
                            entry.getValue()[1]
                    }
            );
        }


        // -----------------------------------------------------
        // ADD SAVED ATTENDANCE
        // -----------------------------------------------------

        for (
                AttendanceRecord record :
                savedAttendance.values()
        ) {

            String subject =
                    record.getSubject();


            // Ignore unknown subjects.

            if (!current.containsKey(subject)) {
                continue;
            }


            int[] values =
                    current.get(subject);


            // One more class has now happened.

            values[1]++;


            // If present, attended also increases.

            if (record.isPresent()) {
                values[0]++;
            }
        }


        return current;
    }


    // =========================================================
    // UPDATE ATTENDANCE SUMMARY TABLE
    // =========================================================

    private void updateAttendanceTable() {

        if (attendanceModel == null) {
            return;
        }


        attendanceModel.setRowCount(0);


        // Get attendance including today's saved records.

        Map<String, int[]> currentAttendance =
                calculateCurrentAttendance();


        for (
                Map.Entry<String, int[]> entry :
                currentAttendance.entrySet()
        ) {

            String subject =
                    entry.getKey();


            int attended =
                    entry.getValue()[0];


            int total =
                    entry.getValue()[1];


            double percentage =
                    AttendanceCalculator
                            .calculatePercentage(
                                    attended,
                                    total
                            );


            String status;


            if (percentage >= 75) {
                status = "SAFE";
            } else {
                status = "LOW";
            }


            int classesNeeded =
                    AttendanceCalculator
                            .classesNeeded(
                                    attended,
                                    total
                            );


            attendanceModel.addRow(
                    new Object[]{
                            subject,
                            attended,
                            total,
                            String.format(
                                    "%.1f%%",
                                    percentage
                            ),
                            status,
                            classesNeeded
                    }
            );
        }
    }


    // =========================================================
    // GRADES PAGE
    // =========================================================

    private void showGradesPage() {

        JPanel panel =
                new JPanel(
                        new BorderLayout()
                );


        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        40, 40, 40, 40
                )
        );


        JLabel title =
                new JLabel(
                        "Grades"
                );


        title.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        28
                )
        );


        JLabel message =
                new JLabel(
                        "Grade management will be added here."
                );


        message.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        16
                )
        );


        panel.add(
                title,
                BorderLayout.NORTH
        );


        panel.add(
                message,
                BorderLayout.CENTER
        );


        contentPanel.removeAll();


        contentPanel.add(
                panel,
                BorderLayout.CENTER
        );


        contentPanel.revalidate();
        contentPanel.repaint();
    }


    // =========================================================
    // ATTENDANCE RENDERER
    // =========================================================

    private static class AttendanceRenderer
            extends JCheckBox
            implements TableCellRenderer {


        public AttendanceRenderer() {

            setHorizontalAlignment(
                    SwingConstants.CENTER
            );

            setOpaque(true);
        }


        @Override
        public Component
        getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {


            boolean present =
                    value != null &&
                    (Boolean) value;


            setSelected(present);


            if (present) {

                setText("Present");

            } else {

                setText("Absent");
            }


            if (isSelected) {

                setBackground(
                        table.getSelectionBackground()
                );

                setForeground(
                        table.getSelectionForeground()
                );

            } else {

                setBackground(
                        table.getBackground()
                );

                setForeground(
                        table.getForeground()
                );
            }


            return this;
        }
    }


    // =========================================================
    // MAIN
    // =========================================================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(
                () -> {

                    Dashboard dashboard =
                            new Dashboard(
                                    "Student",
                                    "S3"
                            );

                    dashboard.setVisible(true);
                }
        );
    }
}
