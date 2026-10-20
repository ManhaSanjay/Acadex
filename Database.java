
import java.sql.*;

public final class Database {
    private static int activeUserId = 0;

private static String databaseUrlForUser(int userId) {
    if (userId < 1) {
        throw new IllegalArgumentException("Invalid user ID.");
    }

    if (userId == 1) {
        // Preserve the existing academic database for account 1.
        return "jdbc:sqlite:acadex.db";
    }

    return "jdbc:sqlite:acadex_user_" + userId + ".db";
}

public static synchronized void setActiveUserId(int userId) {
    if (userId < 1) {
        throw new IllegalArgumentException("Invalid user ID.");
    }

    activeUserId = userId;
}

public static Connection getConnectionForUser(int userId)
        throws SQLException {
    Connection c = DriverManager.getConnection(
            databaseUrlForUser(userId));

    try (Statement s = c.createStatement()) {
        s.execute("PRAGMA foreign_keys = ON");
    }

    return c;
}
    private Database() {}

public static Connection getConnection() throws SQLException {
    return getConnectionForUser(activeUserId);
}

// Dedicated connection for the central server database.
// Uses the directory from which Server.java is launched.
public static Connection getServerConnection() throws SQLException {
    Connection c = DriverManager.getConnection("jdbc:sqlite:acadex.db");

    try (Statement s = c.createStatement()) {
        s.execute("PRAGMA foreign_keys = ON");
    }

    return c;
}

    public static void initialize() throws SQLException {
        try (Connection c = getConnection();
             Statement s = c.createStatement()) {

            s.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    display_name TEXT NOT NULL,
                    role TEXT NOT NULL DEFAULT 'STUDENT'
                )
            """);

            s.execute("""
                CREATE TABLE IF NOT EXISTS subjects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    code TEXT NOT NULL UNIQUE,
                    name TEXT NOT NULL,
                    credits INTEGER NOT NULL DEFAULT 3
                )
            """);

            s.execute("""
                CREATE TABLE IF NOT EXISTS grades (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    subject_id INTEGER NOT NULL UNIQUE,
                    cie REAL DEFAULT 0,
                    ese REAL DEFAULT 0,
                    target_grade TEXT DEFAULT 'S',
                    FOREIGN KEY (subject_id)
                        REFERENCES subjects(id) ON DELETE CASCADE
                )
            """);

            s.execute("""
                CREATE TABLE IF NOT EXISTS assessments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    subject_id INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    max_marks REAL NOT NULL DEFAULT 10,
                    marks_obtained REAL DEFAULT NULL,
                    due_date TEXT DEFAULT NULL,
                    FOREIGN KEY (subject_id)
                        REFERENCES subjects(id) ON DELETE CASCADE
                )
            """);

            s.execute("""
                CREATE TABLE IF NOT EXISTS attendance (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    subject_id INTEGER NOT NULL,
                    classes_attended INTEGER NOT NULL DEFAULT 0,
                    classes_held INTEGER NOT NULL DEFAULT 0,
                    attendance_date TEXT DEFAULT NULL,
                    hour INTEGER DEFAULT NULL,
                    present INTEGER DEFAULT NULL,
                    FOREIGN KEY (subject_id)
                        REFERENCES subjects(id) ON DELETE CASCADE
                )
            """);

            s.execute("""
                CREATE TABLE IF NOT EXISTS holidays (
                    holiday_date TEXT PRIMARY KEY,
                    description TEXT DEFAULT ''
                )
            """);

            s.execute("""
                CREATE TABLE IF NOT EXISTS forum_posts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    message TEXT NOT NULL,
                    category TEXT NOT NULL DEFAULT 'General',
                    author TEXT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);
        }

        migrateColumns();
        // Seed demo data only in the original shared database.
