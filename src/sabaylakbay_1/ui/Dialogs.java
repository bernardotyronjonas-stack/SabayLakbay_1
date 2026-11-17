package sabaylakbay_1.ui;

import sabaylakbay_1.ui.components.RoundButton;
import sabaylakbay_1.ui.components.RoundTextField;
import sabaylakbay_1.ui.components.TextAreaBox;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * The app's pop-up boxes (used instead of the plain Java JOptionPane).
 * Shown inside the main window: the page behind dims, Enter = main button, Esc = cancel.
 *
 *   Dialogs.message(this, Dialogs.Kind.SUCCESS, "Saved", "Your profile was updated.");
 *   if (Dialogs.confirm(this, "Leave trip?", "You can ask to join again later.", "Leave trip", true)) ...
 *   if (Dialogs.form(this, "Add expense", "Shown to every member.", panel, "Add")) ...
 *   String reason = Dialogs.input(this, "Suspend account", "Why?", "Reason", "Suspend", true);
 */
public final class Dialogs {

    public enum Kind { INFO, SUCCESS, WARNING, DANGER, QUESTION }

    private Dialogs() { }

    // ================= simple versions =================

    public static void message(Component parent, Kind kind, String title, String text) {
        new Sheet(parent, kind, null, title, text, null, "OK", null, false).open();
    }

    /** Yes / no question. danger = red main button (delete, leave, block...). */
    public static boolean confirm(Component parent, String title, String text, String okText, boolean danger) {
        return new Sheet(parent, danger ? Kind.DANGER : Kind.QUESTION, null, title, text, null, okText, "Cancel", danger).open();
    }

    /** A box with your own fields inside. Returns true when the main button was clicked. */
    public static boolean form(Component parent, String title, String subtitle, JComponent body, String okText) {
        return new Sheet(parent, Kind.QUESTION, null, title, subtitle, body, okText, "Cancel", false).open();
    }

    /** Same as form() but with a chosen icon in the badge, e.g. "calendar", "wallet", "star". */
    public static boolean form(Component parent, String icon, String title, String subtitle, JComponent body, String okText) {
        return new Sheet(parent, Kind.QUESTION, icon, title, subtitle, body, okText, "Cancel", false).open();
    }

    /** form() that only closes when check says the fields are fine (the fields show their own mistakes). */
    public static boolean form(Component parent, String icon, String title, String subtitle, JComponent body, String okText,
                               java.util.function.BooleanSupplier check) {
        Sheet s = new Sheet(parent, Kind.QUESTION, icon, title, subtitle, body, okText, "Cancel", false);
        s.validator = () -> check.getAsBoolean() ? null : "Please fix the fields above.";
        return s.open();
    }

    /** Asks for one line of text. Returns null when cancelled. Empty text is not accepted when required. */
    public static String input(Component parent, String title, String text, String placeholder, String okText,
                               boolean danger, boolean required) {
        TextAreaBox box = new TextAreaBox(placeholder, 3);
        Sheet s = new Sheet(parent, danger ? Kind.DANGER : Kind.QUESTION, null, title, text, box, okText, "Cancel", danger);
        s.validator = () -> {
            if (required && box.getText().trim().isEmpty()) return "Please type something first.";
            return null;
        };
        s.firstFocus = box.getArea();
        return s.open() ? box.getText().trim() : null;
    }

    /** One-line text input (e.g. an email). */
    public static String inputLine(Component parent, String icon, String title, String text, String placeholder, String okText) {
        RoundTextField f = new RoundTextField(placeholder);
        Sheet s = new Sheet(parent, Kind.QUESTION, icon, title, text, f, okText, "Cancel", false);
        s.validator = () -> f.getText().trim().isEmpty() ? "Please fill this in first." : null;
        s.firstFocus = f;
        return s.open() ? f.getText().trim() : null;
    }

    // ================= file pickers (the normal Windows file window) =================

    /** Lets the user pick a .jpg or .png. Returns null when cancelled. */
    public static java.io.File chooseImage(Component parent, String title) {
        FileDialog fd = fileDialog(parent, title, FileDialog.LOAD);
        fd.setFile("*.jpg;*.jpeg;*.png");
        fd.setFilenameFilter((dir, name) -> name.toLowerCase().matches(".*\\.(jpe?g|png)$"));
        fd.setVisible(true);
        if (fd.getFile() == null) return null;
        java.io.File f = new java.io.File(fd.getDirectory(), fd.getFile());
        if (!f.getName().toLowerCase().matches(".*\\.(jpe?g|png)$")) {
            message(parent, Kind.WARNING, "That's not a photo", "Please choose a .jpg or .png file.");
            return null;
        }
        return f;
    }

