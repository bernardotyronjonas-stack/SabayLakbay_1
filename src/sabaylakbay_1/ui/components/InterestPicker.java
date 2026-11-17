package sabaylakbay_1.ui.components;

import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Grid of tiles to pick what the traveler likes (Hiking, Beach, Food trip...).
 *   InterestPicker p = new InterestPicker(Backend.INTERESTS, 3);
 *   p.setSelected(list);   p.getSelected();
 */
public class InterestPicker extends JPanel {

    private final Map<String, Tile> tiles = new LinkedHashMap<>();
    private final List<Runnable> listeners = new ArrayList<>();

    public InterestPicker(String[] interests, int columns) {
        super(new GridLayout(0, columns, 12, 12));
        setOpaque(false);
        for (String i : interests) {
            Tile t = new Tile(i);
            tiles.put(i, t);
            add(t);
        }
    }

    public List<String> getSelected() {
        List<String> out = new ArrayList<>();
        tiles.forEach((k, t) -> { if (t.on) out.add(k); });
        return out;
    }

    public void setSelected(List<String> selected) {
        tiles.forEach((k, t) -> t.set(selected != null && selected.contains(k)));
    }

    /** Runs whenever a tile is turned on or off. */
    public void onChange(Runnable r) { listeners.add(r); }

    /** One choice: icon in a circle + name, check mark when chosen. */
    private final class Tile extends JComponent {
        private final String name;
        private boolean on, hover;
        private float anim;            // 0 = off, 1 = on (fades)
        private final Timer timer;

        Tile(String name) {
            this.name = name;
            setPreferredSize(new Dimension(150, 104));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFocusable(true);
            timer = new Timer(15, e -> {
                float target = on ? 1f : 0f;
                anim += (target - anim) * 0.3f;
                if (Math.abs(target - anim) < 0.02f) { anim = target; ((Timer) e.getSource()).stop(); }
                repaint();
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { toggle(); requestFocusInWindow(); }
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
            getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "toggle");
            getActionMap().put("toggle", new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { toggle(); }
            });
        }

        void toggle() { set(!on); for (Runnable r : listeners) r.run(); }

