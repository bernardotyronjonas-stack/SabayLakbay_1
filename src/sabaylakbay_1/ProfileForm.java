package sabaylakbay_1;

import sabaylakbay_1.model.Review;
import sabaylakbay_1.model.TravelGroup;
import sabaylakbay_1.model.User;
import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.ui.Page;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * PROFILE FORM (your own profile)
 * Opens from: sidebar "Profile" or your name at the bottom of the sidebar
 * Shows: your info and rating, stats, trips to review, reviews about you, blocked users
 * Buttons: Edit profile, Log out, Rate companions, Write a review, Unblock
 * Backend: updateProfile, getReviewsForUser, getBlockedUsers, unblockUser
 */
public class ProfileForm extends Page {

    private final JPanel box = new Theme.ScrollableBox(null);

    public ProfileForm(MainDashboard app) {
        super(app);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(Theme.pad(28, 32, 32, 32));
        add(Theme.scroll(box));
    }

    private void addFull(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        box.add(c);
    }

    @Override
    public void refresh() {
        box.removeAll();
        User me = Backend.getCurrentUser();
        int id = me.getId();
        // BACKEND: Backend.getGroupsCreatedBy() (sample data for now)
        List<TravelGroup> created = Backend.getGroupsCreatedBy(id);
        // BACKEND: Backend.getGroupsJoinedBy() (sample data for now)
        List<TravelGroup> joined = Backend.getGroupsJoinedBy(id);
        // BACKEND: Backend.getReviewsForUser() (sample data for now)
        List<Review> reviews = Backend.getReviewsForUser(id);
        // BACKEND: Backend.getAverageRating() (sample data for now)
        double avg = Backend.getAverageRating(id);

        // ----- header card -----
        RoundPanel head = RoundPanel.card(new BorderLayout(20, 0));
        head.setBorder(Theme.pad(24, 24, 24, 24));
        head.add(new Avatar(me, 84), BorderLayout.WEST);
        JPanel t = Theme.vbox();
        t.add(Theme.left(Theme.label(me.getFullName(), Font.BOLD, 22, Theme.TEXT_DARK)));
        t.add(Theme.left(Theme.label("@" + me.getUsername() + "  \u2022  " + me.getEmail() + "  \u2022  " + me.getPhone(), Font.PLAIN, 12, Theme.TEXT_GRAY)));
        t.add(Theme.gap(8));
        t.add(Theme.left(Theme.row(6, new StarRating((int) Math.round(avg), 16, false),
                Theme.label(avg > 0 ? String.format("%.1f from %d reviews", avg, reviews.size()) : "No ratings yet", Font.PLAIN, 12, Theme.TEXT_GRAY))));
        if (me.getBio() != null) {
            t.add(Theme.gap(8));
            t.add(Theme.left(Theme.label(me.getBio(), Font.PLAIN, 13, Theme.TEXT_DARK)));
        }
        JPanel tc = Theme.clear(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
        tc.add(t, gc);
        head.add(tc);
        RoundButton edit = new RoundButton("Edit profile", RoundButton.Style.OUTLINE).icon("edit").small();
        edit.addActionListener(e -> editProfile(me));
        RoundButton logout = new RoundButton("Log out", RoundButton.Style.DANGER).icon("logout").small();
        logout.addActionListener(e -> app.logout());
        JPanel lw = Theme.clear(new GridBagLayout());
        lw.add(Theme.row(8, edit, logout));
        head.add(lw, BorderLayout.EAST);
        addFull(head);
        box.add(Theme.gap(16));

        // ----- stats -----
        long completed = joined.stream().filter(TravelGroup::isCompleted).count()
                + created.stream().filter(TravelGroup::isCompleted).count();
        JPanel stats = Theme.clear(new GridLayout(1, 4, 14, 0));
        stats.add(stat(String.valueOf(joined.size()), "Groups joined"));
        stats.add(stat(String.valueOf(created.size()), "Groups created"));
        stats.add(stat(String.valueOf(completed), "Completed trips"));
        stats.add(stat(avg > 0 ? String.format("%.1f", avg) : "-", "Average rating"));
        addFull(stats);
        box.add(Theme.gap(24));

        // ----- trips to review -----
        List<TravelGroup> toReview = new ArrayList<>();
        List<TravelGroup> all = new ArrayList<>(joined);
        all.addAll(created);
        for (TravelGroup g : all)
            if (g.isCompleted() && !g.isCancelled()
                    // BACKEND: Backend.hasRatedCompanions(), Backend.hasReviewedTrip() (sample data for now)
                    && (!Backend.hasRatedCompanions(g.getId(), id) || !Backend.hasReviewedTrip(g.getId(), id)))
                toReview.add(g);
        if (!toReview.isEmpty()) {
            box.add(Theme.left(Theme.sectionTitle("Trips to review")));
            box.add(Theme.gap(10));
            for (TravelGroup g : toReview) {
                RoundPanel r = new RoundPanel(new BorderLayout(14, 0), 14, Theme.GREEN_LIGHT, null);
                r.setBorder(Theme.pad(14, 16, 14, 16));
                JPanel rt = Theme.vbox();
                rt.add(Theme.left(Theme.label(g.getTitle(), Font.BOLD, 14, Theme.TEXT_DARK)));
                rt.add(Theme.left(Theme.label("Completed " + Theme.date(g.getEndDate()), Font.PLAIN, 12, Theme.TEXT_GRAY)));
                r.add(rt);
                // BACKEND: Backend.hasRatedCompanions() (sample data for now)
                boolean rated = Backend.hasRatedCompanions(g.getId(), id);
                // BACKEND: Backend.hasReviewedTrip() (sample data for now)
                boolean reviewed = Backend.hasReviewedTrip(g.getId(), id);
                RoundButton rate = new RoundButton(rated ? "Rated \u2713" : "Rate companions", RoundButton.Style.OUTLINE).small();
                rate.setEnabled(!rated);
                rate.addActionListener(e -> { if (ReviewForm.rateCompanions(this, g)) refresh(); });
                RoundButton rev = new RoundButton(reviewed ? "Reviewed \u2713" : "Write a review").small();
                rev.setEnabled(!reviewed);
                rev.addActionListener(e -> { if (ReviewForm.reviewTrip(this, g)) refresh(); });
                JPanel b = Theme.clear(new GridBagLayout());
                b.add(Theme.row(8, rate, rev));
                r.add(b, BorderLayout.EAST);
                addFull(r);
                box.add(Theme.gap(10));
            }
            box.add(Theme.gap(14));
        }

        // ----- reviews about me -----
        box.add(Theme.left(Theme.sectionTitle("What other travelers say about you")));
        box.add(Theme.gap(10));
        if (reviews.isEmpty()) box.add(Theme.left(Theme.label("No reviews yet. Join a trip to get your first one!", Font.PLAIN, 13, Theme.TEXT_GRAY)));
        for (Review r : reviews) {
            RoundPanel p = RoundPanel.card(new BorderLayout(12, 0));
            p.setBorder(Theme.pad(12, 14, 12, 14));
            // BACKEND: Backend.getUser() (sample data for now)
            p.add(new Avatar(Backend.getUser(r.getReviewerId()), 34), BorderLayout.WEST);
            JPanel rt = Theme.vbox();
            rt.add(Theme.left(Theme.row(8, Theme.label(r.getReviewerName(), Font.BOLD, 12, Theme.TEXT_DARK),
                    new StarRating(r.getRating(), 13, false), Theme.label(Theme.ago(r.getCreatedAt()), Font.PLAIN, 11, Theme.HINT))));
            rt.add(Theme.gap(4));
            rt.add(Theme.left(Theme.wrapLabel(r.getComment(), 700, Font.PLAIN, 13, Theme.TEXT_DARK)));
            p.add(rt);
            addFull(p);
            box.add(Theme.gap(8));
        }
        box.add(Theme.gap(20));

        // ----- blocked users -----
        box.add(Theme.left(Theme.sectionTitle("Blocked users")));
        box.add(Theme.gap(10));
        // BACKEND: Backend.getBlockedUsers() (sample data for now)
        List<User> blocked = Backend.getBlockedUsers(id);
        if (blocked.isEmpty()) box.add(Theme.left(Theme.label("You haven't blocked anyone.", Font.PLAIN, 13, Theme.TEXT_GRAY)));
        for (User u : blocked) {
            RoundPanel p = RoundPanel.card(new BorderLayout(12, 0));
            p.setBorder(Theme.pad(10, 14, 10, 14));
            p.add(new Avatar(u, 32), BorderLayout.WEST);
            JPanel c = Theme.clear(new GridBagLayout());
            c.add(Theme.label(u.getFullName(), Font.BOLD, 13, Theme.TEXT_DARK));
            JPanel cw = Theme.clear(new BorderLayout());
            cw.add(c, BorderLayout.WEST);
            p.add(cw);
            RoundButton unblock = new RoundButton("Unblock", RoundButton.Style.OUTLINE).small();
            // BACKEND: Backend.unblockUser() (button only for now)
            unblock.addActionListener(e -> { if (Theme.run(this, () -> Backend.unblockUser(id, u.getId()))) refresh(); });
            p.add(unblock, BorderLayout.EAST);
            addFull(p);
            box.add(Theme.gap(8));
        }

        box.revalidate();
        box.repaint();
    }

    /** Edit name, phone and bio. */
    private void editProfile(User me) {
        RoundTextField name = new RoundTextField("Full name");
        name.setText(me.getFullName());
        RoundTextField phone = new RoundTextField("09XX XXX XXXX");
        phone.setText(me.getPhone());
        TextAreaBox bio = new TextAreaBox("A short line about you", 3);
        bio.setText(me.getBio());
        bio.setPreferredSize(new Dimension(340, 80));
        JPanel p = Theme.vbox();
        String[] labels = {"Full name", "Phone number", "Bio"};
        JComponent[] inputs = {name, phone, bio};
        for (int i = 0; i < labels.length; i++) {
            p.add(Theme.left(Theme.label(labels[i], Font.BOLD, 12, Theme.TEXT_DARK)));
            p.add(Theme.gap(5));
            if (i < 2) inputs[i].setPreferredSize(new Dimension(340, 38));
            p.add(Theme.left(inputs[i]));
            p.add(Theme.gap(12));
        }
        if (JOptionPane.showConfirmDialog(this, p, "Edit Profile", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        if (name.getText().isBlank()) { Theme.error(this, "Name can't be empty."); return; }
        User changed = new User();
        changed.setId(me.getId());
        changed.setFullName(name.getText().trim());
        changed.setPhone(phone.getText().trim());
        changed.setBio(bio.getText().trim());
        // BACKEND: Backend.updateProfile() (button only for now)
        Theme.run(this, () -> Backend.updateProfile(changed));
    }

    private static JComponent stat(String value, String label) {
        RoundPanel p = RoundPanel.card(null);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(Theme.pad(16, 18, 16, 18));
        p.add(Theme.left(Theme.label(value, Font.BOLD, 24, Theme.GREEN)));
        p.add(Theme.left(Theme.label(label, Font.PLAIN, 12, Theme.TEXT_GRAY)));
        return p;
    }
}
