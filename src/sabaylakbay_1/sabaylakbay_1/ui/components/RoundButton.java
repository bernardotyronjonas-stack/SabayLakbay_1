package sabaylakbay_1.ui.components;

import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** Rounded button in the app's styles. */
public class RoundButton extends JButton {

    public enum Style { PRIMARY, ORANGE, OUTLINE, LIGHT, DANGER, GHOST, DANGER_SOLID }

    private Style style;

    public RoundButton(String text, Style style) {
        super(text);
        this.style = style;
        setFont(Theme.font(Font.BOLD, 13));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(9, 18, 9, 18));
        setIconTextGap(8);
        applyColors();
    }

    public RoundButton(String text) { this(text, Style.PRIMARY); }

    public RoundButton icon(String iconName) {
        setIcon(Icons.get(iconName, 16, getForeground()));
        return this;
    }

    public RoundButton small() {
        setFont(Theme.font(Font.BOLD, 12));
        setMargin(new Insets(6, 12, 6, 12));
        return this;
    }

    public void setStyle(Style s) { style = s; applyColors(); repaint(); }

    private void applyColors() {
        switch (style) {
            case PRIMARY: case ORANGE: case DANGER_SOLID: setForeground(Color.WHITE); break;
            case DANGER: setForeground(Theme.RED); break;
            default: setForeground(Theme.GREEN);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(d.height, 36));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        boolean hover = getModel().isRollover() && isEnabled();
        RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10);
        Color fill = null, stroke = null;
        switch (style) {
            case PRIMARY: fill = hover ? Theme.GREEN_HOVER : Theme.GREEN; break;
            case ORANGE:  fill = hover ? Theme.ORANGE_HOVER : Theme.ORANGE; break;
            case OUTLINE: fill = hover ? Theme.GREEN_LIGHT : Color.WHITE; stroke = Theme.GREEN; break;
            case LIGHT:   fill = hover ? new Color(0xD5EADF) : Theme.GREEN_LIGHT; break;
            case DANGER:  fill = hover ? Theme.RED_LIGHT : Color.WHITE; stroke = new Color(0xF0B4B4); break;
            case GHOST:   fill = hover ? Theme.GREEN_LIGHT : null; break;
            case DANGER_SOLID: fill = hover ? new Color(0xC23030) : Theme.RED; break;
        }
        if (!isEnabled()) { fill = new Color(0xE5E7EB); stroke = null; }
        if (fill != null) { g2.setColor(fill); g2.fill(r); }
        if (stroke != null) { g2.setColor(stroke); g2.draw(r); }
        g2.dispose();
        if (!isEnabled()) setForeground(Theme.HINT); else applyColors();
        super.paintComponent(g);
    }
}
