package sabaylakbay_1;

import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.model.AdminStats;
import sabaylakbay_1.model.CreatorStat;
import sabaylakbay_1.model.Report;
import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Page;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;

/**
 * ADMIN DASHBOARD OVERVIEW
 * Opens from: admin sidebar "Dashboard" (first admin page)
 * Shows: total users, total groups, active trips, completed trips, pending reports,
 *        most visited places (graph), top performing creators, latest reports
 * Backend: getAdminStats, getMostVisitedPlaces, getTopCreators, getReports
 */
public class AdminOverviewForm extends Page {

    private final AdminDashboard admin;
    private final JPanel box = new Theme.ScrollableBox(null);

    public AdminOverviewForm(AdminDashboard admin) {
        this.admin = admin;
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(Theme.pad(0, 32, 32, 32));
        RoundButton analytics = new RoundButton("View analytics", RoundButton.Style.OUTLINE).icon("chart");
        analytics.addActionListener(e -> admin.showPage("ANALYTICS"));
        add(header("Dashboard Overview", "How SabayLakbay is doing today, " + Theme.date(LocalDate.now()) + ".", analytics),
                BorderLayout.NORTH);
        add(Theme.scroll(box));
    }

    @Override
    public void refresh() {
        box.removeAll();
        AdminStats s = Backend.getAdminStats();                       // BACKEND
        DecimalFormat n = new DecimalFormat("#,###");

        // ----- stat cards -----
        JPanel stats = Theme.clear(new GridLayout(1, 5, 14, 0));
        stats.add(stat("users", Theme.GREEN, n.format(s.getTotalUsers()), "Travelers", null));
        stats.add(stat("map", Theme.BLUE, n.format(s.getTotalGroups()), "Groups", null));
        stats.add(stat("suitcase", new Color(0x0F7287), n.format(s.getActiveTrips()), "Active trips", null));
        stats.add(stat("check", new Color(0x4F7A28), n.format(s.getCompletedTrips()), "Completed trips", null));
        stats.add(stat("flag", Theme.ORANGE, n.format(s.getPendingReports()), "Pending reports", () -> admin.openReports(0)));
        addFull(stats);
        box.add(Theme.gap(18));

        // ----- most visited places + top creators -----
        JPanel row = Theme.clear(new GridLayout(1, 2, 16, 0));
        BarChart places = new BarChart(Theme.GREEN, "travelers");
        places.setData(Backend.getMostVisitedPlaces(8));              // BACKEND
        row.add(card("Most visited places", "pin", places));
        // BACKEND: Backend.getTopCreators() reads from MySQL
        row.add(card("Top performing creators", "trend", creatorsTable(Backend.getTopCreators(5))));
        addFull(row);
        box.add(Theme.gap(18));

        // ----- latest reports -----
        JPanel reports = Theme.vbox();
        List<Report> latest = Backend.getReports(null);              // BACKEND
        for (int i = 0; i < Math.min(4, latest.size()); i++) {
            reports.add(AdminReportsForm.reportRow(admin, latest.get(i)));
            reports.add(Theme.gap(8));
        }
        JLabel all = Theme.link("See all reports", () -> admin.openReports(4));
        all.setIcon(Icons.get("arrowRight", 14, Theme.LINK));
        all.setHorizontalTextPosition(SwingConstants.LEFT);
        addFull(card("Latest reports", "flag", reports, all));

        box.revalidate();
        box.repaint();
    }

    private void addFull(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        box.add(c);
    }

