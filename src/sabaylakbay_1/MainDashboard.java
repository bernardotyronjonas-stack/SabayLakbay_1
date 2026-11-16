package sabaylakbay_1;

import sabaylakbay_1.ui.AppWindow;
import sabaylakbay_1.ui.Page;
import sabaylakbay_1.ui.components.FadeCards;
import sabaylakbay_1.ui.Sidebar;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.model.TravelGroup;
import sabaylakbay_1.backend.Backend;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MAIN DASHBOARD: opens after Log In.
 * Left: green sidebar (ui/Sidebar.java). Right: the page you picked.
 * Pages: HomeForm, MyGroupsForm, CreateGroupForm, MyTripsForm, MessagesForm, NotificationsForm, ProfileForm
 * Popups: GroupDetailsForm, TravelerProfileForm
 * The methods below (openGroup, openGroupChat...) are how pages jump to each other.
 */
public class MainDashboard extends JPanel {

    private static final String[][] MENU = {
            {"HOME", "Home", "home"},
            {"GROUPS", "My Groups", "users"},
            {"CREATE", "Create Group", "plusCircle"},
            {"TRIPS", "My Trips", "suitcase"},
            {"MESSAGES", "Messages", "chat"},
            {"NOTIFS", "Notifications", "bell"},
            {"PROFILE", "Profile", "user"}};

    private final Sidebar sidebar;
    private final FadeCards content = new FadeCards(true);
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private String current;

    private final HomeForm homeForm;
    private final MyGroupsForm myGroupsForm;
    private final CreateGroupForm createGroupForm;
    private final MyTripsForm myTripsForm;
    private final MessagesForm messagesForm;
    private final NotificationsForm notificationsForm;
    private final ProfileForm profileForm;

    public MainDashboard() {
        super(new BorderLayout());
        sidebar = new Sidebar(Theme.GREEN, null, MENU, this::showPage, "Traveler", "chevronRight",
                () -> showPage("PROFILE"));
        content.setBackground(Theme.BG);

        homeForm = new HomeForm(this);
        myGroupsForm = new MyGroupsForm(this);
        createGroupForm = new CreateGroupForm(this);
        myTripsForm = new MyTripsForm(this);
        messagesForm = new MessagesForm(this);
        notificationsForm = new NotificationsForm(this);
        profileForm = new ProfileForm(this);

        addPage("HOME", homeForm);
        addPage("GROUPS", myGroupsForm);
        addPage("CREATE", createGroupForm);
        addPage("TRIPS", myTripsForm);
        addPage("MESSAGES", messagesForm);
        addPage("NOTIFS", notificationsForm);
        addPage("PROFILE", profileForm);

        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        showPage("HOME");
    }

    private void addPage(String key, Page p) {
        pages.put(key, p);
        content.addCard(p, key);
    }

    // ===== navigation used by all pages =====

    public void showPage(String key) {
        if ("CREATE".equals(key)) createGroupForm.startCreate();
        showPageNoReset(key);
    }

    private void showPageNoReset(String key) {
        sidebar.setSelected(key);
        pages.get(key).refresh();
        if (!key.equals(current)) content.show(key);   // fade to the new page
        current = key;
        refreshBadges();
    }

    /** Opens the full details of a group in a popup. */
    public void openGroup(int groupId) {
        new GroupDetailsForm(this, groupId).setVisible(true);
        refreshCurrent();
    }

    public void editGroup(TravelGroup g) {
        createGroupForm.startEdit(g);
        showPageNoReset("CREATE");
    }

    public void openGroupChat(int groupId) {
        messagesForm.selectGroup(groupId);
        showPageNoReset("MESSAGES");
    }

    public void openDirectChat(int userId) {
        messagesForm.selectDirect(userId);
        showPageNoReset("MESSAGES");
    }

    public void openTrip(int groupId) {
        myTripsForm.select(groupId);
        showPageNoReset("TRIPS");
    }

    public void openMyGroups(int tab) {
        myGroupsForm.selectTab(tab);
        showPageNoReset("GROUPS");
    }

    public void openUserProfile(int userId) {
        if (userId == Backend.currentUserId()) { showPage("PROFILE"); return; }
        new TravelerProfileForm(this, userId).setVisible(true);
        refreshCurrent();
    }

    /** Reloads the page that is showing now (after a dialog changed something). */
    public void refreshCurrent() {
        if (current != null) pages.get(current).refresh();
        refreshBadges();
    }

    public void refreshBadges() {
        // BACKEND: Backend.getUnreadCount() (sample data for now)
        sidebar.setBadge("NOTIFS", Backend.getUnreadCount(Backend.currentUserId()));
        int pending = 0;
        // BACKEND: Backend.getGroupsCreatedBy() (sample data for now)
        for (TravelGroup g : Backend.getGroupsCreatedBy(Backend.currentUserId()))
            // BACKEND: Backend.getPendingRequestsForGroup() (sample data for now)
            if (!g.isCancelled()) pending += Backend.getPendingRequestsForGroup(g.getId()).size();
        sidebar.setBadge("GROUPS", pending);
    }

    public void logout() {
        if (!Theme.confirm(this, "Log out of SabayLakbay?")) return;
        // BACKEND: Backend.logout() (button only for now)
        Backend.logout();
        AppWindow.show(new LoginForm(), "SabayLakbay - Log In");
    }
}
