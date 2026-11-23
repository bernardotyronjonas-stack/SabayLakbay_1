package sabaylakbay_1;

import sabaylakbay_1.ui.SubPage;
import sabaylakbay_1.model.*;
import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.AppWindow;
import sabaylakbay_1.ui.Dialogs;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * GROUP DETAILS FORM (popup)
 * Opens from: clicking a group card, notifications, My Groups "View"
 * Shows: photo, budget, location, transportation, date, meeting place/time, participants, slots,
 *        trip description, creator notes, members, organizer reviews
 * Buttons change depending on the user:
 *   - not joined: Send Join Request / Cancel Request
 *   - member:     Leave Group, View Itinerary, Group Chat
 *   - creator:    Cancel Group, Edit, Join Requests, Group Chat
 *   - everyone:   Contact creator, Report this group
 * Backend: getGroup, getMembers, getRequest, sendJoinRequest, joinNow, cancelJoinRequest, leaveGroup, cancelGroup,
 *          isSaved / toggleSaved (heart)
 */
public class GroupDetailsForm extends SubPage {   // opens inside the main window, with a Back bar

    private final MainDashboard app;
    private final int groupId;
    private TravelGroup g;

    public GroupDetailsForm(MainDashboard app, int groupId) {
        super(AppWindow.get(), "Group Details", true);
        this.app = app;
        this.groupId = groupId;
        setSize(900, 720);
        setLocationRelativeTo(AppWindow.get());
        reload();
    }

    private void reload() {
        try {
            // BACKEND: Backend.getGroup() reads from MySQL
            g = Backend.getGroup(groupId);
        } catch (RuntimeException ex) {
            Theme.error(app, ex.getMessage());
            dispose();
            return;
        }
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        itineraryAnchor = null;
        root.add(Theme.scroll(buildContent()), BorderLayout.CENTER);
        root.add(buildActionBar(), BorderLayout.SOUTH);
        setContentPane(root);
        revalidate();
        repaint();
    }

