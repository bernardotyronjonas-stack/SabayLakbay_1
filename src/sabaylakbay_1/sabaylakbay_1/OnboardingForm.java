package sabaylakbay_1;

import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.model.User;
import sabaylakbay_1.ui.AppWindow;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Sign-up steps after the Register form (and once for older accounts with no interests yet):
 *   Step 1  What do you like?  (Hiking, Beach, Island hopping...)
 *   Step 2  Where do you live?  (province, city, barangay, street, ZIP + optional map pin)
 * The answers decide what the Home page suggests ("Picked for you") and how far each trip is.
 *
 * Backend: register(user, password, idPhoto, interests), saveInterests, updateLocation
 */
public class OnboardingForm extends JPanel {

    private final boolean signUp;          // true = new account, false = logged-in traveler setting up
    private final User pending;            // sign-up only: the account to create
    private final String password;
    private final File idPhoto;

    private final InterestPicker interests = new InterestPicker(Backend.INTERESTS, 3);
    private final AddressForm home = new AddressForm(false);
    private final FadeCards steps = new FadeCards();
    private final JLabel stepLabel = Theme.label("STEP 1 OF 2", Font.BOLD, 12, Theme.GREEN);
    private final Progress progress = new Progress();
    private final JLabel title = Theme.label("What do you love to do?", Font.BOLD, 28, Theme.TEXT_DARK);
    private final JLabel sub = Theme.label("Pick at least one. We'll suggest trips and groups that match what you like.", Font.PLAIN, 14, Theme.TEXT_GRAY);
    private final JLabel counter = Theme.label(" ", Font.PLAIN, 13, Theme.TEXT_GRAY);
    private final RoundButton back = new RoundButton("Back", RoundButton.Style.OUTLINE);
    private final RoundButton next = new RoundButton("Continue");
    private final JLabel skip;
    private int step = 1;

    /** After the Register form: nothing is saved until the last step. */
    public OnboardingForm(User pending, String password, File idPhoto) {
        this(true, pending, password, idPhoto);
    }

    /** For a logged-in traveler who hasn't picked interests yet. */
    public OnboardingForm() {
        this(false, Backend.getCurrentUser(), null, null);
    }

    private OnboardingForm(boolean signUp, User pending, String password, File idPhoto) {
        super(new GridBagLayout());
        this.signUp = signUp;
        this.pending = pending;
        this.password = password;
        this.idPhoto = idPhoto;
        setBackground(Theme.BG);

        RoundPanel card = new RoundPanel(new BorderLayout(0, 0), 24, Color.WHITE, Theme.BORDER);
        card.setBorder(Theme.pad(32, 40, 28, 40));
        card.setPreferredSize(new Dimension(820, 780));

        // ----- top: logo, step, progress, title
        JPanel top = Theme.vbox();
        JPanel brand = Theme.clear(new BorderLayout());
        brand.add(Theme.row(10, new LogoIcon(Theme.GREEN, 26, 23), Theme.label("SabayLakbay", Font.BOLD, 18, Theme.TEXT_DARK)), BorderLayout.WEST);
        brand.add(stepLabel, BorderLayout.EAST);
        top.add(fullWidth(brand));
        top.add(Theme.gap(18));
        progress.setPreferredSize(new Dimension(100, 6));
        top.add(fullWidth(progress));
        top.add(Theme.gap(26));
        top.add(Theme.left(title));
        top.add(Theme.gap(6));
        top.add(Theme.left(sub));
        top.add(Theme.gap(22));
        card.add(top, BorderLayout.NORTH);

        // ----- the two steps
        JPanel s1 = Theme.clear(new BorderLayout(0, 12));
        s1.add(interests, BorderLayout.NORTH);
        s1.add(counter, BorderLayout.SOUTH);
        interests.onChange(this::updateButtons);
        steps.addCard(s1, "1");
        JPanel s2 = Theme.clear(new BorderLayout());
        s2.add(home, BorderLayout.CENTER);
        steps.addCard(s2, "2");
        card.add(steps, BorderLayout.CENTER);

        // ----- buttons
        JPanel foot = Theme.clear(new BorderLayout());
        foot.setBorder(Theme.pad(22, 0, 0, 0));
        back.setPreferredSize(new Dimension(110, 44));
        back.addActionListener(e -> goBack());
        next.setPreferredSize(new Dimension(170, 44));
        next.addActionListener(e -> goNext());
        skip = Theme.link("Skip for now", this::skipStep);
        foot.add(back, BorderLayout.WEST);
        foot.add(Theme.row(20, skip, next), BorderLayout.EAST);
        card.add(foot, BorderLayout.SOUTH);

        add(card);

        if (!signUp && pending != null) {
            interests.setSelected(Backend.getInterests(pending.getId()));
            home.load(pending);
        }
        showStep(1);
    }

