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
 * real Mayon photo in full color, a dark fade only on the left behind the text, frosted-glass chips,
 * and a white card on the right with underlined Log In / Register tabs.
 * Everything is placed in doLayout() so it fits any screen size.
 */
public abstract class AuthScreen extends JPanel {

    protected static final int FORM_W = 352;
    private static final int CARD_W = FORM_W + 64, FORM_TOP = 100;

    protected final RoundPanel card = new RoundPanel(null, 18, Color.WHITE, null);
    protected final JPanel form = Theme.clear(null);
    private JButton defaultButton;

    // left side
    private final LogoIcon logo = new LogoIcon(Color.WHITE, 34, 30);
    private final JLabel brand = Theme.label("SabayLakbay", Font.BOLD, 26, Color.WHITE);
    private final JLabel tagline;
    private final JLabel headline = Theme.label("<html>Travel together,<br>explore more.</html>", Font.BOLD, 50, Color.WHITE);
    private final JLabel intro;
    private final JPanel perks;                // frosted chips under the intro
    private final Glass place;                 // "Mayon Volcano, Albay" chip at the bottom
    private final JLabel credit;

    // the photo scaled once to the window size (smooth and fast)
    private BufferedImage scaled;
    private BufferedImage scaledFrom;

    /** @param registerTab true = Register tab is active, false = Log In tab is active */
    protected AuthScreen(boolean registerTab) {
        super(null);
        Color soft = new Color(255, 255, 255, 215);
        tagline = Theme.label("Travel Together \u2022 Share Experiences \u2022 Create Memories", Font.PLAIN, 12, soft);
        intro = Theme.label("<html>Create or join travel groups, meet new friends, and make<br>unforgettable memories, for less.</html>",
                Font.PLAIN, 16, soft);

        perks = Theme.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
        String[][] items = {{"shield", "ID-verified travelers"}, {"users", "Group trips"}, {"wallet", "Shared costs"}};
        for (String[] it : items) {
            Glass chip = new Glass(new FlowLayout(FlowLayout.LEFT, 8, 0), 999);
            chip.setBorder(Theme.pad(8, 8, 8, 14));
            JLabel l = Theme.label(it[1], Theme.MEDIUM, 13, Color.WHITE);
            l.setIcon(Icons.get(it[0], 15, Color.WHITE));
            l.setIconTextGap(8);
            chip.add(l);
            perks.add(chip);
            perks.add(Box.createHorizontalStrut(10));
        }

        place = new Glass(new BorderLayout(10, 0), 14);
        place.setBorder(Theme.pad(10, 12, 10, 16));
        place.add(new JLabel(Icons.get("pin", 18, Color.WHITE)), BorderLayout.WEST);
        JPanel pt = Theme.vbox();
        pt.add(Theme.left(Theme.label(sabaylakbay_1.model.Photos.LOGIN_PLACE, Font.BOLD, 13, Color.WHITE)));
        pt.add(Theme.left(Theme.label("A little closer to your next adventure.", Font.PLAIN, 11, soft)));
        place.add(pt, BorderLayout.CENTER);

        credit = Theme.label(sabaylakbay_1.model.Photos.LOGIN_CREDIT, Font.PLAIN, 10, new Color(255, 255, 255, 160));
        credit.setHorizontalAlignment(SwingConstants.RIGHT);
        for (JComponent c : new JComponent[]{logo, brand, tagline, headline, intro, perks, place, credit}) add(c);

        JLabel tabLogin = tab("Log In", !registerTab);
        JLabel tabRegister = tab("Register", registerTab);
        tabLogin.setBounds(32, 24, FORM_W / 2, 48);
        tabRegister.setBounds(32 + FORM_W / 2, 24, FORM_W / 2, 48);
        tabLogin.addMouseListener(click(this::goToLogin));
        tabRegister.addMouseListener(click(this::goToRegister));
        TabLine line = new TabLine(registerTab);
        line.setBounds(32, 24, FORM_W, 48);
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
        headline.setBounds(left, mid - 150, 640, 128);
        intro.setBounds(left, mid - 8, 600, 52);
        Dimension pp = perks.getPreferredSize();
        perks.setBounds(left, mid + 66, Math.min(pp.width, w / 2), pp.height);
        Dimension pl = place.getPreferredSize();
        place.setBounds(left, h - pl.height - 40, pl.width + 8, pl.height);
        credit.setBounds(w - 560, h - 28, 532, 16);
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
        l.setFont(Theme.font(active ? Font.BOLD : Theme.MEDIUM, 15));
        l.setForeground(active ? Theme.GREEN : Theme.TEXT_GRAY);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return l;
    }

