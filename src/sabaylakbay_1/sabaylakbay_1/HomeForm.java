package sabaylakbay_1;

import sabaylakbay_1.model.GroupFilter;
import sabaylakbay_1.model.Recommendation;
import sabaylakbay_1.model.User;
import sabaylakbay_1.ui.AppWindow;
import sabaylakbay_1.model.SortOption;
import sabaylakbay_1.model.TravelGroup;
import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Page;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.List;

/**
 * HOME FORM (Main Menu)
 * Opens from: sidebar "Home" (first page after login)
 * Shows: photo banner with search bar, Filter (travel date, budget, slots), Sort (newest / upcoming), group cards
 * Click a card: opens GroupDetailsForm
 * Backend: Backend.searchGroups()
 */
public class HomeForm extends Page {

    private final RoundTextField searchField = new RoundTextField("Search destination, group, or creator...").withIcon("search").borderless();
    private final JComboBox<String> sortBox = new JComboBox<>(new String[]{"Newest", "Upcoming trip", "Nearest to me"});
    private final JPanel filterPanel;
    private final JCheckBox anyDate = new JCheckBox("Any date", true);
    private final DateField dateFrom = DateField.date();
    private final DateField dateTo = DateField.date();
    private final RoundTextField maxBudget = new RoundTextField("e.g. 2000");
    private final JSpinner minSlots = new JSpinner(new SpinnerNumberModel(0, 0, 50, 1));

    private final JLabel sectionTitle = Theme.sectionTitle("Featured Groups");
    private final JPanel picks = Theme.vbox();          // "Picked for you" section
    private final JPanel nearBox = Theme.vbox();        // "Groups near you" section (meeting places in a radius)
    private boolean nearAll;                            // "See all" in the near-you section

    /** Radius choices, like Marketplace. */
    private static final int[] RADII = {1, 2, 5, 10, 20, 40, 60, 80, 100, 250, 500};
    private final JLabel countLabel = Theme.label("", Font.PLAIN, 12, Theme.TEXT_GRAY);
    private final JLabel seeAll;
    private final JPanel grid = Theme.clear(new GridLayout(0, 3, 20, 20));
    private int lastColumns = 3;
    private boolean showAll;
    private boolean filtersOn;

