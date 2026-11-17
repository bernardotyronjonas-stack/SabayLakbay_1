package sabaylakbay_1.ui.components;

import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Slim message bar across the top of the main window (part of the window, not floating).
 * It slides open, stays a few seconds, then slides closed. Click it to close it sooner.
 *   Toast.show("Saved");
 */
public class Toast extends JComponent {

    private static final int HEIGHT = 40;
    private static Toast bar;            // the one bar of the app (made by AppWindow)

    private String text = "";
    private float open;                  // 0 = hidden, 1 = fully shown
    private long shownAt;
    private Timer timer;

    /** AppWindow puts this at the top of the window. */
    public static Toast create() {
        bar = new Toast();
        return bar;
    }

    private Toast() {
        setFont(Theme.font(Theme.MEDIUM, 13));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { shownAt = 0; }
        });
    }

    public static void show(String text) {
        if (bar == null) return;
        SwingUtilities.invokeLater(() -> bar.start(text));
    }

    private void start(String t) {
        text = t;
        shownAt = System.currentTimeMillis();
        if (timer == null) {
            timer = new Timer(15, e -> step());
        }
        timer.start();
        repaint();
    }

    private void step() {
        boolean stay = System.currentTimeMillis() - shownAt < 3200;
        float target = stay ? 1f : 0f;
        open += (target - open) * 0.22f;
        if (Math.abs(target - open) < 0.01f) {
            open = target;
            if (!stay) timer.stop();
        }
        revalidate();
        repaint();
        SwingUtilities.invokeLater(sabaylakbay_1.ui.SubPage::relayout);   // keep open detail pages in place
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(10, Math.round(HEIGHT * open));
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (open <= 0.01f) return;
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        int w = getWidth(), h = getHeight();
        g2.setColor(Theme.GREEN);
        g2.fillRect(0, 0, w, h);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text) + 26;
        int x = (w - tw) / 2, y = h - HEIGHT;           // content slides down with the bar
        Icons.get("check", 16, new Color(0xA7E3C6)).paintIcon(this, g2, x, y + (HEIGHT - 16) / 2);
        g2.setColor(Color.WHITE);
        g2.drawString(text, x + 26, y + (HEIGHT - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
