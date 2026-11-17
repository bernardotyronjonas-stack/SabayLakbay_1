package sabaylakbay_1.ui;

import sabaylakbay_1.model.RequestStatus;
import sabaylakbay_1.ui.components.Chip;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Colors, fonts and small helpers shared by every screen. */
public final class Theme {

    public static final Color GREEN = new Color(0x0B4F3A);
    public static final Color GREEN_HOVER = new Color(0x0D6A4D);
    public static final Color GREEN_LIGHT = new Color(0xE7F4EE);
    public static final Color ORANGE = new Color(0xF4600C);
    public static final Color ORANGE_HOVER = new Color(0xDB5309);
    public static final Color BG = new Color(0xF6F7F9);
    public static final Color WHITE = Color.WHITE;
    public static final Color TEXT_DARK = new Color(0x101828);
    public static final Color TEXT_GRAY = new Color(0x667085);
    public static final Color HINT = new Color(0x98A2B3);
    public static final Color BORDER = new Color(0xE9ECEF);
    public static final Color LINK = new Color(0x137A4F);
    public static final Color RED = new Color(0xD93C3C);
    public static final Color RED_LIGHT = new Color(0xFDECEC);
    public static final Color YELLOW = new Color(0xF5B301);
    public static final Color YELLOW_LIGHT = new Color(0xFFF6DD);
    public static final Color BLUE_LIGHT = new Color(0xE8F0FB);
    public static final Color BLUE = new Color(0x2B5FA8);

    /** Medium weight, e.g. Theme.font(Theme.MEDIUM, 14). */
    public static final int MEDIUM = 0x100;