    public HomeForm(MainDashboard app) {
        super(app);
        JPanel box = new Theme.ScrollableBox(null);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(Theme.pad(28, 32, 32, 32));

        // ----- hero -----
        box.add(full(new Hero()));
        box.add(Theme.gap(26));

        // ----- picked for you (from interests + home location) -----
        box.add(picks);
        picks.setAlignmentX(LEFT_ALIGNMENT);

        // ----- groups near you (meeting place within the chosen radius) -----
        box.add(nearBox);
        nearBox.setAlignmentX(LEFT_ALIGNMENT);

        // ----- filter + sort row -----
        RoundButton filterBtn = new RoundButton("Filter").icon("filter").small();
        filterBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        filterPanel = buildFilterPanel();
        filterPanel.setVisible(false);
        filterBtn.addActionListener(e -> {
            filterPanel.setVisible(!filterPanel.isVisible());
            box.revalidate();
            box.repaint();
        });
        sortBox.setFont(Theme.font(Font.PLAIN, 12));
        sortBox.addActionListener(e -> refresh());
        JPanel sortRow = Theme.row(8, Theme.label("Sort by", Font.PLAIN, 12, Theme.TEXT_GRAY), sortBox);
        JPanel filterRow = Theme.clear(new BorderLayout());
        filterRow.add(Theme.row(0, filterBtn), BorderLayout.WEST);
        filterRow.add(sortRow, BorderLayout.EAST);
        box.add(full(filterRow));
        box.add(Theme.gap(12));
        box.add(full(filterPanel));
        box.add(Theme.gap(14));

        // ----- section header -----
        seeAll = Theme.link("See all", () -> { showAll = !showAll; refresh(); });
        seeAll.setIcon(Icons.get("arrowRight", 14, Theme.LINK));
        seeAll.setHorizontalTextPosition(SwingConstants.LEFT);
        JPanel head = Theme.clear(new BorderLayout());
        head.add(Theme.row(10, sectionTitle, countLabel), BorderLayout.WEST);
        head.add(seeAll, BorderLayout.EAST);
        box.add(full(head));
        box.add(Theme.gap(14));
        box.add(full(grid));

        add(Theme.scroll(box));
        searchField.addActionListener(e -> refresh());

        // 4 cards per row on wide (full HD) screens, 3 otherwise
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                if (columns() != lastColumns && grid.getLayout() instanceof GridLayout) {
                    lastColumns = columns();
                    refresh();
                }
            }
        });
    }

    private static JComponent full(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    /** Featured groups fill two rows of cards. */
    private int featuredCount() {
        return columns() * 2;
    }

    private int columns() {
        return getWidth() >= 1500 ? 4 : 3;
    }

    private JPanel buildFilterPanel() {
        RoundPanel p = RoundPanel.card(new GridBagLayout());
        p.setBorder(Theme.pad(16, 18, 16, 18));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 14);
        c.anchor = GridBagConstraints.SOUTHWEST;

        anyDate.setOpaque(false);
        anyDate.setFont(Theme.font(Font.PLAIN, 12));
        anyDate.addActionListener(e -> { dateFrom.setEnabled(!anyDate.isSelected()); dateTo.setEnabled(!anyDate.isSelected()); });
        dateFrom.setEnabled(false);
        dateTo.setEnabled(false);
        dateTo.setDate(LocalDate.now().plusMonths(1));
        maxBudget.setPreferredSize(new Dimension(120, 40));
        minSlots.setPreferredSize(new Dimension(70, 40));
        minSlots.setFont(Theme.font(Font.PLAIN, 13));

        RoundButton apply = new RoundButton("Apply").small();
        apply.addActionListener(e -> { filtersOn = true; refresh(); });
        RoundButton reset = new RoundButton("Reset", RoundButton.Style.GHOST).small();
        reset.addActionListener(e -> {
            anyDate.setSelected(true); dateFrom.setEnabled(false); dateTo.setEnabled(false);
            maxBudget.setText(""); minSlots.setValue(0); filtersOn = false; refresh();
        });

        c.gridx = 0; p.add(field("Travel date from", dateFrom), c);
        c.gridx = 1; p.add(field("to", dateTo), c);
        c.gridx = 2; p.add(field(" ", anyDate), c);
        c.gridx = 3; p.add(field("Max budget (\u20B1)", maxBudget), c);
        c.gridx = 4; p.add(field("Min. slots left", minSlots), c);
        c.gridx = 5; c.weightx = 1; c.anchor = GridBagConstraints.SOUTHEAST; c.insets = new Insets(0, 0, 0, 0);
        p.add(Theme.row(6, reset, apply), c);
        return p;
    }

    private static JPanel field(String label, JComponent input) {
        JPanel f = Theme.vbox();
        f.add(Theme.left(Theme.label(label, Font.BOLD, 11, Theme.TEXT_GRAY)));
        f.add(Theme.gap(5));
        f.add(Theme.left(input));
        return f;
    }

    private GroupFilter buildFilter() {
        GroupFilter f = new GroupFilter();
        f.setKeyword(searchField.getText());
        int s = sortBox.getSelectedIndex();
        f.setSort(s == 1 ? SortOption.UPCOMING : s == 2 ? SortOption.NEAREST : SortOption.NEWEST);
        if (filtersOn) {
            if (!anyDate.isSelected()) { f.setDateFrom(dateFrom.getDate()); f.setDateTo(dateTo.getDate()); }
            String b = maxBudget.getText().replace(",", "").trim();
            if (!b.isEmpty()) {
                try { f.setMaxBudget(Double.parseDouble(b)); }
                catch (NumberFormatException ex) { Theme.error(this, "Max budget must be a number."); }
            }
            f.setMinSlots((Integer) minSlots.getValue());
        }
        return f;
    }

    @Override
    public void refresh() {
        GroupFilter f = buildFilter();
        List<TravelGroup> list;
        try {
            // BACKEND: Backend.searchGroups() reads from MySQL
            list = Backend.searchGroups(f);
        } catch (RuntimeException ex) {
            Theme.error(this, ex.getMessage());
            return;
        }
        boolean searching = filtersOn || !searchField.getText().isBlank();
        buildPicks(searching);
        buildNear(searching);
        sectionTitle.setText(searching ? "Search Results" : showAll ? "All Groups" : "Featured Groups");
        countLabel.setText(list.size() + (list.size() == 1 ? " group" : " groups"));
        seeAll.setVisible(!searching && list.size() > featuredCount());
        seeAll.setText(showAll ? "Show less" : "See all");

        grid.removeAll();
        int limit = (searching || showAll) ? list.size() : Math.min(featuredCount(), list.size());
        for (int i = 0; i < limit; i++) grid.add(new GroupCard(app, list.get(i)));
        if (list.isEmpty()) {
            grid.setLayout(new BorderLayout());
            // only talk about "your search" when there is one; otherwise there are simply no trips yet
            grid.add(searching
                    ? empty("search", "No groups match your search. Try other words or filters.")
                    : empty("map", "No trips yet. Be the first to start one.", "Create a group", () -> app.showPage("CREATE")));
        } else {
            grid.setLayout(new GridLayout(0, columns(), 20, 20));
        }
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        grid.revalidate();
        grid.repaint();
    }

    /** The big photo banner with the search bar. */
    private class Hero extends JPanel {
        private final JLabel title = Theme.label("Your next adventure starts here", Font.BOLD, 30, Color.WHITE);
        private final JLabel sub = Theme.label(greeting() + " Discover and join travel groups, or create your own.", Font.PLAIN, 14, new Color(255, 255, 255, 225));
        private final RoundPanel search = new RoundPanel(new BorderLayout(), 12, Color.WHITE, null);

        Hero() {
            super(null);
            setOpaque(false);
            setPreferredSize(new Dimension(800, 186));     // short enough that the first trips show on a laptop screen
            RoundButton go = new RoundButton("Search");
            go.addActionListener(e -> refresh());
            search.setBorder(Theme.pad(5, 4, 5, 6));
            search.add(searchField);
            search.add(go, BorderLayout.EAST);
            add(title);
            add(sub);
            add(search);
        }

        @Override
        public void doLayout() {
            int w = getWidth();
            title.setBounds(30, 24, w - 60, 40);
            sub.setBounds(30, 66, w - 60, 22);
            search.setBounds(30, 106, Math.min(660, w - 60), 50);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            int w = getWidth(), h = getHeight();
            BufferedImage img = ImageBox.load(sabaylakbay_1.model.Photos.HERO);
            ImageBox.paintRounded(g2, w, h, 18, true, b -> {
                if (img != null) ImageBox.drawCover(b, img, w, h);
                else ImageBox.drawPlaceholder(b, "", w, h);
                b.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 150), w * 0.7f, 0, new Color(0, 0, 0, 10)));
                b.fillRect(0, 0, w, h);
            });
            g2.dispose();
        }
    }

    /** "Good morning, Juan." based on the time of day. */
    private static String greeting() {
        int h = java.time.LocalTime.now().getHour();
        String part = h < 12 ? "Good morning" : h < 18 ? "Good afternoon" : "Good evening";
        sabaylakbay_1.model.User u = Backend.getCurrentUser();
        String first = u == null || u.getFullName() == null ? "" : ", " + u.getFullName().trim().split("\\s+")[0];
        return part + first + ".";
    }

    // ================= GROUPS NEAR YOU =================

    /** The radius the traveler picked last time (saved on this computer), 20 km at first. */
    private static int radius() {
        try {
            return java.util.prefs.Preferences.userNodeForPackage(HomeForm.class).getInt("nearRadius." + Backend.currentUserId(), 20);
        } catch (Exception e) { return 20; }
    }

    private static void saveRadius(int km) {
        try {
            java.util.prefs.Preferences.userNodeForPackage(HomeForm.class).putInt("nearRadius." + Backend.currentUserId(), km);
        } catch (Exception ignore) { /* not saved, still used this time */ }
    }

    /** "Groups near you": trips whose MEETING PLACE is within the radius of the traveler's home. */
    private void buildNear(boolean searching) {
        nearBox.removeAll();
        if (searching) { nearBox.revalidate(); nearBox.repaint(); return; }
        User me = Backend.getCurrentUser();
        int km = radius();

        JPanel head = Theme.clear(new BorderLayout());
        JPanel left = Theme.vbox();
        left.add(Theme.left(Theme.sectionTitle("Groups near you")));
        left.add(Theme.gap(3));
        head.add(left, BorderLayout.WEST);

        if (!me.hasLocation()) {
            left.add(Theme.left(Theme.label("Trips that meet up close to your home.", Font.PLAIN, 13, Theme.TEXT_GRAY)));
            nearBox.add(full(head));
            nearBox.add(Theme.gap(12));
            RoundPanel ask = new RoundPanel(new BorderLayout(16, 0), 18, Theme.GREEN_LIGHT, null);
            ask.setBorder(Theme.pad(16, 20, 16, 20));
            ask.add(new JLabel(Icons.get("pin", 24, Theme.GREEN)), BorderLayout.WEST);
            ask.add(Theme.middleLeft(Theme.label("Pin your home on the map to see groups that meet up near you.",
                    Font.PLAIN, 13, Theme.TEXT_DARK)), BorderLayout.CENTER);
            RoundButton set = new RoundButton("Set home location").small();
            set.addActionListener(e -> app.showPage("PROFILE"));
            JPanel sw = Theme.clear(new GridBagLayout());
            sw.add(set);
            ask.add(sw, BorderLayout.EAST);
            nearBox.add(full(ask));
            nearBox.add(Theme.gap(30));
            nearBox.revalidate();
            nearBox.repaint();
            return;
        }

        List<TravelGroup> near = Backend.getGroupsNearMe(km);                     // BACKEND
        left.add(Theme.left(Theme.label("Meet-up points within " + km + " km of " + me.getLocation()
                + "  \u2022  " + near.size() + (near.size() == 1 ? " group" : " groups"), Font.PLAIN, 13, Theme.TEXT_GRAY)));
        RoundButton change = new RoundButton("Within " + km + " km", RoundButton.Style.OUTLINE).icon("pin").small();
        change.setToolTipText("Change how far to look");
        change.addActionListener(e -> chooseRadius());
        JPanel right;
        if (near.size() > columns()) {
            JLabel all = Theme.link(nearAll ? "Show less" : "See all " + near.size(), () -> { nearAll = !nearAll; refresh(); });
            right = Theme.row(18, all, change);
        } else {
            right = Theme.row(0, change);
        }
        JPanel rw = Theme.clear(new GridBagLayout());
        rw.add(right);
        head.add(rw, BorderLayout.EAST);
        nearBox.add(full(head));
        nearBox.add(Theme.gap(12));

        if (near.isEmpty()) {
            RoundPanel none = RoundPanel.card(new BorderLayout(14, 0));
            none.setBorder(Theme.pad(16, 20, 16, 20));
            none.add(new JLabel(Icons.get("search", 22, Theme.TEXT_GRAY)), BorderLayout.WEST);
            none.add(Theme.middleLeft(Theme.label("No groups meet up within " + km + " km yet. Try a bigger radius.",
                    Font.PLAIN, 13, Theme.TEXT_GRAY)), BorderLayout.CENTER);
            int bigger = km;
            for (int r : RADII) if (r > km) { bigger = r; break; }
            if (bigger > km) {
                int b = bigger;
                RoundButton widen = new RoundButton("Show within " + b + " km", RoundButton.Style.OUTLINE).small();
                widen.addActionListener(e -> { saveRadius(b); refresh(); });
                JPanel ww = Theme.clear(new GridBagLayout());
                ww.add(widen);
                none.add(ww, BorderLayout.EAST);
            }
            nearBox.add(full(none));
        } else {
            int show = nearAll ? near.size() : Math.min(columns(), near.size());
            JPanel grid = Theme.clear(new GridLayout(0, columns(), 20, 20));
            for (int i = 0; i < show; i++) grid.add(new GroupCard(app, near.get(i)));
            for (int i = show; i < columns(); i++) grid.add(Theme.clear(new BorderLayout()));   // keep cards card-sized
            nearBox.add(full(grid));
        }
        nearBox.add(Theme.gap(30));
        nearBox.revalidate();
        nearBox.repaint();
    }

    /** Marketplace-style radius picker: a slider and a map with the circle and the groups inside it. */
    private void chooseRadius() {
        User me = Backend.getCurrentUser();
        List<TravelGroup> all = Backend.getGroupsNearMe(100000);                  // BACKEND: every trip with a meet-up pin
        int start = 0;
        for (int i = 0; i < RADII.length; i++) if (RADII[i] <= radius()) start = i;

        JLabel value = Theme.label("", Font.BOLD, 22, Theme.TEXT_DARK);
        JLabel count = Theme.label("", Font.PLAIN, 13, Theme.TEXT_GRAY);
        JSlider slider = new JSlider(0, RADII.length - 1, start);
        slider.setOpaque(false);
        slider.setSnapToTicks(true);
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        java.util.Hashtable<Integer, JLabel> labels = new java.util.Hashtable<>();
        for (int i : new int[]{0, 3, 5, 8, 10}) labels.put(i, Theme.label(RADII[i] + " km", Font.PLAIN, 11, Theme.TEXT_GRAY));
        slider.setLabelTable(labels);
        slider.setPaintLabels(true);
        slider.setPreferredSize(new Dimension(520, 56));

        MapView map = new MapView(false);
        map.setPreferredSize(new Dimension(520, 300));
        map.setPin(me.getLatitude(), me.getLongitude());

        Runnable update = () -> {
            int km = RADII[slider.getValue()];
            value.setText(km + " km");
            java.util.List<double[]> dots = new java.util.ArrayList<>();
            int n = 0;
            for (TravelGroup g : all) {
                Double d = Backend.distanceKm(g);
                if (d != null && d <= km) { n++; dots.add(new double[]{g.getMeetingLatitude(), g.getMeetingLongitude()}); }
            }
            count.setText(n == 0 ? "No groups meet up in this area yet." : n + (n == 1 ? " group meets up" : " groups meet up") + " in this area");
            map.setRadiusKm((double) km);
            map.setDots(dots);
            map.fitRadius();
        };
        slider.addChangeListener(e -> update.run());
        update.run();
        SwingUtilities.invokeLater(update);      // again once the map has its real size

        JPanel body = Theme.vbox();
        JPanel top = Theme.clear(new BorderLayout());
        top.add(value, BorderLayout.WEST);
        top.add(Theme.middleLeft(count), BorderLayout.EAST);
        top.setAlignmentX(LEFT_ALIGNMENT);
        body.add(top);
        body.add(Theme.gap(6));
        slider.setAlignmentX(LEFT_ALIGNMENT);
        body.add(slider);
        body.add(Theme.gap(10));
        map.setAlignmentX(LEFT_ALIGNMENT);
        body.add(map);
        body.add(Theme.gap(6));
        body.add(Theme.left(Theme.label("Center: your home (" + me.getLocation() + "). Orange dots are meet-up points.",
                Font.PLAIN, 11, Theme.TEXT_GRAY)));

        if (sabaylakbay_1.ui.Dialogs.form(this, "pin", "How far should we look?",
                "Show groups whose meeting place is within this distance of your home.", body, "Apply")) {
            saveRadius(RADII[slider.getValue()]);
            nearAll = false;
            refresh();
        }
    }

    /** "Picked for you": trips matching the traveler's interests and near their home. */
    private void buildPicks(boolean searching) {
        picks.removeAll();
        if (searching) { picks.revalidate(); picks.repaint(); return; }
        User me = Backend.getCurrentUser();
        List<String> likes = Backend.getInterests(me.getId());       // BACKEND
        if (likes.isEmpty() && !me.hasLocation()) {
            // nothing to go on yet: invite the traveler to personalize
            RoundPanel ask = new RoundPanel(new BorderLayout(16, 0), 18, Theme.GREEN_LIGHT, null);
            ask.setBorder(Theme.pad(18, 20, 18, 20));
            ask.add(new JLabel(Icons.get("heart", 26, Theme.GREEN)), BorderLayout.WEST);
            JPanel t = Theme.vbox();
            t.add(Theme.left(Theme.label("Get trips picked for you", Font.BOLD, 15, Theme.TEXT_DARK)));
            t.add(Theme.gap(3));
            t.add(Theme.left(Theme.label("Tell us what you like and where you're based. It takes 30 seconds.", Font.PLAIN, 13, Theme.TEXT_GRAY)));
            ask.add(Theme.middleLeft(t), BorderLayout.CENTER);
            RoundButton go = new RoundButton("Personalize").small();
            go.addActionListener(e -> AppWindow.show(new sabaylakbay_1.OnboardingForm(), "SabayLakbay - Get started"));
            JPanel gw = Theme.clear(new GridBagLayout());
            gw.add(go);
            ask.add(gw, BorderLayout.EAST);
            picks.add(full(ask));
            picks.add(Theme.gap(26));
        } else {
            List<Recommendation> recs = Backend.getRecommendations(columns());   // BACKEND
            if (!recs.isEmpty()) {
                JPanel head = Theme.clear(new BorderLayout());
                JPanel left = Theme.vbox();
                left.add(Theme.left(Theme.sectionTitle("Picked for you")));
                left.add(Theme.gap(3));
                String basis = likes.isEmpty() ? "Trips near " + me.getLocation()
                        : "Based on " + String.join(", ", likes.size() > 3 ? likes.subList(0, 3) : likes)
                          + (likes.size() > 3 ? " and more" : "")
                          + (me.hasLocation() ? " \u2022 near " + me.getLocation() : "");
                left.add(Theme.left(Theme.label(basis, Font.PLAIN, 13, Theme.TEXT_GRAY)));
                head.add(left, BorderLayout.WEST);
                JLabel edit = Theme.link("Edit interests", () -> app.showPage("PROFILE"));
                JPanel editTop = Theme.clear(new BorderLayout());   // lines the link up with the heading, not the middle
                edit.setBorder(Theme.pad(3, 0, 0, 0));
                editTop.add(edit, BorderLayout.NORTH);
                head.add(editTop, BorderLayout.EAST);
                picks.add(full(head));
                picks.add(Theme.gap(12));
                JPanel row = Theme.clear(new GridLayout(1, columns(), 20, 0));
                for (Recommendation r : recs) {
                    JPanel cell = Theme.clear(new BorderLayout(0, 6));
                    JLabel why = Theme.label(r.getReason(), Font.BOLD, 12, Theme.GREEN);
                    why.setIcon(Icons.get(r.getReason().startsWith("Near") ? "pin" : "heartFilled", 13, Theme.GREEN));
                    why.setIconTextGap(6);
                    why.setBorder(Theme.pad(0, 10, 0, 0));
                    cell.add(why, BorderLayout.NORTH);
                    cell.add(new GroupCard(app, r.getGroup()), BorderLayout.CENTER);
                    row.add(cell);
                }
                for (int i = recs.size(); i < columns(); i++) row.add(Theme.clear(new BorderLayout()));
                picks.add(full(row));
                picks.add(Theme.gap(30));
            }
        }
        picks.revalidate();
        picks.repaint();
    }
}
