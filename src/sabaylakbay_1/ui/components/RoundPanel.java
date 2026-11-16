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

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, radius, radius);
        if (fill != null) { g2.setColor(fill); g2.fill(r); }
        if (stroke != null) { g2.setColor(stroke); g2.draw(r); }
        g2.dispose();
    }

    @Override public boolean isOptimizedDrawingEnabled() { return false; }
}
