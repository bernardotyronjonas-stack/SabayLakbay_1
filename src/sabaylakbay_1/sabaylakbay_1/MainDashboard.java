package sabaylakbay_1;

import sabaylakbay_1.ui.SubPage;
import sabaylakbay_1.ui.AppWindow;
import sabaylakbay_1.ui.Page;
import sabaylakbay_1.ui.components.FadeCards;
import sabaylakbay_1.ui.Sidebar;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.model.Message;
import sabaylakbay_1.model.TravelGroup;
import sabaylakbay_1.model.User;
import sabaylakbay_1.ui.components.MessagePopup;
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

    // new-message alerts: checks the database every few seconds
    private int[] seen = {0, 0};                 // newest direct message id and group message id already alerted
    private final Timer messageCheck = new Timer(4000, e -> checkNewMessages());
    private boolean checking;

    public MainDashboard() {
        super(new BorderLayout());
        Theme.useAdminColors(false);             // traveler green look
        sidebar = new Sidebar(Color.WHITE, null, MENU, this::showPage, "Traveler", "chevronRight",
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
        SubPage.setArea(content);    // detail pages (trip details, profiles...) open over this area
        showPage("HOME");

        // BACKEND: Backend.getLatestMessageIds() reads from MySQL (alerts start from now)
        seen = Backend.getLatestMessageIds();
        messageCheck.start();
    }

    /**
     * NEW-MESSAGE ALERTS. Every 4 seconds (in the background so the app never freezes):
     * any new direct message or group chat message for this traveler updates the Messages badge,
     * refreshes the open chat, and pops up "New message from ..." in the corner.
     */
    private void checkNewMessages() {
        if (!isDisplayable() || Backend.getCurrentUser() == null) { messageCheck.stop(); return; }
        if (checking) return;
        checking = true;
        int me = Backend.currentUserId();
        int[] from = seen.clone();
        new SwingWorker<java.util.List<Message>, Void>() {
            @Override protected java.util.List<Message> doInBackground() {
                // BACKEND: Backend.getNewMessages() reads from MySQL
                return Backend.getNewMessages(me, from[0], from[1]);
            }
            @Override protected void done() {
                checking = false;
                java.util.List<Message> news;
                try { news = get(); } catch (Exception ex) { return; }      // database busy: try again next time
                if (news.isEmpty() || Backend.getCurrentUser() == null || Backend.currentUserId() != me) return;
                for (Message m : news) {
                    if (m.getGroupId() == 0) seen[0] = Math.max(seen[0], m.getId());
                    else seen[1] = Math.max(seen[1], m.getId());
                }
                onNewMessages(news);
            }
        }.execute();
    }

    private void onNewMessages(java.util.List<Message> news) {
        boolean onMessages = "MESSAGES".equals(current);
        Window w = SwingUtilities.getWindowAncestor(this);
        boolean focused = w != null && w.isFocused();
        if (onMessages) messagesForm.liveUpdate();            // shows them in the open chat right away
        refreshBadges();

        // pop up the newest one, unless the traveler is already looking at that chat
        Message top = null;
        for (Message m : news) {
            if (onMessages && focused && messagesForm.isOpen(m)) continue;
            top = m;
            break;
        }
        if (top == null) return;
        Message m = top;
        User sender = Backend.getUser(m.getSenderId());                       // BACKEND
        String where = m.getGroupId() == 0 ? null : Backend.getGroup(m.getGroupId()).getTitle();
        int more = news.size() - 1;
        String text = m.getContent() + (more > 0 ? "  (+" + more + " more)" : "");
        MessagePopup.show(sender, sender.getFullName(), text, where, () -> {
            if (m.getGroupId() == 0) openDirectChat(m.getSenderId());
            else openGroupChat(m.getGroupId());
        });
        if (!focused && w != null) {
            try {   // flash the app on the Windows taskbar
                if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.USER_ATTENTION_WINDOW))
                    Taskbar.getTaskbar().requestWindowUserAttention(w);
            } catch (Exception ignore) { }
        }
    }

    private void addPage(String key, Page p) {
        pages.put(key, p);
        content.addCard(p, key);
    }

    // ===== navigation used by all pages =====

    public void showPage(String key) {
        SubPage.closeAll();          // leaving the page closes any open detail page
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
        // BACKEND: Backend.getUnreadCount() reads from MySQL
        sidebar.refreshUser();
        sidebar.setBadge("NOTIFS", Backend.getUnreadCount(Backend.currentUserId()));
        // BACKEND: Backend.getUnreadMessageCount() reads from MySQL
        sidebar.setBadge("MESSAGES", Backend.getUnreadMessageCount(Backend.currentUserId()));
        int pending = 0;
        // BACKEND: Backend.getGroupsCreatedBy() reads from MySQL
        for (TravelGroup g : Backend.getGroupsCreatedBy(Backend.currentUserId()))
            // BACKEND: Backend.getPendingRequestsForGroup() reads from MySQL
            if (!g.isCancelled()) pending += Backend.getPendingRequestsForGroup(g.getId()).size();
        sidebar.setBadge("GROUPS", pending);
    }

    public void logout() {
        if (!Theme.confirm(this, "Log out?", "See you on your next trip.", "Log out", false)) return;
        // BACKEND: Backend.logout() saves to MySQL
        messageCheck.stop();
        Backend.logout();
        AppWindow.show(new LoginForm(), "SabayLakbay - Log In");
    }
}
