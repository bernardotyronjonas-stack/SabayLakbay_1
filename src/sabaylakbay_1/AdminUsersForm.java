package sabaylakbay_1;

import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.model.AccountStatus;
import sabaylakbay_1.model.User;
import sabaylakbay_1.ui.Page;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * ADMIN USER MANAGEMENT
 * Opens from: admin sidebar "User Management"
 * Shows: all registered travelers with search (name or email) and a status filter
 * Each row: name and email, date joined, groups, reports against them, account status
 * Buttons: View (AdminUserDetailForm), Suspend / Reactivate, Remove
 * Backend: getAllUsers, countGroupsOf, countReportsAgainstUser, suspendUser, reactivateUser, removeUser
 */
public class AdminUsersForm extends Page {

    private static final String[] FILTERS = {"All statuses", "Active", "Suspended", "Removed"};
    private static final int[] COL = {0, 120, 80, 90, 110, 250};   // widths: name (fills), joined, groups, reports, status, actions

    private final AdminDashboard admin;
    private final RoundTextField search = new RoundTextField("Search by name or email").withIcon("search");
    private final JComboBox<String> filter = new JComboBox<>(FILTERS);
    private final JLabel count = Theme.label("", Font.PLAIN, 12, Theme.TEXT_GRAY);
    private final JPanel rows = Theme.vbox();

    public AdminUsersForm(AdminDashboard admin) {
        this.admin = admin;
        add(header("User Management", "All registered travelers. Search, view, suspend or remove accounts.", null),
                BorderLayout.NORTH);

        JPanel box = new Theme.ScrollableBox(null);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(Theme.pad(0, 32, 32, 32));

        // ----- search bar -----
        search.setPreferredSize(new Dimension(360, 40));
        search.addActionListener(e -> refresh());
        filter.setFont(Theme.font(Font.PLAIN, 13));
        filter.setPreferredSize(new Dimension(160, 40));
        filter.addActionListener(e -> refresh());
        RoundButton go = new RoundButton("Search").icon("search");
        go.addActionListener(e -> refresh());
        JPanel bar = Theme.clear(new BorderLayout());
        bar.add(Theme.row(10, search, filter, go), BorderLayout.WEST);
        bar.add(count, BorderLayout.EAST);
        bar.setAlignmentX(LEFT_ALIGNMENT);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        box.add(bar);
        box.add(Theme.gap(16));

        // ----- table -----
        RoundPanel table = RoundPanel.card(new BorderLayout());
        table.setBorder(Theme.pad(8, 18, 8, 18));
        JPanel inner = Theme.vbox();
        inner.add(row(true, null));
        inner.add(rows);
        table.add(inner);
        table.setAlignmentX(LEFT_ALIGNMENT);
        box.add(table);
        add(Theme.scroll(box));
    }

    @Override
    public void refresh() {
        AccountStatus status = filter.getSelectedIndex() == 0 ? null : AccountStatus.values()[filter.getSelectedIndex() - 1];
        List<User> users = Backend.getAllUsers(search.getText().trim(), status);       // BACKEND
        count.setText(users.size() + (users.size() == 1 ? " traveler" : " travelers"));
        rows.removeAll();
        for (User u : users) {
            rows.add(Theme.separator());
            rows.add(row(false, u));
        }
        if (users.isEmpty()) rows.add(empty("users", "No travelers match your search."));
        rows.revalidate();
        rows.repaint();
    }

