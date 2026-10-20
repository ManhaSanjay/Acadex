
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class LoginFrame extends JFrame {
    private final JTextField username = new JTextField("student", 18);
    private final JPasswordField password =
            new JPasswordField("student123", 18);
    private final UserDAO dao = new UserDAO();

    public LoginFrame() {
        super("Acadex | Sign in");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(440, 320);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
        root.setBackground(new Color(245, 247, 251));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 235)),
                BorderFactory.createEmptyBorder(20, 22, 20, 22)));
        card.setBackground(Color.WHITE);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 5, 6, 5);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;

        JLabel title = new JLabel("ACADEX", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(new Color(45, 65, 105));
        card.add(title, g);

        g.gridy++;
        card.add(new JLabel(
                "Academic Management System", SwingConstants.CENTER), g);

        g.gridwidth = 1;
        g.gridy++;
        g.gridx = 0;
        card.add(new JLabel("Username"), g);
        g.gridx = 1;
        card.add(username, g);

        g.gridx = 0;
        g.gridy++;
        card.add(new JLabel("Password"), g);
        g.gridx = 1;
        card.add(password, g);

        JButton login = new JButton("Sign in");
        JButton register = new JButton("Register");

        login.addActionListener(e -> login());
        register.addActionListener(e -> register());

        g.gridx = 0;
        g.gridy++;
        g.gridwidth = 1;
        card.add(login, g);
        g.gridx = 1;
        card.add(register, g);

        g.gridx = 0;
        g.gridy++;
        g.gridwidth = 2;
        JLabel hint = new JLabel(
                "Demo: student/student123 · admin/admin123",
                SwingConstants.CENTER);
        hint.setFont(new Font("SansSerif", Font.PLAIN, 10));
        card.add(hint, g);

        root.add(card);
        setContentPane(root);
        getRootPane().setDefaultButton(login);
    }

    private void login() {
        try {
            User user = dao.loginRemote(
                    username.getText(),
                    new String(password.getPassword()));

            dispose();
            new Mainframe(user).setVisible(true);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not sign in: " + ex.getMessage() +
                    "\n\nCheck that the Acadex server is running.",
                    "Sign-in failed",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void register() {
        JTextField newUsername = new JTextField(18);
        JTextField displayName = new JTextField(18);
        JPasswordField newPassword = new JPasswordField(18);
        JPasswordField confirmPassword = new JPasswordField(18);

        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("Username (3–24 characters)"));
        panel.add(newUsername);
        panel.add(new JLabel("Display name"));
        panel.add(displayName);
        panel.add(new JLabel("Password (at least 8 characters)"));
        panel.add(newPassword);
        panel.add(new JLabel("Confirm password"));
        panel.add(confirmPassword);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Create an Acadex account",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        String user = newUsername.getText().trim();
        String name = displayName.getText().trim();
        String pass = new String(newPassword.getPassword());
        String confirm = new String(confirmPassword.getPassword());

        if (!pass.equals(confirm)) {
            JOptionPane.showMessageDialog(
                    this, "The passwords don't match.",
                    "Registration", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String message = dao.registerRemote(user, name, pass);
            JOptionPane.showMessageDialog(
                    this, message, "Registration successful",
                    JOptionPane.INFORMATION_MESSAGE);
            username.setText(user);
            password.setText("");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    this, "Could not register: " + ex.getMessage(),
                    "Registration failed", JOptionPane.WARNING_MESSAGE);
        }
    }
}

