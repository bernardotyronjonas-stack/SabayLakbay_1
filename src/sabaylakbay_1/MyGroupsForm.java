package sabaylakbay_1;

import sabaylakbay_1.model.JoinRequest;
import sabaylakbay_1.model.RequestStatus;
import sabaylakbay_1.model.TravelGroup;
import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.ui.Page;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * MY GROUPS FORM
 * Opens from: sidebar "My Groups"
 * Tabs:
 *   Created by me : your groups + their join requests (Approve / Reject), Edit, Cancel
 *   Joined        : groups you are in (View, Itinerary, Chat, Leave)
 *   My Requests   : requests you sent and their status (Pending / Approved / Rejected), Cancel Request
 * Backend: getGroupsCreatedBy, getGroupsJoinedBy, getRequestsByUser, getPendingRequestsForGroup,
 *          approveRequest, rejectRequest, cancelGroup, leaveGroup, cancelJoinRequest
 */
public class MyGroupsForm extends Page {

    private final TabBar tabs = new TabBar("Created by me", "Joined", "My Requests");
    private final JPanel list = new Theme.ScrollableBox(null);

    public MyGroupsForm(MainDashboard app) {
        super(app);
        RoundButton create = new RoundButton("Create Group").icon("plus");
        create.addActionListener(e -> app.showPage("CREATE"));

        JPanel top = Theme.vbox();
        top.add(header("My Groups", "Manage the trips you created and the groups you joined.", create));
        JPanel tw = Theme.clear(new BorderLayout());
        tw.setBorder(Theme.pad(0, 32, 0, 32));
        tw.add(tabs);
        top.add(tw);
        add(top, BorderLayout.NORTH);

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(Theme.pad(20, 32, 32, 32));
        add(Theme.scroll(list), BorderLayout.CENTER);
        tabs.onChange(i -> refresh());
    }

    public void selectTab(int i) { tabs.setSelected(i); }

    @Override
    public void refresh() {
        int me = Backend.currentUserId();
        list.removeAll();
        try {
            // BACKEND: Backend.getGroupsCreatedBy() reads from MySQL
            List<TravelGroup> created = Backend.getGroupsCreatedBy(me);
            // BACKEND: Backend.getGroupsJoinedBy() reads from MySQL
            List<TravelGroup> joined = Backend.getGroupsJoinedBy(me);
            // BACKEND: Backend.getRequestsByUser() reads from MySQL
            List<JoinRequest> reqs = Backend.getRequestsByUser(me);
            tabs.setTitle(0, "Created by me (" + created.size() + ")");
            tabs.setTitle(1, "Joined (" + joined.size() + ")");
            tabs.setTitle(2, "My Requests (" + reqs.size() + ")");

            switch (tabs.getSelected()) {
                case 0:
                    if (created.isEmpty()) list.add(empty("users", "You haven't created a group yet."));
                    for (TravelGroup g : created) addRow(createdCard(g));
                    break;
                case 1:
                    if (joined.isEmpty()) list.add(empty("suitcase", "You haven't joined a group yet. Browse groups on Home."));
                    for (TravelGroup g : joined) addRow(joinedCard(g));
                    break;
                default:
                    if (reqs.isEmpty()) list.add(empty("send", "You haven't sent any join requests."));
                    for (JoinRequest r : reqs) addRow(requestCard(r));
            }
        } catch (RuntimeException ex) {
            Theme.error(this, ex.getMessage());
        }
        list.revalidate();
        list.repaint();
    }

    private void addRow(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        list.add(c);
        list.add(Theme.gap(14));
    }

