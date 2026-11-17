package sabaylakbay_1.ui.components;

import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** Panel with rounded corners, optional border. */
public class RoundPanel extends JPanel {
    private final int radius;
    private Color fill;
    private Color stroke;
    private int margin;            // space kept around the card for its shadow (hover cards only)
    private float lift;            // 0 = resting, 1 = mouse over
    private float target;
    private Color hoverStroke;
    private Timer anim;

    public RoundPanel(LayoutManager lm, int radius, Color fill, Color stroke) {
        super(lm);
        this.radius = radius;
        this.fill = fill;
        this.stroke = stroke;
        setOpaque(false);
    }

    /** White card with a light border. */
    public static RoundPanel card(LayoutManager lm) {
        return new RoundPanel(lm, 14, Color.WHITE, Theme.BORDER);
    }

    public void setFill(Color c) { fill = c; repaint(); }
    public void setStroke(Color c) { stroke = c; repaint(); }

    /**
     * Makes the card react to the mouse: a soft shadow grows and the border turns green
     * while the pointer is anywhere over it (also over its buttons and photo).
     */
    public RoundPanel hoverLift() {
        margin = 8;
        hoverStroke = new Color(0xA9CDBB);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(margin, margin, margin + 2, margin));
        HoverTracker.register(this);
        return this;
    }

    void setHovered(boolean on) {
        target = on ? 1f : 0f;
        if (anim == null) {
            anim = new Timer(15, e -> {
                float step = 0.12f;
                lift = lift < target ? Math.min(target, lift + step) : Math.max(target, lift - step);
                repaint();
                if (lift == target) anim.stop();
            });
        }
        anim.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        float m = margin;
        float w = getWidth() - 2 * m - 1, h = getHeight() - 2 * m - 3;
        if (margin > 0) {
            // soft shadow: faint at rest, deeper on hover
            float strength = 0.35f + 0.65f * ease(lift);
            for (int i = 0; i < margin; i++) {
                float a = 0.055f * strength * (1f - (float) i / margin);
                g2.setColor(new Color(0f, 0.12f, 0.08f, a));
                float grow = i;
                g2.fill(new RoundRectangle2D.Float(m - grow + 0.5f, m - grow + 2.5f + 2 * ease(lift),
                        w + 2 * grow, h + 2 * grow, radius + grow * 2, radius + grow * 2));
            }
        }
        RoundRectangle2D r = new RoundRectangle2D.Float(m + 0.5f, m + 0.5f, w, h, radius, radius);
        if (fill != null) { g2.setColor(fill); g2.fill(r); }
        Color line = stroke;
        if (hoverStroke != null && stroke != null) line = mix(stroke, hoverStroke, ease(lift));
        if (line != null) { g2.setColor(line); g2.draw(r); }
        g2.dispose();
    }

    private static float ease(float t) { return t * t * (3 - 2 * t); }

    private static Color mix(Color a, Color b, float t) {
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** One app-wide mouse watcher that tells hover cards when the pointer enters or leaves them. */
    private static final class HoverTracker {
        private static final java.util.Set<RoundPanel> cards =
                java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
        private static RoundPanel current;
        private static boolean installed;

        static void register(RoundPanel p) {
            cards.add(p);
            if (installed) return;
            installed = true;
            Toolkit.getDefaultToolkit().addAWTEventListener(e -> {
                if (!(e instanceof java.awt.event.MouseEvent)) return;
                java.awt.event.MouseEvent me = (java.awt.event.MouseEvent) e;
                RoundPanel found = null;
                if (me.getID() != java.awt.event.MouseEvent.MOUSE_EXITED) {
                    for (Component c = me.getComponent(); c != null; c = c.getParent()) {
                        if (c instanceof RoundPanel && cards.contains(c)) { found = (RoundPanel) c; break; }
                    }
                } else if (current != null && current.isShowing()) {
                    // leaving one part of the card: still hovered while the pointer is inside the card
                    Point at = me.getLocationOnScreen();
                    if (new Rectangle(current.getLocationOnScreen(), current.getSize()).contains(at)) found = current;
                }
                if (found != current) {
                    if (current != null) current.setHovered(false);
                    current = found;
                    if (current != null) current.setHovered(true);
                }
            }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        }
    }

    @Override public boolean isOptimizedDrawingEnabled() { return false; }
}
