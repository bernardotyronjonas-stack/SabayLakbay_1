package sabaylakbay_1.ui.components;

import sabaylakbay_1.model.JoinRequest;
import sabaylakbay_1.model.RequestStatus;
import sabaylakbay_1.model.TravelGroup;
import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.MainDashboard;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.Avatar;
import sabaylakbay_1.ui.components.ImageBox;
import sabaylakbay_1.ui.components.RoundButton;
import sabaylakbay_1.ui.components.RoundPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** One travel group card on the Home page. */
public class GroupCard extends RoundPanel {

    public GroupCard(MainDashboard app, TravelGroup g) {
        super(new BorderLayout(), 16, Color.WHITE, Theme.BORDER);
        int me = Backend.currentUserId();
        hoverLift();
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { app.openGroup(g.getId()); }
        });

        // ----- photo with category and difficulty -----
        Chip category = new Chip(g.getCategory(), new Color(255, 255, 255, 235), Theme.TEXT_DARK);
        Chip difficulty = new Chip(String.valueOf(g.getDifficulty()), new Color(0, 0, 0, 120), Color.WHITE);
        ImageBox photo = new ImageBox(g.getImagePath(), g.getTitle(), 16, false) {
            @Override public void doLayout() {
                Dimension c = category.getPreferredSize(), d = difficulty.getPreferredSize();
                category.setBounds(12, 12, c.width, c.height);
                difficulty.setBounds(getWidth() - d.width - 12, 12, d.width, d.height);
            }
        };
        photo.add(category);
        photo.add(difficulty);
        photo.setPreferredSize(new Dimension(300, 150));
        add(photo, BorderLayout.NORTH);

        // ----- text -----
        JPanel body = Theme.vbox();
        body.setBorder(Theme.pad(16, 18, 18, 18));

        JLabel title = Theme.label(g.getTitle(), Font.BOLD, 16, Theme.TEXT_DARK);
        body.add(Theme.left(title));
        body.add(Theme.gap(8));

        JPanel creator = Theme.clear(new FlowLayout(FlowLayout.LEFT, 6, 0));
        // BACKEND: Backend.getUser() reads from MySQL
        creator.add(new Avatar(Backend.getUser(g.getCreatorId()), 20));
        creator.add(Theme.label(g.getCreatorName(), Font.PLAIN, 12, Theme.TEXT_GRAY));
        body.add(Theme.left(creator));
        body.add(Theme.gap(10));

        JLabel price = Theme.label(Theme.peso(g.getBudgetPerPerson()) + " /person", Font.BOLD, 19, Theme.ORANGE);
        body.add(Theme.left(price));
        body.add(Theme.gap(10));

        JPanel row = Theme.clear(new BorderLayout());
        row.add(Theme.iconText("calendar", Theme.date(g.getStartDate()), Theme.TEXT_GRAY), BorderLayout.WEST);
        JLabel slots = Theme.iconText("users", g.getCurrentMembers() + "/" + g.getMaxMembers() + " members",
                g.isFull() ? Theme.RED : Theme.TEXT_GRAY);
        row.add(slots, BorderLayout.EAST);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        body.add(row);
        body.add(Theme.gap(8));
        // BACKEND: Backend.distanceKm() = distance from the traveler's home pin to the trip pin
        Double km = Backend.distanceKm(g);
        String where = km == null ? g.getDestination()
                : g.getDestination() + "  \u2022  " + (km < 1 ? "under 1 km" : Math.round(km) + " km away");
        body.add(Theme.left(Theme.iconText("pin", where, Theme.TEXT_GRAY)));
        body.add(Theme.gap(16));

        RoundButton btn = new RoundButton("Join");
        // BACKEND: Backend.getRequest() reads from MySQL
        JoinRequest req = Backend.getRequest(g.getId(), me);
        if (g.getCreatorId() == me) { btn.setText("Manage"); btn.setStyle(RoundButton.Style.OUTLINE); }
        // BACKEND: Backend.isMember() reads from MySQL
        else if (Backend.isMember(g.getId(), me)) { btn.setText("Joined"); btn.setStyle(RoundButton.Style.LIGHT); btn.icon("check"); }
        else if (req != null && req.getStatus() == RequestStatus.PENDING) { btn.setText("Request pending"); btn.setStyle(RoundButton.Style.LIGHT); }
        else if (g.isFull()) { btn.setText("Full"); btn.setEnabled(false); }
        btn.addActionListener(e -> app.openGroup(g.getId()));
        JPanel bw = Theme.clear(new BorderLayout());
        bw.add(btn);
        bw.setAlignmentX(LEFT_ALIGNMENT);
        bw.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        body.add(bw);

        add(body, BorderLayout.CENTER);
    }
}