    /** Top part shared by every group row: photo, title, chips, info. */
    private JPanel groupSummary(TravelGroup g, JComponent... buttons) {
        JPanel row = Theme.clear(new BorderLayout(16, 0));
        ImageBox img = new ImageBox(g.getImagePath(), g.getTitle(), 12, true);
        img.setPreferredSize(new Dimension(130, 90));
        row.add(img, BorderLayout.WEST);

        JPanel t = Theme.vbox();
        JPanel titleRow = Theme.row(10, Theme.label(g.getTitle(), Font.BOLD, 16, Theme.TEXT_DARK));
        if (g.isCancelled()) titleRow.add(new Chip("Cancelled", Theme.RED_LIGHT, Theme.RED));
        else if (g.isCompleted()) titleRow.add(new Chip("Completed", new Color(0xEEF0F2), Theme.TEXT_GRAY));
        else if (g.isFull()) titleRow.add(new Chip("Full", Theme.RED_LIGHT, Theme.RED));
        else titleRow.add(new Chip("Open", Theme.GREEN_LIGHT, Theme.GREEN));
        t.add(Theme.left(titleRow));
        t.add(Theme.gap(8));
        t.add(Theme.left(Theme.row(18,
                Theme.iconText("calendar", Theme.dateRange(g.getStartDate(), g.getEndDate()), Theme.TEXT_GRAY),
                Theme.iconText("pin", g.getDestination(), Theme.TEXT_GRAY),
                Theme.iconText("users", g.getCurrentMembers() + "/" + g.getMaxMembers() + " members", Theme.TEXT_GRAY))));
        t.add(Theme.gap(6));
        t.add(Theme.left(Theme.label(Theme.peso(g.getBudgetPerPerson()) + " est. / person", Font.BOLD, 14, Theme.ORANGE)));
        JPanel tc = Theme.clear(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        tc.add(t, c);
        row.add(tc, BorderLayout.CENTER);

        JPanel b = Theme.clear(new GridBagLayout());
        b.add(Theme.row(8, buttons));
        row.add(b, BorderLayout.EAST);
        return row;
    }

    private RoundButton viewButton(TravelGroup g) {
        RoundButton v = new RoundButton("View", RoundButton.Style.OUTLINE).small();
        v.addActionListener(e -> app.openGroup(g.getId()));
        return v;
    }

    // ----- Created tab -----
    private JComponent createdCard(TravelGroup g) {
        RoundPanel card = RoundPanel.card(null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(Theme.pad(16, 16, 16, 18));

        JComponent[] buttons;
        if (g.isCancelled() || g.isCompleted()) {
            buttons = new JComponent[]{viewButton(g)};
        } else {
            RoundButton edit = new RoundButton("Edit", RoundButton.Style.OUTLINE).icon("edit").small();
            edit.addActionListener(e -> app.editGroup(g));
            RoundButton cancel = new RoundButton("Cancel", RoundButton.Style.DANGER).small();
            cancel.addActionListener(e -> {
                if (Theme.confirm(this, "Cancel this trip?", "\"" + g.getTitle() + "\" will be cancelled and every member will be notified.", "Cancel trip", true)
                        // BACKEND: Backend.cancelGroup() saves to MySQL
                        && Theme.run(this, () -> Backend.cancelGroup(g.getId()))) { refresh(); app.refreshBadges(); }
            });
            buttons = new JComponent[]{viewButton(g), edit, cancel};
        }
        JPanel sum = groupSummary(g, buttons);
        sum.setAlignmentX(LEFT_ALIGNMENT);
        card.add(sum);

        if (g.isCancelled() || g.isCompleted()) return card;

        // BACKEND: Backend.getPendingRequestsForGroup() reads from MySQL
        List<JoinRequest> pending = Backend.getPendingRequestsForGroup(g.getId());
        card.add(Theme.gap(14));
        card.add(Theme.separator());
        card.add(Theme.gap(12));
        card.add(Theme.left(Theme.label("Join requests (" + pending.size() + ")", Font.BOLD, 13, Theme.TEXT_DARK)));
        card.add(Theme.gap(8));
        if (pending.isEmpty())
            card.add(Theme.left(Theme.label("No pending requests.", Font.PLAIN, 12, Theme.TEXT_GRAY)));
        for (JoinRequest r : pending) {
            JPanel row = Theme.clear(new BorderLayout(12, 0));
            row.setBorder(Theme.pad(6, 0, 6, 0));
            // BACKEND: Backend.getUser() reads from MySQL
            Avatar av = new Avatar(Backend.getUser(r.getUserId()), 36);
            av.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            av.setToolTipText("View profile and reviews");
            av.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) { app.openUserProfile(r.getUserId()); }
            });
            row.add(av, BorderLayout.WEST);
            JPanel t = Theme.vbox();
            // BACKEND: Backend.getAverageRating() reads from MySQL
            double rating = Backend.getAverageRating(r.getUserId());
            // BACKEND: Backend.getVerificationStatus() reads from MySQL
            t.add(Theme.left(Theme.row(8, Theme.label(r.getUserName(), Font.BOLD, 13, Theme.TEXT_DARK),
                    Theme.verificationChip(Backend.getVerificationStatus(r.getUserId())),
                    Theme.label(Theme.stars(rating) + "  \u2022  " + Theme.ago(r.getCreatedAt()), Font.PLAIN, 11, Theme.TEXT_GRAY))));
            if (r.getMessage() != null && !r.getMessage().isBlank())
                t.add(Theme.left(Theme.label("\"" + r.getMessage() + "\"", Font.ITALIC, 12, Theme.TEXT_GRAY)));
            row.add(t, BorderLayout.CENTER);

            RoundButton reject = new RoundButton("Reject", RoundButton.Style.DANGER).icon("x").small();
            reject.addActionListener(e -> {
                // BACKEND: Backend.rejectRequest() saves to MySQL
                if (Theme.run(this, () -> Backend.rejectRequest(r.getId()))) { refresh(); app.refreshBadges(); }
            });
            RoundButton approve = new RoundButton("Approve").icon("check").small();
            approve.addActionListener(e -> {
                // BACKEND: Backend.approveRequest() saves to MySQL
                if (Theme.run(this, () -> Backend.approveRequest(r.getId()))) { refresh(); app.refreshBadges(); }
            });
            JPanel b = Theme.clear(new GridBagLayout());
            b.add(Theme.row(8, reject, approve));
            row.add(b, BorderLayout.EAST);
            row.setAlignmentX(LEFT_ALIGNMENT);
            card.add(row);
        }
        return card;
    }

    // ----- Joined tab -----
    private JComponent joinedCard(TravelGroup g) {
        RoundPanel card = RoundPanel.card(new BorderLayout());
        card.setBorder(Theme.pad(16, 16, 16, 18));
        RoundButton chat = new RoundButton("Chat", RoundButton.Style.OUTLINE).icon("chat").small();
        chat.addActionListener(e -> app.openGroupChat(g.getId()));
        RoundButton trip = new RoundButton("Itinerary", RoundButton.Style.OUTLINE).icon("suitcase").small();
        trip.addActionListener(e -> app.openTrip(g.getId()));
        if (g.isCompleted() || g.isCancelled()) {
            card.add(groupSummary(g, viewButton(g), trip));
        } else {
            RoundButton leave = new RoundButton("Leave", RoundButton.Style.DANGER).small();
            leave.addActionListener(e -> {
                if (Theme.confirm(this, "Leave this trip?", "You'll be removed from \"" + g.getTitle() + "\".", "Leave trip", true)
                        // BACKEND: Backend.leaveGroup() saves to MySQL
                        && Theme.run(this, () -> Backend.leaveGroup(g.getId(), Backend.currentUserId()))) refresh();
            });
            card.add(groupSummary(g, viewButton(g), trip, chat, leave));
        }
        return card;
    }

    // ----- Requests tab -----
    private JComponent requestCard(JoinRequest r) {
        RoundPanel card = RoundPanel.card(new BorderLayout(14, 0));
        card.setBorder(Theme.pad(16, 18, 16, 18));
        JPanel t = Theme.vbox();
        t.add(Theme.left(Theme.row(10, Theme.label(r.getGroupTitle(), Font.BOLD, 15, Theme.TEXT_DARK), Theme.statusChip(r.getStatus()))));
        t.add(Theme.gap(6));
        t.add(Theme.left(Theme.label("Sent " + Theme.ago(r.getCreatedAt())
                + (r.getMessage() == null || r.getMessage().isBlank() ? "" : "  \u2022  \"" + r.getMessage() + "\""),
                Font.PLAIN, 12, Theme.TEXT_GRAY)));
        card.add(t, BorderLayout.CENTER);

        RoundButton view = new RoundButton("View Group", RoundButton.Style.OUTLINE).small();
        view.addActionListener(e -> app.openGroup(r.getGroupId()));
        JPanel b = Theme.clear(new GridBagLayout());
        JPanel btns = Theme.row(8, view);
        if (r.getStatus() == RequestStatus.PENDING) {
            RoundButton cancel = new RoundButton("Cancel Request", RoundButton.Style.DANGER).small();
            cancel.addActionListener(e -> {
                if (Theme.confirm(this, "Cancel your request?", "Your request to join \"" + r.getGroupTitle() + "\" will be withdrawn.", "Cancel request", true)
                        // BACKEND: Backend.cancelJoinRequest() saves to MySQL
                        && Theme.run(this, () -> Backend.cancelJoinRequest(r.getId()))) refresh();
            });
            btns.add(cancel);
        }
        b.add(btns);
        card.add(b, BorderLayout.EAST);
        return card;
    }
}
