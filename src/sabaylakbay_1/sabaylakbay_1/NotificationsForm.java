package sabaylakbay_1;

import sabaylakbay_1.model.Notification;
import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Page;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.RoundButton;
import sabaylakbay_1.ui.components.RoundPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.List;

/**
 * NOTIFICATIONS FORM
 * Opens from: sidebar "Notifications"
 * Shows: request approved / rejected, new join requests, trip updates. Click one to open it.
 * Backend: getNotifications, markAllRead
 */
public class NotificationsForm extends Page {

    private final JPanel list = new Theme.ScrollableBox(null);
    private final RoundButton markAll = new RoundButton("Mark all as read", RoundButton.Style.OUTLINE).icon("check").small();

    public NotificationsForm(MainDashboard app) {
        super(app);
        markAll.addActionListener(e -> {
            // BACKEND: Backend.markAllRead() saves to MySQL
            if (Theme.run(this, () -> Backend.markAllRead(Backend.currentUserId()))) { refresh(); app.refreshBadges(); }
        });
        add(header("Notifications", "Updates about your requests and trips.", markAll), BorderLayout.NORTH);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(Theme.pad(0, 32, 32, 32));
        add(Theme.scroll(list));
    }

    @Override
    public void refresh() {
        list.removeAll();
        try {
            // BACKEND: Backend.getNotifications() reads from MySQL
            List<Notification> items = Backend.getNotifications(Backend.currentUserId());
            if (items.isEmpty()) list.add(empty("bell", "No notifications yet. Updates about your trips will show here."));
            markAll.setVisible(items.stream().anyMatch(n -> !n.isRead()));   // only when there's something to mark
            for (Notification n : items) {
                JComponent row = row(n);
                row.setAlignmentX(LEFT_ALIGNMENT);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
                list.add(row);
                list.add(Theme.gap(10));
            }
        } catch (RuntimeException ex) {
            Theme.error(this, ex.getMessage());
        }
        list.revalidate();
        list.repaint();
    }

    private JComponent row(Notification n) {
        String text = n.getMessage().toLowerCase();
        String icon; Color color; Color bg;
        if (text.contains("approved!")) { icon = "check"; color = Theme.GREEN; bg = Theme.GREEN_LIGHT; }
        else if (text.contains("not approved") || text.contains("cancelled")) { icon = "x"; color = Theme.RED; bg = Theme.RED_LIGHT; }
        else if (text.contains("wants to join")) { icon = "users"; color = Theme.BLUE; bg = Theme.BLUE_LIGHT; }
        else if (text.contains("update") || text.contains("activity")) { icon = "megaphone"; color = new Color(0x9A6B00); bg = Theme.YELLOW_LIGHT; }
        else { icon = "bell"; color = Theme.TEXT_GRAY; bg = new Color(0xF0F2F4); }

        RoundPanel p = new RoundPanel(new BorderLayout(14, 0), 14, n.isRead() ? Color.WHITE : new Color(0xF4FAF7),
                n.isRead() ? Theme.BORDER : new Color(0xBFD8CC));
        p.setBorder(Theme.pad(14, 16, 14, 18));
        p.add(new CircleIcon(icon, color, bg), BorderLayout.WEST);
        JPanel t = Theme.vbox();
        t.add(Theme.left(Theme.wrapLabel(n.getMessage(), 640, n.isRead() ? Font.PLAIN : Font.BOLD, 13, Theme.TEXT_DARK)));
        t.add(Theme.gap(3));
        t.add(Theme.left(Theme.label(Theme.ago(n.getCreatedAt()), Font.PLAIN, 11, Theme.TEXT_GRAY)));
        JPanel tc = Theme.clear(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        tc.add(t, c);
        p.add(tc);
        p.add(new JLabel(Icons.get("chevronRight", 16, Theme.HINT)), BorderLayout.EAST);
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (text.contains("wants to join")) app.openMyGroups(0);
                else if (n.getRelatedGroupId() > 0) app.openGroup(n.getRelatedGroupId());
            }
        });
        return p;
    }

    private static class CircleIcon extends JComponent {
        private final String icon; private final Color color, bg;
        CircleIcon(String icon, Color color, Color bg) {
            this.icon = icon; this.color = color; this.bg = bg;
            setPreferredSize(new Dimension(40, 40));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            int y = (getHeight() - 40) / 2;
            g2.setColor(bg);
            g2.fill(new Ellipse2D.Float(0, y, 40, 40));
            Icons.get(icon, 18, color).paintIcon(this, g2, 11, y + 11);
            g2.dispose();
        }
    }
}
