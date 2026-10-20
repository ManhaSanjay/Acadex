public class GradeRecord {
    private final int id, subjectId; private final double cie, ese; private final String targetGrade;
    public GradeRecord(int id,int subjectId,double cie,double ese,String targetGrade){this.id=id;this.subjectId=subjectId;this.cie=cie;this.ese=ese;this.targetGrade=targetGrade;}
    public int getId(){return id;} public int getSubjectId(){return subjectId;} public double getCie(){return cie;} public double getEse(){return ese;} public String getTargetGrade(){return targetGrade;}
    public double total(){return cie+ese;}
}
