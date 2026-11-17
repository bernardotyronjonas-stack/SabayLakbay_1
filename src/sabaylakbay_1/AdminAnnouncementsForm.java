package sabaylakbay_1;

import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.model.AdminLog;
import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Page;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

/**
 * ADMIN ANNOUNCEMENTS
 * Opens from: admin sidebar "Announcements"
 * Write a message and send it to every active traveler (or only verified ones). It shows in their Notifications.
 * Below: announcements sent before.
 * Backend: sendAnnouncement, getAnnouncements
 */
public class AdminAnnouncementsForm extends Page {

    private static final int MAX = 450;

    private final AdminDashboard admin;
    private final TextAreaBox text = new TextAreaBox("e.g. Typhoon warning this weekend: please check PAGASA before your trip and follow LGU advisories.", 4);
    private final JLabel chars = Theme.label("0 / " + MAX, Font.PLAIN, 12, Theme.TEXT_GRAY);
    private final JRadioButton everyone = radio("All active travelers");
    private final JRadioButton verified = radio("Verified travelers only");
    private final JPanel history = Theme.vbox();

    public AdminAnnouncementsForm(AdminDashboard admin) {
        this.admin = admin;
        add(header("Announcements", "Send a notice to travelers, like safety advisories, rule changes or maintenance.", null),
                BorderLayout.NORTH);

        JPanel box = new Theme.ScrollableBox(null);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(Theme.pad(0, 32, 32, 32));

        // ----- compose -----
        RoundPanel card = RoundPanel.card(new BorderLayout());
        card.setBorder(Theme.pad(20, 22, 20, 22));
        JPanel c = Theme.vbox();
        c.add(Theme.left(Theme.label("New announcement", Font.BOLD, 15, Theme.TEXT_DARK)));
        c.add(Theme.gap(12));
        text.setAlignmentX(LEFT_ALIGNMENT);
        text.setMaximumSize(new Dimension(Integer.MAX_VALUE, text.getPreferredSize().height));
        c.add(text);
        c.add(Theme.gap(6));
        JPanel under = Theme.clear(new BorderLayout());
        under.add(chars, BorderLayout.EAST);
        under.setAlignmentX(LEFT_ALIGNMENT);
        under.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        c.add(under);
        c.add(Theme.gap(8));
        ButtonGroup g = new ButtonGroup();
        g.add(everyone);
        g.add(verified);
        everyone.setSelected(true);
        c.add(Theme.left(Theme.row(18, Theme.label("Send to", Theme.MEDIUM, 13, new Color(0x344054)), everyone, verified)));
        c.add(Theme.gap(16));
        RoundButton send = new RoundButton("Send announcement").icon("send");
        send.addActionListener(e -> send());
        JPanel sr = Theme.clear(new BorderLayout());
        sr.add(Theme.label("Travelers see it in their Notifications. Suspended and removed accounts don't get it.",
                Font.PLAIN, 12, Theme.TEXT_GRAY), BorderLayout.WEST);
        sr.add(send, BorderLayout.EAST);
        sr.setAlignmentX(LEFT_ALIGNMENT);
        sr.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        c.add(sr);
        card.add(c);
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        box.add(card);
        box.add(Theme.gap(26));

        // ----- sent before -----
        box.add(Theme.left(Theme.sectionTitle("Sent before")));
        box.add(Theme.gap(10));
        history.setAlignmentX(LEFT_ALIGNMENT);
        box.add(history);
        add(Theme.scroll(box));

        text.getArea().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { count(); }
            public void removeUpdate(DocumentEvent e) { count(); }
            public void changedUpdate(DocumentEvent e) { count(); }
        });
    }

    private void count() {
        int n = text.getText().length();
        chars.setText(n + " / " + MAX);
        chars.setForeground(n > MAX ? Theme.RED : Theme.TEXT_GRAY);
    }

    private void send() {
        String msg = text.getText().trim();
        if (msg.isEmpty()) { text.getArea().requestFocusInWindow(); return; }
        String who = verified.isSelected() ? "every verified traveler" : "every active traveler";
        if (!Theme.confirm(this, "Send this announcement?", "It will go to " + who + " right away and can't be taken back.",
                "Send", false)) return;
        // BACKEND: Backend.sendAnnouncement() saves to MySQL
        if (Theme.run(this, () -> Backend.sendAnnouncement(msg, verified.isSelected()))) {
            text.setText("");
            count();
            refresh();
        }
    }

    @Override
    public void refresh() {
        history.removeAll();
        // BACKEND: Backend.getAnnouncements() reads from MySQL
        List<AdminLog> sent = Backend.getAnnouncements();
        if (sent.isEmpty()) history.add(empty("megaphone", "No announcements yet."));
        for (AdminLog l : sent) {
            RoundPanel p = RoundPanel.card(new BorderLayout(14, 0));
            p.setBorder(Theme.pad(14, 16, 14, 16));
            JLabel icon = new JLabel(Icons.get("megaphone", 18, Theme.GREEN));
            icon.setVerticalAlignment(SwingConstants.TOP);
            p.add(icon, BorderLayout.WEST);
            JPanel t = Theme.vbox();
            String safe = l.getDetails() == null ? "" : l.getDetails().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            t.add(Theme.left(Theme.label("<html><body style='width:620px'>" + safe + "</body></html>", Font.PLAIN, 13, Theme.TEXT_DARK)));
            t.add(Theme.gap(6));
            t.add(Theme.left(Theme.label(l.getTarget() + "  \u2022  by " + l.getAdminName() + "  \u2022  " + Theme.ago(l.getCreatedAt()),
                    Font.PLAIN, 12, Theme.TEXT_GRAY)));
            p.add(t, BorderLayout.CENTER);
            p.setAlignmentX(LEFT_ALIGNMENT);
            p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
            history.add(p);
            history.add(Theme.gap(10));
        }
        history.revalidate();
        history.repaint();
    }

    private static JRadioButton radio(String text) {
        JRadioButton r = new JRadioButton(text);
        r.setOpaque(false);
        r.setFont(Theme.font(Font.PLAIN, 13));
        r.setForeground(Theme.TEXT_DARK);
        r.setFocusPainted(false);
        r.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return r;
    }
}
