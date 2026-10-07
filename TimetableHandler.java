import java.time.DayOfWeek;
import java.time.LocalDate;

public class TimetableHandler {

    private Timetable timetable;
    private Database database;

    public TimetableHandler(Timetable timetable, Database database) {
        this.timetable = timetable;
        this.database = database;
    }

    // SET SUBJECT
    public void setSubject(int day, int period, Subject subject) {

        Subject[][] schedule = timetable.getTimetable();

        if (day < 0 || day >= schedule.length) {
            return;
        }

        if (period < 0 || period >= schedule[day].length) {
            return;
        }

        schedule[day][period] = subject;
    }


    // GET SCHEDULE
    public Subject[] getSchedule(LocalDate date) {

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        int day = dayOfWeek.getValue() - 1;

        Subject[][] schedule = timetable.getTimetable();

        if (day < 0 || day >= schedule.length) {
            return null;
        }

        return schedule[day];
    }


    // EDIT TIMETABLE
    public void editTimetable(int day, int period, Subject newSubject) {

        Subject[][] schedule = timetable.getTimetable();

        if (day < 0 || day >= schedule.length) {
            return;
        }

        if (period < 0 || period >= schedule[day].length) {
            return;
        }

        schedule[day][period] = newSubject;
    }


    // SAVE TIMETABLE
    public void saveTimetable() {

        Subject[][] schedule = timetable.getTimetable();

        for (int i = 0; i < schedule.length; i++) {

            for (int j = 0; j < schedule[i].length; j++) {

                Subject subject = schedule[i][j];

                if (subject != null) {

                    database.saveTimetableEntry(
                        i,
                        j,
                        subject.getSubId()
                    );
                }
            }
        }
    }
}


// TIMETABLE CLASS
class Timetable {

    private Subject[][] timetable;

    public Timetable(int numberOfDays, int numberOfPeriods) {
        timetable = new Subject[numberOfDays][numberOfPeriods];
    }

    public Subject[][] getTimetable() {
        return timetable;
    }

    public void setSubject(int day, int period, Subject subject) {
        timetable[day][period] = subject;
    }

    public Subject getSubject(int day, int period) {
        return timetable[day][period];
    }
}

//LOAD TIMETABLE
public void LoadTimetable() {

    // Get timetable rows from the database
    ResultSet rs = database.loadTimetable();

    try {
        while (rs.next()) {

            int day = rs.getInt("day");
            int period = rs.getInt("period");
            int subjectId = rs.getInt("subject_id");

            // Create/fetch the Subject using the subject ID
            Subject subject = new Subject(subjectId);

            // Put the subject into the timetable
            timetable[day][period] = subject;
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
}
