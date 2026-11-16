package sabaylakbay_1;

import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.model.ChartPoint;
import sabaylakbay_1.model.MonthlyCount;
import sabaylakbay_1.ui.Page;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

/**
 * ADMIN REPORTS AND ANALYTICS
 * Opens from: admin sidebar "Analytics", or "View analytics" on the Dashboard Overview
 * Period: last 6 or 12 months
 * Shows: new registrations over time, trips created vs completed, popular destinations,
 *        reports filed vs resolved, and totals for the period
 * Button: Export summary (asks where to save, then Backend writes the file)
 * Backend: getRegistrationsPerMonth, getTripsPerMonth, getMostVisitedPlaces, getReportsPerMonth, exportSummaryReport
 */
public class AdminAnalyticsForm extends Page {

    private static final String[] PERIODS = {"Last 6 months", "Last 12 months"};

    private final JComboBox<String> period = new JComboBox<>(PERIODS);
    private final JPanel box = new Theme.ScrollableBox(null);

    public AdminAnalyticsForm(AdminDashboard admin) {
        period.setSelectedIndex(1);
        period.setFont(Theme.font(Font.PLAIN, 13));
        period.setPreferredSize(new Dimension(160, 38));
        period.addActionListener(e -> refresh());
        RoundButton export = new RoundButton("Export summary").icon("download");
        export.addActionListener(e -> export());
        add(header("Reports and Analytics", "Growth, trips, destinations and safety reports over time.",
                Theme.row(10, period, export)), BorderLayout.NORTH);

        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(Theme.pad(0, 32, 32, 32));
        add(Theme.scroll(box));
    }

    private int months() { return period.getSelectedIndex() == 0 ? 6 : 12; }

    @Override
    public void refresh() {
        box.removeAll();
        int m = months();
        List<ChartPoint> regs = Backend.getRegistrationsPerMonth(m);        // BACKEND
        List<MonthlyCount> trips = Backend.getTripsPerMonth(m);             // BACKEND
        List<MonthlyCount> reports = Backend.getReportsPerMonth(m);         // BACKEND
        List<ChartPoint> places = Backend.getMostVisitedPlaces(8);          // BACKEND

        int newUsers = (int) regs.stream().mapToDouble(ChartPoint::getValue).sum();
        int created = trips.stream().mapToInt(MonthlyCount::getFirst).sum();
        int completed = trips.stream().mapToInt(MonthlyCount::getSecond).sum();
        int filed = reports.stream().mapToInt(MonthlyCount::getFirst).sum();
        int resolved = reports.stream().mapToInt(MonthlyCount::getSecond).sum();
        DecimalFormat n = new DecimalFormat("#,###");

        // ----- totals for the period -----
        JPanel totals = Theme.clear(new GridLayout(1, 4, 14, 0));
        totals.add(total(n.format(newUsers), "New registrations"));
        totals.add(total(n.format(created), "Trips created"));
        totals.add(total(n.format(completed), "Trips completed"));
        totals.add(total(filed == 0 ? "-" : Math.round(resolved * 100.0 / filed) + "%",
                "Complaints resolved (" + resolved + " of " + filed + ")"));
        addFull(totals);
        box.add(Theme.gap(16));

        // ----- charts -----
        LineChart regChart = new LineChart(Theme.GREEN);
        regChart.setData(regs);
        ColumnChart tripChart = new ColumnChart("Created", Theme.GREEN, "Completed", new Color(0x7DB9A0));
        tripChart.setData(trips);
        BarChart placeChart = new BarChart(new Color(0x0F7287), "travelers");
        placeChart.setData(places);
        ColumnChart reportChart = new ColumnChart("Filed", Theme.ORANGE, "Resolved", Theme.GREEN);
        reportChart.setData(reports);

        JPanel grid = Theme.clear(new GridLayout(2, 2, 16, 16));
        grid.add(chartCard("New registrations", "users", regChart));
        grid.add(chartCard("Trips created and completed", "suitcase", tripChart));
        grid.add(chartCard("Popular destinations", "pin", placeChart));
        grid.add(chartCard("Reports and resolved complaints", "flag", reportChart));
        addFull(grid);

        box.revalidate();
        box.repaint();
    }

    private void addFull(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        box.add(c);
    }

    private static JComponent total(String value, String label) {
        RoundPanel p = RoundPanel.card(null);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(Theme.pad(16, 18, 16, 18));
        p.add(Theme.left(Theme.label(value, Font.BOLD, 26, Theme.GREEN)));
        p.add(Theme.left(Theme.label(label, Font.PLAIN, 12, Theme.TEXT_GRAY)));
        return p;
    }

    private static JComponent chartCard(String title, String icon, JComponent chart) {
        chart.setPreferredSize(new Dimension(460, 270));
        return AdminOverviewForm.card(title, icon, chart);
    }

    private void export() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save summary report");
        fc.setFileFilter(new FileNameExtensionFilter("CSV file (*.csv)", "csv"));
        fc.setSelectedFile(new File("SabayLakbay-summary.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        Theme.run(this, () -> Backend.exportSummaryReport(months(), f));         // BACKEND
    }
}
