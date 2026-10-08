import java.awt.*;
import javax.swing.*;

public class AdminFrame extends JFrame {

    private User user;

    public AdminFrame(User user) {

        this.user = user;

        setTitle("Acadex - Admin");
        setSize(700, 500);

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        setLocationRelativeTo(null);

        JPanel panel =
                new JPanel(
                        new GridLayout(
                                7,
                                1,
                                10,
                                10
                        )
                );

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        30,
                        50,
                        30,
                        50
                )
        );

        JLabel welcome =
                new JLabel(
                        "Welcome Admin: "
                        + user.getUsername(),
                        SwingConstants.CENTER
                );

        JButton reports =
                new JButton("Generate Reports");

        JButton users =
                new JButton("Manage Users");

        JButton activities =
                new JButton("Monitor Activities");

        JButton notifications =
                new JButton("Send Notifications");

        JButton security =
                new JButton("Backup / Security");

        JButton records =
                new JButton(
                        "Update Student Records"
                );

        panel.add(welcome);
        panel.add(reports);
        panel.add(users);
        panel.add(activities);
        panel.add(notifications);
        panel.add(security);
        panel.add(records);

        add(panel);

        reports.addActionListener(
                e ->
                        JOptionPane.showMessageDialog(
                                this,
                                "Generate Reports"
                        )
        );

        users.addActionListener(
                e ->
                        JOptionPane.showMessageDialog(
                                this,
                                "Manage Users"
                        )
        );

        activities.addActionListener(
                e ->
                        JOptionPane.showMessageDialog(
                                this,
                                "Monitor Activities"
                        )
        );

        notifications.addActionListener(
                e ->
                        JOptionPane.showMessageDialog(
                                this,
                                "Notifications"
                        )
        );

        security.addActionListener(
                e ->
                        JOptionPane.showMessageDialog(
                                this,
                                "Backup / Security"
                        )
        );

        records.addActionListener(
                e ->
                        JOptionPane.showMessageDialog(
                                this,
                                "Update Student Records"
                        )
        );

        setVisible(true);
    }
}