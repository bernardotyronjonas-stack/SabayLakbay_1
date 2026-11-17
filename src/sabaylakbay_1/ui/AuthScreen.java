package sabaylakbay_1.ui;

import sabaylakbay_1.ui.components.ImageBox;
import sabaylakbay_1.ui.components.LogoIcon;
import sabaylakbay_1.ui.components.RoundPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Shared look of LoginForm and RegisterForm (full screen):
 * Mayon background, text on the left, white card on the right with Log In / Register tabs.
 * Everything is placed in doLayout() so it fits any screen size.
 */
public abstract class AuthScreen extends JPanel {

    protected static final int FORM_W = 352;
    private static final int CARD_W = FORM_W + 64, FORM_TOP = 100;

    protected final RoundPanel card = new RoundPanel(null, 22, Color.WHITE, null);
    protected final JPanel form = Theme.clear(null);
    private JButton defaultButton;

    // left side
    private final LogoIcon logo = new LogoIcon(Color.WHITE, 34, 30);
    private final JLabel brand = Theme.label("SabayLakbay", Font.BOLD, 26, Color.WHITE);
    private final JLabel tagline;
    private final JLabel headline = Theme.label("<html>Travel together,<br>explore more.</html>", Font.BOLD, 44, Color.WHITE);
    private final JLabel intro;
    private final JLabel spot = Theme.iconText("pin", "Mayon Volcano, Albay", Color.WHITE);
    private final JLabel spotSub;

    /** @param registerTab true = Register tab is active, false = Log In tab is active */
    protected AuthScreen(boolean registerTab) {
        super(null);
        Color soft = new Color(255, 255, 255, 215);
        tagline = Theme.label("Travel Together \u2022 Share Experiences \u2022 Create Memories", Font.PLAIN, 12, soft);
        intro = Theme.label("<html>Create or join travel groups, meet new friends, and make<br>unforgettable memories \u2014 for less.</html>",
                Font.PLAIN, 15, soft);
        spot.setFont(Theme.font(Font.BOLD, 12));
        spotSub = Theme.label("A little closer to your next adventure.", Font.PLAIN, 10, soft);
        for (JComponent c : new JComponent[]{logo, brand, tagline, headline, intro, spot, spotSub}) add(c);

        JLabel tabLogin = tab("Log In", !registerTab);
        JLabel tabRegister = tab("Register", registerTab);
        tabLogin.setBounds(32, 28, FORM_W / 2, 44);
        tabRegister.setBounds(32 + FORM_W / 2, 28, FORM_W / 2, 44);
        tabLogin.addMouseListener(click(this::goToLogin));
        tabRegister.addMouseListener(click(this::goToRegister));
        TabLine line = new TabLine(registerTab);
        line.setBounds(32, 28, FORM_W, 44);
        card.add(tabLogin);
        card.add(tabRegister);
        card.add(line);         // added last = drawn behind the tab labels
        card.add(form);
        add(card);
    }

    /** Call at the end of the subclass constructor with the height of the form. */
    protected void finish(int formHeight) {
        form.setBounds(32, FORM_TOP, FORM_W, formHeight);
        card.setSize(CARD_W, FORM_TOP + formHeight + 34);
    }

    /** Enter key presses this button. */
    protected void setDefaultButton(JButton b) { defaultButton = b; }

    @Override
    public void addNotify() {
        super.addNotify();
        if (defaultButton != null && getRootPane() != null) getRootPane().setDefaultButton(defaultButton);
    }

    /** Places everything based on the window size. */
    @Override
    public void doLayout() {
        int w = getWidth(), h = getHeight();
        int left = Math.max(48, (int) (w * 0.05));
        logo.setBounds(left, 46, 34, 30);
        brand.setBounds(left + 42, 42, 300, 36);
        tagline.setBounds(left, 84, 420, 20);
        int mid = h / 2;
        headline.setBounds(left, mid - 110, 640, 112);
        intro.setBounds(left, mid + 10, 560, 50);
        spot.setBounds(left - 2, h - 88, 300, 18);
        spotSub.setBounds(left + 21, h - 69, 300, 14);
        int right = Math.max(48, (int) (w * 0.07));
        card.setLocation(w - CARD_W - right, Math.max(20, (h - card.getHeight()) / 2));
    }

