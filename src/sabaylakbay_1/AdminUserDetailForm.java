package sabaylakbay_1;

import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.model.AccountStatus;
import sabaylakbay_1.model.Report;
import sabaylakbay_1.model.Review;
import sabaylakbay_1.model.User;
import sabaylakbay_1.ui.AppWindow;
import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * ADMIN USER DETAILS (popup)
 * Opens from: "View" in User Management, top creators, or a reported user
 * Shows: account info and status, reports about this user, reviews from other travelers
 * Buttons: Suspend / Reactivate, Remove account
 * PRIVACY: private messages are not shown here.
 * Backend: getUser, getReportsAgainstUser, getReviewsForUser, suspendUser, reactivateUser, removeUser
 */
public class AdminUserDetailForm extends JDialog {

    public AdminUserDetailForm(AdminDashboard admin, int userId) {
        super(AppWindow.get(), "Traveler Account", true);
        setSize(720, 700);
        setLocationRelativeTo(AppWindow.get());

        User u = Backend.getUser(userId);                                          // BACKEND
        JPanel box = new Theme.ScrollableBox(null);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(Theme.pad(24, 28, 24, 28));

        // ----- account -----
        JPanel head = Theme.clear(new BorderLayout(16, 0));
        head.add(new Avatar(u, 72), BorderLayout.WEST);
        JPanel t = Theme.vbox();
        t.add(Theme.left(Theme.row(10, Theme.label(u.getFullName(), Font.BOLD, 20, Theme.TEXT_DARK), AdminDashboard.accountChip(u.getStatus()))));
        t.add(Theme.left(Theme.label("@" + u.getUsername() + "  \u2022  " + u.getEmail() + "  \u2022  " + u.getPhone(),
                Font.PLAIN, 12, Theme.TEXT_GRAY)));
        t.add(Theme.gap(4));
        // BACKEND: Backend.getAverageRating() (sample data for now)
        double avg = Backend.getAverageRating(userId);
        t.add(Theme.left(Theme.label("Joined " + Theme.date(u.getCreatedAt().toLocalDate()) + "  \u2022  "
                // BACKEND: Backend.countGroupsOf() (sample data for now)
                + groupsText(Backend.countGroupsOf(userId)) + "  \u2022  " + Theme.stars(avg), Font.PLAIN, 12, Theme.TEXT_GRAY)));
        head.add(Theme.middleLeft(t));
        full(box, head);
        box.add(Theme.gap(18));

        RoundPanel privacy = new RoundPanel(new BorderLayout(10, 0), 10, Theme.BLUE_LIGHT, null);
        privacy.setBorder(Theme.pad(10, 12, 10, 12));
        privacy.add(new JLabel(Icons.get("lock", 16, Theme.BLUE)), BorderLayout.WEST);
        privacy.add(Theme.label("Private messages are not shown. Admins only see messages that were reported.",
                Font.PLAIN, 12, Theme.BLUE));
        full(box, privacy);
        box.add(Theme.gap(20));

        // ----- reports about this user -----
        List<Report> reports = Backend.getReportsAgainstUser(userId);              // BACKEND
        box.add(Theme.left(Theme.sectionTitle("Reports about this user (" + reports.size() + ")")));
        box.add(Theme.gap(10));
        if (reports.isEmpty()) box.add(Theme.left(Theme.label("No reports.", Font.PLAIN, 13, Theme.TEXT_GRAY)));
        for (Report r : reports) {
            full(box, AdminReportsForm.reportRow(admin, r));
            box.add(Theme.gap(8));
        }
        box.add(Theme.gap(14));

        // ----- reviews -----
        List<Review> reviews = Backend.getReviewsForUser(userId);                  // BACKEND
        box.add(Theme.left(Theme.sectionTitle("Reviews from other travelers (" + reviews.size() + ")")));
        box.add(Theme.gap(10));
        if (reviews.isEmpty()) box.add(Theme.left(Theme.label("No reviews yet.", Font.PLAIN, 13, Theme.TEXT_GRAY)));
        for (Review r : reviews) {
            full(box, GroupDetailsForm.reviewRow(r));
            box.add(Theme.gap(8));
        }

        // ----- buttons -----
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER), Theme.pad(14, 28, 14, 28)));
        if (u.getStatus() != AccountStatus.REMOVED) {
            RoundButton remove = new RoundButton("Remove account", RoundButton.Style.DANGER).icon("trash");
            remove.addActionListener(e -> AdminUsersForm.remove(this, u, admin));
            bar.add(remove);
        }
        if (u.getStatus() == AccountStatus.ACTIVE) {
            RoundButton suspend = new RoundButton("Suspend", RoundButton.Style.OUTLINE).icon("block");
            suspend.addActionListener(e -> AdminUsersForm.suspend(this, u, admin));
            bar.add(suspend);
        } else if (u.getStatus() == AccountStatus.SUSPENDED) {
            RoundButton reactivate = new RoundButton("Reactivate", RoundButton.Style.OUTLINE).icon("check");
            reactivate.addActionListener(e -> AdminUsersForm.reactivate(this, u, admin));
            bar.add(reactivate);
        }
        RoundButton close = new RoundButton("Close");
        close.addActionListener(e -> dispose());
        bar.add(close);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(Theme.scroll(box), BorderLayout.CENTER);
        root.add(bar, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private static String groupsText(int n) {
        return n + (n == 1 ? " group" : " groups");
    }

    private static void full(JPanel box, JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        box.add(c);
    }
}
