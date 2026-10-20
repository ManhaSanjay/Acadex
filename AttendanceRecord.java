public class AttendanceRecord {
    private final int id,subjectId,classesHeld,classesAttended;
    public AttendanceRecord(int id,int subjectId,int classesHeld,int classesAttended){this.id=id;this.subjectId=subjectId;this.classesHeld=classesHeld;this.classesAttended=classesAttended;}
    public int getId(){return id;} public int getSubjectId(){return subjectId;} public int getClassesHeld(){return classesHeld;} public int getClassesAttended(){return classesAttended;}
    public double percentage(){return AttendanceCalc.percentage(classesAttended,classesHeld);}
}
