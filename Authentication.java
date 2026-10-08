public class Authentication {

    private UserDAO userDAO;

    public Authentication() {

        userDAO =
                new UserDAO();
    }

    public User login(
            String username,
            String password) {

        User user =
                userDAO.getUser(username);

        if (user == null) {
            return null;
        }

        boolean valid =
                PasswordUtil.verifyPassword(
                        password,
                        user.getPasswordHash()
                );

        if (valid) {
            return user;
        }

        return null;
    }

    public boolean registerUser(
            String username,
            String password,
            String role,
            String semester) {

        if (username == null ||
                username.trim().isEmpty()) {

            return false;
        }

        if (password == null ||
                password.isEmpty()) {

            return false;
        }

        if (role == null ||
                role.trim().isEmpty()) {

            return false;
        }

        role =
                role.trim().toUpperCase();

        if (!role.equals("ADMIN") &&
                !role.equals("STUDENT")) {

            return false;
        }

        if (role.equals("STUDENT")) {

            if (semester == null ||
                    semester.trim().isEmpty()) {

                return false;
            }

            semester =
                    semester.trim();

        } else {

            semester = null;
        }

        String passwordHash =
                PasswordUtil.hashPassword(password);

        User user =
                new User(
                        username.trim(),
                        passwordHash,
                        role,
                        semester
                );

        return userDAO.addUser(user);
    }
}