    private void showStep(int s) {
        step = s;
        steps.show(String.valueOf(s));
        progress.set(s);
        stepLabel.setText("STEP " + s + " OF 2");
        if (s == 1) {
            title.setText("What do you love to do?");
            sub.setText("Pick at least one. We'll suggest trips and groups that match what you like.");
            back.setVisible(signUp);
            skip.setVisible(!signUp);
        } else {
            title.setText("Where do you live?");
            sub.setText("Enter your home address. The map pin is optional and only used to show trips near you.");
            back.setVisible(true);
            skip.setVisible(true);
        }
        updateButtons();
        revalidate();
        repaint();
    }

    private void updateButtons() {
        int n = interests.getSelected().size();
        counter.setText(n == 0 ? "Nothing picked yet" : n + (n == 1 ? " interest" : " interests") + " picked");
        counter.setForeground(n == 0 ? Theme.TEXT_GRAY : Theme.GREEN);
        if (step == 1) {
            next.setText("Continue");
            next.setEnabled(n > 0);
        } else {
            next.setText(signUp ? "Create account" : "Finish");
            next.setEnabled(true);
        }
    }

    private void goBack() {
        if (step == 2) showStep(1);
        else AppWindow.show(new RegisterForm(), "SabayLakbay - Register");
    }

    private void goNext() {
        if (step == 1) {
            if (interests.getSelected().isEmpty()) return;
            showStep(2);
        } else {
            if (home.validateAll()) finish(true);
        }
    }

    private void skipStep() {
        if (step == 1) AppWindow.show(new MainDashboard(), "SabayLakbay");     // setup mode only
        else finish(false);
    }

    /** Saves everything. useLocation = false when the location step was skipped. */
    private void finish(boolean useLocation) {
        User addr = new User();
        if (useLocation) home.applyTo(addr);
        List<String> picked = interests.getSelected();
        if (signUp) {
            if (useLocation) home.applyTo(pending);
            // BACKEND: creates the account, ID verification, interests and home location together
            if (Theme.run(this, () -> Backend.register(pending, password, idPhoto, picked)))
                AppWindow.show(new LoginForm(pending.getEmail()), "SabayLakbay - Log In");
        } else {
            if (Theme.run(this, () -> {
                Backend.saveInterests(pending.getId(), picked);
                if (useLocation) Backend.updateAddress(pending.getId(), addr, addr.getLatitude(), addr.getLongitude());
            })) {
                Toast.show("All set! Your Home page now shows trips picked for you.");
                AppWindow.show(new MainDashboard(), "SabayLakbay");
            }
        }
    }

    private static JComponent fullWidth(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    /** Two-part progress bar. */
    private static final class Progress extends JComponent {
        private int step = 1;
        void set(int s) { step = s; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            int gap = 8, w = (getWidth() - gap) / 2, h = getHeight();
            for (int i = 0; i < 2; i++) {
                g2.setColor(i < step ? Theme.GREEN : new Color(0xEAECF0));
                g2.fillRoundRect(i * (w + gap), 0, w, h, h, h);
            }
            g2.dispose();
        }
    }
}
