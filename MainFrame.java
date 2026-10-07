import javax.swing.*;

public class MainFrame extends JFrame {

    private Dashboard dashboard;
    private TableRender grades;
    private Settings settings;

    public MainFrame() {

        setTitle("Acadex");
        setSize(1200,700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        dashboard = new Dashboard();
        grades = new TableRender();
        settings = new Settings();

        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Dashboard", dashboard);
        tabs.addTab("Grades", grades);
        tabs.addTab("Settings", settings);

        add(tabs);

        setVisible(true);
    }

    public boolean showConfirmationDialog(String msg){

        int result = JOptionPane.showConfirmDialog(
                this,
                msg,
                "Confirm",
                JOptionPane.YES_NO_OPTION
        );

        return result == JOptionPane.YES_OPTION;
    }

    public void showSuccessMessage(String msg){
        JOptionPane.showMessageDialog(this,msg);
    }

    public void showErrorMessage(String msg){
        JOptionPane.showMessageDialog(
                this,
                msg,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