    protected void goToLogin() {
        if (this instanceof sabaylakbay_1.LoginForm) return;
        AppWindow.show(new sabaylakbay_1.LoginForm(), "SabayLakbay - Log In");
    }

    protected void goToRegister() {
        AppWindow.show(new sabaylakbay_1.RegisterForm(), "SabayLakbay - Register");
    }

    // ================= helpers for the forms =================

    protected int addTitle(String title, String sub, int y) {
        add(Theme.label(title, Font.BOLD, 26, Theme.TEXT_DARK), 0, y, FORM_W, 36);
        add(Theme.label(sub, Font.PLAIN, 14, Theme.TEXT_GRAY), 0, y + 38, FORM_W, 20);
        return y + 78;
    }

    /** Label + input box. Returns the y position for the next row. */
    protected int addField(String label, JComponent input, int x, int w, int y) {
        add(Theme.label(label, Theme.MEDIUM, 13, new Color(0x344054)), x, y, w, 18);
        add(input, x, y + 24, w, 44);
        return y + 82;
    }

    protected void add(JComponent c, int x, int y, int w, int h) {
        c.setBounds(x, y, w, h);
        form.add(c);
    }

    protected static JCheckBox checkBox(String text) {
        JCheckBox c = new JCheckBox(text);
        c.setOpaque(false);
        c.setFocusPainted(false);
        c.setFont(Theme.font(Font.PLAIN, 13));
        c.setForeground(Theme.TEXT_GRAY);
        return c;
    }

    protected static JLabel footerLink(String text, String link, Runnable action) {
        JLabel l = Theme.label("<html>" + text + "<b><font color='#137A4F'>" + link + "</font></b></html>",
                Font.PLAIN, 13, Theme.TEXT_GRAY);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(click(action));
        return l;
    }

    protected static MouseAdapter click(Runnable r) {
        return new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { r.run(); }
        };
    }

    private static JLabel tab(String text, boolean active) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(Theme.font(active ? Font.BOLD : Theme.MEDIUM, 14));
        l.setForeground(active ? Theme.TEXT_DARK : Theme.TEXT_GRAY);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return l;
    }

    /** Background photo from src/images/mayon.jpg (plain green if the file is missing). */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        int w = getWidth(), h = getHeight();
        BufferedImage img = ImageBox.load("/images/mayon.jpg");
        if (img != null) {
            ImageBox.drawCover(g2, img, w, h);
            g2.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 150), w * 0.6f, 0, new Color(0, 0, 0, 30)));
        } else {
            g2.setPaint(new GradientPaint(0, 0, new Color(0x123B2E), w, h, new Color(0x2F5D4A)));
        }
        g2.fillRect(0, 0, w, h);
        // soft shadow behind the card
        Rectangle r = card.getBounds();
        for (int i = 0; i < 12; i++) {
            g2.setColor(new Color(0, 0, 0, 6));
            g2.fillRoundRect(r.x - i + 2, r.y - i + 8, r.width + i * 2 - 4, r.height + i * 2 - 4, 16 + i * 2, 16 + i * 2);
        }
        g2.dispose();
    }

    /** Segmented switch behind the two tabs: gray track, white pill under the active tab. */
    private static class TabLine extends JComponent {
        private final boolean right;
        TabLine(boolean right) { this.right = right; }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(0xF2F4F7));
            g2.fillRoundRect(0, 0, w, h, 14, 14);
            int px = right ? w / 2 + 2 : 4, pw = w / 2 - 6;
            g2.setColor(new Color(16, 24, 40, 18));
            g2.fillRoundRect(px, 5, pw, h - 8, 10, 10);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(px, 4, pw, h - 9, 10, 10);
            g2.dispose();
        }
    }
}
