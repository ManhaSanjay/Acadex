import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame() {

        setTitle("Acadex Login");
        setSize(400, 250);
        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );
        setLocationRelativeTo(null);

        JPanel panel =
                new JPanel(
                        new GridLayout(
                                3,
                                2,
                                10,
                                10
                        )
                );

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        30,
                        30,
                        30,
                        30
                )
        );

        JLabel usernameLabel =
                new JLabel("Username:");

        JLabel passwordLabel =
                new JLabel("Password:");

        usernameField =
                new JTextField();

        passwordField =
                new JPasswordField();

        JButton loginButton =
                new JButton("Login");

        panel.add(usernameLabel);
        panel.add(usernameField);

        panel.add(passwordLabel);
        panel.add(passwordField);

        panel.add(new JLabel());
        panel.add(loginButton);

        add(panel);

        loginButton.addActionListener(
                e -> login()
        );

        setVisible(true);
    }

    private void login() {

        String username =
                usernameField.getText().trim();

        String password =
                new String(
                        passwordField.getPassword()
                );

        if (username.isEmpty() ||
                password.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter username and password."
            );

            return;
        }

        Authentication authentication =
                new Authentication();

        User user =
                authentication.login(
                        username,
                        password
                );

        if (user == null) {

            JOptionPane.showMessageDialog(
                    this,
                    "Invalid username or password.",
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        dispose();

        if (user.getRole().equals("ADMIN")) {

            new AdminFrame(user);

        } else {

            new MainFrame(user);
        }
    }
}