public class AttendanceCalculator {

    public static final double REQUIRED_PERCENTAGE = 75.0;


    // =========================================================
    // CALCULATE ATTENDANCE PERCENTAGE
    // =========================================================

    public static double calculatePercentage(
            int attended,
            int total) {

        if (total == 0) {

            return 0;
        }

        return
                ((double) attended / total)
                        * 100;
    }


    // =========================================================
    // CHECK IF ATTENDANCE IS SAFE
    // =========================================================

    public static boolean isSafe(
            int attended,
            int total) {

        return calculatePercentage(
                attended,
                total
        ) >= REQUIRED_PERCENTAGE;
    }


    // =========================================================
    // CLASSES NEEDED TO REACH 75%
    // =========================================================

    public static int classesNeeded(
            int attended,
            int total) {

        int needed = 0;


        while (
                calculatePercentage(
                        attended + needed,
                        total + needed
                )
                < REQUIRED_PERCENTAGE
        ) {

            needed++;
        }


        return needed;
    }
}
