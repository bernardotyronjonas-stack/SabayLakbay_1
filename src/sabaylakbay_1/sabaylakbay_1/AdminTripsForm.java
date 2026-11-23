package sabaylakbay_1;

import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.model.Report;
import sabaylakbay_1.model.TravelGroup;
import sabaylakbay_1.model.User;
import sabaylakbay_1.ui.Dialogs;
import sabaylakbay_1.ui.Page;
import sabaylakbay_1.ui.SubPage;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.*;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * ADMIN TRIPS
 * Opens from: admin sidebar "Trips"
 * Tabs: Upcoming, Happening now, Completed, Cancelled, All. Search by trip, destination or organizer.
 * Each row: cover, trip name, destination, organizer, dates, members, reports, status
 * Buttons: View (trip details, members, reports about the trip), Cancel trip (with a reason)
 * Backend: getAllTrips, countReportsAgainstTrip, getReportsAgainstTrip, getMembers, adminCancelTrip
 */
public class AdminTripsForm extends Page {

    private static final String[] TABS = {"Upcoming", "Happening now", "Completed", "Cancelled", "All"};

    private final AdminDashboard admin;
    private final TabBar tabs = new TabBar(TABS);
    private final RoundTextField search = new RoundTextField("Search trip, destination or organizer").withIcon("search");
    private final JLabel count = Theme.label("", Font.PLAIN, 12, Theme.TEXT_GRAY);
    private final JPanel list = new Theme.ScrollableBox(null);

    public AdminTripsForm(AdminDashboard admin) {
        this.admin = admin;
        JPanel top = Theme.vbox();
        top.add(header("Trips", "Every trip on SabayLakbay. Check trips that were reported and cancel unsafe or fake ones.", null));
        JPanel tw = Theme.clear(new BorderLayout());
        tw.setBorder(Theme.pad(0, 32, 0, 32));
        tw.add(tabs);
        top.add(tw);

        search.setPreferredSize(new Dimension(360, 40));
        search.addActionListener(e -> refresh());
        RoundButton go = new RoundButton("Search").icon("search");
        go.addActionListener(e -> refresh());
        JPanel bar = Theme.clear(new BorderLayout());
        bar.setBorder(Theme.pad(16, 32, 0, 32));
        bar.add(Theme.row(10, search, go), BorderLayout.WEST);
        bar.add(count, BorderLayout.EAST);
        top.add(bar);
        add(top, BorderLayout.NORTH);

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(Theme.pad(16, 32, 32, 32));
        add(Theme.scroll(list));
        tabs.onChange(i -> refresh());
    }

    /** Shows all trips (past and upcoming) to one destination. */
    public void showDestination(String destination) {
        search.setText(destination);
        tabs.setSelected(4);          // All
    }

    @Override
    public void refresh() {
        // BACKEND: Backend.getAllTrips() reads from MySQL
        List<TravelGroup> trips = Backend.getAllTrips(search.getText().trim(), tabs.getSelected());
        count.setText(trips.size() + (trips.size() == 1 ? " trip" : " trips"));
        list.removeAll();
        if (trips.isEmpty()) list.add(empty("map", "No trips here."));
        for (TravelGroup g : trips) {
            JComponent row = row(g);
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            list.add(row);
            list.add(Theme.gap(10));
        }
        list.revalidate();
        list.repaint();
    }

