package sabaylakbay_1.ui.components;

import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

/** Rounded text field with gray hint text and an optional icon on the left. */
public class RoundTextField extends JTextField {
    private final String hint;
    private Icon leftIcon;
    private boolean borderless;

    public RoundTextField(String hint) {
        this.hint = hint;
        setOpaque(false);
        setFont(Theme.font(Font.PLAIN, 13));
        setForeground(Theme.TEXT_DARK);
        setCaretColor(Theme.TEXT_DARK);
        setBorder(new EmptyBorder(0, 12, 0, 12));
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e) { repaint(); }
        });
    }

    public RoundTextField withIcon(String icon) {
        leftIcon = Icons.get(icon, 17, Theme.TEXT_GRAY);
        setBorder(new EmptyBorder(0, 40, 0, 12));
        return this;
    }

    /** No box (used inside the search bar). */
    public RoundTextField borderless() { borderless = true; return this; }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, 40);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        if (!borderless) {
            RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.setColor(isEditable() ? Color.WHITE : new Color(0xF3F4F6));
            g2.fill(r);
            g2.setColor(hasFocus() ? Theme.GREEN : new Color(0xD9DEE3));
            g2.draw(r);
        }
        if (leftIcon != null) leftIcon.paintIcon(this, g2, 13, (getHeight() - leftIcon.getIconHeight()) / 2);
        g2.dispose();
        super.paintComponent(g);
        if (getDocument().getLength() == 0 && hint != null) {
            Graphics2D h = (Graphics2D) g.create();
            Theme.aa(h);
            h.setFont(getFont());
            h.setColor(Theme.HINT);
            FontMetrics fm = h.getFontMetrics();
            h.drawString(hint, getInsets().left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            h.dispose();
        }
    }
}