    // ================= content =================
    private JComponent buildContent() {
        int me = Backend.currentUserId();
        JPanel box = new Theme.ScrollableBox(null);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        ImageBox photo = new ImageBox(g.getImagePath(), g.getTitle(), 0, false);
        photo.setPreferredSize(new Dimension(800, 240));
        photo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        photo.setAlignmentX(LEFT_ALIGNMENT);
        box.add(photo);

        JPanel body = Theme.vbox();
        body.setBorder(Theme.pad(24, 32, 30, 32));
        body.setAlignmentX(LEFT_ALIGNMENT);
        box.add(body);

        // title + status + how to join, and the heart to save it (right)
        Chip joinChip = g.isOpenJoin() ? new Chip("Open join", Theme.GREEN_LIGHT, Theme.GREEN)
                                       : new Chip("Approval needed", new Color(0xF2F4F7), new Color(0x475467));
        joinChip.setToolTipText(g.isOpenJoin() ? "Verified travelers join right away"
                                               : "The organizer approves each request");
        JPanel titleRow = Theme.row(12, Theme.label(g.getTitle(), Font.BOLD, 24, Theme.TEXT_DARK), statusChip(), joinChip);
        JPanel titleLine = Theme.clear(new BorderLayout(12, 0));
        titleLine.add(titleRow, BorderLayout.CENTER);
        if (g.getCreatorId() != me) {
            JPanel hw = Theme.clear(new GridBagLayout());
            hw.add(new HeartButton(g.getId(), 40));
            titleLine.add(hw, BorderLayout.EAST);
        }
        titleLine.setAlignmentX(LEFT_ALIGNMENT);
        titleLine.setMaximumSize(new Dimension(Integer.MAX_VALUE, titleLine.getPreferredSize().height));
        body.add(titleLine);
        body.add(Theme.gap(4));
        body.add(Theme.left(Theme.iconText("pin", g.getDestination(), Theme.TEXT_GRAY)));
        body.add(Theme.gap(18));

        // organizer
        // BACKEND: Backend.getUser() reads from MySQL
        User creator = Backend.getUser(g.getCreatorId());
        // BACKEND: Backend.getAverageRating() reads from MySQL
        double rating = Backend.getAverageRating(creator.getId());
        // BACKEND: Backend.getReviewsForUser() reads from MySQL
        int count = Backend.getReviewsForUser(creator.getId()).size();
        RoundPanel org = new RoundPanel(new BorderLayout(12, 0), 14, new Color(0xF8FAF9), Theme.BORDER);
        org.setBorder(Theme.pad(12, 14, 12, 14));
        org.add(clickableUser(new Avatar(creator, 42), creator.getId()), BorderLayout.WEST);
        JPanel orgText = Theme.vbox();
        orgText.add(Theme.left(clickableUser(Theme.label(creator.getFullName(), Font.BOLD, 14, Theme.TEXT_DARK), creator.getId())));
        orgText.add(Theme.left(Theme.label("Organizer  \u2022  " + Theme.stars(rating)
                + (count > 0 ? " (" + count + (count == 1 ? " review)" : " reviews)") : ""), Font.PLAIN, 12, Theme.TEXT_GRAY)));
        org.add(Theme.middleLeft(orgText), BorderLayout.CENTER);
        if (creator.getId() != me) {
            RoundButton msg = new RoundButton("Contact creator", RoundButton.Style.OUTLINE).icon("chat").small();
            msg.addActionListener(e -> { dispose(); app.openDirectChat(creator.getId()); });
            JPanel r = Theme.clear(new GridBagLayout());
            r.add(msg);
            org.add(r, BorderLayout.EAST);
        }
        body.add(fullWidth(org));
        body.add(Theme.gap(20));

        // info tiles: 8 in two even rows of 4. Location (under the title) and slots left (in the
        // status tag) are already shown above, so they don't get tiles of their own.
        JPanel info = Theme.clear(new GridLayout(0, 4, 12, 12));
        info.add(tile("calendar", "Travel date", Theme.dateRange(g.getStartDate(), g.getEndDate())));
        info.add(tile("clock", "Meeting time", Theme.time(g.getMeetingTime())));
        info.add(tile("map", "Meeting place", g.getMeetingPlace()));
        info.add(tile("bus", "Transportation", g.getTransportMode()));
        info.add(tile("users", "Participants", g.getCurrentMembers() + " of " + g.getMaxMembers()));
        info.add(tile("wallet", "Est. budget / person", Theme.peso(g.getBudgetPerPerson())));
        info.add(tile("grid", "Category", g.getCategory()));
        info.add(tile("trend", "Difficulty", String.valueOf(g.getDifficulty())));
        body.add(fullWidth(info));
        body.add(Theme.gap(24));

        // ITINERARY: right under the trip details, so travelers see the plan before they decide to join.
        // Always shown; when there are no stops yet it says so (and the organizer can add them).
        // BACKEND: Backend.getItinerary() reads from MySQL
        List<Activity> stops = Backend.getItinerary(g.getId());
        JLabel itTitle = Theme.sectionTitle("Itinerary");
        itineraryAnchor = itTitle;
        itineraryStops = stops.size();
        JLabel itCount = Theme.label(stops.isEmpty() ? "No stops yet"
                : stops.size() + (stops.size() == 1 ? " stop" : " stops") + " \u2022 the plan you're joining", Font.PLAIN, 12, Theme.TEXT_GRAY);
        body.add(Theme.left(Theme.row(10, itTitle, itCount)));
        body.add(Theme.gap(8));
        RoundPanel itBox = new RoundPanel(new BorderLayout(), 14, new Color(0xFAFBFC), Theme.BORDER);
        itBox.setBorder(Theme.pad(14, 18, 14, 18));
        if (!stops.isEmpty()) {
            ItineraryView it = new ItineraryView();
            it.show(stops, g.getStartDate(), null, null);
            itBox.add(it);
        } else {
            boolean mine = g.getCreatorId() == me;
            JPanel empty = Theme.clear(new BorderLayout(14, 0));
            empty.add(new JLabel(Icons.get("map", 22, Theme.GREEN)), BorderLayout.WEST);
            empty.add(Theme.middleLeft(Theme.label(mine
                    ? "You haven't added the plan yet. Travelers decide faster when they can see it."
                    : "The organizer hasn't added the plan yet. You can ask what's planned before joining.",
                    Font.PLAIN, 13, Theme.TEXT_GRAY)), BorderLayout.CENTER);
            RoundButton act = mine ? new RoundButton("Add the itinerary", RoundButton.Style.OUTLINE).icon("plus").small()
                                   : new RoundButton("Ask the organizer", RoundButton.Style.OUTLINE).icon("chat").small();
            act.addActionListener(e -> {
                dispose();
                if (mine) app.editGroup(g); else app.openDirectChat(g.getCreatorId());
            });
            JPanel aw = Theme.clear(new GridBagLayout());
            aw.add(act);
            empty.add(aw, BorderLayout.EAST);
            itBox.add(empty);
        }
        body.add(fullWidth(itBox));
        body.add(Theme.gap(24));

        // description
        body.add(Theme.left(Theme.sectionTitle("About this trip")));
        body.add(Theme.gap(8));
        body.add(Theme.left(Theme.wrapLabel(g.getDescription(), 760, Font.PLAIN, 13, Theme.TEXT_DARK)));
        body.add(Theme.gap(20));

        // creator notes
        if (g.getCreatorNotes() != null && !g.getCreatorNotes().isBlank()) {
            RoundPanel notes = new RoundPanel(new BorderLayout(10, 0), 12, Theme.YELLOW_LIGHT, null);
            notes.setBorder(Theme.pad(14, 14, 14, 14));
            notes.add(new JLabel(Icons.get("note", 18, new Color(0x9A6B00))), BorderLayout.WEST);
            JPanel nt = Theme.vbox();
            nt.add(Theme.left(Theme.label("Notes from the creator", Font.BOLD, 12, new Color(0x7A5500))));
            nt.add(Theme.gap(4));
            nt.add(Theme.left(Theme.wrapLabel(g.getCreatorNotes(), 700, Font.PLAIN, 13, Theme.TEXT_DARK)));
            notes.add(nt);
            body.add(fullWidth(notes));
            body.add(Theme.gap(24));
        }

        // where the group meets (only when the organizer pinned it). Distance is from the traveler's home.
        if (g.hasMeetingPin()) {
            JPanel head = Theme.clear(new BorderLayout());
            JPanel ht = Theme.vbox();
            ht.add(Theme.left(Theme.sectionTitle("Where you meet")));
            Double km = Backend.distanceKm(g);                                     // BACKEND
            ht.add(Theme.left(Theme.label(g.getMeetingPlace() + (km == null ? ""
                    : "  \u2022  " + (km < 1 ? "under 1 km" : Math.round(km) + " km") + " from your home"),
                    Font.PLAIN, 12, Theme.TEXT_GRAY)));
            head.add(ht, BorderLayout.WEST);
            RoundButton dir = new RoundButton("Directions to meet-up").icon("send").small();
            dir.addActionListener(e -> MapView.openDirections(this, g.getMeetingLatitude(), g.getMeetingLongitude()));
            JPanel hb = Theme.clear(new GridBagLayout());
            hb.add(dir);
            head.add(hb, BorderLayout.EAST);
            body.add(fullWidth(head));
            body.add(Theme.gap(10));
            MapView meet = new MapView(false);
            meet.setView(g.getMeetingLatitude(), g.getMeetingLongitude(), 14);
            meet.setPin(g.getMeetingLatitude(), g.getMeetingLongitude());
            meet.setPreferredSize(new Dimension(700, 240));
            body.add(fullWidth(meet));
            body.add(Theme.gap(24));
        }

        // where the trip goes (only when the organizer pinned it)
        if (g.hasPin()) {
            JPanel head = Theme.clear(new BorderLayout());
            head.add(Theme.sectionTitle("Where you're going"), BorderLayout.WEST);
            RoundButton google = new RoundButton("Open in Google Maps", RoundButton.Style.OUTLINE).icon("map").small();
            google.addActionListener(e -> MapView.openInGoogleMaps(this, g.getLatitude(), g.getLongitude()));
            RoundButton directions = new RoundButton("Directions").icon("send").small();
            directions.addActionListener(e -> MapView.openDirections(this, g.getLatitude(), g.getLongitude()));
            head.add(Theme.row(8, google, directions), BorderLayout.EAST);
            body.add(fullWidth(head));
            body.add(Theme.gap(10));
            MapView map = new MapView(false);
            map.setView(g.getLatitude(), g.getLongitude(), 10);
            map.setPin(g.getLatitude(), g.getLongitude());
            map.setPreferredSize(new Dimension(700, 280));
            body.add(fullWidth(map));
            body.add(Theme.gap(24));
        }

        // members
        // BACKEND: Backend.getMembers() reads from MySQL
        List<User> members = Backend.getMembers(groupId);
        body.add(Theme.left(Theme.sectionTitle("Members (" + members.size() + ")")));
        body.add(Theme.gap(10));
        JPanel mgrid = Theme.clear(new GridLayout(0, 3, 10, 10));
        for (User u : members) mgrid.add(memberRow(u, u.getId() == g.getCreatorId()));
        body.add(fullWidth(mgrid));
        body.add(Theme.gap(24));

        // reviews
        // BACKEND: Backend.getTripReviews() reads from MySQL
        List<Review> reviews = g.isCompleted() ? Backend.getTripReviews(groupId)
                // BACKEND: Backend.getReviewsForUser() reads from MySQL
                : Backend.getReviewsForUser(creator.getId());
        body.add(Theme.left(Theme.sectionTitle(g.isCompleted() ? "Trip reviews" : "What travelers say about " + creator.getFullName().split(" ")[0])));
        body.add(Theme.gap(10));
        if (reviews.isEmpty()) body.add(Theme.left(Theme.label("No reviews yet.", Font.PLAIN, 13, Theme.TEXT_GRAY)));
        for (int i = 0; i < Math.min(3, reviews.size()); i++) {
            body.add(fullWidth(reviewRow(reviews.get(i))));
            body.add(Theme.gap(8));
        }

        // report
        if (creator.getId() != me) {
            body.add(Theme.gap(18));
            JLabel report = Theme.link("Report this group", () -> {
                new ReportForm(this, ReportTarget.GROUP, groupId, g.getTitle()).setVisible(true);
            });
            report.setForeground(Theme.RED);
            report.setIcon(Icons.get("flag", 14, Theme.RED));
            body.add(Theme.left(report));
        }
        return box;
    }

