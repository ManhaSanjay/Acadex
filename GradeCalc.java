class GradeCalculator {
//Calculate total
    public double calculateTotal(double cie, double ese) {
        return cie + ese;
    }

//Calculate Grade
    public String calculateGrade(double total) {

        if (total >= 90)
            return "S";
        else if (total >= 85)
            return "A+";
        else if (total >= 80)
            return "A";
        else if (total >= 75)
            return "B+";
        else if (total >= 70)
            return "B";
        else if (total >= 65)
            return "C+";
        else if (total >= 60)
            return "C";
	else if (total >= 55)
            return "D";
        else if (total >= 50)
            return "P";
	else
		return "F";
    }

//Calculate how many marks needed to get target grade
    public double calculateRequiredESE(double cie,
                                       double targetTotal) {

        double requiredESE = targetTotal - cie;

        if (requiredESE <= 0)
            return 0;

        return requiredESE;
    }

 // Calculate SGPA
    public double calculateSGPA(double[] credits, double[] gradePoints) {

        double totalCreditPoints = 0;
        double totalCredits = 0;

        for (int i = 0; i < credits.length; i++) {

            totalCreditPoints =
                    totalCreditPoints + (credits[i] * gradePoints[i]);

            totalCredits = totalCredits + credits[i];
        }

        return totalCreditPoints / totalCredits;
    }

  // Calculate CGPA
    public double calculateCGPA(double[] sgpa, double[] semesterCredits) {

        double totalWeightedSGPA = 0;
        double totalCredits = 0;

        for (int i = 0; i < sgpa.length; i++) {

            totalWeightedSGPA =
                    totalWeightedSGPA + (sgpa[i] * semesterCredits[i]);

            totalCredits = totalCredits + semesterCredits[i];
        }

        return totalWeightedSGPA / totalCredits;
    }
}
