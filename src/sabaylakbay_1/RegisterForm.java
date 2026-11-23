package sabaylakbay_1;

import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.model.User;
import sabaylakbay_1.ui.AppWindow;
import sabaylakbay_1.ui.Dialogs;
import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * REGISTER: step-by-step sign-up (full screen).
 *   1 Account      email + password (strength meter and checklist)
 *   2 About you    photo, name, phone, birthday (18+), gender, emergency contact
 *   3 Verify ID    ID type + photo of the ID
 *   4 Interests    what they like (for "Picked for you" on Home)
 *   5 Address      province, city, barangay, street, ZIP (+ optional map pin) + Terms, then Create account
 * Mistakes show under each field. After Create account the traveler is logged in right away.
 *
 * Backend: emailTaken, register(user, password, idPhoto, idType, avatar, interests), login
 */
public class RegisterForm extends JPanel {

    private static final String[][] STEPS = {
            {"Account", "Email and password"},
            {"About you", "Your basic details"},
            {"Verify your ID", "ID photo and face check"},
            {"Interests", "What you love to do"},
            {"Address", "Where you live"}};
    private static final String[] ID_TYPES = {"Select ID type", "National ID (PhilSys)", "Driver's License", "Passport",
            "UMID", "SSS ID", "PRC ID", "Postal ID", "Voter's ID", "Other government ID"};
    private static final String[] GENDERS = {"Female", "Male", "Prefer not to say"};

    // step 1
    private final RoundTextField email = new RoundTextField("you@email.com");
    private final PasswordField pass = new PasswordField("Create a password");
    private final PasswordField confirm = new PasswordField("Type it again");
    private final Strength strength = new Strength();
    private final JLabel ruleLen = rule("At least 8 characters");
    private final JLabel ruleNum = rule("Has a number");
    private final JLabel ruleUpper = rule("Has an uppercase letter");
    // step 2
    private final RoundTextField name = new RoundTextField("e.g. Juan Dela Cruz");
    private final RoundTextField phone = new RoundTextField("9XX XXX XXXX");
    private final JComboBox<String> bMonth = new JComboBox<>();
    private final JComboBox<String> bDay = new JComboBox<>();
    private final JComboBox<String> bYear = new JComboBox<>();
    private final JLabel ageHint = Theme.label(" ", Font.PLAIN, 12, Theme.TEXT_GRAY);
    private final ButtonGroup genderGroup = new ButtonGroup();
    private final JToggleButton[] genderButtons = new JToggleButton[GENDERS.length];
    private final RoundTextField emName = new RoundTextField("e.g. Maria Dela Cruz (mother)");
    private final RoundTextField emPhone = new RoundTextField("09XX XXX XXXX");
    private final AvatarPicker avatar = new AvatarPicker();
    // step 3
    private final JComboBox<String> idType = new JComboBox<>(ID_TYPES);
    private final IdDrop idDrop = new IdDrop();
    private final FaceCamera faceCam = new FaceCamera();
    // step 4 + 5
    private final InterestPicker interests = new InterestPicker(Backend.INTERESTS, 3);
    private final JLabel interestCount = Theme.label(" ", Font.PLAIN, 13, Theme.TEXT_GRAY);
    private final AddressForm address = new AddressForm(true);
    private final JCheckBox agree = new JCheckBox();

    // error labels under fields
    private final JLabel errEmail = err(), errPass = err(), errConfirm = err(), errName = err(), errPhone = err(),
            errBirth = err(), errEmPhone = err(), errId = err(), errFace = err(), errInterests = err(), errAgree = err();

    private JComponent[] steps;
    private JScrollPane scroll;
    /** Holds the 5 steps; it is only as tall as the step that's showing (so short steps don't scroll). */
    private final FadeCards cards = new FadeCards() {
        @Override public Dimension getPreferredSize() {
            Dimension d = steps == null ? new Dimension(640, 400) : steps[step - 1].getPreferredSize();
            return new Dimension(640, d.height);
        }
    };
    private final Stepper stepper = new Stepper();
    private final JLabel stepOf = Theme.label("STEP 1 OF 5", Font.BOLD, 12, Theme.GREEN);
    private final JLabel title = Theme.label("Create your account", Font.BOLD, 30, Theme.TEXT_DARK);
    private final JLabel subtitle = Theme.label("Start with your email and a strong password.", Font.PLAIN, 14, Theme.TEXT_GRAY);
    private final RoundButton back = new RoundButton("Back", RoundButton.Style.OUTLINE);
    private final RoundButton next = new RoundButton("Continue");
    private int step = 1;

