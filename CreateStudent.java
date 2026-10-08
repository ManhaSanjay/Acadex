public class CreateStudent {

    public static void main(String[] args) {

        CreateUserTable.createTable();

        Authentication authentication =
                new Authentication();

        boolean success =
                authentication.registerUser(
                        "student1",
                        "student123",
                        "STUDENT",
                        "S1"
                );

        if (success) {

            System.out.println(
                    "Student account created successfully."
            );

        } else {

            System.out.println(
                    "Student account could not be created."
            );
        }
    }
}