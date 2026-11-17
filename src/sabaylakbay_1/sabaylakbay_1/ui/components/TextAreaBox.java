package sabaylakbay_1.ui.components;

import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/** Multi-line rounded text box with a hint. */
public class TextAreaBox extends RoundPanel {
    private final JTextArea area;

    public TextAreaBox(String hint, int rows) {
        super(new BorderLayout(), 10, Color.WHITE, new Color(0xD9DEE3));
        area = new JTextArea(rows, 20) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getDocument().getLength() == 0 && hint != null) {
                    Graphics2D h = (Graphics2D) g.create();
                    Theme.aa(h);
                    h.setColor(Theme.HINT);
                    h.setFont(getFont());
                    h.drawString(hint, getInsets().left, getInsets().top + h.getFontMetrics().getAscent());
                    h.dispose();
                }
            }
        };
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Theme.font(Font.PLAIN, 13));
        area.setForeground(Theme.TEXT_DARK);
        area.setOpaque(false);
        area.setBorder(new EmptyBorder(10, 12, 10, 12));
        area.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { setStroke(Theme.GREEN); }
            @Override public void focusLost(FocusEvent e) { setStroke(new Color(0xD9DEE3)); }
        });
        JScrollPane sp = Theme.scroll(area);
        add(sp);
    }

    public String getText() { return area.getText(); }
    public void setText(String s) { area.setText(s == null ? "" : s); area.setCaretPosition(0); }
    public JTextArea getArea() { return area; }
}
