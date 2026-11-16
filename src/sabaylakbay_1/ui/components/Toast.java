package sabaylakbay_1.ui.components;

import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Small dark note that fades in at the bottom of the window and fades out after a few seconds.
 * Toast.show("Saved") works from any screen or popup.
 */
public class Toast extends JComponent {

    private static Toast current;

    private final String text;
    private float alpha;
    private Timer timer;

    private Toast(String text) {
        this.text = text;
        setFont(Theme.font(Font.BOLD, 13));
    }

    public static void show(String text) {
        Window w = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        if (!(w instanceof RootPaneContainer)) {
            for (Window x : Window.getWindows()) if (x.isShowing() && x instanceof RootPaneContainer) w = x;
        }
        if (!(w instanceof RootPaneContainer)) return;
        JLayeredPane layer = ((RootPaneContainer) w).getLayeredPane();
        if (current != null) current.close();
        Toast t = new Toast(text);
        current = t;
        FontMetrics fm = t.getFontMetrics(t.getFont());
        int tw = fm.stringWidth(text) + 70, th = 44;
        t.setBounds((layer.getWidth() - tw) / 2, layer.getHeight() - th - 32, tw, th);
        layer.add(t, JLayeredPane.POPUP_LAYER);
        t.start();
    }

    private void start() {
        long begin = System.currentTimeMillis();
        timer = new Timer(15, e -> {
            long ms = System.currentTimeMillis() - begin;
            if (ms < 200) alpha = ms / 200f;                       // fade in
            else if (ms < 2600) alpha = 1f;                        // stay
            else if (ms < 3000) alpha = 1f - (ms - 2600) / 400f;   // fade out
            else { close(); return; }
            repaint();
        });
        timer.start();
    }

    private void close() {
        if (timer != null) timer.stop();
        Container p = getParent();
        if (p != null) {
            p.remove(this);
            p.repaint(getX(), getY(), getWidth(), getHeight());
        }
        if (current == this) current = null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
        g2.setColor(new Color(0x1E2A35));
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
        Icons.get("info", 18, new Color(0x9FD9BE)).paintIcon(this, g2, 18, (getHeight() - 18) / 2);
        g2.setColor(Color.WHITE);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, 46, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
