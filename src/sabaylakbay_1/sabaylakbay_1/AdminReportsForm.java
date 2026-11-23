package sabaylakbay_1;

import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.model.Report;
import sabaylakbay_1.model.ReportStatus;
import sabaylakbay_1.ui.Page;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.RoundButton;
import sabaylakbay_1.ui.components.RoundPanel;
import sabaylakbay_1.ui.components.TabBar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * ADMIN REPORTS AND COMPLAINTS
 * Opens from: admin sidebar "Reports & Complaints", the Pending reports card, "See all reports"
 * Tabs: Pending, Under review, Resolved, Dismissed, All
 * Each report shows what was reported (user / group / message), the reason, who reported it and its status.
 * "Review" opens ReportReviewForm, where the admin sets the status, records the action and notifies the user.
 * Backend: getReports
 */
public class AdminReportsForm extends Page {

    private static final ReportStatus[] TABS = {ReportStatus.PENDING, ReportStatus.UNDER_REVIEW,
            ReportStatus.RESOLVED, ReportStatus.DISMISSED, null};

    private final AdminDashboard admin;
    private final TabBar tabs = new TabBar("Pending", "Under review", "Resolved", "Dismissed", "All");
    private final JPanel list = new Theme.ScrollableBox(null);

    public AdminReportsForm(AdminDashboard admin) {
        this.admin = admin;
        JPanel top = Theme.vbox();
        top.add(header("Reports and Complaints", "Review what travelers reported and record the action taken.", null));
        JPanel tw = Theme.clear(new BorderLayout());
        tw.setBorder(Theme.pad(0, 32, 0, 32));
        tw.add(tabs);
        top.add(tw);
        add(top, BorderLayout.NORTH);

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(Theme.pad(18, 32, 32, 32));
        add(Theme.scroll(list));
        tabs.onChange(i -> refresh());
    }

    public void selectTab(int i) {
        tabs.onChange(null);
        tabs.setSelected(i);
        tabs.onChange(x -> refresh());
    }

    @Override
    public void refresh() {
        String[] names = {"Pending", "Under review", "Resolved", "Dismissed", "All"};
        for (int i = 0; i < TABS.length; i++)
            tabs.setTitle(i, names[i] + " (" + Backend.getReports(TABS[i]).size() + ")");   // BACKEND

        list.removeAll();
        List<Report> reports = Backend.getReports(TABS[tabs.getSelected()]);               // BACKEND
        if (reports.isEmpty()) list.add(empty("shield", "Nothing here. All caught up."));
        for (Report r : reports) {
            list.add(reportRow(admin, r));
            list.add(Theme.gap(10));
        }
        list.revalidate();
        list.repaint();
    }

    /** One report row (also used on the Dashboard Overview). */
    static JComponent reportRow(AdminDashboard admin, Report r) {
        RoundPanel p = RoundPanel.card(new BorderLayout(16, 0));
        p.setBorder(Theme.pad(14, 18, 14, 18));

        JPanel t = Theme.vbox();
        t.add(Theme.left(Theme.row(10, AdminDashboard.targetChip(r.getTargetType()),
                Theme.label(r.getTargetName(), Font.BOLD, 14, Theme.TEXT_DARK),
                Theme.label(r.getReason(), Font.PLAIN, 13, Theme.TEXT_GRAY))));
        t.add(Theme.gap(5));
        String meta = "Reported by " + r.getReporterName() + "  \u2022  " + Theme.ago(r.getCreatedAt())
                + (r.getActionTaken() != null ? "  \u2022  Action: " + r.getActionTaken() : "");
        t.add(Theme.left(Theme.label(meta, Font.PLAIN, 12, Theme.TEXT_GRAY)));
        p.add(Theme.middleLeft(t), BorderLayout.CENTER);

        RoundButton review = new RoundButton(r.getStatus() == ReportStatus.PENDING || r.getStatus() == ReportStatus.UNDER_REVIEW
                ? "Review" : "View", RoundButton.Style.OUTLINE).small();
        review.addActionListener(e -> admin.openReport(r));
        JPanel right = Theme.clear(new GridBagLayout());
        right.add(Theme.row(12, AdminDashboard.reportStatusChip(r.getStatus()), review));
        p.add(right, BorderLayout.EAST);

        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { admin.openReport(r); }
            @Override public void mouseEntered(MouseEvent e) { p.setStroke(new Color(0xBFD8CC)); }
            @Override public void mouseExited(MouseEvent e) { p.setStroke(Theme.BORDER); }
        });
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }
}
