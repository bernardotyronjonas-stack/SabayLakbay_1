package sabaylakbay_1;

import sabaylakbay_1.model.GroupFilter;
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
    private final JComboBox<String> sortBox = new JComboBox<>(new String[]{"Newest", "Upcoming trip"});
    private final JPanel filterPanel;
    private final JCheckBox anyDate = new JCheckBox("Any date", true);
    private final DateField dateFrom = DateField.date();
    private final DateField dateTo = DateField.date();
    private final RoundTextField maxBudget = new RoundTextField("e.g. 2000");
    private final JSpinner minSlots = new JSpinner(new SpinnerNumberModel(0, 0, 50, 1));

    private final JLabel sectionTitle = Theme.sectionTitle("Featured Groups");
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
        box.add(Theme.gap(18));

        // ----- filter + sort row -----
        RoundButton filterBtn = new RoundButton("Filter").icon("filter").small();
        filterBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        filterPanel = buildFilterPanel();
        filterPanel.setVisible(false);
        filterBtn.addActionListener(e -> {
            filterPanel.setVisible(!filterPanel.isVisible());
            box.revalidate();
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
        f.setSort(sortBox.getSelectedIndex() == 1 ? SortOption.UPCOMING : SortOption.NEWEST);
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
            // BACKEND: Backend.searchGroups() (sample data for now)
            list = Backend.searchGroups(f);
        } catch (RuntimeException ex) {
            Theme.error(this, ex.getMessage());
            return;
        }
        boolean searching = filtersOn || !searchField.getText().isBlank();
        sectionTitle.setText(searching ? "Search Results" : showAll ? "All Groups" : "Featured Groups");
        countLabel.setText(list.size() + (list.size() == 1 ? " group" : " groups"));
        seeAll.setVisible(!searching && list.size() > featuredCount());
        seeAll.setText(showAll ? "Show less" : "See all");

        grid.removeAll();
        int limit = (searching || showAll) ? list.size() : Math.min(featuredCount(), list.size());
        for (int i = 0; i < limit; i++) grid.add(new GroupCard(app, list.get(i)));
        if (list.isEmpty()) {
            grid.setLayout(new BorderLayout());
            grid.add(empty("search", "No groups match your search. Try other filters."));
        } else {
            grid.setLayout(new GridLayout(0, columns(), 20, 20));
        }
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        grid.revalidate();
        grid.repaint();
    }

    /** The big photo banner with the search bar. */
    private class Hero extends JPanel {
        private final JLabel title = Theme.label("<html>Your next adventure<br>starts here</html>", Font.BOLD, 34, Color.WHITE);
        private final JLabel sub = Theme.label("Discover and join travel groups, or create your own.", Font.PLAIN, 14, new Color(255, 255, 255, 225));
        private final RoundPanel search = new RoundPanel(new BorderLayout(), 12, Color.WHITE, null);

        Hero() {
            super(null);
            setOpaque(false);
            setPreferredSize(new Dimension(800, 250));
            RoundButton go = new RoundButton("Search", RoundButton.Style.ORANGE);
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
            title.setBounds(30, 26, w - 60, 92);
            sub.setBounds(30, 120, w - 60, 22);
            search.setBounds(30, 158, Math.min(660, w - 60), 50);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            int w = getWidth(), h = getHeight();
            BufferedImage img = ImageBox.load("/images/mayon.jpg");
            ImageBox.paintRounded(g2, w, h, 18, true, b -> {
                if (img != null) ImageBox.drawCover(b, img, w, h);
                else ImageBox.drawPlaceholder(b, "", w, h);
                b.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 150), w * 0.7f, 0, new Color(0, 0, 0, 10)));
                b.fillRect(0, 0, w, h);
            });
            g2.dispose();
        }
    }
}
