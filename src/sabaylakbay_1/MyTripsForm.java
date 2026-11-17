package sabaylakbay_1;

import sabaylakbay_1.model.*;
import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Page;
import sabaylakbay_1.ui.Dialogs;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * MY TRIPS FORM (Trip Planning)
 * Opens from: sidebar "My Trips"
 * Left: Upcoming / Completed trips. Right: details of the selected trip
 * Shows: meeting place and time, estimated expenses, itinerary (Add activity for the creator),
 *        trip updates (announcements), and for completed trips: Rate companions / Write a review
 * Backend: getItinerary, addActivity, deleteActivity, getExpenses, addExpense, deleteExpense, getAnnouncements
 */
public class MyTripsForm extends Page {

    private final TabBar tabs = new TabBar("Upcoming", "Completed");
    private final JPanel tripList = Theme.vbox();
    private final JPanel detail = new Theme.ScrollableBox(null);
    private int selectedId;

    public MyTripsForm(MainDashboard app) {
        super(app);
        add(header("My Trips", "Plan your trips: itinerary, meeting place, expenses and updates.", null), BorderLayout.NORTH);

        JPanel left = Theme.clear(new BorderLayout());
        left.setPreferredSize(new Dimension(320, 0));
        left.setBorder(Theme.pad(0, 32, 24, 0));
        left.add(tabs, BorderLayout.NORTH);
        tripList.setBorder(Theme.pad(14, 0, 0, 4));
        JPanel listWrap = Theme.clear(new BorderLayout());
        listWrap.add(tripList, BorderLayout.NORTH);
        left.add(Theme.scroll(listWrap), BorderLayout.CENTER);

        detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
        detail.setBorder(Theme.pad(0, 24, 32, 32));

        JPanel body = Theme.clear(new BorderLayout());
        body.add(left, BorderLayout.WEST);
        body.add(Theme.scroll(detail), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
        tabs.onChange(i -> { selectedId = 0; refresh(); });
    }

    /** Open a specific trip (called from other pages). */
    public void select(int groupId) {
        selectedId = groupId;
        try {
            // BACKEND: Backend.getGroup() reads from MySQL
            TravelGroup g = Backend.getGroup(groupId);
            int idx = g.isCompleted() ? 1 : 0;
            if (tabs.getSelected() != idx) {
                tabs.onChange(null);
                tabs.setSelected(idx);
                tabs.onChange(i -> { selectedId = 0; refresh(); });
            }
        } catch (RuntimeException ignored) { }
    }

    private List<TravelGroup> myTrips(boolean completed) {
        int me = Backend.currentUserId();
        // BACKEND: Backend.getGroupsCreatedBy() reads from MySQL
        List<TravelGroup> all = new ArrayList<>(Backend.getGroupsCreatedBy(me));
        // BACKEND: Backend.getGroupsJoinedBy() reads from MySQL
        all.addAll(Backend.getGroupsJoinedBy(me));
        List<TravelGroup> out = new ArrayList<>();
        for (TravelGroup g : all) if (!g.isCancelled() && g.isCompleted() == completed) out.add(g);
        out.sort((a, b) -> completed ? b.getStartDate().compareTo(a.getStartDate()) : a.getStartDate().compareTo(b.getStartDate()));
        return out;
    }

    @Override
    public void refresh() {
        tripList.removeAll();
        List<TravelGroup> trips;
        try {
            trips = myTrips(tabs.getSelected() == 1);
            tabs.setTitle(0, "Upcoming (" + myTrips(false).size() + ")");
            tabs.setTitle(1, "Completed (" + myTrips(true).size() + ")");
        } catch (RuntimeException ex) {
            Theme.error(this, ex.getMessage());
            return;
        }
        if (trips.stream().noneMatch(t -> t.getId() == selectedId)) selectedId = trips.isEmpty() ? 0 : trips.get(0).getId();
        for (TravelGroup g : trips) {
            tripList.add(tripItem(g));
            tripList.add(Theme.gap(10));
        }
        if (trips.isEmpty()) tripList.add(empty("suitcase", tabs.getSelected() == 0 ? "No upcoming trips." : "No completed trips yet."));
        tripList.revalidate();
        tripList.repaint();
        showDetail();
    }

    private JComponent tripItem(TravelGroup g) {
        boolean sel = g.getId() == selectedId;
        RoundPanel p = new RoundPanel(new BorderLayout(12, 0), 14, sel ? Theme.GREEN_LIGHT : Color.WHITE, sel ? new Color(0xA9D3BF) : Theme.BORDER);
        p.setBorder(Theme.pad(10, 10, 10, 12));
        ImageBox img = new ImageBox(g.getImagePath(), g.getTitle(), 10, true);
        img.setPreferredSize(new Dimension(56, 56));
        p.add(img, BorderLayout.WEST);
        JPanel t = Theme.vbox();
        t.add(Theme.left(Theme.label(g.getTitle(), Font.BOLD, 13, Theme.TEXT_DARK)));
        t.add(Theme.gap(3));
        t.add(Theme.left(Theme.label(Theme.dateRange(g.getStartDate(), g.getEndDate()), Font.PLAIN, 11, Theme.TEXT_GRAY)));
        t.add(Theme.gap(3));
        boolean organizer = g.getCreatorId() == Backend.currentUserId();
        String when = "";
        if (!g.isCompleted()) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), g.getStartDate());
            when = days <= 0 ? "  \u2022  ongoing" : "  \u2022  in " + days + (days == 1 ? " day" : " days");
        }
        t.add(Theme.left(Theme.label((organizer ? "Organizer" : "Member") + when, Font.BOLD, 11, Theme.LINK)));
        JPanel tc = Theme.clear(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        tc.add(t, c);
        p.add(tc);
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { selectedId = g.getId(); refresh(); }
        });
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    // ================= detail =================
    private void showDetail() {
        detail.removeAll();
        if (selectedId == 0) {
            detail.add(empty("map", "Join or create a group to start planning a trip."));
            detail.revalidate();
            detail.repaint();
            return;
        }
        // BACKEND: Backend.getGroup() reads from MySQL
        TravelGroup g = Backend.getGroup(selectedId);
        boolean organizer = g.getCreatorId() == Backend.currentUserId();

        ImageBox banner = new ImageBox(g.getImagePath(), g.getTitle(), 16, true);
        banner.setPreferredSize(new Dimension(600, 150));
        addDetail(banner);
        detail.add(Theme.gap(16));

        JPanel head = Theme.clear(new BorderLayout());
        JPanel ht = Theme.vbox();
        ht.add(Theme.left(Theme.label(g.getTitle(), Font.BOLD, 22, Theme.TEXT_DARK)));
        ht.add(Theme.gap(4));
        ht.add(Theme.left(Theme.row(16,
                Theme.iconText("calendar", Theme.dateRange(g.getStartDate(), g.getEndDate()), Theme.TEXT_GRAY),
                Theme.iconText("pin", g.getDestination(), Theme.TEXT_GRAY),
                Theme.iconText("users", g.getCurrentMembers() + " members", Theme.TEXT_GRAY))));
        head.add(ht, BorderLayout.WEST);
        RoundButton view = new RoundButton("Group details", RoundButton.Style.OUTLINE).small();
        view.addActionListener(e -> app.openGroup(g.getId()));
        RoundButton chat = new RoundButton("Group chat").icon("chat").small();
        chat.addActionListener(e -> app.openGroupChat(g.getId()));
        JPanel hb = Theme.clear(new GridBagLayout());
        hb.add(Theme.row(8, view, chat));
        head.add(hb, BorderLayout.EAST);
        addDetail(head);
        detail.add(Theme.gap(18));

        // BACKEND: Backend.canReview() reads from MySQL
        if (g.isCompleted() && Backend.canReview(g.getId(), Backend.currentUserId())) {   // organizers receive reviews
            addDetail(reviewCard(g));
            detail.add(Theme.gap(16));
        }

        // meeting + expenses side by side
        JPanel two = Theme.clear(new GridLayout(1, 2, 16, 0));
        two.add(meetingCard(g));
        two.add(expensesCard(g, organizer));
        addDetail(two);
        detail.add(Theme.gap(16));

        addDetail(itineraryCard(g, organizer));
        detail.add(Theme.gap(16));
        addDetail(updatesCard(g));

        detail.revalidate();
        detail.repaint();
    }

    private void addDetail(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        detail.add(c);
    }

    private static RoundPanel card(String title, String icon, JComponent action) {
        RoundPanel p = RoundPanel.card(null);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(Theme.pad(18, 20, 18, 20));
        JPanel h = Theme.clear(new BorderLayout());
        JLabel t = Theme.sectionTitle(title);
        t.setIcon(Icons.get(icon, 18, Theme.GREEN));
        t.setIconTextGap(8);
        h.add(t, BorderLayout.WEST);
        if (action != null) h.add(action, BorderLayout.EAST);
        h.setAlignmentX(LEFT_ALIGNMENT);
        h.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        p.add(h);
        p.add(Theme.gap(12));
        return p;
    }

    private static JComponent kv(String k, String v) {
        JPanel r = Theme.clear(new BorderLayout(10, 0));
        r.add(Theme.label(k, Font.PLAIN, 13, Theme.TEXT_GRAY), BorderLayout.WEST);
        r.add(Theme.label(v == null || v.isBlank() ? "-" : v, Font.BOLD, 13, Theme.TEXT_DARK), BorderLayout.EAST);
        r.setAlignmentX(LEFT_ALIGNMENT);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        r.setBorder(Theme.pad(4, 0, 4, 0));
        return r;
    }

    private JComponent meetingCard(TravelGroup g) {
        RoundPanel p = card("Meeting place & time", "pin", null);
        p.add(kv("Place", g.getMeetingPlace()));
        p.add(kv("Date", Theme.day(g.getStartDate())));
        p.add(kv("Time", Theme.time(g.getMeetingTime())));
        p.add(kv("Transportation", g.getTransportMode()));
        p.add(kv("Organizer", g.getCreatorName()));
        return p;
    }

    private JComponent expensesCard(TravelGroup g, boolean organizer) {
        JComponent addBtn = null;
        if (organizer && !g.isCompleted()) addBtn = Theme.link("+ Add", () -> addExpense(g));
        RoundPanel p = card("Estimated expenses", "wallet", addBtn);
        // BACKEND: Backend.getExpenses() reads from MySQL
        List<Expense> list = Backend.getExpenses(g.getId());
        double total = 0;
        for (Expense e : list) {
            total += e.getAmount();
            JPanel r = Theme.clear(new BorderLayout(8, 0));
            r.add(Theme.label(e.getLabel(), Font.PLAIN, 13, Theme.TEXT_GRAY), BorderLayout.CENTER);
            JPanel right = Theme.row(6, Theme.label(Theme.peso(e.getAmount()), Font.BOLD, 13, Theme.TEXT_DARK));
            if (organizer && !g.isCompleted()) {
                JLabel del = new JLabel(Icons.get("trash", 14, Theme.HINT));
                del.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                del.setToolTipText("Remove");
                del.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent ev) {
                        // BACKEND: Backend.deleteExpense() saves to MySQL
                        if (Theme.run(MyTripsForm.this, () -> Backend.deleteExpense(e.getId()))) refresh();
                    }
                });
                right.add(del);
            }
            r.add(right, BorderLayout.EAST);
            r.setAlignmentX(LEFT_ALIGNMENT);
            r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
            r.setBorder(Theme.pad(4, 0, 4, 0));
            p.add(r);
        }
        if (list.isEmpty()) p.add(Theme.left(Theme.label("No expenses listed yet.", Font.PLAIN, 13, Theme.TEXT_GRAY)));
        p.add(Theme.gap(8));
        p.add(Theme.separator());
        p.add(Theme.gap(8));
        JPanel tot = Theme.clear(new BorderLayout());
        tot.add(Theme.label("Total per person", Font.BOLD, 13, Theme.TEXT_DARK), BorderLayout.WEST);
        tot.add(Theme.label(Theme.peso(total), Font.BOLD, 15, Theme.ORANGE), BorderLayout.EAST);
        tot.setAlignmentX(LEFT_ALIGNMENT);
        tot.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        p.add(tot);
        p.add(Theme.gap(4));
        p.add(Theme.left(Theme.label("Posted budget: " + Theme.peso(g.getBudgetPerPerson()), Font.PLAIN, 11, Theme.TEXT_GRAY)));
        return p;
    }

    private JComponent itineraryCard(TravelGroup g, boolean organizer) {
        JComponent addBtn = null;
        if (organizer && !g.isCompleted()) {
            RoundButton b = new RoundButton("Add activity", RoundButton.Style.LIGHT).icon("plus").small();
            b.addActionListener(e -> addActivity(g));
            addBtn = b;
        }
        RoundPanel p = card("Itinerary", "map", addBtn);
        // BACKEND: Backend.getItinerary() reads from MySQL
        List<Activity> acts = Backend.getItinerary(g.getId());
        if (acts.isEmpty())
            p.add(Theme.left(Theme.label(organizer ? "No activities yet. Add the first one." : "The organizer hasn't added activities yet.",
                    Font.PLAIN, 13, Theme.TEXT_GRAY)));
        LocalDate day = null;
        for (Activity a : acts) {
            if (!a.getDate().equals(day)) {
                day = a.getDate();
                long n = ChronoUnit.DAYS.between(g.getStartDate(), day) + 1;
                if (p.getComponentCount() > 2) p.add(Theme.gap(10));
                p.add(Theme.left(Theme.label("Day " + n + "  \u2022  " + Theme.day(day), Font.BOLD, 13, Theme.GREEN)));
                p.add(Theme.gap(6));
            }
            JPanel r = Theme.clear(new BorderLayout(14, 0));
            r.setBorder(Theme.pad(6, 0, 6, 0));
            JLabel time = Theme.label(Theme.time(a.getTime()), Font.BOLD, 12, Theme.TEXT_DARK);
            time.setPreferredSize(new Dimension(70, 18));
            r.add(time, BorderLayout.WEST);
            JPanel t = Theme.vbox();
            t.add(Theme.left(Theme.label(a.getTitle(), Font.PLAIN, 13, Theme.TEXT_DARK)));
            if (a.getLocation() != null && !a.getLocation().isBlank())
                t.add(Theme.left(Theme.iconText("pin", a.getLocation(), Theme.TEXT_GRAY)));
            if (a.getDetails() != null && !a.getDetails().isBlank())
                t.add(Theme.left(Theme.label(a.getDetails(), Font.ITALIC, 12, Theme.TEXT_GRAY)));
            r.add(t, BorderLayout.CENTER);
            JPanel right = Theme.row(8);
            if (organizer && !g.isCompleted()) {
                JLabel del = new JLabel(Icons.get("trash", 14, Theme.HINT));
                del.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                del.setToolTipText("Remove");
                del.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        if (Theme.confirm(MyTripsForm.this, "Remove this activity?", "\"" + a.getTitle() + "\" will be taken off the itinerary.", "Remove", true)
                                // BACKEND: Backend.deleteActivity() saves to MySQL
                                && Theme.run(MyTripsForm.this, () -> Backend.deleteActivity(a.getId()))) refresh();
                    }
                });
                right.add(del);
            }
            r.add(right, BorderLayout.EAST);
            r.setAlignmentX(LEFT_ALIGNMENT);
            r.setMaximumSize(new Dimension(Integer.MAX_VALUE, r.getPreferredSize().height));
            p.add(r);
        }
        return p;
    }

    private JComponent updatesCard(TravelGroup g) {
        RoundPanel p = card("Trip updates", "megaphone", null);
        // BACKEND: Backend.getAnnouncements() reads from MySQL
        List<Message> ann = Backend.getAnnouncements(g.getId());
        if (ann.isEmpty()) p.add(Theme.left(Theme.label("No updates from the organizer yet.", Font.PLAIN, 13, Theme.TEXT_GRAY)));
        for (Message m : ann) {
            RoundPanel r = new RoundPanel(new BorderLayout(10, 0), 10, Theme.YELLOW_LIGHT, null);
            r.setBorder(Theme.pad(10, 12, 10, 12));
            r.add(new JLabel(Icons.get("megaphone", 16, new Color(0x9A6B00))), BorderLayout.WEST);
            JPanel t = Theme.vbox();
            t.add(Theme.left(Theme.wrapLabel(m.getContent(), 520, Font.PLAIN, 13, Theme.TEXT_DARK)));
            t.add(Theme.left(Theme.label(m.getSenderName() + "  \u2022  " + Theme.ago(m.getSentAt()), Font.PLAIN, 11, Theme.TEXT_GRAY)));
            r.add(t);
            r.setAlignmentX(LEFT_ALIGNMENT);
            r.setMaximumSize(new Dimension(Integer.MAX_VALUE, r.getPreferredSize().height));
            p.add(r);
            p.add(Theme.gap(8));
        }
        return p;
    }

    private JComponent reviewCard(TravelGroup g) {
        int me = Backend.currentUserId();
        RoundPanel p = new RoundPanel(new BorderLayout(14, 0), 14, Theme.GREEN_LIGHT, null);
        p.setBorder(Theme.pad(16, 18, 16, 18));
        p.add(new JLabel(Icons.get("starFilled", 26, Theme.YELLOW)), BorderLayout.WEST);
        JPanel t = Theme.vbox();
        t.add(Theme.left(Theme.label("How was the trip?", Font.BOLD, 15, Theme.TEXT_DARK)));
        t.add(Theme.left(Theme.label("Rate your companions and leave a review to help other travelers.", Font.PLAIN, 12, Theme.TEXT_GRAY)));
        p.add(t);
        // BACKEND: Backend.hasRatedCompanions() reads from MySQL
        boolean rated = Backend.hasRatedCompanions(g.getId(), me);
        // BACKEND: Backend.hasReviewedTrip() reads from MySQL
        boolean reviewed = Backend.hasReviewedTrip(g.getId(), me);
        RoundButton rate = new RoundButton(rated ? "Companions rated" : "Rate companions", RoundButton.Style.OUTLINE).small();
        if (rated) rate.icon("check");
        rate.setEnabled(!rated);
        rate.addActionListener(e -> { if (ReviewForm.rateCompanions(this, g)) refresh(); });
        RoundButton review = new RoundButton(reviewed ? "Reviewed" : "Write a review").small();
        if (reviewed) review.icon("check");
        review.setEnabled(!reviewed);
        review.addActionListener(e -> { if (ReviewForm.reviewTrip(this, g)) refresh(); });
        JPanel b = Theme.clear(new GridBagLayout());
        b.add(Theme.row(8, rate, review));
        p.add(b, BorderLayout.EAST);
        return p;
    }

    // ================= add dialogs =================
    private void addActivity(TravelGroup g) {
        DateField date = DateField.date();
        date.setDate(g.getStartDate());
        date.setRange(g.getStartDate(), g.getEndDate());
        DateField time = DateField.time();
        time.setTime(java.time.LocalTime.of(8, 0));
        RoundTextField title = new RoundTextField("e.g. Swim at Batlag Falls");
        RoundTextField loc = new RoundTextField("e.g. Batlag Falls");
        RoundTextField details = new RoundTextField("e.g. Bring a change of clothes");
        JPanel p = form(new String[]{"Date", "Time", "Activity *", "Location", "Details"},
                new JComponent[]{date, time, title, loc, details});
        if (!Dialogs.form(this, "calendar", "Add an activity", "It shows in the itinerary for every member.", p, "Add activity"))
            return;
        if (date.getDate().isBefore(g.getStartDate()) || date.getDate().isAfter(g.getEndDate())) {
            Theme.error(this, "The date must be within the trip (" + Theme.dateRange(g.getStartDate(), g.getEndDate()) + ").");
            return;
        }
        if (title.getText().isBlank()) { Theme.error(this, "Activity name is required."); return; }
        Activity a = new Activity();
        a.setGroupId(g.getId());
        a.setDate(date.getDate());
        a.setTime(time.getTime());
        a.setTitle(title.getText().trim());
        a.setLocation(loc.getText().trim());
        a.setDetails(details.getText().trim());
        // BACKEND: Backend.addActivity() saves to MySQL
        if (Theme.run(this, () -> Backend.addActivity(a))) refresh();
    }

    private void addExpense(TravelGroup g) {
        RoundTextField label = new RoundTextField("e.g. Van rental (shared)");
        RoundTextField amount = new RoundTextField("e.g. 450");
        JPanel p = form(new String[]{"Expense *", "Amount per person (\u20B1) *"}, new JComponent[]{label, amount});
        if (!Dialogs.form(this, "wallet", "Add an expense", "Members see the estimated cost per person.", p, "Add expense"))
            return;
        if (label.getText().isBlank()) { Theme.error(this, "Expense name is required."); return; }
        Expense e = new Expense();
        e.setGroupId(g.getId());
        e.setLabel(label.getText().trim());
        try {
            e.setAmount(Double.parseDouble(amount.getText().replace(",", "").trim()));
        } catch (NumberFormatException ex) {
            Theme.error(this, "Amount must be a number.");
            return;
        }
        // BACKEND: Backend.addExpense() saves to MySQL
        if (Theme.run(this, () -> Backend.addExpense(e))) refresh();
    }

    private static JPanel form(String[] labels, JComponent[] inputs) {
        JPanel p = Theme.vbox();
        for (int i = 0; i < labels.length; i++) {
            p.add(Theme.left(Theme.label(labels[i], Font.BOLD, 12, Theme.TEXT_DARK)));
            p.add(Theme.gap(5));
            inputs[i].setPreferredSize(new Dimension(340, 38));
            p.add(Theme.left(inputs[i]));
            p.add(Theme.gap(12));
        }
        return p;
    }
}