    /**
     * Background: the real photo in full color, with fades only where text sits
     * (left side, top and bottom), so it looks designed instead of a picture plugged in.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        int w = getWidth(), h = getHeight();
        BufferedImage img = ImageBox.load(sabaylakbay_1.model.Photos.LOGIN);
        if (img != null) {
            // scaled to the screen's real pixels (Windows display scaling 125% / 150%), so it stays sharp
            double sx = g2.getTransform().getScaleX(), sy = g2.getTransform().getScaleY();
            BufferedImage photo = scaledPhoto(img, (int) Math.ceil(w * sx), (int) Math.ceil(h * sy));
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.drawImage(photo, 0, 0, w, h, null);
            // left: deep green-black fade behind the headline, clear on the right
            g2.setPaint(new LinearGradientPaint(0, 0, w, 0, new float[]{0f, 0.32f, 0.58f, 1f},
                    new Color[]{new Color(4, 20, 15, 215), new Color(4, 20, 15, 150), new Color(4, 20, 15, 20), new Color(4, 20, 15, 0)}));
            g2.fillRect(0, 0, w, h);
            // top: so the logo always reads
            g2.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 110), 0, 170, new Color(0, 0, 0, 0)));
            g2.fillRect(0, 0, w, 170);
            // bottom: grounds the place chip
            g2.setPaint(new GradientPaint(0, h - 280, new Color(0, 0, 0, 0), 0, h, new Color(0, 0, 0, 150)));
            g2.fillRect(0, h - 280, w, 280);
        } else {
            // no photo yet (offline): rich green with soft light
            g2.setPaint(new GradientPaint(0, 0, new Color(0x0E3B2D), w, h, new Color(0x1E5A45)));
            g2.fillRect(0, 0, w, h);
            g2.setPaint(new RadialGradientPaint(new Point(w * 3 / 4, h / 4), Math.max(w, h) / 2f, new float[]{0f, 1f},
                    new Color[]{new Color(255, 255, 255, 40), new Color(255, 255, 255, 0)}));
            g2.fillRect(0, 0, w, h);
        }
        // big soft shadow under the card
        Rectangle r = card.getBounds();
        for (int i = 0; i < 22; i++) {
            g2.setColor(new Color(0, 0, 0, 5));
            g2.fillRoundRect(r.x - i + 4, r.y - i + 18, r.width + i * 2 - 8, r.height + i * 2 - 16, 18 + i * 2, 18 + i * 2);
        }
        g2.dispose();
    }

    /** Scales the photo to fill the window once (high quality), and again only when the size changes. */
    private BufferedImage scaledPhoto(BufferedImage img, int w, int h) {
        if (scaled != null && scaledFrom == img && scaled.getWidth() == w && scaled.getHeight() == h) return scaled;
        BufferedImage out = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_RGB);
        Graphics2D b = out.createGraphics();
        b.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        b.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        ImageBox.drawCover(b, img, w, h);
        b.dispose();
        scaled = out;
        scaledFrom = img;
        return out;
    }

    /** Frosted-glass panel: see-through white with a light edge. */
    private static class Glass extends JPanel {
        private final int arc;
        Glass(LayoutManager lm, int arc) {
            super(lm);
            this.arc = arc;
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            int w = getWidth(), h = getHeight(), a = Math.min(arc, h);
            g2.setColor(new Color(255, 255, 255, 34));
            g2.fillRoundRect(0, 0, w - 1, h - 1, a, a);
            g2.setColor(new Color(255, 255, 255, 70));
            g2.drawRoundRect(0, 0, w - 1, h - 1, a, a);
            g2.dispose();
        }
    }

    /** Underline tabs: a thin gray line across, a thick colored line under the active tab. */
    private static class TabLine extends JComponent {
        private final boolean right;
        TabLine(boolean right) { this.right = right; }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(0xEAECF0));
            g2.fillRect(0, h - 1, w, 1);
            g2.setColor(Theme.GREEN);
            g2.fillRoundRect(right ? w / 2 : 0, h - 3, w / 2, 3, 3, 3);
            g2.dispose();
        }
    }
}
