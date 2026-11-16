package sabaylakbay_1;

import sabaylakbay_1.model.Report;
import sabaylakbay_1.model.ReportTarget;
import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.RoundButton;
import sabaylakbay_1.ui.components.TextAreaBox;

import javax.swing.*;
import java.awt.*;

/**
 * REPORT FORM (popup)
 * Opens from: "Report this group" or "Report" on a traveler profile
 * Fields: reason (choose one) and details
 * Backend: Backend.report()
 */
public class ReportForm extends JDialog {

    private static final String[] REASONS = {
            "Scam or fraud", "Fake profile or group", "Harassment or bullying",
            "Inappropriate content", "Safety concern", "Other"};

    public ReportForm(Window owner, ReportTarget type, int targetId, String targetName) {
        super(owner, "Report", ModalityType.APPLICATION_MODAL);
        JPanel p = Theme.vbox();
        p.setBackground(Color.WHITE);
        p.setOpaque(true);
        p.setBorder(Theme.pad(22, 24, 20, 24));

        p.add(Theme.left(Theme.label("Report " + (type == ReportTarget.USER ? "user" : "group"), Font.BOLD, 18, Theme.TEXT_DARK)));
        p.add(Theme.gap(4));
        p.add(Theme.left(Theme.label(targetName, Font.PLAIN, 13, Theme.TEXT_GRAY)));
        p.add(Theme.gap(16));
        p.add(Theme.left(Theme.label("Reason", Font.BOLD, 12, Theme.TEXT_DARK)));
        p.add(Theme.gap(6));

        ButtonGroup bg = new ButtonGroup();
        JRadioButton[] radios = new JRadioButton[REASONS.length];
        for (int i = 0; i < REASONS.length; i++) {
            radios[i] = new JRadioButton(REASONS[i]);
            radios[i].setOpaque(false);
            radios[i].setFont(Theme.font(Font.PLAIN, 13));
            bg.add(radios[i]);
            p.add(Theme.left(radios[i]));
        }
        p.add(Theme.gap(12));
        p.add(Theme.left(Theme.label("Details", Font.BOLD, 12, Theme.TEXT_DARK)));
        p.add(Theme.gap(6));
        TextAreaBox details = new TextAreaBox("Tell us what happened", 4);
        details.setPreferredSize(new Dimension(380, 100));
        p.add(Theme.left(details));
        p.add(Theme.gap(16));

        RoundButton cancel = new RoundButton("Cancel", RoundButton.Style.GHOST);
        cancel.addActionListener(e -> dispose());
        RoundButton send = new RoundButton("Submit Report", RoundButton.Style.PRIMARY);
        send.addActionListener(e -> {
            String reason = null;
            for (JRadioButton r : radios) if (r.isSelected()) reason = r.getText();
            Report rep = new Report();
            rep.setReporterId(Backend.currentUserId());
            rep.setTargetType(type);
            rep.setTargetId(targetId);
            rep.setReason(reason);
            rep.setDetails(details.getText().trim());
            if (reason == null) { Theme.error(this, "Please choose a reason."); return; }
            // BACKEND: Backend.report() (button only for now)
            if (Theme.run(this, () -> Backend.report(rep))) dispose();
        });
        JPanel btns = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.add(cancel);
        btns.add(send);
        btns.setAlignmentX(LEFT_ALIGNMENT);
        p.add(btns);

        setContentPane(p);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }
}
