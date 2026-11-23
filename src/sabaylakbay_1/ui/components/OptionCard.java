package sabaylakbay_1.ui.components;

import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * A big choice card with a radio circle, an icon, a title and one line of explanation.
 * Put several in a ButtonGroup so only one can be picked (e.g. "Request to join" / "Open join").
 * Works with the mouse and the keyboard (Tab, then Space).
 */
public class OptionCard extends JToggleButton {

    private final String title, text, icon;

    public OptionCard(String icon, String title, String text) {
        this.icon = icon;
        this.title = title;
        this.text = text;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(280, 76));
        getAccessibleContext().setAccessibleName(title + ". " + text);
        addItemListener(e -> repaint());
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        boolean on = isSelected();
        int w = getWidth(), h = getHeight();
        RoundRectangle2D box = new RoundRectangle2D.Float(1, 1, w - 3, h - 3, 14, 14);
        g2.setColor(on ? Theme.GREEN_LIGHT : getModel().isRollover() ? new Color(0xF9FAFB) : Color.WHITE);
        g2.fill(box);
        g2.setStroke(new BasicStroke(on || Theme.showFocus(this) ? 2f : 1f));
        g2.setColor(on || Theme.showFocus(this) ? Theme.GREEN : new Color(0xD9DEE3));
        g2.draw(box);

        // radio circle
        int cy = h / 2;
        g2.setStroke(new BasicStroke(1.6f));
        g2.setColor(on ? Theme.GREEN : new Color(0x98A2B3));
        g2.draw(new Ellipse2D.Float(16, cy - 9, 18, 18));
        if (on) g2.fill(new Ellipse2D.Float(20.5f, cy - 4.5f, 9, 9));

        // icon + text
        Icons.get(icon, 20, on ? Theme.GREEN : Theme.TEXT_GRAY).paintIcon(this, g2, 46, cy - 10);
        g2.setColor(Theme.TEXT_DARK);
        g2.setFont(Theme.font(Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, 78, cy - 4);
        g2.setColor(Theme.TEXT_GRAY);
        g2.setFont(Theme.font(Font.PLAIN, 12));
        fm = g2.getFontMetrics();
        String t = text;
        while (fm.stringWidth(t) > w - 92 && t.length() > 4) t = t.substring(0, t.length() - 2);
        if (!t.equals(text)) t = t.trim() + "…";
        g2.drawString(t, 78, cy + 14);
        g2.dispose();
    }
}