    private JComponent row(TravelGroup g) {
        RoundPanel p = RoundPanel.card(new BorderLayout(16, 0));
        p.setBorder(Theme.pad(12, 14, 12, 16));
        ImageBox img = new ImageBox(g.getImagePath(), g.getTitle(), 10, true);
        img.setPreferredSize(new Dimension(96, 64));
        p.add(img, BorderLayout.WEST);

        JPanel t = Theme.vbox();
        // BACKEND: Backend.countReportsAgainstTrip() reads from MySQL
        int reports = Backend.countReportsAgainstTrip(g.getId());
        JPanel title = Theme.row(10, Theme.label(g.getTitle(), Font.BOLD, 14, Theme.TEXT_DARK), statusChip(g));
        if (reports > 0) title.add(new Chip(reports + " report" + (reports > 1 ? "s" : ""), Theme.RED_LIGHT, Theme.RED));
        t.add(Theme.left(title));
        t.add(Theme.gap(4));
        t.add(Theme.left(Theme.label(g.getDestination() + "  \u2022  " + Theme.dateRange(g.getStartDate(), g.getEndDate())
                + "  \u2022  " + g.getCurrentMembers() + " of " + g.getMaxMembers() + " travelers", Font.PLAIN, 12, Theme.TEXT_GRAY)));
        t.add(Theme.left(Theme.label("Organizer: " + g.getCreatorName() + "  \u2022  " + Theme.peso(g.getBudgetPerPerson()) + " per person",
                Font.PLAIN, 12, Theme.TEXT_GRAY)));
        p.add(Theme.middleLeft(t), BorderLayout.CENTER);

        RoundButton view = new RoundButton("View", RoundButton.Style.OUTLINE).small();
        view.addActionListener(e -> { new AdminTripDetail(g.getId()).setVisible(true); admin.refreshCurrent(); });
        JPanel actions = Theme.row(6, view);
        if (!g.isCancelled() && !g.isCompleted()) {
            RoundButton cancel = new RoundButton("Cancel trip", RoundButton.Style.DANGER).small();
            cancel.addActionListener(e -> { if (cancel(this, g)) admin.refreshCurrent(); });
            actions.add(cancel);
        }
        JPanel r = Theme.clear(new GridBagLayout());
        r.add(actions);
        p.add(r, BorderLayout.EAST);
        return p;
    }

    static Chip statusChip(TravelGroup g) {
        LocalDate today = LocalDate.now();
        if (g.isCancelled()) return new Chip("Cancelled", Theme.RED_LIGHT, Theme.RED);
        if (g.isCompleted()) return new Chip("Completed", new Color(0xEEF0F2), Theme.TEXT_GRAY);
        if (!g.getStartDate().isAfter(today)) return new Chip("Happening now", Theme.BLUE_LIGHT, Theme.BLUE);
        if (g.isFull()) return new Chip("Full", Theme.YELLOW_LIGHT, new Color(0x9A6B00));
        return new Chip("Upcoming", Theme.GREEN_LIGHT, Theme.GREEN);
    }

    /** Asks for a reason, then cancels. Everyone in the trip is told why. */
    static boolean cancel(Component parent, TravelGroup g) {
        String reason = Dialogs.input(parent, "Cancel \"" + g.getTitle() + "\"?",
                "The organizer and every member will be notified with your reason. This can't be undone.",
                "e.g. Unsafe destination during a typhoon warning", "Cancel trip", true, true);
        if (reason == null) return false;
        // BACKEND: Backend.adminCancelTrip() saves to MySQL
        return Theme.run(parent, () -> Backend.adminCancelTrip(g.getId(), reason));
    }