    /** Asks where to save a file. Returns null when cancelled. */
    public static java.io.File chooseSave(Component parent, String title, String fileName) {
        FileDialog fd = fileDialog(parent, title, FileDialog.SAVE);
        fd.setFile(fileName);
        fd.setVisible(true);
        return fd.getFile() == null ? null : new java.io.File(fd.getDirectory(), fd.getFile());
    }

    private static FileDialog fileDialog(Component parent, String title, int mode) {
        Window w = windowOf(parent);
        return w instanceof Dialog ? new FileDialog((Dialog) w, title, mode) : new FileDialog((Frame) w, title, mode);
    }

    // ================= the box itself =================

    /**
     * The box itself. It is drawn INSIDE the main window (no separate window):
     * the page behind dims, the card sits in the middle, and open() waits until it's closed.
     */
    private static final class Sheet {

        private boolean ok;
        Validator validator;
        Component firstFocus;
        private final JLabel errorLabel = Theme.label(" ", Font.PLAIN, 12, Theme.RED);
        private final Card card = new Card(true);
        private final Overlay overlay = new Overlay();
        private final int minW;
        private SecondaryLoop loop;
        private java.awt.event.ComponentAdapter follow;

        interface Validator { String check(); }

        Sheet(Component parent, Kind kind, String icon, String title, String text, JComponent body,
              String okText, String cancelText, boolean danger) {
            card.setLayout(new BorderLayout(0, 0));

            // ---- header: badge + title + text
            JPanel head = new JPanel(new BorderLayout(14, 0));
            head.setOpaque(false);
            head.add(badge(kind, icon), BorderLayout.WEST);
            JPanel words = Theme.vbox();
            JLabel t = Theme.label(title, Font.BOLD, 18, Theme.TEXT_DARK);
            t.setAlignmentX(0f);
            words.add(t);
            if (text != null && !text.isEmpty()) {
                words.add(Theme.gap(4));
                int w = body == null ? 300 : Math.max(300, body.getPreferredSize().width - 60);
                JLabel l = Theme.label("<html><div style='width:" + w + "px;line-height:1.4'>"
                        + Theme.escape(text).replace("\n", "<br>") + "</div></html>", Font.PLAIN, 13, Theme.TEXT_GRAY);
                l.setAlignmentX(0f);
                words.add(l);
            }
            head.add(words, BorderLayout.CENTER);
            card.add(head, BorderLayout.NORTH);

            // ---- body (your fields)
            if (body != null) {
                JPanel b = new JPanel(new BorderLayout());
                b.setOpaque(false);
                b.setBorder(new EmptyBorder(18, 0, 0, 0));
                b.add(body, BorderLayout.CENTER);
                card.add(b, BorderLayout.CENTER);
            }

            // ---- buttons
            RoundButton okBtn = new RoundButton(okText, danger ? RoundButton.Style.DANGER_SOLID : RoundButton.Style.PRIMARY);
            okBtn.setPreferredSize(new Dimension(Math.max(96, okBtn.getPreferredSize().width), 40));
            okBtn.addActionListener(e -> {
                if (validator != null) {
                    String err = validator.check();
                    if (err != null) { errorLabel.setText(err); layoutCard(); return; }
                }
                ok = true;
                close();
            });
            JPanel foot = new JPanel(new BorderLayout());
            foot.setOpaque(false);
            foot.setBorder(new EmptyBorder(20, 0, 0, 0));
            foot.add(errorLabel, BorderLayout.WEST);
            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            btns.setOpaque(false);
            if (cancelText != null) {
                RoundButton cancel = new RoundButton(cancelText, RoundButton.Style.OUTLINE);
                cancel.setPreferredSize(new Dimension(Math.max(96, cancel.getPreferredSize().width), 40));
                cancel.addActionListener(e -> close());
                btns.add(cancel);
            }
            btns.add(okBtn);
            foot.add(btns, BorderLayout.EAST);
            card.add(foot, BorderLayout.SOUTH);

            // Enter = main button (text areas keep Enter for new lines), Esc = cancel
            card.registerKeyboardAction(e -> okBtn.doClick(), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                    JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            card.registerKeyboardAction(e -> close(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            card.setFocusCycleRoot(true);
            minW = (body == null ? 440 : 500) + Card.SHADOW * 2;
            if (firstFocus == null) firstFocus = okBtn;
            overlay.add(card);
        }

        boolean open() {
            AppWindow w = AppWindow.get();
            if (w == null) return false;
            JLayeredPane layers = w.getLayeredPane();
            overlay.setBounds(0, 0, layers.getWidth(), layers.getHeight());
            layers.add(overlay, Integer.valueOf(JLayeredPane.MODAL_LAYER));
            layoutCard();
            follow = new java.awt.event.ComponentAdapter() {
                @Override public void componentResized(java.awt.event.ComponentEvent e) {
                    overlay.setBounds(0, 0, layers.getWidth(), layers.getHeight());
                    layoutCard();
                }
            };
            layers.addComponentListener(follow);

            // the page behind fades to a soft dark
            long start = System.currentTimeMillis();
            Timer fade = new Timer(15, null);
            fade.addActionListener(e -> {
                overlay.p = Math.min(1f, (System.currentTimeMillis() - start) / 160f);
                overlay.repaint();
                if (overlay.p >= 1f) fade.stop();
            });
            fade.start();
            SwingUtilities.invokeLater(() -> { if (firstFocus != null) firstFocus.requestFocusInWindow(); });

            // wait here until the box is closed (the window keeps painting and reacting)
            loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
            loop.enter();
            return ok;
        }

        private void layoutCard() {
            Dimension d = card.getPreferredSize();
            int w = Math.max(minW, d.width), h = d.height;
            int ow = overlay.getWidth(), oh = overlay.getHeight();
            card.setBounds(Math.max(0, (ow - w) / 2), Math.max(0, (oh - h) / 2 - 20), w, Math.min(h, Math.max(200, oh)));
            card.revalidate();
            overlay.repaint();
        }

        private void close() {
            AppWindow w = AppWindow.get();
            if (w != null) {
                JLayeredPane layers = w.getLayeredPane();
                layers.removeComponentListener(follow);
                layers.remove(overlay);
                layers.repaint();
            }
            if (loop != null) { loop.exit(); loop = null; }
        }
    }

    /** Dark see-through layer over the page; it also stops clicks reaching the page. */
    private static final class Overlay extends JComponent {
        float p;
        Overlay() {
            setLayout(null);
            addMouseListener(new java.awt.event.MouseAdapter() { });
            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() { });
        }
        @Override protected void paintComponent(Graphics g) {
            g.setColor(new Color(10, 22, 18, Math.round(110 * p)));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // ================= pieces =================

    private static JComponent badge(Kind kind, String icon) {
        Color bg, fg; String ic;
        switch (kind) {
            case SUCCESS: bg = Theme.GREEN_LIGHT; fg = Theme.GREEN; ic = "check"; break;
            case WARNING: bg = Theme.YELLOW_LIGHT; fg = new Color(0xB7791F); ic = "info"; break;
            case DANGER:  bg = Theme.RED_LIGHT; fg = Theme.RED; ic = "info"; break;
            case INFO:    bg = Theme.BLUE_LIGHT; fg = Theme.BLUE; ic = "info"; break;
            default:      bg = Theme.GREEN_LIGHT; fg = Theme.GREEN; ic = "info";
        }
        if (icon != null) ic = icon;
        final Color fbg = bg;
        Icon i = Icons.get(ic, 20, fg);
        JComponent c = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.aa(g2);
                g2.setColor(fbg);
                g2.fillOval(0, 0, 44, 44);
                i.paintIcon(this, g2, 22 - i.getIconWidth() / 2, 22 - i.getIconHeight() / 2);
                g2.dispose();
            }
        };
        c.setPreferredSize(new Dimension(44, 44));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(c, BorderLayout.NORTH);
        return wrap;
    }

    /** White rounded card with a soft shadow. */
    private static final class Card extends JPanel {
        static final int SHADOW = 18;
        private final boolean shadow;
        Card(boolean shadow) {
            this.shadow = shadow;
            setOpaque(!shadow);
            setBackground(Color.WHITE);
            int s = shadow ? SHADOW : 0;
            setBorder(new EmptyBorder(26 + s, 26 + s, 22 + s, 26 + s));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            if (shadow) {
                int s = SHADOW;
                for (int i = 0; i < s; i++) {
                    float a = 0.10f * (1f - (float) i / s) * (1f - (float) i / s);
                    g2.setColor(new Color(0f, 0.08f, 0.05f, a));
                    g2.fill(new RoundRectangle2D.Float(s - i, s - i + 4, getWidth() - 2 * (s - i), getHeight() - 2 * (s - i), 18 + i * 2, 18 + i * 2));
                }
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(s, s, getWidth() - 2 * s, getHeight() - 2 * s, 18, 18));
            } else {
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.BORDER);
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
            g2.dispose();
        }
    }


    private static Window windowOf(Component c) {
        Window w = c == null ? null : (c instanceof Window ? (Window) c : SwingUtilities.getWindowAncestor(c));
        return w != null ? w : AppWindow.get();
    }

}
