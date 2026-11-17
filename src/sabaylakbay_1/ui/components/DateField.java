package sabaylakbay_1.ui.components;

import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Date or time box. Click it to open a calendar (date) or a list of times (time).
 *   DateField d = DateField.date();   d.setDate(LocalDate.now());   d.getDate();
 *   DateField t = DateField.time();   t.setTime(LocalTime.of(8, 0)); t.getTime();
 * Optional: d.setRange(min, max) greys out days outside the range.
 * Keyboard: Up / Down change the value by one day (or 15 minutes), Enter or Space opens the picker.
 */
public class DateField extends JComponent {

    private static final DateTimeFormatter SHOW_DATE = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy");
    private static final DateTimeFormatter SHOW_TIME = DateTimeFormatter.ofPattern("h:mm a");
    private static final Color LINE = new Color(0xD9DEE3);

    private final boolean timeOnly;
    private LocalDate date = LocalDate.now();
    private LocalTime time = LocalTime.of(8, 0);
    private LocalDate min, max;
    private boolean hover;
    private final List<Runnable> listeners = new ArrayList<>();
    private JPopupMenu popup;

    private DateField(boolean timeOnly) {
        this.timeOnly = timeOnly;
        setFocusable(true);
        setFont(Theme.font(Font.PLAIN, 13));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(timeOnly ? 150 : 200, 40));
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (!isEnabled()) return;
                requestFocusInWindow();
                if (popup != null && popup.isVisible()) popup.setVisible(false); else openPicker();
            }
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
        });
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e) { repaint(); }
        });
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (!isEnabled()) return;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP: step(-1); break;
                    case KeyEvent.VK_DOWN: step(1); break;
                    case KeyEvent.VK_ENTER: case KeyEvent.VK_SPACE: openPicker(); e.consume(); break;
                    default:
                }
            }
        });
    }

    public static DateField date() { return new DateField(false); }

    public static DateField time() { return new DateField(true); }

    // ===== values =====
    public LocalDate getDate() { return date; }

    public void setDate(LocalDate d) { if (d != null) { date = d; changed(); } }

    public LocalTime getTime() { return time; }

    public void setTime(LocalTime t) { if (t != null) { time = t.withSecond(0).withNano(0); changed(); } }

    /** Days before min or after max can't be picked. Either can be null. */
    public void setRange(LocalDate min, LocalDate max) { this.min = min; this.max = max; }

    /** Runs whenever the value changes. */
    public void onChange(Runnable r) { listeners.add(r); }

    private void changed() { repaint(); for (Runnable r : listeners) r.run(); }

    private void step(int dir) {
        if (timeOnly) setTime(time.plusMinutes(15L * dir));
        else {
            LocalDate d = date.plusDays(dir);
            if (allowed(d)) setDate(d);
        }
    }

    private boolean allowed(LocalDate d) {
        return (min == null || !d.isBefore(min)) && (max == null || !d.isAfter(max));
    }

    @Override public void setEnabled(boolean b) { super.setEnabled(b); setCursor(Cursor.getPredefinedCursor(b ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR)); repaint(); }

    // ===== the box =====
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        int w = getWidth(), h = getHeight();
        boolean open = popup != null && popup.isVisible();
        boolean focus = (hasFocus() || open) && isEnabled();
        g2.setColor(isEnabled() ? Color.WHITE : new Color(0xF3F5F6));
        g2.fill(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 10, 10));
        if (focus) {
            g2.setColor(new Color(11, 79, 58, 40));
            g2.setStroke(new BasicStroke(3f));
            g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3, h - 3, 9, 9));
            g2.setStroke(new BasicStroke(1f));
        }
        g2.setColor(focus ? Theme.GREEN : (hover && isEnabled() ? new Color(0xB8C1C9) : LINE));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 10, 10));

        Color ink = isEnabled() ? Theme.TEXT_DARK : Theme.HINT;
        Icon ic = Icons.get(timeOnly ? "clock" : "calendar", 16, focus ? Theme.GREEN : Theme.TEXT_GRAY);
        ic.paintIcon(this, g2, 12, (h - ic.getIconHeight()) / 2);
        g2.setFont(getFont());
        g2.setColor(ink);
        FontMetrics fm = g2.getFontMetrics();
        String text = timeOnly ? SHOW_TIME.format(time) : SHOW_DATE.format(date);
        g2.drawString(text, 36, (h - fm.getHeight()) / 2 + fm.getAscent());
        chevron(g2, w - 18, h / 2, 9, open, isEnabled() ? Theme.TEXT_GRAY : Theme.HINT);
        g2.dispose();
    }

    private static void chevron(Graphics2D g2, int cx, int cy, int size, boolean up, Color c) {
        Path2D p = new Path2D.Float();
        float s = size / 2f;
        if (up) { p.moveTo(cx - s, cy + s / 2); p.lineTo(cx, cy - s / 2); p.lineTo(cx + s, cy + s / 2); }
        else    { p.moveTo(cx - s, cy - s / 2); p.lineTo(cx, cy + s / 2); p.lineTo(cx + s, cy - s / 2); }
        g2.setColor(c);
        g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(p);
    }

    // ===== the pickers =====
    private void openPicker() {
        popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER), new EmptyBorder(10, 10, 10, 10)));
        popup.setBackground(Color.WHITE);
        popup.add(timeOnly ? timeList() : new Calendar());
        popup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) { }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) { SwingUtilities.invokeLater(DateField.this::repaint); }
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) { }
        });
        popup.show(this, 0, getHeight() + 4);
        repaint();
    }

    private JComponent timeList() {
        DefaultListModel<LocalTime> m = new DefaultListModel<>();
        for (int i = 0; i < 24 * 4; i++) m.addElement(LocalTime.of(0, 0).plusMinutes(15L * i));
        JList<LocalTime> list = new JList<>(m);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean f) {
                super.getListCellRendererComponent(l, SHOW_TIME.format((LocalTime) v), i, sel, false);
                boolean current = v.equals(time);
                setBorder(new EmptyBorder(7, 12, 7, 12));
                setFont(Theme.font(current ? Font.BOLD : Font.PLAIN, 13));
                setBackground(current ? Theme.GREEN : (sel ? Theme.GREEN_LIGHT : Color.WHITE));
                setForeground(current ? Color.WHITE : (sel ? Theme.GREEN : Theme.TEXT_DARK));
                return this;
            }
        });
        list.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        list.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) { list.setSelectedIndex(list.locationToIndex(e.getPoint())); }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) {
                LocalTime t = list.getSelectedValue();
                if (t != null) { setTime(t); popup.setVisible(false); }
            }
        });
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(null);
        sp.setPreferredSize(new Dimension(Math.max(150, getWidth() - 22), 240));
        int near = (time.getHour() * 60 + time.getMinute()) / 15;
        SwingUtilities.invokeLater(() -> {
            Rectangle r = list.getCellBounds(Math.max(0, near - 3), Math.min(m.size() - 1, near + 5));
            if (r != null) list.scrollRectToVisible(r);
        });
        return sp;
    }

    /** Month view with arrows. */
    private final class Calendar extends JPanel {
        private YearMonth month = YearMonth.from(date);
        private final JLabel title = Theme.label("", Font.BOLD, 14, Theme.TEXT_DARK);
        private final Grid grid = new Grid();

        Calendar() {
            super(new BorderLayout(0, 8));
            setOpaque(false);
            JPanel head = new JPanel(new BorderLayout());
            head.setOpaque(false);
            head.add(arrow(false), BorderLayout.WEST);
            title.setHorizontalAlignment(SwingConstants.CENTER);
            head.add(title, BorderLayout.CENTER);
            head.add(arrow(true), BorderLayout.EAST);
            add(head, BorderLayout.NORTH);
            add(grid, BorderLayout.CENTER);

            JPanel foot = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            foot.setOpaque(false);
            LocalDate today = LocalDate.now();
            if (allowed(today)) {
                foot.add(Theme.link("Today", () -> { setDate(today); popup.setVisible(false); }));
            }
            add(foot, BorderLayout.SOUTH);
            refresh();
        }

        private void refresh() {
            title.setText(month.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + month.getYear());
            grid.repaint();
        }

        private JButton arrow(boolean next) {
            JButton b = new JButton() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.aa(g2);
                    if (getModel().isRollover()) { g2.setColor(Theme.GREEN_LIGHT); g2.fillOval(0, 0, getWidth(), getHeight()); }
                    Path2D p = new Path2D.Float();
                    int cx = getWidth() / 2, cy = getHeight() / 2;
                    if (next) { p.moveTo(cx - 2, cy - 5); p.lineTo(cx + 3, cy); p.lineTo(cx - 2, cy + 5); }
                    else      { p.moveTo(cx + 2, cy - 5); p.lineTo(cx - 3, cy); p.lineTo(cx + 2, cy + 5); }
                    g2.setColor(Theme.GREEN);
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.draw(p);
                    g2.dispose();
                }
            };
            b.setPreferredSize(new Dimension(30, 30));
            b.setContentAreaFilled(false);
            b.setBorder(null);
            b.setFocusable(false);
            b.setRolloverEnabled(true);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> { month = next ? month.plusMonths(1) : month.minusMonths(1); refresh(); });
            return b;
        }

        /** 7 x 6 day cells. */
        private final class Grid extends JComponent {
            static final int CELL = 38, HEAD = 26;
            private LocalDate over;

            Grid() {
                setPreferredSize(new Dimension(CELL * 7, HEAD + CELL * 6));
                addMouseMotionListener(new MouseMotionAdapter() {
                    @Override public void mouseMoved(MouseEvent e) {
                        over = dayAt(e.getPoint());
                        setCursor(Cursor.getPredefinedCursor(over != null && allowed(over) ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                        repaint();
                    }
                });
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseExited(MouseEvent e) { over = null; repaint(); }
                    @Override public void mouseReleased(MouseEvent e) {
                        LocalDate d = dayAt(e.getPoint());
                        if (d != null && allowed(d)) { setDate(d); popup.setVisible(false); }
                    }
                });
            }

            private LocalDate first() {
                LocalDate f = month.atDay(1);
                int shift = f.getDayOfWeek().getValue() % 7;    // Sunday first
                return f.minusDays(shift);
            }

            private LocalDate dayAt(Point p) {
                if (p.y < HEAD) return null;
                int col = p.x / CELL, row = (p.y - HEAD) / CELL;
                if (col < 0 || col > 6 || row < 0 || row > 5) return null;
                return first().plusDays(row * 7L + col);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.aa(g2);
                g2.setFont(Theme.font(Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                String[] names = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
                g2.setColor(Theme.HINT);
                for (int i = 0; i < 7; i++)
                    g2.drawString(names[i], i * CELL + (CELL - fm.stringWidth(names[i])) / 2, 16);

                LocalDate d = first();
                LocalDate today = LocalDate.now();
                for (int r = 0; r < 6; r++) {
                    for (int c = 0; c < 7; c++, d = d.plusDays(1)) {
                        int x = c * CELL, y = HEAD + r * CELL;
                        boolean inMonth = YearMonth.from(d).equals(month);
                        boolean ok = allowed(d);
                        boolean sel = d.equals(date);
                        int pad = 3;
                        if (sel) {
                            g2.setColor(Theme.GREEN);
                            g2.fillOval(x + pad, y + pad, CELL - pad * 2, CELL - pad * 2);
                        } else if (d.equals(over) && ok) {
                            g2.setColor(Theme.GREEN_LIGHT);
                            g2.fillOval(x + pad, y + pad, CELL - pad * 2, CELL - pad * 2);
                        }
                        if (d.equals(today) && !sel) {
                            g2.setColor(Theme.GREEN);
                            g2.setStroke(new BasicStroke(1.4f));
                            g2.drawOval(x + pad, y + pad, CELL - pad * 2 - 1, CELL - pad * 2 - 1);
                        }
                        String s = String.valueOf(d.getDayOfMonth());
                        g2.setFont(Theme.font(sel || d.equals(today) ? Font.BOLD : Font.PLAIN, 13));
                        fm = g2.getFontMetrics();
                        Color ink = sel ? Color.WHITE : !ok ? new Color(0xCDD3D8)
                                : !inMonth ? Theme.HINT
                                : d.getDayOfWeek() == DayOfWeek.SUNDAY ? new Color(0xC2410C) : Theme.TEXT_DARK;
                        g2.setColor(ink);
                        g2.drawString(s, x + (CELL - fm.stringWidth(s)) / 2, y + (CELL - fm.getHeight()) / 2 + fm.getAscent());
                    }
                }
                g2.dispose();
            }
        }
    }
}
