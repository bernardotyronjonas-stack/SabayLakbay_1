package sabaylakbay_1;

import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.model.AdminLog;
import sabaylakbay_1.ui.Dialogs;
import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Page;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ADMIN ACTIVITY LOG
 * Opens from: admin sidebar "Activity Log"
 * Shows: every admin action (ID checks, suspensions, removals, cancelled trips, announcements, report decisions...)
 * with who did it and when. Search, filter by action, export to CSV.
 * Backend: getAdminLogs, exportAdminLogs
 */
public class AdminLogForm extends Page {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a");

    private final RoundTextField search = new RoundTextField("Search name, trip or details").withIcon("search");
    private final JComboBox<String> action = new JComboBox<>();
    private final JLabel count = Theme.label("", Font.PLAIN, 12, Theme.TEXT_GRAY);
    private final JPanel rows = Theme.vbox();

    public AdminLogForm(AdminDashboard admin) {
        RoundButton export = new RoundButton("Export CSV", RoundButton.Style.OUTLINE).icon("download");
        export.addActionListener(e -> export());
        add(header("Activity Log", "A record of what admins did and when. Entries can't be edited or deleted.", export),
                BorderLayout.NORTH);

        JPanel box = new Theme.ScrollableBox(null);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(Theme.pad(0, 32, 32, 32));

        action.addItem("All actions");
        for (String a : Backend.LOG_ACTIONS) action.addItem(a);
        action.setFont(Theme.font(Font.PLAIN, 13));
        action.setPreferredSize(new Dimension(200, 40));
        action.addActionListener(e -> refresh());
        search.setPreferredSize(new Dimension(340, 40));
        search.addActionListener(e -> refresh());
        RoundButton go = new RoundButton("Search").icon("search");
        go.addActionListener(e -> refresh());
        JPanel bar = Theme.clear(new BorderLayout());
        bar.add(Theme.row(10, search, action, go), BorderLayout.WEST);
        bar.add(count, BorderLayout.EAST);
        bar.setAlignmentX(LEFT_ALIGNMENT);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        box.add(bar);
        box.add(Theme.gap(16));

        RoundPanel table = RoundPanel.card(new BorderLayout());
        table.setBorder(Theme.pad(6, 18, 6, 18));
        table.add(rows);
        table.setAlignmentX(LEFT_ALIGNMENT);
        box.add(table);
        add(Theme.scroll(box));
    }

    private String selectedAction() {
        return action.getSelectedIndex() <= 0 ? null : (String) action.getSelectedItem();
    }

    @Override
    public void refresh() {
        // BACKEND: Backend.getAdminLogs() reads from MySQL
        List<AdminLog> logs = Backend.getAdminLogs(selectedAction(), search.getText().trim());
        count.setText(logs.size() + (logs.size() == 1 ? " entry" : " entries"));
        rows.removeAll();
        if (logs.isEmpty()) rows.add(empty("note", "Nothing recorded yet. Admin actions show up here."));
        boolean first = true;
        for (AdminLog l : logs) {
            if (!first) rows.add(Theme.separator());
            first = false;
            rows.add(row(l));
        }
        rows.revalidate();
        rows.repaint();
    }

    private JComponent row(AdminLog l) {
        JPanel r = Theme.clear(new BorderLayout(14, 0));
        r.setBorder(Theme.pad(12, 0, 12, 0));
        String[] look = look(l.getAction());
        JLabel icon = new JLabel(Icons.get(look[0], 16, Color.decode(look[1]))) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.aa(g2);
                g2.setColor(Color.decode(look[2]));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        icon.setPreferredSize(new Dimension(34, 34));
        JPanel iw = Theme.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
        iw.add(icon);
        r.add(iw, BorderLayout.WEST);

        JPanel t = Theme.vbox();
        String safe = esc(l.getTarget() == null ? "" : l.getTarget());
        t.add(Theme.left(Theme.label("<html><b>" + esc(l.getAction()) + "</b>" + (safe.isEmpty() ? "" : "  <font color='#475467'>" + safe + "</font>")
                + "</html>", Font.PLAIN, 13, Theme.TEXT_DARK)));
        if (l.getDetails() != null && !l.getDetails().isBlank())
            t.add(Theme.left(Theme.label("<html><body style='width:560px'>" + esc(l.getDetails()) + "</body></html>",
                    Font.PLAIN, 12, Theme.TEXT_GRAY)));
        r.add(t, BorderLayout.CENTER);

        JPanel when = Theme.vbox();
        when.add(right(Theme.label(l.getCreatedAt() == null ? "" : WHEN.format(l.getCreatedAt()), Font.PLAIN, 12, Theme.TEXT_DARK)));
        when.add(right(Theme.label("by " + l.getAdminName(), Font.PLAIN, 12, Theme.TEXT_GRAY)));
        r.add(when, BorderLayout.EAST);
        r.setAlignmentX(LEFT_ALIGNMENT);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, r.getPreferredSize().height));
        return r;
    }

    /** icon, icon color, circle color for each kind of action. */
    private static String[] look(String a) {
        if (Backend.LOG_APPROVE_ID.equals(a) || Backend.LOG_REACTIVATE.equals(a)) return new String[]{"check", "#0B4F3A", "#E7F4EE"};
        if (Backend.LOG_REJECT_ID.equals(a)) return new String[]{"x", "#D93C3C", "#FDECEC"};
        if (Backend.LOG_SUSPEND.equals(a)) return new String[]{"block", "#9A6B00", "#FFF6DD"};
        if (Backend.LOG_REMOVE.equals(a) || Backend.LOG_ERASE.equals(a)) return new String[]{"trash", "#D93C3C", "#FDECEC"};
        if (Backend.LOG_CANCEL_TRIP.equals(a)) return new String[]{"map", "#D93C3C", "#FDECEC"};
        if (Backend.LOG_ANNOUNCE.equals(a)) return new String[]{"megaphone", "#2B5FA8", "#E8F0FB"};
        if (Backend.LOG_BIRTHDAY.equals(a)) return new String[]{"calendar", "#2B5FA8", "#E8F0FB"};
        return new String[]{"flag", "#B4520E", "#FDEFE6"};
    }

    private static JPanel right(JComponent c) {
        JPanel p = Theme.clear(new BorderLayout());
        p.add(c, BorderLayout.EAST);
        return p;
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void export() {
        File f = Dialogs.chooseSave(this, "Save activity log", "SabayLakbay-activity-log.csv");
        if (f == null) return;
        // BACKEND: Backend.exportAdminLogs() reads from MySQL
        Theme.run(this, () -> Backend.exportAdminLogs(selectedAction(), search.getText().trim(), f));
    }
}
