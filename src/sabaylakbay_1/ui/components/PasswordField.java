package sabaylakbay_1.ui.components;

import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/** Rounded password field with hint text and a show/hide eye on the right. */
public class PasswordField extends JPasswordField {
    private static final char DOT = '\u2022';
    private final String hint;
    private boolean visible;

    public PasswordField(String hint) {
        this.hint = hint;
        setOpaque(false);
        setFont(Theme.font(Font.PLAIN, 13));
        setForeground(Theme.TEXT_DARK);
        setCaretColor(Theme.TEXT_DARK);
        setEchoChar(DOT);
        setBorder(new EmptyBorder(0, 12, 0, 40));
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e) { repaint(); }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (onEye(e.getX())) {
                    visible = !visible;
                    setEchoChar(visible ? (char) 0 : DOT);
                    repaint();
                }
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                setCursor(onEye(e.getX()) ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            }
        });
    }

    private boolean onEye(int x) { return x > getWidth() - 38; }

    public String getValue() { return new String(getPassword()); }

    @Override
    public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, 40); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10);
        g2.setColor(Color.WHITE);
        g2.fill(r);
        g2.setColor(hasFocus() ? Theme.GREEN : new Color(0xD9DEE3));
        g2.draw(r);
        // eye icon
        float cx = getWidth() - 20, cy = getHeight() / 2f;
        g2.setColor(Theme.TEXT_GRAY);
        g2.setStroke(new BasicStroke(1.6f));
        Path2D eye = new Path2D.Float();
        eye.moveTo(cx - 9, cy); eye.quadTo(cx, cy - 9, cx + 9, cy); eye.quadTo(cx, cy + 9, cx - 9, cy);
        g2.draw(eye);
        g2.draw(new Ellipse2D.Float(cx - 3, cy - 3, 6, 6));
        if (visible) g2.draw(new Line2D.Float(cx - 8, cy + 7, cx + 8, cy - 7));
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
