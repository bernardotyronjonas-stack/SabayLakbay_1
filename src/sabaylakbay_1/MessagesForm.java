package sabaylakbay_1;

import sabaylakbay_1.model.Message;
import sabaylakbay_1.model.TravelGroup;
import sabaylakbay_1.model.User;
import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Page;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * MESSAGES FORM (Communication)
 * Opens from: sidebar "Messages", "Group Chat" buttons, "Contact creator"
 * Left: group chats + direct messages. Right: chat history and the message box
 * The creator can tick "Post as announcement"
 * Backend: getGroupMessages, sendGroupMessage, getDirectMessages, getDirectContacts, sendDirectMessage
 */
public class MessagesForm extends Page {

    private final JPanel convList = Theme.vbox();
    private final JPanel chatHeader = Theme.clear(new BorderLayout());
    private final JPanel messagesBox = new Theme.ScrollableBox(null);
    private final JScrollPane messagesScroll;
    private final RoundTextField input = new RoundTextField("Type a message...");
    private final JCheckBox announce = new JCheckBox("Post as announcement");
    private final JPanel inputBar;

    private int groupId;     // > 0 when a group chat is open
    private int userId;      // > 0 when a direct chat is open
    private int extraContact; // a new direct chat that has no messages yet

    public MessagesForm(MainDashboard app) {
        super(app);
        add(header("Messages", "Chat with your travel groups and other travelers.", null), BorderLayout.NORTH);

        // left: conversations
        RoundPanel left = RoundPanel.card(new BorderLayout());
        left.setPreferredSize(new Dimension(300, 0));
        convList.setBorder(Theme.pad(10, 10, 10, 10));
        JPanel lw = Theme.clear(new BorderLayout());
        lw.add(convList, BorderLayout.NORTH);
        left.add(Theme.scroll(lw));

        // right: chat
        RoundPanel right = RoundPanel.card(new BorderLayout());
        chatHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER), Theme.pad(14, 18, 14, 18)));
        right.add(chatHeader, BorderLayout.NORTH);
        messagesBox.setLayout(new BoxLayout(messagesBox, BoxLayout.Y_AXIS));
        messagesBox.setBorder(Theme.pad(16, 18, 16, 18));
        messagesScroll = Theme.scroll(messagesBox);
        right.add(messagesScroll, BorderLayout.CENTER);

        RoundButton send = new RoundButton("Send").icon("send");
        send.addActionListener(e -> send());
        input.addActionListener(e -> send());
        announce.setOpaque(false);
        announce.setFont(Theme.font(Font.PLAIN, 12));
        announce.setForeground(Theme.TEXT_GRAY);
        inputBar = Theme.clear(new BorderLayout(10, 0));
        inputBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER), Theme.pad(12, 16, 12, 16)));
        inputBar.add(input, BorderLayout.CENTER);
        inputBar.add(Theme.row(10, announce, send), BorderLayout.EAST);
        right.add(inputBar, BorderLayout.SOUTH);

        JPanel body = Theme.clear(new BorderLayout(16, 0));
        body.setBorder(Theme.pad(0, 32, 28, 32));
        body.add(left, BorderLayout.WEST);
        body.add(right, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    public void selectGroup(int id) { groupId = id; userId = 0; }

    public void selectDirect(int id) {
        userId = id;
        groupId = 0;
        extraContact = id;
    }

    private List<TravelGroup> chatGroups() {
        int me = Backend.currentUserId();
        List<TravelGroup> out = new ArrayList<>();
        // BACKEND: Backend.getGroupsCreatedBy() (sample data for now)
        for (TravelGroup g : Backend.getGroupsCreatedBy(me)) if (!g.isCancelled()) out.add(g);
        // BACKEND: Backend.getGroupsJoinedBy() (sample data for now)
        for (TravelGroup g : Backend.getGroupsJoinedBy(me)) if (!g.isCancelled()) out.add(g);
        return out;
    }

    @Override
    public void refresh() {
        int me = Backend.currentUserId();
        convList.removeAll();
        try {
            List<TravelGroup> groups = chatGroups();
            // BACKEND: Backend.getDirectContacts() (sample data for now)
            List<User> contacts = new ArrayList<>(Backend.getDirectContacts(me));
            if (extraContact > 0 && contacts.stream().noneMatch(u -> u.getId() == extraContact)
                    // BACKEND: Backend.isBlocked() (sample data for now)
                    && !Backend.isBlocked(me, extraContact))
                // BACKEND: Backend.getUser() (sample data for now)
                contacts.add(0, Backend.getUser(extraContact));

            if (groupId == 0 && userId == 0) {
                if (!groups.isEmpty()) groupId = groups.get(0).getId();
                else if (!contacts.isEmpty()) userId = contacts.get(0).getId();
            }

            convList.add(Theme.left(sectionLabel("GROUP CHATS")));
            if (groups.isEmpty()) convList.add(Theme.left(Theme.label("  Join a group to chat.", Font.PLAIN, 12, Theme.HINT)));
            for (TravelGroup g : groups) {
                // BACKEND: Backend.getGroupMessages() (sample data for now)
                List<Message> msgs = Backend.getGroupMessages(g.getId());
                Message last = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
                ImageBox icon = new ImageBox(g.getImagePath(), g.getTitle(), 40, true);
                icon.setPreferredSize(new Dimension(40, 40));
                convList.add(convItem(icon, g.getTitle(), last, g.getId() == groupId, () -> { selectGroup(g.getId()); refresh(); }));
            }
            convList.add(Theme.gap(10));
            convList.add(Theme.left(sectionLabel("DIRECT MESSAGES")));
            if (contacts.isEmpty()) convList.add(Theme.left(Theme.label("  No direct messages yet.", Font.PLAIN, 12, Theme.HINT)));
            for (User u : contacts) {
                // BACKEND: Backend.getDirectMessages() (sample data for now)
                List<Message> msgs = Backend.getDirectMessages(me, u.getId());
                Message last = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
                convList.add(convItem(new Avatar(u, 40), u.getFullName(), last, u.getId() == userId,
                        () -> { userId = u.getId(); groupId = 0; refresh(); }));
            }
            showChat();
        } catch (RuntimeException ex) {
            Theme.error(this, ex.getMessage());
        }
        convList.revalidate();
        convList.repaint();
    }

    private static JLabel sectionLabel(String s) {
        JLabel l = Theme.label(s, Font.BOLD, 11, Theme.HINT);
        l.setBorder(Theme.pad(6, 8, 6, 0));
        return l;
    }

    private JComponent convItem(JComponent icon, String name, Message last, boolean selected, Runnable onClick) {
        RoundPanel p = new RoundPanel(new BorderLayout(10, 0), 12, selected ? Theme.GREEN_LIGHT : null, null);
        p.setBorder(Theme.pad(8, 8, 8, 8));
        p.add(icon, BorderLayout.WEST);
        JPanel t = Theme.vbox();
        JPanel top = Theme.clear(new BorderLayout());
        top.add(Theme.label(name, Font.BOLD, 13, Theme.TEXT_DARK), BorderLayout.CENTER);
        if (last != null) top.add(Theme.label(Theme.ago(last.getSentAt()), Font.PLAIN, 10, Theme.HINT), BorderLayout.EAST);
        top.setAlignmentX(LEFT_ALIGNMENT);
        t.add(top);
        String preview = last == null ? "No messages yet" : (last.getSenderId() == Backend.currentUserId() ? "You: " : "") + last.getContent();
        if (preview.length() > 34) preview = preview.substring(0, 34) + "...";
        t.add(Theme.left(Theme.label(preview, Font.PLAIN, 12, Theme.TEXT_GRAY)));
        p.add(t);
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onClick.run(); }
        });
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        return p;
    }

    private void showChat() {
        int me = Backend.currentUserId();
        chatHeader.removeAll();
        messagesBox.removeAll();
        List<Message> msgs;

        if (groupId > 0) {
            // BACKEND: Backend.getGroup() (sample data for now)
            TravelGroup g = Backend.getGroup(groupId);
            JPanel t = Theme.vbox();
            t.add(Theme.left(Theme.label(g.getTitle(), Font.BOLD, 15, Theme.TEXT_DARK)));
            t.add(Theme.left(Theme.label(g.getCurrentMembers() + " members  \u2022  organized by " + g.getCreatorName(), Font.PLAIN, 12, Theme.TEXT_GRAY)));
            chatHeader.add(t, BorderLayout.WEST);
            RoundButton details = new RoundButton("Trip details", RoundButton.Style.OUTLINE).small();
            details.addActionListener(e -> app.openTrip(groupId));
            chatHeader.add(details, BorderLayout.EAST);
            // BACKEND: Backend.getGroupMessages() (sample data for now)
            msgs = Backend.getGroupMessages(groupId);
            announce.setVisible(g.getCreatorId() == me);
            announce.setSelected(false);
            setInputEnabled(true);
        } else if (userId > 0) {
            // BACKEND: Backend.getUser() (sample data for now)
            User u = Backend.getUser(userId);
            JPanel t = Theme.row(10, new Avatar(u, 34));
            JPanel tt = Theme.vbox();
            tt.add(Theme.left(Theme.label(u.getFullName(), Font.BOLD, 15, Theme.TEXT_DARK)));
            // BACKEND: Backend.getAverageRating() (sample data for now)
            tt.add(Theme.left(Theme.label(Theme.stars(Backend.getAverageRating(userId)), Font.PLAIN, 12, Theme.TEXT_GRAY)));
            t.add(tt);
            chatHeader.add(t, BorderLayout.WEST);
            RoundButton profile = new RoundButton("View profile", RoundButton.Style.OUTLINE).small();
            profile.addActionListener(e -> app.openUserProfile(userId));
            chatHeader.add(Theme.row(8, profile), BorderLayout.EAST);
            // BACKEND: Backend.getDirectMessages() (sample data for now)
            msgs = Backend.getDirectMessages(me, userId);
            announce.setVisible(false);
            // BACKEND: Backend.isBlocked() (sample data for now)
            boolean blocked = Backend.isBlocked(me, userId) || Backend.isBlocked(userId, me);
            setInputEnabled(!blocked);
        } else {
            messagesBox.add(empty("chat", "No conversations yet."));
            setInputEnabled(false);
            messagesBox.revalidate();
            return;
        }

        if (msgs.isEmpty()) messagesBox.add(empty("chat", "No messages yet. Say hi!"));
        int lastSender = -1;
        for (Message m : msgs) {
            if (m.isAnnouncement()) {
                messagesBox.add(announcement(m));
                lastSender = -1;
            } else {
                messagesBox.add(bubble(m, m.getSenderId() == me, m.getSenderId() != lastSender));
                lastSender = m.getSenderId();
            }
            messagesBox.add(Theme.gap(6));
        }
        chatHeader.revalidate();
        messagesBox.revalidate();
        messagesBox.repaint();
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = messagesScroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private void setInputEnabled(boolean on) {
        input.setEnabled(on);
        input.setEditable(on);
        inputBar.setVisible(true);
        for (Component c : ((JPanel) inputBar.getComponent(1)).getComponents()) c.setEnabled(on);
    }

    private JComponent bubble(Message m, boolean mine, boolean showName) {
        JPanel row = Theme.clear(new FlowLayout(mine ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        JPanel col = Theme.vbox();
        if (!mine && showName && groupId > 0)
            col.add(Theme.left(Theme.label(m.getSenderName(), Font.BOLD, 11, Theme.TEXT_GRAY)));
        RoundPanel b = new RoundPanel(new BorderLayout(), 16, mine ? Theme.GREEN : new Color(0xF0F2F4), null);
        b.setBorder(Theme.pad(9, 14, 9, 14));
        Color fg = mine ? Color.WHITE : Theme.TEXT_DARK;
        JLabel text = m.getContent().length() > 48 ? Theme.wrapLabel(m.getContent(), 340, Font.PLAIN, 13, fg)
                : Theme.label(m.getContent(), Font.PLAIN, 13, fg);
        b.add(text);
        b.setAlignmentX(mine ? RIGHT_ALIGNMENT : LEFT_ALIGNMENT);
        JPanel bw = Theme.clear(new FlowLayout(mine ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 2));
        bw.add(b);
        bw.setAlignmentX(LEFT_ALIGNMENT);
        col.add(bw);
        JLabel time = Theme.label(Theme.ago(m.getSentAt()), Font.PLAIN, 10, Theme.HINT);
        JPanel tw = Theme.clear(new FlowLayout(mine ? FlowLayout.RIGHT : FlowLayout.LEFT, 4, 0));
        tw.add(time);
        tw.setAlignmentX(LEFT_ALIGNMENT);
        col.add(tw);

        if (!mine) {
            JPanel withAvatar = Theme.clear(new BorderLayout(8, 0));
            JPanel av = Theme.clear(new BorderLayout());
            // BACKEND: Backend.getUser() (sample data for now)
            av.add(showName ? new Avatar(Backend.getUser(m.getSenderId()), 30) : Box.createHorizontalStrut(30), BorderLayout.NORTH);
            withAvatar.add(av, BorderLayout.WEST);
            withAvatar.add(col);
            row.add(withAvatar);
        } else {
            row.add(col);
        }
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private JComponent announcement(Message m) {
        RoundPanel p = new RoundPanel(new BorderLayout(10, 0), 12, Theme.YELLOW_LIGHT, new Color(0xF3DFA2));
        p.setBorder(Theme.pad(10, 14, 10, 14));
        p.add(new JLabel(Icons.get("megaphone", 18, new Color(0x9A6B00))), BorderLayout.WEST);
        JPanel t = Theme.vbox();
        t.add(Theme.left(Theme.label("ANNOUNCEMENT  \u2022  " + m.getSenderName() + "  \u2022  " + Theme.ago(m.getSentAt()),
                Font.BOLD, 10, new Color(0x7A5500))));
        t.add(Theme.gap(3));
        t.add(Theme.left(Theme.wrapLabel(m.getContent(), 520, Font.PLAIN, 13, Theme.TEXT_DARK)));
        p.add(t);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    private void send() {
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        int me = Backend.currentUserId();
        boolean ok = groupId > 0
                // BACKEND: Backend.sendGroupMessage() (button only for now)
                ? Theme.run(this, () -> Backend.sendGroupMessage(groupId, me, text, announce.isSelected()))
                // BACKEND: Backend.sendDirectMessage() (button only for now)
                : Theme.run(this, () -> Backend.sendDirectMessage(me, userId, text));
        if (ok) {
            input.setText("");
            refresh();
            input.requestFocusInWindow();
        }
    }
}
