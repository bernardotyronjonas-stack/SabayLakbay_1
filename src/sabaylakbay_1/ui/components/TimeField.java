package sabaylakbay_1.ui.components;

import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A time box with no dropdown: type the hour and the minutes, then pick AM or PM.
 *
 *   [clock]  [ 6 ] : [ 00 ]   ( AM | PM )
 *
 * Typing digits, the Up / Down arrows and the mouse wheel all work on the hour and minute boxes.
 * The minutes step by 5 with the arrows; any minute 00-59 can be typed.
 */
public class TimeField extends JPanel {

    private static final Color LINE = new Color(0xD9DEE3);

    private final JTextField hour = box(2);
    private final JTextField minute = box(2);
    private final JToggleButton am = half("AM"), pm = half("PM");
    private final List<Runnable> listeners = new ArrayList<>();
    private LocalTime time = LocalTime.of(8, 0);
    private boolean busy;
    private final JLabel clock;

    /** In a narrow box (the stop popup) the clock icon steps aside so AM / PM always fit. */
    @Override public void doLayout() {
        clock.setVisible(getWidth() >= 196);
        super.doLayout();
    }

    public TimeField() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);
        setBorder(new EmptyBorder(4, 10, 4, 6));
        setPreferredSize(new Dimension(220, 40));

        clock = new JLabel(Icons.get("clock", 16, Theme.TEXT_GRAY));
        clock.setBorder(new EmptyBorder(0, 0, 0, 8));
        JLabel colon = new JLabel(":");
        colon.setFont(Theme.font(Font.BOLD, 14));
        colon.setForeground(Theme.TEXT_DARK);
        colon.setBorder(new EmptyBorder(0, 3, 0, 3));

        ButtonGroup g = new ButtonGroup();
        g.add(am); g.add(pm);
        JPanel ampm = new JPanel(new GridLayout(1, 2, 0, 0)) {
            @Override protected void paintComponent(Graphics gr) {
                Graphics2D g2 = (Graphics2D) gr.create();
                Theme.aa(g2);
                g2.setColor(new Color(0xF2F4F7));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
            }
        };
        ampm.setOpaque(false);
        ampm.setBorder(new EmptyBorder(2, 2, 2, 2));
        ampm.add(am); ampm.add(pm);

        add(clock); add(hour); add(colon); add(minute);
        add(Box.createHorizontalStrut(8));
        add(ampm);
        add(Box.createHorizontalGlue());
        ampm.setMaximumSize(new Dimension(80, 30));

        hour.getAccessibleContext().setAccessibleName("Hour");
        minute.getAccessibleContext().setAccessibleName("Minutes");
        am.getAccessibleContext().setAccessibleName("AM");
        pm.getAccessibleContext().setAccessibleName("PM");

        wire(hour, true);
        wire(minute, false);
        am.addActionListener(e -> { if (!busy) setTime(time.getHour() >= 12 ? time.minusHours(12) : time); });
        pm.addActionListener(e -> { if (!busy) setTime(time.getHour() < 12 ? time.plusHours(12) : time); });
        show(time);
    }

    public LocalTime getTime() { commit(); return time; }

    public void setTime(LocalTime t) {
        if (t == null) return;
        time = t.withSecond(0).withNano(0);
        show(time);
        for (Runnable r : listeners) r.run();
    }

    public void onChange(Runnable r) { listeners.add(r); }

    @Override public void setEnabled(boolean b) {
        super.setEnabled(b);
        hour.setEnabled(b); minute.setEnabled(b); am.setEnabled(b); pm.setEnabled(b);
        repaint();
    }

    // ===== typing =====
    private void wire(JTextField f, boolean isHour) {
        ((javax.swing.text.AbstractDocument) f.getDocument()).setDocumentFilter(new javax.swing.text.DocumentFilter() {
            @Override public void replace(FilterBypass fb, int off, int len, String s, javax.swing.text.AttributeSet a)
                    throws javax.swing.text.BadLocationException {
                String digits = s == null ? "" : s.replaceAll("\\D", "");
                if (fb.getDocument().getLength() - len + digits.length() <= 2) super.replace(fb, off, len, digits, a);
            }
            @Override public void insertString(FilterBypass fb, int off, String s, javax.swing.text.AttributeSet a)
                    throws javax.swing.text.BadLocationException { replace(fb, off, 0, s, a); }
        });
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { f.selectAll(); TimeField.this.repaint(); }
            @Override public void focusLost(FocusEvent e) { commit(); TimeField.this.repaint(); }
        });
        f.addActionListener(e -> commit());
        f.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP) { step(isHour, 1); e.consume(); }
                else if (e.getKeyCode() == KeyEvent.VK_DOWN) { step(isHour, -1); e.consume(); }
            }
            @Override public void keyReleased(KeyEvent e) {
                // after two hour digits (or one digit 2-9) jump to the minutes, like a phone clock
                if (isHour && Character.isDigit(e.getKeyChar())) {
                    String t = hour.getText();
                    if (t.length() == 2 || (t.length() == 1 && t.charAt(0) >= '2')) { commit(); minute.requestFocusInWindow(); }
                }
            }
        });
        f.addMouseWheelListener(e -> { if (isEnabled()) step(isHour, e.getWheelRotation() < 0 ? 1 : -1); });
    }

    private void step(boolean isHour, int dir) {
        commit();
        if (isHour) { setTime(time.plusHours(dir)); return; }
        int m = time.getMinute();
        int to = dir > 0 ? (m / 5 + 1) * 5 : (m % 5 == 0 ? m - 5 : m - m % 5);   // next / previous 5 minutes
        setTime(time.plusMinutes(to - m));
    }

    /** Reads what was typed into the boxes; a bad value goes back to the last good time. */
    private void commit() {
        if (busy) return;
        try {
            int h = Integer.parseInt(hour.getText().trim());
            int m = minute.getText().isBlank() ? 0 : Integer.parseInt(minute.getText().trim());
            if (h < 1 || h > 12 || m > 59) throw new NumberFormatException();
            int h24 = h % 12 + (pm.isSelected() ? 12 : 0);
            LocalTime t = LocalTime.of(h24, m);
            if (!t.equals(time)) setTime(t); else show(time);
        } catch (NumberFormatException ex) {
            show(time);
        }
    }

    private void show(LocalTime t) {
        busy = true;
        int h = t.getHour() % 12;
        hour.setText(String.valueOf(h == 0 ? 12 : h));
        minute.setText(String.format("%02d", t.getMinute()));
        (t.getHour() < 12 ? am : pm).setSelected(true);
        busy = false;
    }

    // ===== look =====
    private static JTextField box(int cols) {
        JTextField f = new JTextField(cols);
        f.setFont(Theme.font(Font.BOLD, 14));
        f.setForeground(Theme.TEXT_DARK);
        f.setHorizontalAlignment(JTextField.CENTER);
        f.setOpaque(false);
        f.setBorder(new EmptyBorder(4, 2, 4, 2));
        f.setPreferredSize(new Dimension(30, 30));
        f.setMaximumSize(new Dimension(30, 30));
        return f;
    }

    private static JToggleButton half(String text) {
        JToggleButton b = new JToggleButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.aa(g2);
                if (isSelected()) {
                    g2.setColor(isEnabled() ? Theme.GREEN : Theme.HINT);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 7, 7));
                }
                if (hasFocus() && Theme.showFocus(this)) {
                    g2.setColor(Theme.ORANGE);
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 7, 7));
                }
                g2.setFont(getFont());
                g2.setColor(isSelected() ? Color.WHITE : Theme.TEXT_GRAY);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        b.setFont(Theme.font(Font.BOLD, 12));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(38, 26));
        b.setMaximumSize(new Dimension(38, 26));
        return b;
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        int w = getWidth(), h = getHeight();
        boolean focus = isEnabled() && (hour.hasFocus() || minute.hasFocus());
        g2.setColor(isEnabled() ? Color.WHITE : new Color(0xF3F5F6));
        g2.fill(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 10, 10));
        if (focus) {
            g2.setColor(new Color(11, 79, 58, 40));
            g2.setStroke(new BasicStroke(3f));
            g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3, h - 3, 9, 9));
            g2.setStroke(new BasicStroke(1f));
        }
        g2.setColor(focus ? Theme.GREEN : LINE);
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 10, 10));
        // light boxes behind the hour and minutes so they read as fields
        for (JTextField f : new JTextField[]{hour, minute}) {
            Rectangle r = f.getBounds();
            g2.setColor(f.hasFocus() ? new Color(0xE7F1EC) : new Color(0xF6F7F9));
            g2.fill(new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, 7, 7));
        }
        g2.dispose();
    }
}
