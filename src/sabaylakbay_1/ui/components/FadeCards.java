package sabaylakbay_1.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * A panel that switches between screens with a smooth fade:
 * the old screen fades out while the new one slides up slightly into place.
 * Used for the whole app window and for the pages inside each dashboard.
 */
public class FadeCards extends JPanel {

    private static final int DURATION_MS = 260;
    private static final int RISE_PX = 14;

    private final CardLayout cards = new CardLayout();
    private BufferedImage snapshot;
    private float progress = 1f;   // 0 = transition just started, 1 = done
    private Timer timer;
    private long startTime;

    private final boolean rise;

    /** Plain cross-fade (used for the whole window). */
    public FadeCards() {
        this(false);
    }

    /** @param rise true = the new page also slides up a little (used for pages inside a dashboard) */
    public FadeCards(boolean rise) {
        this.rise = rise;
        setLayout(cards);
        setOpaque(true);
    }

    /** Adds a screen that can be shown later with show(key). */
    public void addCard(JComponent c, String key) {
        add(c, key);
    }

    /** Shows a screen added with addCard, with the fade. */
    public void show(String key) {
        takeSnapshot();
        cards.show(this, key);
        startFade();
    }

    /** Replaces everything with one new screen, with the fade (used for Log In -> Dashboard, etc.). */
    public void replace(JComponent screen) {
        takeSnapshot();
        removeAll();
        add(screen, "screen");
        cards.show(this, "screen");
        revalidate();
        startFade();
    }

    private void takeSnapshot() {
        snapshot = null;
        if (!isShowing() || getWidth() <= 0 || getHeight() <= 0) return;
        double scale = getGraphicsConfiguration() == null ? 1 : getGraphicsConfiguration().getDefaultTransform().getScaleX();
        BufferedImage img = new BufferedImage((int) Math.ceil(getWidth() * scale), (int) Math.ceil(getHeight() * scale),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.scale(scale, scale);
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paint(g);
        g.dispose();
        snapshot = img;
    }

    private void startFade() {
        if (snapshot == null) { progress = 1f; repaint(); return; }
        if (timer != null) timer.stop();
        progress = 0f;
        startTime = System.currentTimeMillis();
        timer = new Timer(12, e -> {
            float t = Math.min(1f, (System.currentTimeMillis() - startTime) / (float) DURATION_MS);
            progress = 1f - (1f - t) * (1f - t) * (1f - t);   // ease-out
            if (t >= 1f) {
                ((Timer) e.getSource()).stop();
                snapshot = null;
            }
            repaint();
        });
        timer.start();
    }

    @Override
    public void paint(Graphics g) {
        if (snapshot == null || progress >= 1f) {
            super.paint(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        // new screen, rising into place
        int offset = rise ? Math.round(RISE_PX * (1 - progress)) : 0;
        if (offset > 0) {
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), offset);
        }
        Graphics2D content = (Graphics2D) g2.create();
        content.translate(0, offset);
        super.paint(content);
        content.dispose();
        // old screen fading out on top
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - progress));
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(snapshot, 0, 0, getWidth(), getHeight(), null);
        g2.dispose();
    }

    @Override
    public boolean isOptimizedDrawingEnabled() { return false; }
}
