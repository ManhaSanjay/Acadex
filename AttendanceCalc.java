public final class AttendanceCalc {
    private AttendanceCalc() {}
    public static double percentage(int attended,int held){if(held<=0)return 0.0;return 100.0*attended/held;}
    public static int classesNeeded(int attended,int held,double requiredPercent){if(requiredPercent<=0)return 0;if(requiredPercent>=100)return held-attended;return Math.max(0,(int)Math.ceil((requiredPercent*held-100.0*attended)/(100.0-requiredPercent)));}
    public static String status(int attended,int held){double p=percentage(attended,held);if(held==0)return "No classes recorded";return p>=75?"On track":"Below 75%";}
}
