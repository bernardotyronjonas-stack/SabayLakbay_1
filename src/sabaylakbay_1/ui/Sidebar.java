package sabaylakbay_1.ui;

import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.model.User;
import sabaylakbay_1.ui.components.Avatar;
import sabaylakbay_1.ui.components.Chip;
import sabaylakbay_1.ui.components.LogoIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The menu on the left of MainDashboard (green) and AdminDashboard (dark).
 * Items are given as {key, text, icon}. Clicking one calls onNavigate with its key.
 */
public class Sidebar extends JPanel {

    private final Map<String, NavItem> items = new LinkedHashMap<>();

    /**
     * @param bg          sidebar color
     * @param tag         small label under the logo (e.g. "ADMIN"), or null
     * @param menu        rows of {key, text, icon}
     * @param onNavigate  called with the key of the clicked item
     * @param userSubtitle text under the user's name at the bottom
     * @param userIcon    icon on the right of the user row ("chevronRight" or "logout")
     * @param onUserClick called when the user row is clicked
     */
    public Sidebar(Color bg, String tag, String[][] menu, Consumer<String> onNavigate,
                   String userSubtitle, String userIcon, Runnable onUserClick) {
        super(new BorderLayout());
        setBackground(bg);
        setPreferredSize(new Dimension(236, 0));

        // logo
        JPanel top = Theme.vbox();
        JPanel logo = Theme.clear(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logo.add(new LogoIcon(Color.WHITE, 28, 25));
        logo.add(Theme.label("SabayLakbay", Font.BOLD, 19, Color.WHITE));
        logo.setBorder(Theme.pad(30, 10, tag == null ? 22 : 8, 0));
        top.add(Theme.left(logo));
        if (tag != null) {
            JPanel t = Theme.row(0, new Chip(tag, new Color(255, 255, 255, 40), Color.WHITE));
            t.setBorder(Theme.pad(0, 20, 16, 0));
            top.add(Theme.left(t));
        }
        top.add(divider());
        top.add(Theme.gap(20));

        for (String[] m : menu) {
            NavItem n = new NavItem(m[1], m[2]);
            n.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { onNavigate.accept(m[0]); }
            });
            items.put(m[0], n);
            JPanel wrap = Theme.clear(new BorderLayout());
            wrap.setBorder(Theme.pad(0, 16, 6, 16));
            wrap.add(n);
            wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            wrap.setAlignmentX(LEFT_ALIGNMENT);
            top.add(wrap);
        }
        add(top, BorderLayout.NORTH);

        // logged-in user at the bottom
        JPanel bottom = Theme.vbox();
        bottom.add(divider());
        JPanel userRow = Theme.clear(new BorderLayout(10, 0));
        userRow.setBorder(Theme.pad(16, 18, 18, 14));
        userRow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        userRow.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onUserClick.run(); }
        });
        User me = Backend.getCurrentUser();
        userRow.add(new Avatar(me, 38), BorderLayout.WEST);
        JPanel t = Theme.vbox();
        t.add(Theme.left(Theme.label(me.getFullName(), Font.BOLD, 13, Color.WHITE)));
        t.add(Theme.left(Theme.label(userSubtitle, Font.PLAIN, 11, new Color(255, 255, 255, 170))));
        userRow.add(Theme.middleLeft(t), BorderLayout.CENTER);
        JLabel icon = new JLabel(Icons.get(userIcon, 17, Color.WHITE));
        icon.setToolTipText("logout".equals(userIcon) ? "Log out" : "Profile");
        userRow.add(icon, BorderLayout.EAST);
        userRow.setAlignmentX(LEFT_ALIGNMENT);
        bottom.add(userRow);
        add(bottom, BorderLayout.SOUTH);
    }

    private JComponent divider() {
        JPanel d = new JPanel();
        d.setBackground(new Color(255, 255, 255, 40));
        d.setPreferredSize(new Dimension(1, 1));
        d.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        JPanel wrap = Theme.clear(new BorderLayout());
        wrap.setBorder(Theme.pad(0, 16, 0, 16));
        wrap.add(d);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        wrap.setAlignmentX(LEFT_ALIGNMENT);
        return wrap;
    }

    public void setSelected(String key) {
        items.forEach((k, n) -> n.setSelected(k.equals(key)));
    }

    public void setBadge(String key, int count) {
        NavItem n = items.get(key);
        if (n != null) n.setBadge(count);
    }

    /** One menu item. The highlight fades in and out smoothly. */
    private static class NavItem extends JComponent {
        private final String text;
        private final String icon;
        private boolean selected, hover;
        private float glow;          // 0 = no highlight, 1 = full highlight
        private int badge;
        private final Timer anim;

        NavItem(String text, String icon) {
            this.text = text;
            this.icon = icon;
            setPreferredSize(new Dimension(190, 44));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            anim = new Timer(15, e -> {
                float target = selected ? 1f : hover ? 0.5f : 0f;
                glow += (target - glow) * 0.25f;
                if (Math.abs(target - glow) < 0.01f) { glow = target; ((Timer) e.getSource()).stop(); }
                repaint();
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; anim.start(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; anim.start(); }
            });
        }

        void setSelected(boolean s) { selected = s; anim.start(); }
        void setBadge(int b) { badge = b; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            if (glow > 0.01f) {
                g2.setColor(new Color(255, 255, 255, Math.round(38 * glow)));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
            }
            if (selected) {
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 12, 3, getHeight() - 24, 3, 3));
            }
            Color c = selected ? Color.WHITE : new Color(255, 255, 255, 215);
            Icons.get(icon, 19, c).paintIcon(this, g2, 14, (getHeight() - 19) / 2);
            g2.setColor(c);
            g2.setFont(Theme.font(selected ? Font.BOLD : Font.PLAIN, 13));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, 44, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            if (badge > 0) {
                String s = badge > 9 ? "9+" : String.valueOf(badge);
                g2.setFont(Theme.font(Font.BOLD, 11));
                fm = g2.getFontMetrics();
                int bw = Math.max(20, fm.stringWidth(s) + 10), bh = 20;
                int bx = getWidth() - bw - 10, by = (getHeight() - bh) / 2;
                g2.setColor(Theme.ORANGE);
                g2.fill(new RoundRectangle2D.Float(bx, by, bw, bh, bh, bh));
                g2.setColor(Color.WHITE);
                g2.drawString(s, bx + (bw - fm.stringWidth(s)) / 2, by + (bh - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }
}
