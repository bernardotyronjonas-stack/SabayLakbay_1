package sabaylakbay_1;

import sabaylakbay_1.ui.SubPage;
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
public class AdminUserDetailForm extends SubPage {   // opens inside the main window, with a Back bar

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
        // BACKEND: Backend.getVerificationStatus() reads from MySQL
        t.add(Theme.left(Theme.row(10, Theme.label(u.getFullName(), Font.BOLD, 20, Theme.TEXT_DARK), AdminDashboard.accountChip(u.getStatus()),
                Theme.verificationChip(Backend.getVerificationStatus(userId)))));
        t.add(Theme.left(Theme.label(u.getEmail() + (u.getPhone() == null ? "" : "  \u2022  " + u.getPhone()),
                Font.PLAIN, 12, Theme.TEXT_GRAY)));
        // sign-up details (age, gender, home, emergency contact)
        java.util.List<String> info = new java.util.ArrayList<>();
        if (u.getBirthdate() != null) info.add(java.time.Period.between(u.getBirthdate(), java.time.LocalDate.now()).getYears() + " years old");
        if (u.getGender() != null) info.add(u.getGender());
        if (u.hasAddress()) info.add(u.getFullAddress());
        if (u.getEmergencyPhone() != null) info.add("Emergency: " + (u.getEmergencyName() == null ? "" : u.getEmergencyName() + " ") + u.getEmergencyPhone());
        if (!info.isEmpty()) {
            t.add(Theme.gap(4));
            t.add(Theme.left(Theme.label(String.join("  \u2022  ", info), Font.PLAIN, 12, Theme.TEXT_GRAY)));
        }
        t.add(Theme.gap(4));
        // BACKEND: Backend.getAverageRating() reads from MySQL
        double avg = Backend.getAverageRating(userId);
        t.add(Theme.left(Theme.label("Joined " + Theme.date(u.getCreatedAt().toLocalDate()) + "  \u2022  "
                // BACKEND: Backend.countGroupsOf() reads from MySQL
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
        boolean erased = u.getEmail() != null && u.getEmail().endsWith("@erased.sabaylakbay");
        if (u.getStatus() == AccountStatus.REMOVED && !erased) {
            RoundButton erase = new RoundButton("Erase personal data", RoundButton.Style.DANGER).icon("trash");
            erase.addActionListener(e -> erase(u, admin));
            bar.add(erase);
        }
        if (!erased) {
            RoundButton bday = new RoundButton("Correct birthday", RoundButton.Style.OUTLINE).icon("calendar");
            bday.addActionListener(e -> correctBirthday(u, admin));
            bar.add(bday);
        }
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

    /** Fix the birthday after comparing it with the ID (travelers can't change it themselves). */
    private void correctBirthday(User u, AdminDashboard admin) {
        String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September",
                "October", "November", "December"};
        JComboBox<String> m = new JComboBox<>(months);
        JComboBox<Integer> d = new JComboBox<>();
        for (int i = 1; i <= 31; i++) d.addItem(i);
        JComboBox<Integer> y = new JComboBox<>();
        int now = java.time.LocalDate.now().getYear();
        for (int i = now; i >= now - 100; i--) y.addItem(i);
        java.time.LocalDate cur = u.getBirthdate() != null ? u.getBirthdate() : java.time.LocalDate.of(now - 25, 1, 1);
        m.setSelectedIndex(cur.getMonthValue() - 1);
        d.setSelectedItem(cur.getDayOfMonth());
        y.setSelectedItem(cur.getYear());
        for (JComboBox<?> c : new JComboBox<?>[]{m, d, y}) { c.setFont(Theme.font(Font.PLAIN, 13)); c.setPreferredSize(new Dimension(130, 40)); }
        RoundTextField note = new RoundTextField("e.g. Checked against the National ID");
        JLabel err = Theme.label(" ", Font.PLAIN, 12, Theme.RED);
        JPanel body = Theme.vbox();
        JPanel row = Theme.clear(new GridLayout(1, 3, 8, 0));
        row.add(m); row.add(d); row.add(y);
        row.setAlignmentX(LEFT_ALIGNMENT);
        body.add(row);
        body.add(Theme.gap(12));
        body.add(Theme.left(Theme.label("Note (optional)", Theme.MEDIUM, 13, new Color(0x344054))));
        body.add(Theme.gap(6));
        note.setAlignmentX(LEFT_ALIGNMENT);
        body.add(note);
        body.add(Theme.left(err));
        java.time.LocalDate[] picked = new java.time.LocalDate[1];
        boolean ok = sabaylakbay_1.ui.Dialogs.form(this, "calendar", "Correct " + u.getFullName() + "'s birthday",
                "Only change it to the birthday printed on their ID. The traveler is notified.", body, "Save birthday", () -> {
                    try {
                        picked[0] = java.time.LocalDate.of((Integer) y.getSelectedItem(), m.getSelectedIndex() + 1, (Integer) d.getSelectedItem());
                    } catch (java.time.DateTimeException ex) { err.setText("That date doesn't exist."); return false; }
                    if (picked[0].isAfter(java.time.LocalDate.now())) { err.setText("The birthday can't be in the future."); return false; }
                    return true;
                });
        if (!ok) return;
        // BACKEND: Backend.correctBirthdate() saves to MySQL
        if (Theme.run(this, () -> Backend.correctBirthdate(u.getId(), picked[0], note.getText().trim()))) {
            dispose();
            admin.openUser(u.getId());
        }
    }

    /** Privacy request: wipe everything personal from a removed account. */
    private void erase(User u, AdminDashboard admin) {
        String why = sabaylakbay_1.ui.Dialogs.input(this, "Erase " + u.getFullName() + "'s personal data?",
                "Name, email, phone, birthday, address, emergency contact, interests, profile photo, ID and face photos are deleted. "
                        + "Trips and reviews stay, shown as \"Deleted user\". This can't be undone.",
                "e.g. Traveler asked to delete their account (email of Nov 17)", "Erase data", true, true);
        if (why == null) return;
        // BACKEND: Backend.erasePersonalData() saves to MySQL
        if (Theme.run(this, () -> Backend.erasePersonalData(u.getId(), why))) {
            dispose();
            admin.refreshCurrent();
        }
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
