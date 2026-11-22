package sabaylakbay_1;

import sabaylakbay_1.ui.SubPage;
import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.model.AccountStatus;
import sabaylakbay_1.model.Report;
import sabaylakbay_1.model.ReportStatus;
import sabaylakbay_1.model.ReportTarget;
import sabaylakbay_1.ui.AppWindow;
import sabaylakbay_1.ui.Page;
import sabaylakbay_1.ui.Sidebar;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.Chip;
import sabaylakbay_1.ui.components.FadeCards;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ADMIN DASHBOARD: opens after Log In when the account's role is ADMIN.
 *
 * Left: dark sidebar. Right: the admin page you picked.
 * Pages: AdminOverviewForm, AdminUsersForm, AdminVerificationsForm, AdminTripsForm, AdminReportsForm,
 *        AdminAnnouncementsForm, AdminAnalyticsForm, AdminLogForm
 * Popups: AdminUserDetailForm, ReportReviewForm
 */
public class AdminDashboard extends JPanel {

    public static final Color ADMIN_DARK = new Color(0x17262C);

    private static final String[][] MENU = {
            {"OVERVIEW", "Dashboard", "grid"},
            {"USERS", "User Management", "users"},
            {"VERIFY", "ID Verifications", "shield"},
            {"TRIPS", "Trips", "map"},
            {"REPORTS", "Reports", "flag"},
            {"ANNOUNCE", "Announcements", "megaphone"},
            {"ANALYTICS", "Analytics", "chart"},
            {"LOG", "Activity Log", "note"}};

    private final Sidebar sidebar;
    private final FadeCards content = new FadeCards(true);
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private String current;

    private final AdminReportsForm reportsForm;

    public AdminDashboard() {
        super(new BorderLayout());
        Theme.useAdminColors(true);              // indigo admin look
        sidebar = new Sidebar(Theme.ADMIN_NAVY, "ADMIN PANEL", MENU, this::showPage, "Administrator", "logout", this::logout);
        content.setBackground(Theme.BG);

        reportsForm = new AdminReportsForm(this);
        addPage("OVERVIEW", new AdminOverviewForm(this));
        addPage("USERS", new AdminUsersForm(this));
        addPage("VERIFY", new AdminVerificationsForm(this));
        addPage("TRIPS", new AdminTripsForm(this));
        addPage("REPORTS", reportsForm);
        addPage("ANNOUNCE", new AdminAnnouncementsForm(this));
        addPage("ANALYTICS", new AdminAnalyticsForm(this));
        addPage("LOG", new AdminLogForm(this));

        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        SubPage.setArea(content);    // detail pages (trip details, profiles...) open over this area
        showPage("OVERVIEW");
    }

    private void addPage(String key, Page p) {
        pages.put(key, p);
        content.addCard(p, key);
    }

    // ===== navigation used by the admin pages =====

    public void showPage(String key) {
        SubPage.closeAll();          // leaving the page closes any open detail page
        sidebar.setSelected(key);
        pages.get(key).refresh();
        if (!key.equals(current)) content.show(key);
        current = key;
        refreshBadges();
    }

    /** Opens the Reports page on a tab (0 = Pending, 1 = Under review, 2 = Resolved, 3 = Dismissed, 4 = All). */
    public void openReports(int tab) {
        reportsForm.selectTab(tab);
        showPage("REPORTS");
    }

    /** Popup with a user's account details, reports and reviews. */
    public void openUser(int userId) {
        new AdminUserDetailForm(this, userId).setVisible(true);
        refreshCurrent();
    }

    /** Popup to review one report and record the decision. */
    public void openReport(Report r) {
        new ReportReviewForm(this, r).setVisible(true);
        refreshCurrent();
    }

    public void refreshCurrent() {
        if (current != null) pages.get(current).refresh();
        refreshBadges();
    }

    public void refreshBadges() {
        // BACKEND: Backend.getAdminStats() reads from MySQL
        sabaylakbay_1.model.AdminStats s = Backend.getAdminStats();
        sidebar.setBadge("REPORTS", s.getPendingReports());
        sidebar.setBadge("VERIFY", s.getPendingVerifications());
    }

    public void logout() {
        if (!Theme.confirm(this, "Log out?", "You'll need to log in again to manage SabayLakbay.", "Log out", false)) return;
        // BACKEND: Backend.logout() saves to MySQL
        Backend.logout();
        Theme.useAdminColors(false);             // back to the traveler green
        AppWindow.show(new LoginForm(), "SabayLakbay - Log In");
    }

    // ===== colored labels shared by the admin pages =====

    public static String statusName(ReportStatus s) {
        switch (s) {
            case PENDING: return "Pending";
            case UNDER_REVIEW: return "Under review";
            case RESOLVED: return "Resolved";
            default: return "Dismissed";
        }
    }

    public static Chip reportStatusChip(ReportStatus s) {
        switch (s) {
            case PENDING: return new Chip("Pending", Theme.YELLOW_LIGHT, new Color(0x9A6B00));
            case UNDER_REVIEW: return new Chip("Under review", Theme.BLUE_LIGHT, Theme.BLUE);
            case RESOLVED: return new Chip("Resolved", Theme.GREEN_LIGHT, Theme.GREEN);
            default: return new Chip("Dismissed", new Color(0xEEF0F2), Theme.TEXT_GRAY);
        }
    }

    public static Chip targetChip(ReportTarget t) {
        switch (t) {
            case USER: return new Chip("User", new Color(0xF1ECFB), new Color(0x6B3FC0));
            case GROUP: return new Chip("Group", new Color(0xE6F4F7), new Color(0x0F7287));
            default: return new Chip("Message", new Color(0xFDEFE6), new Color(0xB4520E));
        }
    }

    public static Chip accountChip(AccountStatus s) {
        switch (s) {
            case ACTIVE: return new Chip("Active", Theme.GREEN_LIGHT, Theme.GREEN);
            case SUSPENDED: return new Chip("Suspended", Theme.YELLOW_LIGHT, new Color(0x9A6B00));
            default: return new Chip("Removed", Theme.RED_LIGHT, Theme.RED);
        }
    }
}
