package sabaylakbay_1.ui.components;

import sabaylakbay_1.model.User;
import sabaylakbay_1.ui.AppWindow;
import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * "New message" card that slides up in the bottom-right corner of the main window (inside it, not a separate window).
 * Click it to open the chat. It closes by itself after a few seconds, or with the X.
 *   MessagePopup.show(sender, "Ana Reyes", "Hey! Are you joining?", "Mt. Mayon Adventure", () -> openChat());
 */
public final class MessagePopup extends JComponent {

    private static final int W = 384, H = 112, MARGIN = 14, SHADOW = 10;
    private static MessagePopup current;

    private final Runnable onOpen;
    private final Timer anim;
    private float open;            // 0 hidden, 1 shown
    private long shownAt;
    private boolean hover, closing;

    private MessagePopup(User sender, String name, String text, String where, Runnable onOpen) {
        this.onOpen = onOpen;
        setLayout(new BorderLayout(12, 0));
        setBorder(Theme.pad(SHADOW + 12, SHADOW + 16, SHADOW + 12, SHADOW + 10));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        add(new Avatar(sender, 42), BorderLayout.WEST);
        JPanel t = Theme.vbox();
        JLabel tag = Theme.label(where == null ? "New message" : "New message in " + where, Font.BOLD, 11, Theme.GREEN);
        tag.setIcon(Icons.get("chat", 13, Theme.GREEN));
        tag.setIconTextGap(5);
        t.add(Theme.left(tag));
        t.add(Theme.gap(2));
        t.add(Theme.left(Theme.label(name, Font.BOLD, 14, Theme.TEXT_DARK)));
        String preview = text == null ? "" : text.replace('\n', ' ');
        if (preview.length() > 40) preview = preview.substring(0, 38).trim() + "...";
        t.add(Theme.left(Theme.label(preview, Font.PLAIN, 13, Theme.TEXT_GRAY)));
        add(Theme.middleLeft(t), BorderLayout.CENTER);

        JLabel close = new JLabel(Icons.get("x", 14, Theme.HINT));
        close.setVerticalAlignment(SwingConstants.TOP);
        close.setToolTipText("Close");
        close.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { e.consume(); dismiss(); }
        });
        add(close, BorderLayout.EAST);

        MouseAdapter m = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { dismiss(); if (MessagePopup.this.onOpen != null) MessagePopup.this.onOpen.run(); }
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) {
                if (!contains(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), MessagePopup.this))) { hover = false; repaint(); }
            }
        };
        addMouseListener(m);
        for (Component c : t.getComponents()) c.addMouseListener(m);

        anim = new Timer(15, e -> step());
    }

    /** Shows the card (replacing one that is already showing). */
    public static void show(User sender, String name, String text, String where, Runnable onOpen) {
        AppWindow w = AppWindow.get();
        if (w == null) return;
        if (current != null) current.removeNow();
        MessagePopup p = new MessagePopup(sender, name, text, where, onOpen);
        current = p;
        JLayeredPane layers = w.getLayeredPane();
        layers.add(p, JLayeredPane.POPUP_LAYER);
        p.place();
        layers.addComponentListener(p.follow);
        p.shownAt = System.currentTimeMillis();
        p.anim.start();
    }

    private final ComponentAdapter follow = new ComponentAdapter() {
        @Override public void componentResized(ComponentEvent e) { place(); }
    };

    private void place() {
        Container layers = getParent();
        if (layers == null) return;
        int lift = Math.round((1 - open) * 40);
        setBounds(layers.getWidth() - W - MARGIN, layers.getHeight() - H - MARGIN + lift, W, H);
        revalidate();
    }

    private void step() {
        boolean stay = !closing && (hover || System.currentTimeMillis() - shownAt < 7000);
        float target = stay ? 1f : 0f;
        if (open == target && stay) return;          // fully shown: just wait
        open += (target - open) * 0.2f;
        if (Math.abs(target - open) < 0.02f) {
            open = target;
            if (!stay) { removeNow(); return; }
        }
        place();
        repaint();
    }

    private void dismiss() {
        closing = true;
        anim.start();
    }

    private void removeNow() {
        anim.stop();
        Container layers = getParent();
        if (layers != null) {
            layers.removeComponentListener(follow);
            layers.remove(this);
            layers.repaint();
        }
        if (current == this) current = null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, open))));
        int w = getWidth(), h = getHeight();
        for (int i = SHADOW; i > 0; i -= 2) {                       // soft shadow
            g2.setColor(new Color(16, 24, 40, 6));
            g2.fill(new RoundRectangle2D.Float(SHADOW - i, SHADOW - i + 3, w - SHADOW * 2 + i * 2, h - SHADOW * 2 + i * 2, 18 + i, 18 + i));
        }
        Shape card = new RoundRectangle2D.Float(SHADOW, SHADOW, w - SHADOW * 2, h - SHADOW * 2, 16, 16);
        g2.setColor(hover ? new Color(0xFAFBFC) : Color.WHITE);
        g2.fill(card);
        g2.setColor(Theme.BORDER);
        g2.draw(card);
        g2.setColor(Theme.GREEN);                                    // colored edge on the left
        Shape old = g2.getClip();
        g2.clip(card);
        g2.fillRect(SHADOW, SHADOW, 4, h - SHADOW * 2);
        g2.setClip(old);
        g2.dispose();
    }

    @Override
    protected void paintChildren(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, open))));
        super.paintChildren(g2);
        g2.dispose();
    }
}
