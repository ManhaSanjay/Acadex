public class CreateAdmin {

    public static void main(String[] args) {

        CreateUserTable.createTable();

        Authentication authentication =
                new Authentication();

        boolean success =
                authentication.registerUser(
                        "admin",
                        "admin123",
                        "ADMIN",
                        null
                );

        if (success) {

            System.out.println(
                    "Admin account created successfully."
            );

        } else {

            System.out.println(
                    "Admin account could not be created."
            );
        }
    }
}