    /** Trip details for admins: info, members and reports about the trip. */
    private final class AdminTripDetail extends SubPage {
        AdminTripDetail(int groupId) {
            super(null, "Trip details", null);
            TravelGroup g = Backend.getGroup(groupId);                               // BACKEND
            JPanel box = new Theme.ScrollableBox(null);
            box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
            box.setBorder(Theme.pad(24, 28, 24, 28));

            JPanel head = Theme.clear(new BorderLayout(18, 0));
            ImageBox img = new ImageBox(g.getImagePath(), g.getTitle(), 12, true);
            img.setPreferredSize(new Dimension(180, 120));
            head.add(img, BorderLayout.WEST);
            JPanel t = Theme.vbox();
            t.add(Theme.left(Theme.row(10, Theme.label(g.getTitle(), Font.BOLD, 20, Theme.TEXT_DARK), statusChip(g))));
            t.add(Theme.gap(6));
            t.add(Theme.left(Theme.label(g.getCategory() + "  \u2022  " + g.getDestination() + "  \u2022  "
                    + Theme.dateRange(g.getStartDate(), g.getEndDate()), Font.PLAIN, 13, Theme.TEXT_GRAY)));
            t.add(Theme.left(Theme.label(Theme.peso(g.getBudgetPerPerson()) + " per person  \u2022  "
                    + g.getCurrentMembers() + " of " + g.getMaxMembers() + " travelers"
                    + (g.getMeetingPlace() == null || g.getMeetingPlace().isBlank() ? "" : "  \u2022  Meet at " + g.getMeetingPlace()),
                    Font.PLAIN, 13, Theme.TEXT_GRAY)));
            if (g.getCreatedAt() != null)
                t.add(Theme.left(Theme.label("Posted " + Theme.ago(g.getCreatedAt()), Font.PLAIN, 12, Theme.TEXT_GRAY)));
            head.add(Theme.middleLeft(t), BorderLayout.CENTER);
            full(box, head);

            if (g.getDescription() != null && !g.getDescription().isBlank()) {
                box.add(Theme.gap(18));
                box.add(Theme.left(Theme.sectionTitle("About the trip")));
                box.add(Theme.gap(6));
                String safe = g.getDescription().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
                full(box, Theme.label("<html><body style='width:640px'>" + safe + "</body></html>", Font.PLAIN, 13, new Color(0x344054)));
            }

            // members
            List<User> members = Backend.getMembers(groupId);                         // BACKEND
            box.add(Theme.gap(18));
            box.add(Theme.left(Theme.sectionTitle("Travelers (" + members.size() + ")")));
            box.add(Theme.gap(8));
            for (User u : members) {
                JPanel m = Theme.clear(new BorderLayout(12, 0));
                m.setBorder(Theme.pad(6, 0, 6, 0));
                m.add(new Avatar(u, 34), BorderLayout.WEST);
                JPanel mt = Theme.vbox();
                mt.add(Theme.left(Theme.label(u.getFullName(), Font.BOLD, 13, Theme.TEXT_DARK)));
                mt.add(Theme.left(Theme.label(u.getId() == g.getCreatorId() ? "Organizer" : "Member", Font.PLAIN, 12, Theme.TEXT_GRAY)));
                m.add(Theme.middleLeft(mt), BorderLayout.CENTER);
                RoundButton acc = new RoundButton("Account", RoundButton.Style.GHOST).small();
                acc.addActionListener(e -> admin.openUser(u.getId()));
                JPanel ar = Theme.clear(new GridBagLayout());
                ar.add(acc);
                m.add(ar, BorderLayout.EAST);
                full(box, m);
            }

            // reports about this trip
            List<Report> reports = Backend.getReportsAgainstTrip(groupId);           // BACKEND
            box.add(Theme.gap(18));
            box.add(Theme.left(Theme.sectionTitle("Reports about this trip (" + reports.size() + ")")));
            box.add(Theme.gap(8));
            if (reports.isEmpty()) box.add(Theme.left(Theme.label("No reports.", Font.PLAIN, 13, Theme.TEXT_GRAY)));
            for (Report r : reports) {
                full(box, AdminReportsForm.reportRow(admin, r));
                box.add(Theme.gap(8));
            }

            JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            bar.setBackground(Color.WHITE);
            bar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER), Theme.pad(14, 28, 14, 28)));
            if (!g.isCancelled() && !g.isCompleted()) {
                RoundButton cancel = new RoundButton("Cancel trip", RoundButton.Style.DANGER).icon("x");
                cancel.addActionListener(e -> { if (AdminTripsForm.cancel(this, g)) dispose(); });
                bar.add(cancel);
            }
            RoundButton close = new RoundButton("Close");
            close.addActionListener(e -> dispose());
            bar.add(close);

            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(Color.WHITE);
            root.add(Theme.scroll(box), BorderLayout.CENTER);
            root.add(bar, BorderLayout.SOUTH);
            setContentPane(root);
        }
    }

    private static void full(JPanel box, JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        box.add(c);
    }
}