    // Inter font, bundled in src/sabaylakbay_1/fonts (falls back to Segoe UI if it can't load)
    private static final Font[] INTER = new Font[4];          // regular, medium, semibold, bold
    private static final java.util.Map<String, Font> FONT_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    public static final String FONT = loadFonts();

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter DATE_SHORT = DateTimeFormatter.ofPattern("MMM d");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, MMM d");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a");
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");

    static { Look.install(); }

    private Theme() { }

    // ===== fonts / labels =====
    /**
     * The app font. Font.PLAIN = regular, Font.BOLD = semibold (bold for big titles),
     * Theme.MEDIUM = medium. Big text gets slightly tighter letter spacing.
     */
    public static Font font(int style, int size) {
        int s = size <= 11 ? size + 1 : size;                     // small text a bit larger, easier to read
        String key = style + ":" + s;
        return FONT_CACHE.computeIfAbsent(key, k -> {
            boolean italic = (style & Font.ITALIC) != 0;
            if (INTER[0] == null) return new Font("Segoe UI", style & ~MEDIUM, s);
            Font base;
            if ((style & MEDIUM) != 0) base = INTER[1];
            else if ((style & Font.BOLD) != 0) base = s >= 20 ? INTER[3] : INTER[2];
            else base = INTER[0];
            Font f = base.deriveFont(italic ? Font.ITALIC : Font.PLAIN, (float) s);
            if (s >= 20) {
                java.util.Map<java.awt.font.TextAttribute, Object> a = new java.util.HashMap<>();
                a.put(java.awt.font.TextAttribute.TRACKING, s >= 28 ? -0.025f : -0.015f);
                a.put(java.awt.font.TextAttribute.KERNING, java.awt.font.TextAttribute.KERNING_ON);
                f = f.deriveFont(a);
            }
            return f;
        });
    }

    private static String loadFonts() {
        String[] names = {"Regular", "Medium", "SemiBold", "Bold"};
        try {
            for (int i = 0; i < 4; i++) {
                try (java.io.InputStream in = Theme.class.getResourceAsStream("/sabaylakbay_1/fonts/Inter-" + names[i] + ".ttf")) {
                    if (in == null) throw new java.io.IOException("missing font");
                    INTER[i] = Font.createFont(Font.TRUETYPE_FONT, in);
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(INTER[i]);
                }
            }
            return INTER[0].getFamily();
        } catch (Exception e) {
            java.util.Arrays.fill(INTER, null);
            return "Segoe UI";
        }
    }

    public static JLabel label(String text, int style, int size, Color color) {
        if (text != null && text.startsWith("<html>") && (style & Font.BOLD) != 0 && !text.startsWith("<html><b>"))
            text = "<html><b>" + text.substring(6).replace("</html>", "") + "</b></html>";   // keeps HTML text bold
        JLabel l = new JLabel(symbols(text));
        l.setFont(font(style, size));
        l.setForeground(color);
        return l;
    }

    /** Label that wraps long text to the given width. */
    public static JLabel wrapLabel(String text, int width, int style, int size, Color color) {
        return label("<html><div style='width:" + width + "px'>" + escape(text) + "</div></html>", style, size, color);
    }

    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }

    /**
     * Windows' Segoe UI has no star character, so it shows as a box.
     * This draws the star with Segoe UI Symbol (in yellow) inside the same label.
     */
    public static String symbols(String text) {
        if (text == null || text.indexOf('\u2605') < 0 || text.startsWith("<html>")) return text;
        String face = INTER[0] != null ? FONT : "Segoe UI Symbol";
        return "<html><nobr>" + escape(text).replace("\u2605",
                "<font face='" + face + "' color='#F5B301'>&#9733;</font>") + "</nobr></html>";
    }

    public static JLabel title(String text) { return label(text, Font.BOLD, 28, TEXT_DARK); }

    public static JLabel subtitle(String text) { return label(text, Font.PLAIN, 14, TEXT_GRAY); }

    public static JLabel sectionTitle(String text) { return label(text, Font.BOLD, 17, TEXT_DARK); }

    /** Small icon + text row, e.g. calendar + date. */
    public static JLabel iconText(String icon, String text, Color color) {
        JLabel l = label(text, Font.PLAIN, 12, color);
        l.setIcon(Icons.get(icon, 15, color));
        l.setIconTextGap(7);
        return l;
    }

    public static JLabel link(String text, Runnable action) {
        JLabel l = label(text, Font.BOLD, 12, LINK);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { action.run(); }
        });
        return l;
    }

    // ===== layout helpers =====
    public static JPanel clear(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setOpaque(false);
        return p;
    }

    public static JPanel vbox() {
        JPanel p = clear(null);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    /** Horizontal row with a gap between items (no gap on the outer edges). */
    public static JPanel row(int gap, Component... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
            @Override protected void addImpl(Component comp, Object constraints, int index) {
                if (getComponentCount() > 0 && !(comp instanceof Box.Filler))
                    super.addImpl(Box.createRigidArea(new Dimension(gap, 1)), null, -1);
                super.addImpl(comp, constraints, index);
            }
        };
        p.setOpaque(false);
        for (Component c : items) p.add(c);
        return p;
    }

    /** Puts a component in a full-width left-aligned row (useful inside BoxLayout). */
    public static JPanel left(Component c) {
        JPanel p = clear(new BorderLayout());
        p.add(c, BorderLayout.WEST);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return p;
    }

    /** Wraps a component so it is vertically centered and left aligned, filling the width. */
    public static JPanel middleLeft(Component c) {
        JPanel p = clear(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.weightx = 1;
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(c, g);
        return p;
    }

    public static Component gap(int h) { return Box.createRigidArea(new Dimension(0, h)); }

    public static Component hgap(int w) { return Box.createRigidArea(new Dimension(w, 0)); }

    public static EmptyBorder pad(int t, int l, int b, int r) { return new EmptyBorder(t, l, b, r); }

    public static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return sp;
    }

    /** A panel that always matches the scroll pane width (so content wraps instead of scrolling sideways). */
    public static class ScrollableBox extends JPanel implements Scrollable {
        public ScrollableBox(LayoutManager lm) { super(lm); setOpaque(false); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - 40; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    public static JSeparator separator() {
        JSeparator s = new JSeparator();
        s.setForeground(BORDER);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }

    // ===== formatting =====
    public static String peso(double v) { return "\u20B1" + MONEY.format(v); }

    public static String date(LocalDate d) { return d == null ? "" : DATE.format(d); }

    public static String day(LocalDate d) { return d == null ? "" : DAY.format(d); }

    public static String time(LocalTime t) { return t == null ? "" : TIME.format(t); }

    public static String dateRange(LocalDate a, LocalDate b) {
        if (a == null) return "";
        if (b == null || a.equals(b)) return date(a);
        if (a.getYear() == b.getYear()) return DATE_SHORT.format(a) + " - " + DATE.format(b);
        return date(a) + " - " + date(b);
    }

    public static String ago(LocalDateTime t) {
        if (t == null) return "";
        Duration d = Duration.between(t, LocalDateTime.now());
        long m = d.toMinutes();
        if (m < 1) return "just now";
        if (m < 60) return m + "m ago";
        if (m < 60 * 24) return (m / 60) + "h ago";
        if (m < 60 * 24 * 7) return (m / 1440) + "d ago";
        return date(t.toLocalDate());
    }

    public static String stars(double rating) {
        return rating <= 0 ? "No ratings yet" : "\u2605 " + new DecimalFormat("0.0").format(rating);
    }

    // ===== status chips =====
    public static Chip statusChip(RequestStatus s) {
        switch (s) {
            case PENDING:  return new Chip("Pending", YELLOW_LIGHT, new Color(0x9A6B00));
            case APPROVED: return new Chip("Approved", GREEN_LIGHT, GREEN);
            case REJECTED: return new Chip("Rejected", RED_LIGHT, RED);
            case LEFT:     return new Chip("Left the trip", new Color(0xEEF0F2), TEXT_GRAY);
            default:       return new Chip("Cancelled", new Color(0xEEF0F2), TEXT_GRAY);
        }
    }

    /** "ID verified" / "ID pending" / ... label. */
    public static Chip verificationChip(sabaylakbay_1.model.VerificationStatus s) {
        switch (s) {
            case APPROVED: { Chip c = new Chip("ID verified", GREEN_LIGHT, GREEN); c.setIcon(Icons.get("check", 11, GREEN)); c.setIconTextGap(4); return c; }
            case PENDING:  return new Chip("ID being checked", YELLOW_LIGHT, new Color(0x9A6B00));
            case REJECTED: return new Chip("ID not approved", RED_LIGHT, RED);
            default:       return new Chip("No ID uploaded", new Color(0xEEF0F2), TEXT_GRAY);
        }
    }

    // ===== painting =====
    public static void aa(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    // ===== messages =====
    /** Runs a service call and shows the error message if it fails. Returns true when it worked. */
    public static boolean run(Component parent, Runnable action) {
        try {
            action.run();
            return true;
        } catch (RuntimeException ex) {
            error(parent, ex.getMessage() == null ? ex.toString() : ex.getMessage());
            return false;
        }
    }

    public static void error(Component parent, String msg) {
        Dialogs.message(parent, Dialogs.Kind.WARNING, "Please check this", msg);
    }

    public static void info(Component parent, String msg) {
        Dialogs.message(parent, Dialogs.Kind.INFO, "Good to know", msg);
    }

    public static boolean confirm(Component parent, String msg) {
        return Dialogs.confirm(parent, "Are you sure?", msg, "Yes", false);
    }

    /** Question with your own title and button text. danger = red button. */
    public static boolean confirm(Component parent, String title, String msg, String okText, boolean danger) {
        return Dialogs.confirm(parent, title, msg, okText, danger);
    }
}