    private JComponent row(boolean head, User u) {
        JPanel r = Theme.clear(new GridBagLayout());
        r.setBorder(Theme.pad(10, 0, 10, 0));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        // name + email
        c.gridx = 0; c.weightx = 1;
        if (head) r.add(headCell("TRAVELER", 0), c);
        else {
            JPanel who = Theme.clear(new BorderLayout(12, 0));
            who.add(new Avatar(u, 36), BorderLayout.WEST);
            JPanel t = Theme.vbox();
            t.add(Theme.left(Theme.label(u.getFullName(), Font.BOLD, 13, Theme.TEXT_DARK)));
            t.add(Theme.left(Theme.label(u.getEmail(), Font.PLAIN, 12, Theme.TEXT_GRAY)));
            who.add(Theme.middleLeft(t));
            r.add(who, c);
        }
        c.weightx = 0;
        if (head) {
            String[] names = {"JOINED", "GROUPS", "REPORTS", "STATUS", "ACTIONS"};
            for (int i = 0; i < names.length; i++) { c.gridx = i + 1; r.add(headCell(names[i], COL[i + 1]), c); }
            return wrap(r);
        }
        c.gridx = 1; r.add(sized(Theme.label(Theme.date(u.getCreatedAt().toLocalDate()), Font.PLAIN, 12, Theme.TEXT_DARK), COL[1]), c);
        // BACKEND: Backend.countGroupsOf() (sample data for now)
        c.gridx = 2; r.add(sized(Theme.label(String.valueOf(Backend.countGroupsOf(u.getId())), Font.PLAIN, 13, Theme.TEXT_DARK), COL[2]), c);
        // BACKEND: Backend.countReportsAgainstUser() (sample data for now)
        int reports = Backend.countReportsAgainstUser(u.getId());
        c.gridx = 3; r.add(sized(reports > 0 ? new Chip(reports + " report" + (reports > 1 ? "s" : ""), Theme.RED_LIGHT, Theme.RED)
                : Theme.label("None", Font.PLAIN, 12, Theme.HINT), COL[3]), c);
        c.gridx = 4; r.add(sized(AdminDashboard.accountChip(u.getStatus()), COL[4]), c);

        RoundButton view = new RoundButton("View", RoundButton.Style.OUTLINE).small();
        view.addActionListener(e -> admin.openUser(u.getId()));
        JPanel actions = Theme.row(6, view);
        if (u.getStatus() == AccountStatus.ACTIVE) {
            RoundButton suspend = new RoundButton("Suspend", RoundButton.Style.LIGHT).small();
            suspend.addActionListener(e -> suspend(this, u, admin));
            actions.add(suspend);
        } else if (u.getStatus() == AccountStatus.SUSPENDED) {
            RoundButton reactivate = new RoundButton("Reactivate", RoundButton.Style.LIGHT).small();
            reactivate.addActionListener(e -> reactivate(this, u, admin));
            actions.add(reactivate);
        }
        if (u.getStatus() != AccountStatus.REMOVED) {
            RoundButton remove = new RoundButton("Remove", RoundButton.Style.DANGER).small();
            remove.addActionListener(e -> remove(this, u, admin));
            actions.add(remove);
        }
        c.gridx = 5; r.add(sized(actions, COL[5]), c);
        return wrap(r);
    }

    private static JComponent wrap(JPanel r) {
        r.setAlignmentX(LEFT_ALIGNMENT);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, r.getPreferredSize().height));
        return r;
    }

    private static JComponent headCell(String text, int width) {
        JLabel l = Theme.label(text, Font.BOLD, 11, Theme.TEXT_GRAY);
        return width == 0 ? l : sized(l, width);
    }

    private static JComponent sized(JComponent c, int width) {
        JPanel p = Theme.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.add(c);
        p.setPreferredSize(new Dimension(width, Math.max(24, c.getPreferredSize().height)));
        return p;
    }

    // ===== account actions (also used by AdminUserDetailForm) =====

    static void suspend(Component parent, User u, AdminDashboard admin) {
        String reason = JOptionPane.showInputDialog(parent, "Why are you suspending " + u.getFullName() + "?\n"
                + "They won't be able to log in until you reactivate the account.", "Suspend account", JOptionPane.PLAIN_MESSAGE);
        if (reason == null) return;
        if (reason.isBlank()) { Theme.error(parent, "Please give a reason."); return; }
        if (Theme.run(parent, () -> Backend.suspendUser(u.getId(), reason.trim()))) admin.refreshCurrent();   // BACKEND
    }

    static void reactivate(Component parent, User u, AdminDashboard admin) {
        if (!Theme.confirm(parent, "Reactivate " + u.getFullName() + "'s account?")) return;
        if (Theme.run(parent, () -> Backend.reactivateUser(u.getId()))) admin.refreshCurrent();              // BACKEND
    }

    static void remove(Component parent, User u, AdminDashboard admin) {
        String reason = JOptionPane.showInputDialog(parent, "Remove " + u.getFullName() + "'s account?\n"
                + "Only do this when it is justified under the platform rules. Which rule was broken?",
                "Remove account", JOptionPane.WARNING_MESSAGE);
        if (reason == null) return;
        if (reason.isBlank()) { Theme.error(parent, "Please write which rule was broken."); return; }
        if (Theme.run(parent, () -> Backend.removeUser(u.getId(), reason.trim()))) admin.refreshCurrent();   // BACKEND
    }
}
