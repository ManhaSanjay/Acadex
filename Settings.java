import javax.swing.*;
import java.awt.*;

public class Settings extends JPanel {

    public Settings() {

        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("Settings");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        add(title, BorderLayout.NORTH);

        JPanel buttons = new JPanel(
            new GridLayout(3, 2, 10, 10)
        );

        JButton addSubjectButton =
            new JButton("Add Subject");

        JButton editSubjectButton =
            new JButton("Edit Subject");

        JButton deleteSubjectButton =
            new JButton("Delete Subject");

        JButton addAssessmentButton =
            new JButton("Add Assessment");

        JButton editAssessmentButton =
            new JButton("Edit Assessment");

        JButton deleteAssessmentButton =
            new JButton("Delete Assessment");

        buttons.add(addSubjectButton);
        buttons.add(editSubjectButton);
        buttons.add(deleteSubjectButton);
        buttons.add(addAssessmentButton);
        buttons.add(editAssessmentButton);
        buttons.add(deleteAssessmentButton);

        add(buttons, BorderLayout.CENTER);

        addSubjectButton.addActionListener(e -> addSubject());
        editSubjectButton.addActionListener(e -> editSubject());
        deleteSubjectButton.addActionListener(e -> deleteSubject());

        addAssessmentButton.addActionListener(e -> addAssessment());
        editAssessmentButton.addActionListener(e -> editAssessment());
        deleteAssessmentButton.addActionListener(e -> deleteAssessment());
    }

    public void addSubject() {
        // TODO
    }

    public void editSubject() {
        // TODO
    }

    public void deleteSubject() {
        // TODO
    }

    public void addAssessment() {
        // TODO
    }

    public void editAssessment() {
        // TODO
    }

    public void deleteAssessment() {
        // TODO
    }
}
