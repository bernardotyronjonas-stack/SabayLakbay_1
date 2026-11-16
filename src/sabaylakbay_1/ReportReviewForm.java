package sabaylakbay_1;

import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.model.*;
import sabaylakbay_1.ui.AppWindow;
import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.*;

import javax.swing.*;
import java.awt.*;

/**
 * REPORT REVIEW FORM (admin popup)
 * Opens from: a report row in AdminReportsForm or the Dashboard Overview
 * Shows: what was reported (user, group, or ONE message), the reason and details, who reported it
 * Admin decision: status (Pending / Under review / Resolved / Dismissed), action taken, notes,
 *                 "Notify the affected user"
 * PRIVACY: for a reported message the admin only sees that one message, never the rest of the chat.
 * Backend: getUser, getGroup, getReportedMessage, updateReport
 */
public class ReportReviewForm extends JDialog {

    private static final String[] ACTIONS = {"No action needed", "Warning sent", "Message removed",
            "Group removed", "Account suspended", "Account removed"};

    private final AdminDashboard admin;
    private final Report report;

    private final JComboBox<String> status = new JComboBox<>();
    private final JComboBox<String> action = new JComboBox<>(ACTIONS);
    private final TextAreaBox notes = new TextAreaBox("Notes for the record (what you checked, why you decided this)", 3);
    private final JCheckBox notify = new JCheckBox("Notify the affected user about the decision", true);