    public RegisterForm() {
        super(new BorderLayout());
        setBackground(Color.WHITE);
        add(new Side(), BorderLayout.WEST);

        // ----- the step (scrolls only when a step is taller than the window)
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(Color.WHITE);
        JPanel column = Theme.clear(new BorderLayout());
        column.setBorder(new EmptyBorder(40, 0, 30, 0));

        JPanel head = Theme.vbox();
        head.add(Theme.left(stepOf));
        head.add(Theme.gap(10));
        head.add(Theme.left(title));
        head.add(Theme.gap(6));
        head.add(Theme.left(subtitle));
        head.add(Theme.gap(26));
        column.add(head, BorderLayout.NORTH);

        steps = new JComponent[]{stepAccount(), stepAbout(), stepId(), stepInterests(), stepLocation()};
        for (int i = 0; i < steps.length; i++) cards.addCard(steps[i], String.valueOf(i + 1));
        column.add(cards, BorderLayout.CENTER);

        GridBagConstraints top = new GridBagConstraints();
        top.anchor = GridBagConstraints.NORTH;
        top.weighty = 1;
        right.add(column, top);
        JScrollPane sp = Theme.scroll(right);
        sp.getViewport().setOpaque(true);
        sp.getViewport().setBackground(Color.WHITE);

        // ----- Back / Continue always visible at the bottom
        JPanel foot = Theme.clear(new BorderLayout());
        back.setPreferredSize(new Dimension(110, 46));
        next.setPreferredSize(new Dimension(190, 46));
        back.addActionListener(e -> { if (step > 1) show(step - 1); });
        next.addActionListener(e -> goNext());
        foot.add(back, BorderLayout.WEST);
        foot.add(next, BorderLayout.EAST);
        foot.setPreferredSize(new Dimension(640, 46));
        JPanel footBar = Theme.clear(new BorderLayout());
        footBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER), new EmptyBorder(18, 0, 0, 0)));
        footBar.add(foot, BorderLayout.CENTER);
        JPanel footWrap = Theme.clear(new BorderLayout());
        footWrap.setBorder(new EmptyBorder(22, 0, 0, 0));
        footWrap.add(footBar, BorderLayout.CENTER);
        column.add(footWrap, BorderLayout.SOUTH);     // right under the step, not at the bottom of the window

        add(sp, BorderLayout.CENTER);
        scroll = sp;

        liveChecks();
        show(1);
    }

    // ================= steps =================

    private JComponent stepAccount() {
        JPanel p = form();
        p.add(field("Email address", email, errEmail, null));
        p.add(field("Password", pass, errPass, null));
        JPanel meter = Theme.vbox();
        strength.setPreferredSize(new Dimension(400, 6));
        meter.add(fullWidth(strength));
        meter.add(Theme.gap(10));
        // one left-aligned row with even gaps (3 equal columns didn't line up with the 4 bar parts)
        JPanel rules = Theme.row(20, ruleLen, ruleNum, ruleUpper);
        meter.add(fullWidth(rules));
        meter.setBorder(new EmptyBorder(0, 0, 16, 0));
        p.add(fullWidth(meter));
        p.add(field("Confirm password", confirm, errConfirm, null));
        return top(p);
    }

    private JComponent stepAbout() {
        JPanel p = form();
        JPanel photo = Theme.clear(new BorderLayout(18, 0));
        photo.add(avatar, BorderLayout.WEST);
        JPanel pt = Theme.vbox();
        pt.add(Theme.left(Theme.label("Profile photo", Theme.MEDIUM, 13, new Color(0x344054))));
        pt.add(Theme.gap(4));
        pt.add(Theme.left(Theme.label("Optional. A clear face photo helps travelers trust you.", Font.PLAIN, 12, Theme.TEXT_GRAY)));
        pt.add(Theme.gap(8));
        pt.add(Theme.left(Theme.link("Upload photo", avatar::choose)));
        photo.add(Theme.middleLeft(pt), BorderLayout.CENTER);
        photo.setBorder(new EmptyBorder(0, 0, 20, 0));
        p.add(fullWidth(photo));

        p.add(field("Full name", name, errName, null));

        // phone with +63 in front
        JPanel ph = Theme.clear(new BorderLayout(8, 0));
        JLabel cc = Theme.label("+63", Theme.MEDIUM, 14, Theme.TEXT_DARK);
        RoundPanel ccBox = new RoundPanel(new GridBagLayout(), 10, new Color(0xF9FAFB), new Color(0xD9DEE3));
        ccBox.setPreferredSize(new Dimension(64, 44));
        ccBox.add(cc);
        ph.add(ccBox, BorderLayout.WEST);
        ph.add(phone, BorderLayout.CENTER);
        p.add(field("Mobile number", ph, errPhone, null));

        // birthday as month / day / year
        for (Month m : Month.values()) bMonth.addItem(m.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        for (int d = 1; d <= 31; d++) bDay.addItem(String.valueOf(d));
        int thisYear = LocalDate.now().getYear();
        for (int y = thisYear - 18; y >= thisYear - 90; y--) bYear.addItem(String.valueOf(y));
        bMonth.insertItemAt("Month", 0); bDay.insertItemAt("Day", 0); bYear.insertItemAt("Year", 0);
        bMonth.setSelectedIndex(0); bDay.setSelectedIndex(0); bYear.setSelectedIndex(0);
        JPanel bd = Theme.clear(new GridLayout(1, 3, 10, 0));
        for (JComboBox<String> c : new JComboBox[]{bMonth, bDay, bYear}) {
            c.setPreferredSize(new Dimension(120, 44));
            c.addActionListener(e -> updateAge());
            bd.add(c);
        }
        JPanel birth = Theme.vbox();
        birth.add(fullWidth(bd));
        birth.add(Theme.gap(6));
        birth.add(Theme.left(ageHint));
        p.add(field("Birthday", birth, errBirth, null));

        // gender (optional)
        JPanel g = Theme.clear(new GridLayout(1, 3, 10, 0));
        for (int i = 0; i < GENDERS.length; i++) {
            JToggleButton b = pill(GENDERS[i]);
            genderButtons[i] = b;
            genderGroup.add(b);
            g.add(b);
        }
        p.add(field("Gender (optional)", g, null, null));

        // emergency contact
        JPanel em = Theme.clear(new GridLayout(1, 2, 12, 0));
        em.add(emName);
        em.add(emPhone);
        p.add(field("Emergency contact (optional)", em, errEmPhone,
                "Someone we can reach if something happens during a trip."));
        return top(p);
    }

    private JComponent stepId() {
        JPanel p = form();
        idType.setPreferredSize(new Dimension(400, 44));
        p.add(field("ID type", idType, null, null));
        idDrop.setPreferredSize(new Dimension(560, 170));
        p.add(field("Photo of your ID", idDrop, errId, null));
        faceCam.onChange(() -> errFace.setText(" "));
        p.add(field("Face check", faceCam, errFace,
                "Look at the camera inside the oval. An admin compares the photo with your ID."));

        RoundPanel tips = new RoundPanel(new BorderLayout(14, 0), 14, Theme.BLUE_LIGHT, null);
        tips.setBorder(new EmptyBorder(16, 18, 16, 18));
        tips.add(new JLabel(Icons.get("shield", 22, Theme.BLUE)), BorderLayout.WEST);
        JPanel t = Theme.vbox();
        t.add(Theme.left(Theme.label("Tips for a quick approval", Font.BOLD, 13, new Color(0x1D4380))));
        t.add(Theme.gap(6));
        for (String s : new String[]{"Show all four corners of the ID", "No glare, blur or covered details",
                "The name matches the one on your account"})
            t.add(Theme.left(Theme.iconText("check", s, new Color(0x2B4F7E))));
        t.add(Theme.gap(8));
        t.add(Theme.left(Theme.wrapLabel("An admin checks your ID. You can look around while you wait; "
                + "joining and creating trips unlock once it's approved.", 470, Font.PLAIN, 12, new Color(0x2B4F7E))));
        tips.add(t, BorderLayout.CENTER);
        p.add(fullWidth(tips));
        return top(p);
    }

    private JComponent stepInterests() {
        JPanel p = Theme.clear(new BorderLayout(0, 12));
        p.add(interests, BorderLayout.NORTH);
        JPanel s = Theme.vbox();
        s.add(Theme.left(interestCount));
        s.add(Theme.left(errInterests));
        p.add(s, BorderLayout.CENTER);
        interests.onChange(() -> { errInterests.setText(" "); updateInterestCount(); });
        updateInterestCount();
        return top(p);
    }

    private JComponent stepLocation() {
        JPanel p = Theme.clear(new BorderLayout(0, 14));
        p.add(address, BorderLayout.CENTER);
        JPanel s = Theme.vbox();
        agree.setOpaque(false);
        agree.setFont(Theme.font(Font.PLAIN, 13));
        agree.setForeground(Theme.TEXT_DARK);
        agree.addActionListener(e -> errAgree.setText(" "));
        // "I agree to the Terms of Service and Privacy Policy": the two names open the documents
        JLabel pre = Theme.label("I agree to the ", Font.PLAIN, 13, Theme.TEXT_DARK);
        JLabel mid = Theme.label(" and ", Font.PLAIN, 13, Theme.TEXT_DARK);
        JLabel terms = Theme.link("Terms of Service", () -> readTerms(false));
        JLabel privacy = Theme.link("Privacy Policy", () -> readTerms(true));
        terms.setFont(Theme.font(Font.BOLD, 13));
        privacy.setFont(Theme.font(Font.BOLD, 13));
        for (JLabel l : new JLabel[]{pre, mid}) {
            l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            l.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) { agree.doClick(); }
            });
        }
        JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        line.setOpaque(false);
        line.add(agree); line.add(Theme.hgap(4));
        line.add(pre); line.add(terms); line.add(mid); line.add(privacy);
        s.add(Theme.left(line));
        s.add(Theme.left(errAgree));
        p.add(s, BorderLayout.SOUTH);
        return p;
    }

    /** Opens the Terms or Privacy Policy inside the window; "I agree" ticks the box. */
    private void readTerms(boolean privacy) {
        if (sabaylakbay_1.ui.TermsPage.open(privacy)) {
            agree.setSelected(true);
            errAgree.setText(" ");
        }
    }

    // ================= navigation =================

    private void show(int s) {
        step = s;
        cards.show(String.valueOf(s));
        stepper.setStep(s);
        stepOf.setText("STEP " + s + " OF " + STEPS.length);
        String[][] text = {
                {"Create your account", "Start with your email and a strong password."},
                {"Tell us about you", "This helps other travelers know who they're traveling with."},
                {"Verify your ID", "Add your ID, then a quick face check with your camera. Every traveler is verified."},
                {"What do you love to do?", "Pick at least one. Your Home page will suggest trips that match."},
                {"Where do you live?", "Enter your home address. The map pin is optional and only used to show trips near you."}};
        title.setText(text[s - 1][0]);
        subtitle.setText(text[s - 1][1]);
        back.setVisible(s > 1);
        if (scroll != null) SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));   // start at the top
        if (s == 3) faceCam.start(); else faceCam.stop();    // camera is on only during the ID step
        next.setText(s == STEPS.length ? "Create account" : "Continue");
        if (s == 2) SwingUtilities.invokeLater(name::requestFocusInWindow);
        revalidate();
        repaint();
    }

    private void goNext() {
        if (!valid(step)) return;
        if (step == 1) {
            boolean[] taken = new boolean[1];
            // BACKEND: Backend.emailTaken() checks MySQL before going further
            if (!Theme.run(this, () -> taken[0] = Backend.emailTaken(email.getText().trim()))) return;
            if (taken[0]) { errEmail.setText("An account with this email already exists. Log in instead?"); return; }
        }
        if (step < STEPS.length) show(step + 1);
        else create();
    }

    /** Checks the fields of one step and shows the mistakes under them. */
    private boolean valid(int s) {
        boolean ok = true;
        if (s == 1) {
            String e = email.getText().trim();
            if (!e.matches("[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}")) { errEmail.setText("Enter a valid email, like you@email.com"); ok = false; }
            String p = pass.getValue();
            if (score(p) < 3) { errPass.setText("Use at least 8 characters with a number and an uppercase letter."); ok = false; }
            if (!p.equals(confirm.getValue()) || confirm.getValue().isEmpty()) { errConfirm.setText("Passwords don't match."); ok = false; }
        } else if (s == 2) {
            if (name.getText().trim().split("\\s+").length < 2) { errName.setText("Enter your first and last name."); ok = false; }
            if (phoneDigits(phone.getText()) == null) { errPhone.setText("Enter a 10-digit mobile number starting with 9, like 917 123 4567."); ok = false; }
            LocalDate b = birthday();
            if (b == null) { errBirth.setText("Pick your birth month, day and year."); ok = false; }
            else if (Period.between(b, LocalDate.now()).getYears() < 18) { errBirth.setText("You must be at least 18 to join."); ok = false; }
            if (!emPhone.getText().isBlank() && phoneDigits(emPhone.getText()) == null) { errEmPhone.setText("Check the emergency contact's number."); ok = false; }
        } else if (s == 3) {
            if (idType.getSelectedIndex() == 0 || idDrop.file == null) {
                errId.setText(idType.getSelectedIndex() == 0 ? "Choose the type of ID first." : "Upload a photo of your ID.");
                ok = false;
            }
            if (faceCam.getPhoto() == null) { errFace.setText("Take the face photo with the camera to continue."); ok = false; }
        } else if (s == 4) {
            if (interests.getSelected().isEmpty()) { errInterests.setText("Pick at least one."); ok = false; }
        } else if (s == 5) {
            if (!address.validateAll()) ok = false;
            if (!agree.isSelected()) { errAgree.setText("Please agree to the Terms to create your account."); ok = false; }
        }
        return ok;
    }

    private void create() {
        User u = new User();
        u.setEmail(email.getText().trim());
        u.setFullName(name.getText().trim().replaceAll("\\s+", " "));
        u.setPhone(format(phoneDigits(phone.getText())));
        u.setBirthdate(birthday());
        for (int i = 0; i < GENDERS.length; i++) if (genderButtons[i].isSelected()) u.setGender(GENDERS[i]);
        if (!emName.getText().isBlank()) u.setEmergencyName(emName.getText().trim());
        if (!emPhone.getText().isBlank()) u.setEmergencyPhone(format(phoneDigits(emPhone.getText())));
        address.applyTo(u);      // street, barangay, city, province, ZIP and the optional pin
        String password = pass.getValue();
        next.setEnabled(false);
        next.setText("Creating account...");
        try {
            // BACKEND: saves the account, ID, photo, interests and location, then logs in
            Backend.register(u, password, idDrop.file, (String) idType.getSelectedItem(), faceCam.getPhoto(), avatar.file, interests.getSelected());
            Backend.login(u.getEmail(), password);
        } catch (RuntimeException ex) {
            next.setEnabled(true);
            next.setText("Create account");
            Theme.error(this, ex.getMessage());
            return;
        }
        AppWindow.show(new MainDashboard(), "SabayLakbay");
        Toast.show("Welcome to SabayLakbay, " + u.getFullName().split(" ")[0] + "! We're checking your ID.");
    }

    // ================= live feedback =================

    private void liveChecks() {
        onType(email, () -> errEmail.setText(" "));
        onType(pass, () -> {
            errPass.setText(" ");
            String p = pass.getValue();
            strength.set(p.isEmpty() ? 0 : score(p) + (p.length() >= 12 ? 1 : 0));
            mark(ruleLen, p.length() >= 8);
            mark(ruleNum, p.matches(".*\\d.*"));
            mark(ruleUpper, p.matches(".*[A-Z].*"));
        });
        onType(confirm, () -> errConfirm.setText(" "));
        onType(name, () -> errName.setText(" "));
        onType(phone, () -> errPhone.setText(" "));
        onType(emPhone, () -> errEmPhone.setText(" "));
        idType.addActionListener(e -> errId.setText(" "));
    }

    /** 0 to 3: length 8+, a number, an uppercase letter. */
    private static int score(String p) {
        return (p.length() >= 8 ? 1 : 0) + (p.matches(".*\\d.*") ? 1 : 0) + (p.matches(".*[A-Z].*") ? 1 : 0);
    }

    private void updateAge() {
        errBirth.setText(" ");
        LocalDate b = birthday();
        if (b == null) { ageHint.setText(" "); return; }
        int age = Period.between(b, LocalDate.now()).getYears();
        ageHint.setText(age >= 18 ? "You're " + age + " years old." : "You must be at least 18.");
        ageHint.setForeground(age >= 18 ? Theme.TEXT_GRAY : Theme.RED);
    }

    private LocalDate birthday() {
        if (bMonth.getSelectedIndex() <= 0 || bDay.getSelectedIndex() <= 0 || bYear.getSelectedIndex() <= 0) return null;
        int m = bMonth.getSelectedIndex(), d = bDay.getSelectedIndex(), y = Integer.parseInt((String) bYear.getSelectedItem());
        try { return LocalDate.of(y, m, d); } catch (Exception e) { return null; }    // e.g. February 30
    }

    private void updateInterestCount() {
        int n = interests.getSelected().size();
        if (n > 0) errInterests.setText(" ");
        interestCount.setText(n == 0 ? "Nothing picked yet" : n + (n == 1 ? " interest" : " interests") + " picked");
        interestCount.setForeground(n == 0 ? Theme.TEXT_GRAY : Theme.GREEN);
    }

    /** "917 123 4567", "09171234567" or "+63 917..." -> "9171234567", or null when it isn't a PH mobile number. */
    private static String phoneDigits(String s) {
        String d = s.replaceAll("[^0-9]", "");
        if (d.startsWith("63")) d = d.substring(2);
        if (d.startsWith("0")) d = d.substring(1);
        return d.matches("9\\d{9}") ? d : null;
    }

    private static String format(String digits) {
        return digits == null ? null : "0" + digits.substring(0, 3) + " " + digits.substring(3, 6) + " " + digits.substring(6);
    }

    // ================= small helpers =================

    private static JPanel form() {
        JPanel p = Theme.vbox();
        return p;
    }

    private static JComponent top(JComponent c) {
        JPanel w = Theme.clear(new BorderLayout());
        w.add(c, BorderLayout.NORTH);
        return w;
    }

    /** Label, input, then a red error line (or a gray hint). */
    private static JComponent field(String label, JComponent input, JLabel error, String hint) {
        JPanel f = Theme.vbox();
        f.add(Theme.left(Theme.label(label, Theme.MEDIUM, 13, new Color(0x344054))));
        f.add(Theme.gap(7));
        if (input.getPreferredSize().height < 40) input.setPreferredSize(new Dimension(input.getPreferredSize().width, 44));
        if (input instanceof RoundTextField || input instanceof PasswordField)
            input.setPreferredSize(new Dimension(400, 44));
        f.add(fullWidth(input));
        if (hint != null) {
            f.add(Theme.gap(6));
            f.add(Theme.left(Theme.label(hint, Font.PLAIN, 12, Theme.TEXT_GRAY)));
        }
        if (error != null) {
            f.add(Theme.gap(4));
            f.add(Theme.left(error));
        }
        f.setBorder(new EmptyBorder(0, 0, error == null ? 16 : 8, 0));
        return fullWidth(f);
    }

    private static JComponent fullWidth(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    private static JLabel err() { return Theme.label(" ", Font.PLAIN, 12, Theme.RED); }

    private static JLabel rule(String text) {
        JLabel l = Theme.label(text, Font.PLAIN, 12, Theme.TEXT_GRAY);     // TEXT_GRAY: readable (HINT was too light)
        l.setIcon(Icons.get("check", 13, Theme.HINT));
        l.setIconTextGap(5);
        return l;
    }

    private static void mark(JLabel l, boolean ok) {
        l.setForeground(ok ? Theme.GREEN : Theme.TEXT_GRAY);
        l.setIcon(Icons.get("check", 13, ok ? Theme.GREEN : Theme.HINT));
    }

    private static void onType(javax.swing.text.JTextComponent t, Runnable r) {
        t.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }

    /** Rounded choice button (used for gender). */
    private static JToggleButton pill(String text) {
        JToggleButton b = new JToggleButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.aa(g2);
                boolean on = isSelected();
                g2.setColor(on ? Theme.GREEN_LIGHT : getModel().isRollover() ? new Color(0xF9FAFB) : Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.setColor(on ? Theme.GREEN : new Color(0xD9DEE3));
                g2.setStroke(new BasicStroke(on ? 1.6f : 1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1.5f, getHeight() - 1.5f, 10, 10));
                g2.setFont(Theme.font(on ? Font.BOLD : Theme.MEDIUM, 13));
                g2.setColor(on ? Theme.GREEN : Theme.TEXT_DARK);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(120, 44));
        return b;
    }

    // ================= pieces =================

    /** Left side: brand, photo and the list of steps. */
    private final class Side extends JPanel {
        Side() {
            super(new BorderLayout());
            setPreferredSize(new Dimension(380, 0));
            setBorder(new EmptyBorder(40, 40, 36, 36));
            JPanel top = Theme.vbox();
            top.add(Theme.left(Theme.row(10, new LogoIcon(Color.WHITE, 30, 27), Theme.label("SabayLakbay", Font.BOLD, 22, Color.WHITE))));
            top.add(Theme.gap(40));
            top.add(Theme.left(Theme.label("Join the community", Font.BOLD, 24, Color.WHITE)));
            top.add(Theme.gap(6));
            top.add(Theme.left(Theme.label("<html>Travel with verified people who love<br>the same trips you do.</html>",
                    Font.PLAIN, 14, new Color(255, 255, 255, 210))));
            top.add(Theme.gap(36));
            stepper.setAlignmentX(LEFT_ALIGNMENT);
            top.add(stepper);
            add(top, BorderLayout.NORTH);
            JLabel login = Theme.label("<html>Already have an account? <b><u>Log in</u></b></html>", Font.PLAIN, 13, Color.WHITE);
            login.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            login.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { AppWindow.show(new LoginForm(), "SabayLakbay - Log In"); }
            });
            add(login, BorderLayout.SOUTH);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            int w = getWidth(), h = getHeight();
            BufferedImage img = ImageBox.load(sabaylakbay_1.model.Photos.LOGIN);
            if (img != null) {
                ImageBox.drawCover(g2, img, w, h);
                g2.setPaint(new GradientPaint(0, 0, new Color(6, 34, 25, 200), 0, h, new Color(6, 34, 25, 135)));   // photo shows through
            } else {
                g2.setPaint(new GradientPaint(0, 0, new Color(0x0B4F3A), w, h, new Color(0x123B2E)));
            }
            g2.fillRect(0, 0, w, h);
            g2.dispose();
        }
    }

    /** Vertical list of the 5 steps: done = check, current = white ring, next = faded. */
    private static final class Stepper extends JComponent {
        private int step = 1;
        Stepper() {
            setPreferredSize(new Dimension(300, STEPS.length * 62));
            setMaximumSize(new Dimension(300, STEPS.length * 62));
        }
        void setStep(int s) { step = s; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            for (int i = 0; i < STEPS.length; i++) {
                int y = i * 62, n = i + 1;
                boolean done = n < step, now = n == step;
                if (i < STEPS.length - 1) {
                    g2.setColor(new Color(255, 255, 255, done ? 200 : 60));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawLine(15, y + 34, 15, y + 62);
                }
                if (done) {
                    g2.setColor(Color.WHITE);
                    g2.fillOval(1, y + 2, 28, 28);
                    Path2D c = new Path2D.Float();
                    c.moveTo(9, y + 16); c.lineTo(13.5, y + 20.5); c.lineTo(21, y + 11.5);
                    g2.setColor(Theme.GREEN);
                    g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.draw(c);
                } else {
                    g2.setColor(now ? Color.WHITE : new Color(255, 255, 255, 90));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawOval(2, y + 3, 26, 26);
                    g2.setFont(Theme.font(Font.BOLD, 12));
                    FontMetrics fm = g2.getFontMetrics();
                    String s = String.valueOf(n);
                    g2.drawString(s, 15 - fm.stringWidth(s) / 2, y + 16 + fm.getAscent() / 2 - 1);
                }
                g2.setFont(Theme.font(Font.BOLD, 14));
                g2.setColor(now || done ? Color.WHITE : new Color(255, 255, 255, 140));
                g2.drawString(STEPS[i][0], 44, y + 15);
                g2.setFont(Theme.font(Font.PLAIN, 12));
                g2.setColor(new Color(255, 255, 255, now ? 200 : 110));
                g2.drawString(STEPS[i][1], 44, y + 33);
            }
            g2.dispose();
        }
    }

    /** Password strength bar: 4 parts, red to green. */
    private static final class Strength extends JComponent {
        private int level;   // 0..4
        void set(int l) { level = Math.min(4, l); repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            Color[] c = {Theme.RED, new Color(0xF59E0B), new Color(0x22A06B), Theme.GREEN};
            int gap = 6, w = (getWidth() - gap * 3) / 4, h = getHeight();
            for (int i = 0; i < 4; i++) {
                g2.setColor(i < level ? c[Math.max(0, level - 1)] : new Color(0xEAECF0));
                g2.fillRoundRect(i * (w + gap), 0, w, h, h, h);
            }
            g2.dispose();
        }
    }

    /** Round photo with a small camera badge; click to choose a picture. */
    private final class AvatarPicker extends JComponent {
        File file;
        AvatarPicker() {
            setPreferredSize(new Dimension(84, 84));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Upload a profile photo");
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { choose(); }
            });
        }
        void choose() {
            File f = Dialogs.chooseImage(RegisterForm.this, "Choose a profile photo");
            if (f != null) { file = f; repaint(); }
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            int s = 80;
            BufferedImage img = file == null ? null : ImageBox.load(file.getAbsolutePath());
            Shape circle = new Ellipse2D.Float(0, 0, s, s);
            if (img != null) {
                Shape old = g2.getClip();
                g2.clip(circle);
                ImageBox.drawCover(g2, img, s, s);
                g2.setClip(old);
            } else {
                g2.setColor(new Color(0xF2F4F7));
                g2.fill(circle);
                Icons.get("user", 34, Theme.HINT).paintIcon(this, g2, s / 2 - 17, s / 2 - 17);
            }
            g2.setColor(Color.WHITE);
            g2.fillOval(s - 26, s - 26, 28, 28);
            g2.setColor(Theme.GREEN);
            g2.fillOval(s - 24, s - 24, 24, 24);
            Icons.get("plus", 14, Color.WHITE).paintIcon(this, g2, s - 19, s - 19);
            g2.dispose();
        }
    }

    /** Dashed upload box for the ID photo; shows a preview once chosen. */
    private final class IdDrop extends JComponent {
        File file;
        private boolean hover;
        IdDrop() {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    File f = Dialogs.chooseImage(RegisterForm.this, "Choose a photo of your ID");
                    if (f != null) { file = f; errId.setText(" "); repaint(); }
                }
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            int w = getWidth(), h = getHeight();
            RoundRectangle2D box = new RoundRectangle2D.Float(1, 1, w - 3, h - 3, 16, 16);
            g2.setColor(hover ? new Color(0xF4FAF7) : new Color(0xFCFCFD));
            g2.fill(box);
            g2.setColor(hover || file != null ? Theme.GREEN : new Color(0xC9D3DB));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{7, 6}, 0));
            g2.draw(box);
            g2.setStroke(new BasicStroke(1f));
            BufferedImage img = file == null ? null : ImageBox.load(file.getAbsolutePath());
            if (img != null) {
                int ph = h - 40, pw = Math.min(w / 2, (int) (ph * 1.58));
                Shape clip = new RoundRectangle2D.Float(20, 20, pw, ph, 12, 12);
                Shape old = g2.getClip();
                g2.clip(clip);
                g2.translate(20, 20);
                ImageBox.drawCover(g2, img, pw, ph);
                g2.translate(-20, -20);
                g2.setClip(old);
                int tx = 40 + pw;
                g2.setFont(Theme.font(Font.BOLD, 14));
                g2.setColor(Theme.TEXT_DARK);
                g2.drawString(file.getName().length() > 30 ? file.getName().substring(0, 28) + "\u2026" : file.getName(), tx, h / 2 - 8);
                g2.setFont(Theme.font(Font.PLAIN, 12));
                g2.setColor(Theme.GREEN);
                g2.drawString("Ready to upload", tx, h / 2 + 12);
                g2.setColor(Theme.LINK);
                g2.drawString("Click to change", tx, h / 2 + 32);
            } else {
                g2.setColor(Theme.GREEN_LIGHT);
                g2.fillOval(w / 2 - 26, h / 2 - 54, 52, 52);
                Icons.get("image", 24, Theme.GREEN).paintIcon(this, g2, w / 2 - 12, h / 2 - 40);
                String a = "Click to upload a photo of your ID", b = "JPG or PNG";
                g2.setFont(Theme.font(Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(Theme.TEXT_DARK);
                g2.drawString(a, (w - fm.stringWidth(a)) / 2, h / 2 + 24);
                g2.setFont(Theme.font(Font.PLAIN, 12));
                fm = g2.getFontMetrics();
                g2.setColor(Theme.TEXT_GRAY);
                g2.drawString(b, (w - fm.stringWidth(b)) / 2, h / 2 + 44);
            }
            g2.dispose();
        }
    }
}
