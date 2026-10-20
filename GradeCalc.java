public final class GradeCalc {
    private GradeCalc() {}
    public static String grade(double total){if(total>=90)return "S";if(total>=85)return "A+";if(total>=80)return "A";if(total>=70)return "B+";if(total>=60)return "B";if(total>=50)return "C";if(total>=40)return "P";return "F";}
    public static double clamp(double value,double max){return Math.max(0,Math.min(max,value));}
    public static double requiredEse(double cie,double targetTotal){return Math.max(0,Math.min(60,targetTotal-cie));}
}
