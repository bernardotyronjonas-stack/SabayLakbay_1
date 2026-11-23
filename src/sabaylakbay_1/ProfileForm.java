package sabaylakbay_1;

import sabaylakbay_1.model.Review;
import sabaylakbay_1.model.TravelGroup;
import sabaylakbay_1.model.User;
import sabaylakbay_1.model.VerificationStatus;
import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.ui.Page;
import sabaylakbay_1.ui.Dialogs;
import sabaylakbay_1.ui.PasswordDialogs;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * PROFILE FORM (your own profile)
 * Opens from: sidebar "Profile" or your name at the bottom of the sidebar
 * Shows: your info and rating, stats, trips to review, reviews about you, blocked users
 * Buttons: Edit profile (name, phone, picture), Upload ID, Log out, Rate companions, Write a review, Unblock
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
        // BACKEND: Backend.getGroupsCreatedBy() reads from MySQL
        List<TravelGroup> created = Backend.getGroupsCreatedBy(id);
        // BACKEND: Backend.getGroupsJoinedBy() reads from MySQL
        List<TravelGroup> joined = Backend.getGroupsJoinedBy(id);
        // BACKEND: Backend.getReviewsForUser() reads from MySQL
        List<Review> reviews = Backend.getReviewsForUser(id);
        // BACKEND: Backend.getAverageRating() reads from MySQL
        double avg = Backend.getAverageRating(id);

        // ----- header card -----
        RoundPanel head = RoundPanel.card(new BorderLayout(20, 0));
        head.setBorder(Theme.pad(24, 24, 24, 24));
        head.add(new Avatar(me, 84), BorderLayout.WEST);
        JPanel t = Theme.vbox();
        // BACKEND: Backend.getVerificationStatus() reads from MySQL
        VerificationStatus idStatus = Backend.getVerificationStatus(id);
        t.add(Theme.left(Theme.row(10, Theme.label(me.getFullName(), Font.BOLD, 22, Theme.TEXT_DARK), Theme.verificationChip(idStatus))));
        t.add(Theme.left(Theme.label(me.getEmail() + (me.getPhone() == null ? "" : "  \u2022  " + me.getPhone()), Font.PLAIN, 12, Theme.TEXT_GRAY)));
        t.add(Theme.gap(8));
        t.add(Theme.left(Theme.row(6, new StarRating((int) Math.round(avg), 16, false),
                Theme.label(avg > 0 ? String.format("%.1f from %d %s", avg, reviews.size(), reviews.size() == 1 ? "review" : "reviews") : "No ratings yet", Font.PLAIN, 12, Theme.TEXT_GRAY))));
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

        // ----- ID verification banner -----
        if (idStatus != VerificationStatus.APPROVED) {
            box.add(Theme.gap(12));
            boolean pending = idStatus == VerificationStatus.PENDING;
            RoundPanel idBox = new RoundPanel(new BorderLayout(12, 0), 14, pending ? Theme.YELLOW_LIGHT : Theme.RED_LIGHT, null);
            idBox.setBorder(Theme.pad(14, 18, 14, 18));
            idBox.add(new JLabel(sabaylakbay_1.ui.Icons.get("shield", 22, pending ? new Color(0x9A6B00) : Theme.RED)), BorderLayout.WEST);
            JPanel it = Theme.vbox();
            it.add(Theme.left(Theme.label(pending ? "Your ID is being checked" : idStatus == VerificationStatus.REJECTED
                    ? "Your ID was not approved" : "Verify your ID", Font.BOLD, 14, Theme.TEXT_DARK)));
            it.add(Theme.left(Theme.label(pending ? "You can join and create trips once an admin approves it."
                    : "Take a photo of a government ID with the camera so you can join and create trips.", Font.PLAIN, 12, Theme.TEXT_GRAY)));
            idBox.add(Theme.middleLeft(it));
            if (!pending) {
                RoundButton upload = new RoundButton("Verify ID").icon("shield").small();
                upload.addActionListener(e -> uploadId(id));
                JPanel uw = Theme.clear(new GridBagLayout());
                uw.add(upload);
                idBox.add(uw, BorderLayout.EAST);
            }
            addFull(idBox);
        }
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
        box.add(Theme.gap(16));

        // ----- interests + home (used for "Picked for you" on Home) -----
        addFull(personalCard(me));
        box.add(Theme.gap(16));

        // ----- settings: password and security question -----
        addFull(securityCard());
        box.add(Theme.gap(24));

        // ----- trips to review -----
        List<TravelGroup> toReview = new ArrayList<>();
        // only travelers who joined through a booking can review (organizers receive reviews)
        List<TravelGroup> all = new ArrayList<>(joined);
        for (TravelGroup g : all)
            if (g.isCompleted() && !g.isCancelled()
                    // BACKEND: Backend.hasRatedCompanions(), Backend.hasReviewedTrip() reads from MySQL
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
                // BACKEND: Backend.hasRatedCompanions() reads from MySQL
                boolean rated = Backend.hasRatedCompanions(g.getId(), id);
                // BACKEND: Backend.hasReviewedTrip() reads from MySQL
                boolean reviewed = Backend.hasReviewedTrip(g.getId(), id);
                RoundButton rate = new RoundButton(rated ? "Rated" : "Rate companions", RoundButton.Style.OUTLINE).small();
                if (rated) rate.icon("check");
                rate.setEnabled(!rated);
                rate.addActionListener(e -> { if (ReviewForm.rateCompanions(this, g)) refresh(); });
                RoundButton rev = new RoundButton(reviewed ? "Reviewed" : "Write a review").small();
                if (reviewed) rev.icon("check");
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
            // BACKEND: Backend.getUser() reads from MySQL
            p.add(new Avatar(Backend.getUser(r.getReviewerId()), 34), BorderLayout.WEST);
            JPanel rt = Theme.vbox();
            rt.add(Theme.left(Theme.row(8, Theme.label(r.getReviewerName(), Font.BOLD, 12, Theme.TEXT_DARK),
                    new StarRating(r.getRating(), 13, false), Theme.label(Theme.ago(r.getCreatedAt()), Font.PLAIN, 11, Theme.TEXT_GRAY))));
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
        // BACKEND: Backend.getBlockedUsers() reads from MySQL
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
            // BACKEND: Backend.unblockUser() saves to MySQL
            unblock.addActionListener(e -> { if (Theme.run(this, () -> Backend.unblockUser(id, u.getId()))) refresh(); });
            p.add(unblock, BorderLayout.EAST);
            addFull(p);
            box.add(Theme.gap(8));
        }

        box.revalidate();
        box.repaint();
    }

    /**
     * SETTINGS > Account security: change the password, and set or change the security question
     * used by Forgot password. Backend: passwordChangedAt, mySecurityQuestion (+ PasswordDialogs).
     */
    private JComponent securityCard() {
        RoundPanel p = RoundPanel.card(null);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(Theme.pad(18, 22, 8, 22));
        JPanel head = Theme.clear(new BorderLayout());
        head.add(Theme.iconText("shield", "Account security", Theme.TEXT_DARK), BorderLayout.WEST);
        ((JLabel) head.getComponent(0)).setFont(Theme.font(Font.BOLD, 15));
        head.setAlignmentX(LEFT_ALIGNMENT);
        p.add(head);
        p.add(Theme.gap(6));

        // BACKEND: Backend.passwordChangedAt() reads from MySQL
        java.time.LocalDateTime changed = Backend.passwordChangedAt();
        String pwText = changed == null ? "Not changed since you signed up"
                : "Last changed " + changed.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"));
        RoundButton changePw = new RoundButton("Change password", RoundButton.Style.OUTLINE).icon("lock").small();
        changePw.addActionListener(e -> { if (PasswordDialogs.changePassword(this)) refresh(); });
        p.add(settingRow("Password", pwText, Theme.TEXT_GRAY, changePw, true));

        // BACKEND: Backend.mySecurityQuestion() reads from MySQL
        String q = Backend.mySecurityQuestion();
        RoundButton changeQ = new RoundButton(q == null ? "Set up" : "Change question", RoundButton.Style.OUTLINE).icon("shield").small();
        changeQ.addActionListener(e -> { if (PasswordDialogs.securityQuestion(this)) refresh(); });
        p.add(settingRow("Security question",
                q == null ? "Not set yet. Without it you can't use Forgot password." : q,
                q == null ? Theme.RED : Theme.TEXT_GRAY, changeQ, false));
        return p;
    }

    /** One settings line: title + description on the left, a button on the right, a line under it. */
    private static JComponent settingRow(String title, String text, Color textColor, JComponent button, boolean line) {
        JPanel r = Theme.clear(new BorderLayout(16, 0));
        r.setBorder(line ? BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER), Theme.pad(12, 0, 12, 0))
                         : Theme.pad(12, 0, 12, 0));
        JPanel t = Theme.vbox();
        t.add(Theme.left(Theme.label(title, Font.BOLD, 13, Theme.TEXT_DARK)));
        t.add(Theme.gap(3));
        t.add(Theme.left(Theme.label(text, Font.PLAIN, 12, textColor)));
        r.add(t, BorderLayout.CENTER);
        JPanel bw = Theme.clear(new GridBagLayout());
        bw.add(button);
        r.add(bw, BorderLayout.EAST);
        r.setAlignmentX(LEFT_ALIGNMENT);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, r.getPreferredSize().height));
        return r;
    }

    private File chooseImage(String title) {
        return Dialogs.chooseImage(this, title);
    }

    /** Edit name, phone and profile picture. */
    /** What the traveler likes and where they live, each with an edit button. */
    private JComponent personalCard(User me) {
        RoundPanel card = RoundPanel.card(new GridLayout(1, 2, 28, 0));
        card.setBorder(Theme.pad(20, 22, 20, 22));

        // interests
        List<String> likes = Backend.getInterests(me.getId());            // BACKEND
        JPanel left = Theme.clear(new BorderLayout(0, 12));
        JPanel lh = Theme.clear(new BorderLayout());
        lh.add(Theme.label("What you like", Font.BOLD, 15, Theme.TEXT_DARK), BorderLayout.WEST);
        lh.add(Theme.link("Edit", () -> editInterests(me)), BorderLayout.EAST);
        left.add(lh, BorderLayout.NORTH);
        JPanel chips = Theme.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
        if (likes.isEmpty()) chips.add(Theme.label("Nothing picked yet", Font.PLAIN, 13, Theme.TEXT_GRAY));
        for (String l : likes) {
            Chip c = new Chip(l, Theme.GREEN_LIGHT, Theme.GREEN);
            chips.add(c);
            chips.add(Box.createHorizontalStrut(8));
        }
        left.add(chips, BorderLayout.CENTER);
        card.add(left);

        // home location
        JPanel right = Theme.clear(new BorderLayout(0, 12));
        JPanel rh = Theme.clear(new BorderLayout());
        rh.add(Theme.label("Home address", Font.BOLD, 15, Theme.TEXT_DARK), BorderLayout.WEST);
        rh.add(Theme.link(me.hasAddress() ? "Change" : "Add", () -> editLocation(me)), BorderLayout.EAST);
        right.add(rh, BorderLayout.NORTH);
        JPanel addr = Theme.vbox();
        addr.add(Theme.left(Theme.iconText("pin", me.hasAddress() ? me.getFullAddress()
                : "Not set yet. Add it to see trips near you.", me.hasAddress() ? Theme.TEXT_DARK : Theme.TEXT_GRAY)));
        if (me.hasAddress()) {
            addr.add(Theme.gap(4));
            addr.add(Theme.left(Theme.label(me.hasLocation() ? "Pinned on the map" : "No map pin (distances to trips won't show)",
                    Font.PLAIN, 12, Theme.TEXT_GRAY)));
        }
        right.add(addr, BorderLayout.CENTER);
        card.add(right);
        return card;
    }

    private void editInterests(User me) {
        InterestPicker p = new InterestPicker(Backend.INTERESTS, 3);
        p.setSelected(Backend.getInterests(me.getId()));
        if (!Dialogs.form(this, "heart", "What do you love to do?", "Home suggests trips that match these.", p, "Save")) return;
        // BACKEND: Backend.saveInterests() saves to MySQL
        if (Theme.run(this, () -> Backend.saveInterests(me.getId(), p.getSelected()))) {
            Toast.show("Interests saved.");
            refresh();
        }
    }

    private void editLocation(User me) {
        AddressForm f = new AddressForm(false);
        f.setPreferredSize(new Dimension(640, 560));
        f.load(me);
        if (!Dialogs.form(this, "pin", "Home address", "Trips near this address show first.", f, "Save", f::validateAll)) return;
        User a = new User();
        f.applyTo(a);
        // BACKEND: Backend.updateAddress() saves to MySQL
        if (Theme.run(this, () -> Backend.updateAddress(me.getId(), a, a.getLatitude(), a.getLongitude()))) {
            Toast.show("Home address saved.");
            refresh();
        }
    }

    private void editProfile(User me) {
        RoundTextField name = new RoundTextField("Full name");
        name.setText(me.getFullName());
        RoundTextField phone = new RoundTextField("09XX XXX XXXX");
        phone.setText(me.getPhone());
        File[] picture = new File[1];
        JLabel picName = Theme.label("Keep current picture", Font.PLAIN, 12, Theme.TEXT_GRAY);
        RoundButton pick = new RoundButton("Choose picture", RoundButton.Style.OUTLINE).icon("image").small();
        pick.addActionListener(e -> {
            File f = chooseImage("Choose a profile picture");
            if (f != null) { picture[0] = f; picName.setText(f.getName()); }
        });
        JComboBox<String> gender = new JComboBox<>(new String[]{"Not set", "Female", "Male", "Prefer not to say"});
        if (me.getGender() != null) gender.setSelectedItem(me.getGender());
        RoundTextField emName = new RoundTextField("e.g. Maria Dela Cruz (mother)");
        emName.setText(me.getEmergencyName() == null ? "" : me.getEmergencyName());
        RoundTextField emPhone = new RoundTextField("09XX XXX XXXX");
        emPhone.setText(me.getEmergencyPhone() == null ? "" : me.getEmergencyPhone());

        // birthday: locked once set (an admin checks it against the ID); older accounts can set it once
        JComboBox<String> bMonth = new JComboBox<>(), bDay = new JComboBox<>(), bYear = new JComboBox<>();
        JComponent birthday;
        if (me.getBirthdate() != null) {
            JLabel b = Theme.label(Theme.date(me.getBirthdate()) + "   (checked with your ID, can't be changed)",
                    Font.PLAIN, 13, Theme.TEXT_GRAY);
            b.setIcon(sabaylakbay_1.ui.Icons.get("lock", 14, Theme.HINT));
            b.setIconTextGap(8);
            birthday = b;
        } else {
            bMonth.addItem("Month");
            for (java.time.Month m : java.time.Month.values()) bMonth.addItem(m.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH));
            bDay.addItem("Day");
            for (int d = 1; d <= 31; d++) bDay.addItem(String.valueOf(d));
            bYear.addItem("Year");
            int y = java.time.LocalDate.now().getYear();
            for (int i = y - 18; i >= y - 90; i--) bYear.addItem(String.valueOf(i));
            JPanel row = Theme.clear(new GridLayout(1, 3, 8, 0));
            row.add(bMonth); row.add(bDay); row.add(bYear);
            row.setPreferredSize(new Dimension(420, 40));
            JPanel col = Theme.vbox();
            col.add(Theme.left(row));
            col.add(Theme.gap(4));
            col.add(Theme.left(Theme.label("You can set this once. It must match your ID.", Font.PLAIN, 12, Theme.TEXT_GRAY)));
            birthday = col;
        }

        JPanel em = Theme.clear(new GridLayout(1, 2, 10, 0));
        em.add(emName);
        em.add(emPhone);
        em.setPreferredSize(new Dimension(420, 40));
        JPanel p = Theme.vbox();
        String[] labels = {"Full name", "Phone number", "Profile picture", "Gender", "Birthday", "Emergency contact (name and number)"};
        JComponent[] inputs = {name, phone, Theme.row(10, pick, picName), gender, birthday, em};
        for (int i = 0; i < labels.length; i++) {
            p.add(Theme.left(Theme.label(labels[i], Font.BOLD, 12, Theme.TEXT_DARK)));
            p.add(Theme.gap(5));
            if (i < 2 || i == 3) inputs[i].setPreferredSize(new Dimension(420, 40));
            p.add(Theme.left(inputs[i]));
            p.add(Theme.gap(12));
        }
        java.util.function.BooleanSupplier check = () -> {
            if (name.getText().isBlank()) return false;
            String ep = emPhone.getText().replaceAll("[^0-9]", "");
            if (!ep.isEmpty() && !ep.matches("(63|0)?9\\d{9}")) return false;
            if (me.getBirthdate() == null && bYear.getSelectedIndex() > 0) {
                java.time.LocalDate b = picked(bMonth, bDay, bYear);
                if (b == null || b.plusYears(18).isAfter(java.time.LocalDate.now())) return false;
            }
            return true;
        };
        if (!Dialogs.form(this, "edit", "Edit profile", "Your birthday can't be changed after it's set, because it's checked with your ID.",
                p, "Save changes", check)) return;
        User changed = new User();
        changed.setId(me.getId());
        changed.setFullName(name.getText().trim());
        changed.setPhone(phone.getText().trim());
        changed.setGender(gender.getSelectedIndex() == 0 ? null : (String) gender.getSelectedItem());
        changed.setEmergencyName(emName.getText().isBlank() ? null : emName.getText().trim());
        changed.setEmergencyPhone(emPhone.getText().isBlank() ? null : emPhone.getText().trim());
        if (me.getBirthdate() == null) changed.setBirthdate(picked(bMonth, bDay, bYear));
        // BACKEND: Backend.updateProfile() saves to MySQL (birthday only if it was empty)
        if (Theme.run(this, () -> Backend.updateProfile(changed, picture[0]))) { refresh(); app.refreshBadges(); }
    }

    /** Birthday from the three lists, or null if not fully picked / not a real date. */
    private static java.time.LocalDate picked(JComboBox<String> m, JComboBox<String> d, JComboBox<String> y) {
        if (m.getSelectedIndex() <= 0 || d.getSelectedIndex() <= 0 || y.getSelectedIndex() <= 0) return null;
        try { return java.time.LocalDate.of(Integer.parseInt((String) y.getSelectedItem()), m.getSelectedIndex(), d.getSelectedIndex()); }
        catch (Exception e) { return null; }
    }

    /**
     * New ID: ID type, then two camera photos in one box (to fit a laptop screen):
     * step 1 the ID photo, step 2 the face check. No picking files, so the photos can be trusted.
     */
    private void uploadId(int userId) {
        String[] types = {"Select ID type", "National ID (PhilSys)", "Driver's License", "Passport", "UMID", "SSS ID",
                "PRC ID", "Postal ID", "Voter's ID", "Other government ID"};
        JComboBox<String> type = new JComboBox<>(types);
        type.setPreferredSize(new Dimension(300, 40));

        FaceCamera idCam = FaceCamera.forId();
        FaceCamera faceCam = new FaceCamera();
        CardLayout cards = new CardLayout();
        JPanel camBox = Theme.clear(cards);
        camBox.add(idCam, "id");
        camBox.add(faceCam, "face");
        camBox.setPreferredSize(new Dimension(540, 330));
        camBox.setAlignmentX(LEFT_ALIGNMENT);

        JLabel stepTitle = Theme.label(" ", Font.BOLD, 12, Theme.TEXT_DARK);
        Runnable[] goToId = new Runnable[1];
        JLabel backToId = Theme.link("Retake ID photo", () -> goToId[0].run());
        Runnable showId = () -> {
            stepTitle.setText("Step 1 of 2: Photo of your ID");
            backToId.setVisible(false);
            cards.show(camBox, "id");
            idCam.start();
        };
        Runnable showFace = () -> {
            stepTitle.setText("Step 2 of 2: Face check");
            backToId.setVisible(true);
            cards.show(camBox, "face");
            faceCam.start();
        };
        goToId[0] = showId;
        idCam.onChange(() -> { if (idCam.getPhoto() != null) showFace.run(); });

        JPanel stepRow = Theme.clear(new BorderLayout());
        stepRow.add(stepTitle, BorderLayout.WEST);
        stepRow.add(backToId, BorderLayout.EAST);
        stepRow.setAlignmentX(LEFT_ALIGNMENT);
        stepRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JPanel p = Theme.vbox();
        p.add(Theme.left(Theme.label("ID type", Font.BOLD, 12, Theme.TEXT_DARK)));
        p.add(Theme.gap(5));
        p.add(Theme.left(type));
        p.add(Theme.gap(14));
        p.add(stepRow);
        p.add(Theme.gap(6));
        p.add(camBox);
        showId.run();
        boolean ok = Dialogs.form(this, "shield", "Send your ID again", "An admin compares your face photo with the ID.", p, "Send for checking",
                () -> type.getSelectedIndex() > 0 && idCam.getPhoto() != null && faceCam.getPhoto() != null);
        idCam.stop();
        faceCam.stop();
        if (!ok) return;
        // BACKEND: Backend.resubmitId() saves to MySQL
        if (Theme.run(this, () -> Backend.resubmitId(userId, (String) type.getSelectedItem(), idCam.getPhoto(), faceCam.getPhoto()))) refresh();
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