    /** One number card. If onClick is not null the card is clickable. */
    private static JComponent stat(String icon, Color color, String value, String label, Runnable onClick) {
        RoundPanel p = RoundPanel.card(new BorderLayout(14, 0));
        p.setBorder(Theme.pad(18, 18, 18, 18));
        p.add(new CircleIcon(icon, color), BorderLayout.WEST);
        JPanel t = Theme.vbox();
        t.add(Theme.left(Theme.label(value, Font.BOLD, 26, Theme.TEXT_DARK)));
        t.add(Theme.left(Theme.label(label, Font.PLAIN, 12, Theme.TEXT_GRAY)));
        p.add(Theme.middleLeft(t));
        if (onClick != null) {
            p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            p.setToolTipText("Open reports");
            p.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { onClick.run(); }
                @Override public void mouseEntered(MouseEvent e) { p.setStroke(new Color(0xF3C3A5)); }
                @Override public void mouseExited(MouseEvent e) { p.setStroke(Theme.BORDER); }
            });
        }
        return p;
    }

    /** White card with an icon title, the content, and an optional link on the right. */
    static RoundPanel card(String title, String icon, JComponent body, JComponent... right) {
        RoundPanel p = RoundPanel.card(new BorderLayout(0, 14));
        p.setBorder(Theme.pad(18, 20, 20, 20));
        JPanel h = Theme.clear(new BorderLayout());
        JLabel t = Theme.sectionTitle(title);
        t.setIcon(Icons.get(icon, 18, Theme.GREEN));
        t.setIconTextGap(8);
        h.add(t, BorderLayout.WEST);
        if (right.length > 0) h.add(right[0], BorderLayout.EAST);
        p.add(h, BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    private JComponent creatorsTable(List<CreatorStat> list) {
        JPanel t = Theme.vbox();
        t.add(tableRow(true, "#", "Creator", "Groups", "Completed", "Hosted", "Rating", null, 0));
        int rank = 1;
        for (CreatorStat c : list) {
            t.add(Theme.separator());
            t.add(tableRow(false, String.valueOf(rank++), c.getUser().getFullName(), String.valueOf(c.getGroupsCreated()),
                    String.valueOf(c.getCompletedTrips()), String.valueOf(c.getTravelersHosted()),
                    c.getRating() > 0 ? String.format("\u2605 %.1f", c.getRating()) : "-", new Avatar(c.getUser(), 28), c.getUser().getId()));
        }
        return t;
    }

    private JComponent tableRow(boolean head, String rank, String name, String groups, String done, String hosted,
                                String rating, Avatar avatar, int userId) {
        JPanel r = Theme.clear(new GridBagLayout());
        r.setBorder(Theme.pad(head ? 0 : 8, 0, head ? 6 : 8, 4));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        Font f = Theme.font(head ? Font.BOLD : Font.PLAIN, head ? 11 : 13);
        Color col = head ? Theme.TEXT_GRAY : Theme.TEXT_DARK;
        c.gridx = 0;
        JLabel rk = cell(rank, 28, f, head ? col : Theme.TEXT_GRAY);
        rk.setHorizontalAlignment(SwingConstants.LEFT);
        r.add(rk, c);
        JPanel who = Theme.row(10);
        if (avatar != null) who.add(avatar);
        who.add(Theme.label(name, head ? Font.BOLD : Font.BOLD, head ? 11 : 13, col));
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        r.add(who, c);
        c.weightx = 0; c.fill = GridBagConstraints.NONE;
        String[] vals = {groups, done, hosted, rating};
        for (int i = 0; i < vals.length; i++) {
            c.gridx = 2 + i;
            r.add(cell(vals[i], 66, f, i == 3 && !head ? new Color(0x9A6B00) : col), c);
        }
        r.setAlignmentX(LEFT_ALIGNMENT);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, r.getPreferredSize().height));
        if (!head) {
            r.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            r.setToolTipText("View this creator's account");
            r.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { admin.openUser(userId); }
            });
        }
        return r;
    }

    private static JLabel cell(String text, int width, Font f, Color c) {
        JLabel l = new JLabel(text, SwingConstants.RIGHT);
        l.setFont(f);
        l.setForeground(c);
        l.setPreferredSize(new Dimension(width, 20));
        return l;
    }

    /** Colored circle with an icon inside. */
    static class CircleIcon extends JComponent {
        private final String icon;
        private final Color color;
        CircleIcon(String icon, Color color) {
            this.icon = icon;
            this.color = color;
            setPreferredSize(new Dimension(46, 46));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            int y = (getHeight() - 46) / 2;
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 28));
            g2.fill(new Ellipse2D.Float(0, y, 46, 46));
            Icons.get(icon, 22, color).paintIcon(this, g2, 12, y + 12);
            g2.dispose();
        }
    }
}