    private JComponent statusChip() {
        if (g.isCancelled()) return new Chip("Cancelled", Theme.RED_LIGHT, Theme.RED);
        if (g.isCompleted()) return new Chip("Completed", new Color(0xEEF0F2), Theme.TEXT_GRAY);
        if (g.isFull()) return new Chip("Full", Theme.RED_LIGHT, Theme.RED);
        return new Chip("Open \u2022 " + g.getSlotsAvailable() + (g.getSlotsAvailable() == 1 ? " slot left" : " slots left"), Theme.GREEN_LIGHT, Theme.GREEN);
    }

    private static JComponent fullWidth(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    private static JComponent tile(String icon, String label, String value) {
        RoundPanel t = new RoundPanel(null, 12, new Color(0xF8FAF9), Theme.BORDER);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.setBorder(Theme.pad(12, 14, 12, 14));
        t.add(Theme.left(Theme.iconText(icon, label, Theme.TEXT_GRAY)));
        t.add(Theme.gap(6));
        t.add(Theme.left(Theme.wrapLabel(value == null || value.isBlank() ? "-" : value, 120, Font.BOLD, 13, Theme.TEXT_DARK)));
        return t;
    }

    private JComponent clickableUser(JComponent c, int userId) {
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { app.openUserProfile(userId); reload(); }
        });
        return c;
    }

    private JComponent memberRow(User u, boolean isCreator) {
        RoundPanel r = new RoundPanel(new BorderLayout(10, 0), 12, Color.WHITE, Theme.BORDER);
        r.setBorder(Theme.pad(8, 10, 8, 10));
        r.add(new Avatar(u, 32), BorderLayout.WEST);
        JPanel t = Theme.vbox();
        t.add(Theme.left(Theme.label(u.getFullName() + (u.getId() == Backend.currentUserId() ? " (you)" : ""), Font.BOLD, 12, Theme.TEXT_DARK)));
        // BACKEND: Backend.getAverageRating() reads from MySQL
        String sub = isCreator ? "Organizer" : Theme.stars(Backend.getAverageRating(u.getId()));
        t.add(Theme.left(Theme.label(sub, Font.PLAIN, 11, isCreator ? Theme.LINK : Theme.TEXT_GRAY)));
        r.add(Theme.middleLeft(t), BorderLayout.CENTER);
        clickableUser(r, u.getId());
        r.setToolTipText("View profile and reviews");
        return r;
    }

    static JComponent reviewRow(Review rv) {
        RoundPanel r = new RoundPanel(new BorderLayout(12, 0), 12, Color.WHITE, Theme.BORDER);
        r.setBorder(Theme.pad(12, 14, 12, 14));
        // BACKEND: Backend.getUser() reads from MySQL
        r.add(new Avatar(Backend.getUser(rv.getReviewerId()), 34), BorderLayout.WEST);
        JPanel t = Theme.vbox();
        JPanel head = Theme.row(8, Theme.label(rv.getReviewerName(), Font.BOLD, 12, Theme.TEXT_DARK),
                new StarRating(rv.getRating(), 13, false),
                Theme.label(Theme.ago(rv.getCreatedAt()), Font.PLAIN, 11, Theme.TEXT_GRAY));
        t.add(Theme.left(head));
        t.add(Theme.gap(4));
        t.add(Theme.left(Theme.wrapLabel(rv.getComment(), 640, Font.PLAIN, 13, Theme.TEXT_DARK)));
        r.add(t);
        return r;
    }

    // ================= action bar =================
    private JComponent buildActionBar() {
        int me = Backend.currentUserId();
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER), Theme.pad(14, 32, 14, 32)));

        JPanel price = Theme.vbox();
        price.add(Theme.left(Theme.label(Theme.peso(g.getBudgetPerPerson()), Font.BOLD, 22, Theme.ORANGE)));
        price.add(Theme.left(Theme.label("estimated per person", Font.PLAIN, 11, Theme.TEXT_GRAY)));
        bar.add(price, BorderLayout.WEST);

        JPanel btns = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        // BACKEND: Backend.getRequest() reads from MySQL
        JoinRequest req = Backend.getRequest(groupId, me);
        // BACKEND: Backend.isMember() reads from MySQL
        boolean member = Backend.isMember(groupId, me);

        if (g.isCancelled()) {
            btns.add(Theme.label("This trip was cancelled.", Font.BOLD, 13, Theme.RED));
        } else if (g.getCreatorId() == me) {
            // BACKEND: Backend.getPendingRequestsForGroup() reads from MySQL
            int pending = Backend.getPendingRequestsForGroup(groupId).size();
            if (!g.isCompleted()) {
                RoundButton cancel = new RoundButton("Cancel Group", RoundButton.Style.DANGER);
                cancel.addActionListener(e -> {
                    if (Theme.confirm(this, "Cancel this trip?", "\"" + g.getTitle() + "\" will be cancelled and every member will be notified.", "Cancel trip", true)
                            // BACKEND: Backend.cancelGroup() saves to MySQL
                            && Theme.run(this, () -> Backend.cancelGroup(groupId))) reload();
                });
                btns.add(cancel);
                RoundButton edit = new RoundButton("Edit", RoundButton.Style.OUTLINE).icon("edit");
                edit.addActionListener(e -> { dispose(); app.editGroup(g); });
                btns.add(edit);
                RoundButton manage = new RoundButton("Join Requests (" + pending + ")", RoundButton.Style.OUTLINE);
                manage.addActionListener(e -> { dispose(); app.openMyGroups(0); });
                btns.add(manage);
            }
            btns.add(chatButton());
        } else if (member) {
            if (!g.isCompleted()) {
                RoundButton leave = new RoundButton("Leave Group", RoundButton.Style.DANGER);
                leave.addActionListener(e -> {
                    if (Theme.confirm(this, "Leave this trip?", "You'll be removed from \"" + g.getTitle() + "\". You can ask to join again while there are slots.", "Leave trip", true)
                            // BACKEND: Backend.leaveGroup() saves to MySQL
                            && Theme.run(this, () -> Backend.leaveGroup(groupId, me))) reload();
                });
                btns.add(leave);
            }
            RoundButton trip = new RoundButton("View Itinerary", RoundButton.Style.OUTLINE).icon("suitcase");
            trip.addActionListener(e -> { dispose(); app.openTrip(groupId); });
            btns.add(trip);
            btns.add(chatButton());
        } else if (g.isCompleted()) {
            btns.add(Theme.label("This trip is already completed.", Font.PLAIN, 13, Theme.TEXT_GRAY));
        } else if (req != null && req.getStatus() == RequestStatus.PENDING) {
            btns.add(Theme.statusChip(RequestStatus.PENDING));
            btns.add(Theme.label("Waiting for the creator to approve", Font.PLAIN, 12, Theme.TEXT_GRAY));
            RoundButton cancel = new RoundButton("Cancel Request", RoundButton.Style.DANGER);
            cancel.addActionListener(e -> {
                if (Theme.confirm(this, "Cancel your request?", "The organizer won't see it anymore.", "Cancel request", true)
                        // BACKEND: Backend.cancelJoinRequest() saves to MySQL
                        && Theme.run(this, () -> Backend.cancelJoinRequest(req.getId()))) reload();
            });
            btns.add(cancel);
        } else if (g.isFull()) {
            RoundButton full = new RoundButton("Group is full");
            full.setEnabled(false);
            btns.add(full);
        } else {
            if (req != null && req.getStatus() == RequestStatus.REJECTED)
                btns.add(Theme.label("Your last request was not approved.", Font.PLAIN, 12, Theme.TEXT_GRAY));
            if (itineraryAnchor != null && itineraryStops > 0) {          // see the plan before joining
                RoundButton plan = new RoundButton("See itinerary (" + itineraryStops + ")", RoundButton.Style.OUTLINE).icon("map");
                plan.addActionListener(e -> scrollToItinerary());
                btns.add(plan);
            }
            if (g.isOpenJoin()) {
                RoundButton join = new RoundButton("Join now").icon("check");
                join.addActionListener(e -> joinNow());
                btns.add(join);
            } else {
                RoundButton join = new RoundButton("Send Join Request");
                join.addActionListener(e -> sendRequest());
                btns.add(join);
            }
        }
        bar.add(btns, BorderLayout.EAST);
        return bar;
    }

    private RoundButton chatButton() {
        RoundButton chat = new RoundButton("Group Chat").icon("chat");
        chat.addActionListener(e -> { dispose(); app.openGroupChat(groupId); });
        return chat;
    }

    /** Open-join trips: join right away after one confirmation (no organizer approval). */
    private JComponent itineraryAnchor;      // the Itinerary heading, for "See itinerary"
    private int itineraryStops;

    private void scrollToItinerary() {
        if (itineraryAnchor == null) return;
        JComponent p = (JComponent) itineraryAnchor.getParent();
        Rectangle r = SwingUtilities.convertRectangle(itineraryAnchor, new Rectangle(0, -12, 10, 500), p);
        p.scrollRectToVisible(r);
    }

    private void joinNow() {
        if (!Theme.confirm(this, "Join \"" + g.getTitle() + "\"?",
                "You'll be added to the group right away and can chat with the others. "
                        + "Check the itinerary and the meeting place first.", "Join now", false)) return;
        // BACKEND: Backend.joinNow() saves to MySQL
        if (Theme.run(this, () -> Backend.joinNow(groupId, Backend.currentUserId()))) reload();
    }

    private void sendRequest() {
        TextAreaBox msg = new TextAreaBox("Say hi to the creator (optional)", 4);
        msg.setPreferredSize(new Dimension(380, 110));
        if (!Dialogs.form(this, "send", "Join \"" + g.getTitle() + "\"",
                g.getCreatorName() + " will see your profile and message, then approve or decline.", msg, "Send request")) return;
        // BACKEND: Backend.sendJoinRequest() saves to MySQL
        if (Theme.run(this, () -> Backend.sendJoinRequest(groupId, Backend.currentUserId(), msg.getText().trim()))) reload();
    }
}
