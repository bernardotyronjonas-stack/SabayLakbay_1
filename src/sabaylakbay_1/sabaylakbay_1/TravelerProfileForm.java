package sabaylakbay_1;

import sabaylakbay_1.ui.SubPage;
import sabaylakbay_1.model.ReportTarget;
import sabaylakbay_1.model.Review;
import sabaylakbay_1.model.User;
import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.ui.AppWindow;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.Avatar;
import sabaylakbay_1.ui.components.RoundButton;
import sabaylakbay_1.ui.components.StarRating;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * TRAVELER PROFILE FORM (popup, another user's profile)
 * Opens from: clicking a member, an organizer or a person who sent a join request
 * Shows: rating and reviews from other travelers
 * Buttons: Message, Block / Unblock, Report
 * Backend: getUser, getReviewsForUser, blockUser, unblockUser
 */
public class TravelerProfileForm extends SubPage {   // opens inside the main window, with a Back bar

    private final MainDashboard app;
    private final int userId;

    public TravelerProfileForm(MainDashboard app, int userId) {
        super(AppWindow.get(), "Traveler Profile", true);
        this.app = app;
        this.userId = userId;
        setSize(560, 620);
        setLocationRelativeTo(AppWindow.get());
        reload();
    }

    private void reload() {
        // BACKEND: Backend.getUser() reads from MySQL
        User u = Backend.getUser(userId);
        int me = Backend.currentUserId();
        // BACKEND: Backend.isBlocked() reads from MySQL
        boolean blocked = Backend.isBlocked(me, userId);

        JPanel box = new Theme.ScrollableBox(null);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(Theme.pad(26, 28, 26, 28));

        JPanel head = Theme.clear(new BorderLayout(16, 0));
        head.add(new Avatar(u, 72), BorderLayout.WEST);
        JPanel t = Theme.vbox();
        t.add(Theme.left(Theme.label(u.getFullName(), Font.BOLD, 20, Theme.TEXT_DARK)));
        // BACKEND: Backend.getVerificationStatus() reads from MySQL
        t.add(Theme.left(Theme.verificationChip(Backend.getVerificationStatus(userId))));
        t.add(Theme.gap(6));
        // BACKEND: Backend.getAverageRating() reads from MySQL
        double avg = Backend.getAverageRating(userId);
        // BACKEND: Backend.getReviewsForUser() reads from MySQL
        List<Review> reviews = Backend.getReviewsForUser(userId);
        t.add(Theme.left(Theme.row(6, new StarRating((int) Math.round(avg), 15, false),
                Theme.label(avg > 0 ? String.format("%.1f (%d)", avg, reviews.size()) : "No ratings yet", Font.PLAIN, 12, Theme.TEXT_GRAY))));
        head.add(Theme.middleLeft(t), BorderLayout.CENTER);
        head.setAlignmentX(LEFT_ALIGNMENT);
        box.add(head);
        box.add(Theme.gap(14));
        box.add(Theme.left(Theme.label("Member since " + Theme.date(u.getCreatedAt().toLocalDate()), Font.PLAIN, 12, Theme.TEXT_GRAY)));
        box.add(Theme.gap(16));

        RoundButton msg = new RoundButton("Message").icon("chat").small();
        msg.setEnabled(!blocked);
        msg.addActionListener(e -> { dispose(); app.openDirectChat(userId); });
        RoundButton block = new RoundButton(blocked ? "Unblock" : "Block", RoundButton.Style.DANGER).icon("block").small();
        block.addActionListener(e -> {
            if (blocked) {
                // BACKEND: Backend.unblockUser() saves to MySQL
                if (Theme.run(this, () -> Backend.unblockUser(me, userId))) reload();
            } else if (Theme.confirm(this, "Block " + u.getFullName() + "?", "They won't be able to message you, and you won't see their trips. You can unblock them in your profile.", "Block", true)) {
                // BACKEND: Backend.blockUser() saves to MySQL
                if (Theme.run(this, () -> Backend.blockUser(me, userId))) reload();
            }
        });
        RoundButton report = new RoundButton("Report", RoundButton.Style.GHOST).icon("flag").small();
        report.addActionListener(e -> new ReportForm(this, ReportTarget.USER, userId, u.getFullName()).setVisible(true));
        box.add(Theme.left(Theme.row(8, msg, block, report)));
        box.add(Theme.gap(22));
        box.add(Theme.separator());
        box.add(Theme.gap(18));

        box.add(Theme.left(Theme.sectionTitle("Reviews from other travelers")));
        box.add(Theme.gap(10));
        if (reviews.isEmpty()) box.add(Theme.left(Theme.label("No reviews yet.", Font.PLAIN, 13, Theme.TEXT_GRAY)));
        for (Review r : reviews) {
            JComponent row = GroupDetailsForm.reviewRow(r);
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            box.add(row);
            box.add(Theme.gap(8));
        }

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(Theme.scroll(box));
        setContentPane(root);
        revalidate();
        repaint();
    }
}
