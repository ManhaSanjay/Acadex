# Acadex — Java Swing + SQLite

This is a multi-file Java 17 college-project starter. It uses Swing for the UI, JDBC for database access, and SQLite for local storage. No database server or `sudo` is required.

## Files

All Java source files are in the project root (default package, to keep compilation and the `java Main` launch simple). The original team's file names are retained where possible; `Dashboard.java` and `TableRender.java` each exist only once despite appearing under multiple team members in the original task split.

## One-time setup

1. Extract this ZIP into a writable folder, e.g. `~/Acadex`.
2. Put the SQLite JDBC JAR you downloaded (`sqlite-jdbc-3.53.4.0.jar`, or another `sqlite-jdbc-*.jar`) inside the `lib/` folder.
3. Java 17 is required. Compiled `.class` files are included, so you can start with `java Main`.

If you change source files, compile them from the project root:

```bash
javac *.java
```

Then start the application:

```bash
java Main
```

The `Main` launcher adds the JAR in `lib/` to the classpath for the application, so the launch command stays `java Main`.

## Demo accounts

- Student: `student` / `student123`
- Admin: `admin` / `admin123`

These are demo credentials for a college project, not production security. Change them before any real deployment.

## Database and backups

The database `acadex.db` is created automatically in the current working directory. The app initializes tables and a small set of sample subjects on first run. Admin users can create accounts, delete non-default accounts, and create database backups. Backups are written to `backups/`.

## Included features

- Login with student/admin roles
- Dashboard summary
- Subject listing and admin subject creation/deletion
- Grade entry and calculated grade labels
- Attendance tracking and 75% threshold calculation
- Assessment creation/deletion
- Timetable slot creation/deletion
- User management and database backup
- Account information/settings page

## Important notes

- This is a local desktop app. SQLite is suitable for a single-user/local demo; it is not a replacement for a shared multi-computer database server.
- Demo student and admin accounts are seeded on first database initialization.
- A student's screens are read-only in this starter; admin screens provide editing controls. Per-student data isolation and institutional authentication would need additional requirements and implementation.
- Keep `acadex.db` and the `backups/` folder safe; they contain application data.