        void set(boolean v) { on = v; timer.start(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            int w = getWidth(), h = getHeight();
            Color bg = mix(hover ? new Color(0xF9FAFB) : Color.WHITE, Theme.GREEN_LIGHT, anim);
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 16, 16));
            g2.setStroke(new BasicStroke(anim > 0.5f ? 2f : 1f));
            g2.setColor(anim > 0.5f ? Theme.GREEN : hover || hasFocus() ? new Color(0xC9D3DB) : Theme.BORDER);
            g2.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, 16, 16));

            // icon circle
            int cs = 44, cx = w / 2, cy = 14 + cs / 2;
            g2.setColor(mix(new Color(0xF2F4F7), Theme.GREEN, anim));
            g2.fill(new Ellipse2D.Float(cx - cs / 2f, cy - cs / 2f, cs, cs));
            g2.setColor(mix(new Color(0x475467), Color.WHITE, anim));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            drawIcon(g2, name, cx, cy);

            // name
            g2.setFont(Theme.font(anim > 0.5f ? Font.BOLD : Theme.MEDIUM, 13));
            g2.setColor(anim > 0.5f ? Theme.GREEN : Theme.TEXT_DARK);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(name, (w - fm.stringWidth(name)) / 2, h - 18);

            // check badge
            if (anim > 0.05f) {
                float s = 20 * anim;
                float bx = w - 12 - s / 2, by = 12 + (20 - s) / 2;
                g2.setColor(Theme.GREEN);
                g2.fill(new Ellipse2D.Float(bx - s / 2, by, s, s));
                if (anim > 0.6f) {
                    Path2D c = new Path2D.Float();
                    c.moveTo(bx - 4.5, by + 10.5); c.lineTo(bx - 1, by + 14); c.lineTo(bx + 5, by + 7);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.draw(c);
                }
            }
            g2.dispose();
        }
    }

    /** Small line icons for each interest, drawn around (cx, cy) in a 22 px box. */
    static void drawIcon(Graphics2D g2, String name, int cx, int cy) {
        g2 = (Graphics2D) g2.create();
        g2.translate(cx - 11, cy - 11);
        Path2D p = new Path2D.Float();
        switch (name) {
            case "Hiking":          // mountain with snow line
                p.moveTo(1, 19); p.lineTo(8, 6); p.lineTo(12, 12); p.lineTo(15, 8); p.lineTo(21, 19); p.closePath();
                g2.draw(p);
                g2.draw(new Line2D.Float(5.5f, 11, 10.5f, 11));
                break;
            case "Beach":           // sun over waves
                g2.draw(new Arc2D.Float(6, 3, 10, 10, 0, 180, Arc2D.OPEN));
                g2.draw(new Line2D.Float(4, 8, 18, 8));
                wave(g2, 13); wave(g2, 18);
                break;
            case "Island hopping":  // boat on water
                p.moveTo(3, 12); p.lineTo(19, 12); p.lineTo(16, 16); p.lineTo(6, 16); p.closePath();
                g2.draw(p);
                g2.draw(new Line2D.Float(11, 12, 11, 3));
                Path2D sail = new Path2D.Float();
                sail.moveTo(11, 3); sail.lineTo(17, 10); sail.lineTo(11, 10);
                g2.draw(sail);
                wave(g2, 20);
                break;
            case "Food trip":       // fork and bowl
                g2.draw(new Arc2D.Float(2, 5, 18, 14, 180, 180, Arc2D.OPEN));
                g2.draw(new Line2D.Float(2, 12, 20, 12));
                g2.draw(new Line2D.Float(8, 2, 8, 8));
                g2.draw(new Line2D.Float(11, 2, 11, 8));
                g2.draw(new Line2D.Float(14, 2, 14, 8));
                break;
            case "Road trip":       // car
                p.moveTo(2, 15); p.lineTo(2, 11); p.lineTo(5, 6); p.lineTo(17, 6); p.lineTo(20, 11); p.lineTo(20, 15); p.closePath();
                g2.draw(p);
                g2.draw(new Line2D.Float(2, 11, 20, 11));
                g2.draw(new Ellipse2D.Float(4.5f, 14, 4, 4));
                g2.draw(new Ellipse2D.Float(13.5f, 14, 4, 4));
                break;
            case "Sightseeing":     // camera
                g2.draw(new RoundRectangle2D.Float(2, 6, 18, 13, 4, 4));
                g2.draw(new Ellipse2D.Float(7, 8.5f, 8, 8));
                g2.draw(new Line2D.Float(7, 6, 9, 3));
                g2.draw(new Line2D.Float(9, 3, 13, 3));
                g2.draw(new Line2D.Float(13, 3, 15, 6));
                break;
            case "Camping":         // tent
                p.moveTo(2, 19); p.lineTo(11, 3); p.lineTo(20, 19); p.closePath();
                g2.draw(p);
                Path2D door = new Path2D.Float();
                door.moveTo(8, 19); door.lineTo(11, 13); door.lineTo(14, 19);
                g2.draw(door);
                break;
            case "Adventure":       // compass
                g2.draw(new Ellipse2D.Float(2, 2, 18, 18));
                p.moveTo(14.5, 7.5); p.lineTo(12.5, 12.5); p.lineTo(7.5, 14.5); p.lineTo(9.5, 9.5); p.closePath();
                g2.draw(p);
                break;
            case "City tour":       // buildings
                g2.draw(new Rectangle2D.Float(3, 8, 7, 12));
                g2.draw(new Rectangle2D.Float(10, 3, 9, 17));
                g2.draw(new Line2D.Float(13, 7, 16, 7));
                g2.draw(new Line2D.Float(13, 11, 16, 11));
                g2.draw(new Line2D.Float(13, 15, 16, 15));
                g2.draw(new Line2D.Float(5.5f, 12, 7.5f, 12));
                break;
            default:                // star
                g2.draw(new Ellipse2D.Float(4, 4, 14, 14));
        }
        g2.dispose();
    }

    private static void wave(Graphics2D g2, float y) {
        Path2D w = new Path2D.Float();
        w.moveTo(2, y);
        w.curveTo(5, y - 2.5, 8, y + 2.5, 11, y);
        w.curveTo(14, y - 2.5, 17, y + 2.5, 20, y);
        g2.draw(w);
    }

    private static Color mix(Color a, Color b, float t) {
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
