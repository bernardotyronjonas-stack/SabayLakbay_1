package sabaylakbay_1.ui.components;

import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** 1 to 5 stars. Clickable when editable. */
public class StarRating extends JComponent {
    private int value;
    private int hover;
    private final int star;
    private final boolean editable;

    public StarRating(int value, int starSize, boolean editable) {
        this.value = value;
        this.star = starSize;
        this.editable = editable;
        Dimension d = new Dimension(starSize * 5 + 4 * 3, starSize);
        setPreferredSize(d);
        setMaximumSize(d);
        if (editable) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter m = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) { hover = indexAt(e.getX()); repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = 0; repaint(); }
                @Override public void mouseClicked(MouseEvent e) { StarRating.this.value = indexAt(e.getX()); repaint(); }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }
    }

    private int indexAt(int x) { return Math.max(1, Math.min(5, x / (star + 3) + 1)); }

    public int getValue() { return value; }

    @Override
    protected void paintComponent(Graphics g) {
        int shown = hover > 0 ? hover : value;
        for (int i = 0; i < 5; i++) {
            boolean on = i < shown;
            Icons.get(on ? "starFilled" : "star", star, on ? Theme.YELLOW : new Color(0xC9CED4))
                    .paintIcon(this, g, i * (star + 3), 0);
        }
    }
}