    public ReportReviewForm(AdminDashboard admin, Report report) {
        super(AppWindow.get(), "Report #" + report.getId(), true);
        this.admin = admin;
        this.report = report;
        setSize(760, 720);
        setLocationRelativeTo(AppWindow.get());

        JPanel box = new Theme.ScrollableBox(null);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(Theme.pad(24, 28, 24, 28));

        box.add(Theme.left(Theme.row(10, Theme.label("Report #" + report.getId(), Font.BOLD, 22, Theme.TEXT_DARK),
                AdminDashboard.targetChip(report.getTargetType()), AdminDashboard.reportStatusChip(report.getStatus()))));
        box.add(Theme.gap(4));
        box.add(Theme.left(Theme.label("Submitted by " + report.getReporterName() + "  \u2022  " + Theme.ago(report.getCreatedAt()),
                Font.PLAIN, 12, Theme.TEXT_GRAY)));
        box.add(Theme.gap(20));

        // ----- what was reported -----
        box.add(Theme.left(Theme.sectionTitle("What was reported")));
        box.add(Theme.gap(10));
        full(box, targetPreview());
        box.add(Theme.gap(20));

        // ----- the report -----
        box.add(Theme.left(Theme.sectionTitle("Reason")));
        box.add(Theme.gap(8));
        RoundPanel reason = new RoundPanel(null, 12, new Color(0xF8FAF9), Theme.BORDER);
        reason.setLayout(new BoxLayout(reason, BoxLayout.Y_AXIS));
        reason.setBorder(Theme.pad(14, 16, 14, 16));
        reason.add(Theme.left(Theme.label(report.getReason(), Font.BOLD, 14, Theme.TEXT_DARK)));
        reason.add(Theme.gap(4));
        reason.add(Theme.left(Theme.wrapLabel(report.getDetails(), 620, Font.PLAIN, 13, Theme.TEXT_DARK)));
        full(box, reason);
        box.add(Theme.gap(20));

        // ----- decision -----
        box.add(Theme.left(Theme.sectionTitle("Decision")));
        box.add(Theme.gap(10));
        for (ReportStatus s : ReportStatus.values()) status.addItem(AdminDashboard.statusName(s));
        status.setSelectedIndex(report.getStatus().ordinal());
        action.setEditable(true);
        action.setSelectedItem(report.getActionTaken() == null ? ACTIONS[0] : report.getActionTaken());
        for (JComboBox<String> c : java.util.List.of(status, action)) {
            c.setFont(Theme.font(Font.PLAIN, 13));
            c.setPreferredSize(new Dimension(300, 38));
        }
        notes.setText(report.getAdminNotes());
        notes.setPreferredSize(new Dimension(600, 90));
        notify.setOpaque(false);
        notify.setFont(Theme.font(Font.PLAIN, 13));

        JPanel two = Theme.clear(new GridLayout(1, 2, 16, 0));
        two.add(field("Status", status));
        two.add(field("Action taken", action));
        full(box, two);
        box.add(Theme.gap(12));
        full(box, field("Notes", notes));
        box.add(Theme.gap(10));
        box.add(Theme.left(notify));

        // ----- buttons -----
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER), Theme.pad(14, 28, 14, 28)));
        RoundButton close = new RoundButton("Close", RoundButton.Style.GHOST);
        close.addActionListener(e -> dispose());
        RoundButton save = new RoundButton("Save decision").icon("check");
        save.addActionListener(e -> save());
        bar.add(close);
        bar.add(save);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(Theme.scroll(box), BorderLayout.CENTER);
        root.add(bar, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private static void full(JPanel box, JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        box.add(c);
    }

    private static JPanel field(String label, JComponent input) {
        JPanel f = Theme.clear(new BorderLayout(0, 6));
        f.add(Theme.label(label, Font.BOLD, 12, Theme.TEXT_DARK), BorderLayout.NORTH);
        f.add(input);
        return f;
    }

    /** Short summary of the reported user / group / message. */
    private JComponent targetPreview() {
        RoundPanel p = RoundPanel.card(new BorderLayout(14, 0));
        p.setBorder(Theme.pad(14, 16, 14, 16));
        switch (report.getTargetType()) {
            case USER: {
                User u = Backend.getUser(report.getTargetId());                       // BACKEND
                p.add(new Avatar(u, 44), BorderLayout.WEST);
                JPanel t = Theme.vbox();
                t.add(Theme.left(Theme.row(8, Theme.label(u.getFullName(), Font.BOLD, 14, Theme.TEXT_DARK),
                        AdminDashboard.accountChip(u.getStatus()))));
                // BACKEND: Backend.countReportsAgainstUser() (sample data for now)
                t.add(Theme.left(Theme.label(u.getEmail() + "  \u2022  " + Backend.countReportsAgainstUser(u.getId())
                        + " report(s) in total", Font.PLAIN, 12, Theme.TEXT_GRAY)));
                p.add(Theme.middleLeft(t));
                RoundButton view = new RoundButton("View account", RoundButton.Style.OUTLINE).small();
                view.addActionListener(e -> new AdminUserDetailForm(admin, u.getId()).setVisible(true));
                JPanel r = Theme.clear(new GridBagLayout());
                r.add(view);
                p.add(r, BorderLayout.EAST);
                break;
            }
            case GROUP: {
                TravelGroup g = Backend.getGroup(report.getTargetId());              // BACKEND
                ImageBox img = new ImageBox(g.getImagePath(), g.getTitle(), 10, true);
                img.setPreferredSize(new Dimension(84, 60));
                p.add(img, BorderLayout.WEST);
                JPanel t = Theme.vbox();
                t.add(Theme.left(Theme.label(g.getTitle(), Font.BOLD, 14, Theme.TEXT_DARK)));
                t.add(Theme.left(Theme.label(g.getDestination() + "  \u2022  " + Theme.dateRange(g.getStartDate(), g.getEndDate())
                        + "  \u2022  by " + g.getCreatorName() + "  \u2022  " + Theme.peso(g.getBudgetPerPerson()) + "/person",
                        Font.PLAIN, 12, Theme.TEXT_GRAY)));
                p.add(Theme.middleLeft(t));
                break;
            }
            default: {
                Message m = Backend.getReportedMessage(report.getTargetId());       // BACKEND
                JPanel t = Theme.vbox();
                RoundPanel privacy = new RoundPanel(new BorderLayout(10, 0), 10, Theme.BLUE_LIGHT, null);
                privacy.setBorder(Theme.pad(10, 12, 10, 12));
                privacy.add(new JLabel(Icons.get("lock", 16, Theme.BLUE)), BorderLayout.WEST);
                privacy.add(Theme.label("Only the reported message is shown. The rest of the conversation stays private.",
                        Font.PLAIN, 12, Theme.BLUE));
                t.add(Theme.left(privacy));
                t.add(Theme.gap(12));
                if (m == null) {
                    t.add(Theme.left(Theme.label("This message was already deleted.", Font.ITALIC, 13, Theme.TEXT_GRAY)));
                } else {
                    JPanel row = Theme.clear(new BorderLayout(10, 0));
                    // BACKEND: Backend.getUser() (sample data for now)
                    row.add(new Avatar(Backend.getUser(m.getSenderId()), 34), BorderLayout.WEST);
                    JPanel b = Theme.vbox();
                    b.add(Theme.left(Theme.label(m.getSenderName() + "  \u2022  " + Theme.ago(m.getSentAt()),
                            Font.BOLD, 11, Theme.TEXT_GRAY)));
                    RoundPanel bubble = new RoundPanel(new BorderLayout(), 14, new Color(0xF0F2F4), null);
                    bubble.setBorder(Theme.pad(9, 14, 9, 14));
                    bubble.add(Theme.wrapLabel(m.getContent(), 460, Font.PLAIN, 13, Theme.TEXT_DARK));
                    b.add(Theme.left(bubble));
                    row.add(b);
                    t.add(Theme.left(row));
                }
                p.add(t);
            }
        }
        return p;
    }

    private void save() {
        ReportStatus newStatus = ReportStatus.values()[status.getSelectedIndex()];
        Object a = action.getSelectedItem();
        String act = a == null ? "" : a.toString().trim();
        if (newStatus == ReportStatus.RESOLVED && act.isEmpty()) {
            Theme.error(this, "Please record the action taken before resolving.");
            return;
        }
        // BACKEND: Backend.updateReport() (button only for now)
        if (Theme.run(this, () -> Backend.updateReport(report.getId(), newStatus, act,
                notes.getText().trim(), notify.isSelected()))) {                    // BACKEND
            dispose();
        }
    }
}
