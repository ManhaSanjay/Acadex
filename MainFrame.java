import javax.swing.*;

public class MainFrame extends JFrame {

    private Dashboard dashboard;
    private TableRender grades;
    private Settings settings;

    public MainFrame(User user) {

        setTitle("Acadex");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        dashboard =
                new Dashboard(
                        user.getUsername(),
                        user.getSemester()
                );

        grades = new TableRender();
        settings = new Settings();

        JTabbedPane tabs =
                new JTabbedPane();

        tabs.addTab(
                "Dashboard",
                dashboard.getContentPane()
        );

        tabs.addTab(
                "Grades",
                grades.getContentPane()
        );

        tabs.addTab(
                "Settings",
                settings
        );

        add(tabs);

        setVisible(true);
    }
}