// Newly created account databases start with no academic records.
if (activeUserId == 0) {
    seedData();
}
    }

    private static void migrateColumns() throws SQLException {
        ensureColumn("subjects", "code", "TEXT");
        ensureColumn("subjects", "name", "TEXT");
        ensureColumn("subjects", "credits", "INTEGER DEFAULT 3");
        ensureColumn("subjects", "cie_max",
                     "REAL NOT NULL DEFAULT 40");
        ensureColumn("subjects", "ese_max",
                     "REAL NOT NULL DEFAULT 60");

        ensureColumn("grades", "cie", "REAL DEFAULT 0");
        ensureColumn("grades", "ese", "REAL DEFAULT 0");
        ensureColumn("grades", "target_grade", "TEXT DEFAULT 'S'");

        ensureColumn("assessments", "title", "TEXT");
        ensureColumn("assessments", "max_marks",
                     "REAL DEFAULT 10");
        ensureColumn("assessments", "marks_obtained", "REAL");
        ensureColumn("assessments", "due_date", "TEXT");

        ensureColumn("attendance", "classes_attended",
                     "INTEGER DEFAULT 0");
        ensureColumn("attendance", "classes_held",
                     "INTEGER DEFAULT 0");
        ensureColumn("attendance", "attendance_date", "TEXT");
        ensureColumn("attendance", "hour", "INTEGER");
        ensureColumn("attendance", "present", "INTEGER");

        try (Connection c = getConnection();
             Statement s = c.createStatement()) {

            s.executeUpdate("""
                UPDATE subjects
                SET code = 'SUB' || id
                WHERE code IS NULL OR TRIM(code) = ''
            """);

            s.executeUpdate("""
                UPDATE subjects
                SET name = code
                WHERE name IS NULL OR TRIM(name) = ''
            """);
        }
    }

    private static void ensureColumn(
            String table, String column, String definition)
            throws SQLException {

        try (Connection c = getConnection();
             Statement s = c.createStatement();
             ResultSet r =
                 s.executeQuery("PRAGMA table_info(" + table + ")")) {

            while (r.next()) {
                if (column.equalsIgnoreCase(r.getString("name"))) {
                    return;
                }
            }
        }

        try (Connection c = getConnection();
             Statement s = c.createStatement()) {
            s.execute("ALTER TABLE " + table
                + " ADD COLUMN " + column + " " + definition);
        }
    }

    private static void seedData() throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);

            try {
                // Check existing subjects by COURSE CODE only.
                // Never use subject names to identify a subject.
                addSubjectIfMissing(
                    c, "GAMAT301", "Maths", 3);
                addSubjectIfMissing(
                    c, "PBCST304", "OOP", 4);
                addSubjectIfMissing(
                    c, "PCCST303", "DSA", 4);

                // Ensure each subject has a grade record.
                try (Statement s = c.createStatement()) {
                    s.executeUpdate("""
                        INSERT INTO grades (subject_id)
                        SELECT sub.id
                        FROM subjects sub
                        WHERE NOT EXISTS (
                            SELECT 1
                            FROM grades g
                            WHERE g.subject_id = sub.id
                        )
                    """);

                    // Ensure each subject has an aggregate
                    // attendance record.
                    s.executeUpdate("""
                        INSERT INTO attendance
                            (subject_id, classes_attended, classes_held)
                        SELECT sub.id, 0, 0
                        FROM subjects sub
                        WHERE NOT EXISTS (
                            SELECT 1
                            FROM attendance a
                            WHERE a.subject_id = sub.id
                              AND a.attendance_date IS NULL
                        )
                    """);
                }

                //Seeder data
                addAssessmentIfMissing(
                    c, "GAMAT301", "Series 1", 10);
                addAssessmentIfMissing(
                    c, "GAMAT301", "Series 2", 10);
                addAssessmentIfMissing(
                    c, "GAMAT301", "Assignment", 15);

                addAssessmentIfMissing(
                    c, "PBCST304", "Series 1", 10);
                addAssessmentIfMissing(
                    c, "PBCST304", "Series 2", 10);
                addAssessmentIfMissing(
                    c, "PBCST304", "Assignment", 15);

                addAssessmentIfMissing(
                    c, "PCCST303", "Series 1", 10);
                addAssessmentIfMissing(
                    c, "PCCST303", "Series 2", 10);
                addAssessmentIfMissing(
                    c, "PCCST303", "Assignment", 15);

                c.commit();

            } catch (SQLException e) {
                c.rollback();
                throw e;

            } finally {
                c.setAutoCommit(true);
            }
        }

        seedDemoUsers();
    }

    private static void addSubjectIfMissing(
            Connection c, String code, String name, int credits)
            throws SQLException {

        // Course code is the only lookup criterion.
        try (PreparedStatement p = c.prepareStatement(
                "SELECT id FROM subjects WHERE code = ?")) {

            p.setString(1, code);

            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    return;
                }
            }
        }

        try (PreparedStatement p = c.prepareStatement("""
                INSERT INTO subjects (code, name, credits)
                VALUES (?, ?, ?)
                """)) {

            p.setString(1, code);
            p.setString(2, name);
            p.setInt(3, credits);
            p.executeUpdate();
        }
    }


private static void addAssessmentIfMissing(
        Connection c, String code, String title,
        double maxMarks) throws SQLException {

    try (PreparedStatement p = c.prepareStatement("""
        INSERT INTO assessments
            (subject_id, title, max_marks, marks_obtained)
        SELECT sub.id, ?, ?, ?
        FROM subjects sub
        WHERE sub.code = ?
          AND NOT EXISTS (
              SELECT 1
              FROM assessments a
              WHERE a.subject_id = sub.id
                AND a.title = ?
          )
        """)) {

        p.setString(1, title);
        p.setDouble(2, maxMarks);

        // Use zero to satisfy older databases where
        // marks_obtained is NOT NULL.
        p.setDouble(3, 0);

        p.setString(4, code);
        p.setString(5, title);
        p.executeUpdate();
    }
}


    private static void seedDemoUsers() throws SQLException {
        try (Connection c = getConnection();
             PreparedStatement p = c.prepareStatement("""
                INSERT OR IGNORE INTO users
                    (username, password_hash, display_name, role)
                VALUES (?, ?, ?, ?)
                """)) {

            addUser(p, "student", "student123",
                    "Student", "STUDENT");

            addUser(p, "admin", "admin123",
                    "Administrator", "ADMIN");
        }
    }

    private static void addUser(
            PreparedStatement p, String username,
            String password, String displayName, String role)
            throws SQLException {

        p.setString(1, username);
        p.setString(2, PasswordUtil.hash(password));
        p.setString(3, displayName);
        p.setString(4, role);
        p.executeUpdate();
    }
}

