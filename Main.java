public class Main {

    public static void main(String[] args) {

        CreateUserTable.createTable();

        javax.swing.SwingUtilities.invokeLater(
                () -> new LoginFrame()
        );
    }
}