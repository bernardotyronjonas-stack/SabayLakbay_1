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

        // ----- photo: category (top left), heart to save (top right), difficulty + "Open join" (bottom left) -----
        Chip category = new Chip(g.getCategory(), new Color(255, 255, 255, 235), Theme.TEXT_DARK);
        Chip difficulty = new Chip(String.valueOf(g.getDifficulty()), new Color(0, 0, 0, 120), Color.WHITE);
        Chip open = new Chip("Open join", Theme.GREEN, Color.WHITE);
        open.setToolTipText("Verified travelers join right away, no approval needed");
        open.setVisible(g.isOpenJoin());
        HeartButton heart = new HeartButton(g.getId(), 34);
        ImageBox photo = new ImageBox(g.getImagePath(), g.getTitle(), 16, false) {
            @Override public void doLayout() {
                Dimension c = category.getPreferredSize(), d = difficulty.getPreferredSize(), o = open.getPreferredSize();
                category.setBounds(12, 12, c.width, c.height);
                heart.setBounds(getWidth() - 34 - 10, 10, 34, 34);
                difficulty.setBounds(12, getHeight() - d.height - 12, d.width, d.height);
                open.setBounds(12 + d.width + 6, getHeight() - o.height - 12, o.width, o.height);
            }
        };
        photo.add(category);
        photo.add(heart);
        photo.add(difficulty);
        photo.add(open);
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

        // the price is only an estimate (members settle with the organizer), same wording as the trip page
        JLabel price = Theme.label(Theme.peso(g.getBudgetPerPerson()), Font.BOLD, 19, Theme.ORANGE);
        JLabel per = Theme.label("est. per person", Font.PLAIN, 12, Theme.TEXT_GRAY);
        JPanel priceRow = Theme.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
        priceRow.add(price);
        priceRow.add(Theme.hgap(6));
        JPanel perBox = Theme.clear(new BorderLayout());     // sits on the price's baseline
        per.setBorder(Theme.pad(6, 0, 0, 0));
        perBox.add(per, BorderLayout.CENTER);
        priceRow.add(perBox);
        body.add(Theme.left(priceRow));
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
        // destination + how far the destination pin is from the traveler's home pin
        Double destKm = destinationKm(g);
        JLabel destLabel = Theme.iconText("pin", g.getDestination(), Theme.TEXT_GRAY);
        destLabel.setToolTipText("Destination: " + g.getDestination());
        JPanel destRow = Theme.clear(new BorderLayout(8, 0));
        destRow.add(destLabel, BorderLayout.CENTER);
        if (destKm != null) destRow.add(distanceLabel(destKm), BorderLayout.EAST);
        destRow.setAlignmentX(LEFT_ALIGNMENT);
        destRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        body.add(destRow);
        // meeting place + how far it is from the traveler's home
        // BACKEND: Backend.distanceKm() = distance from the traveler's home pin to the MEETING PLACE pin
        Double km = Backend.distanceKm(g);
        String meet = g.getMeetingPlace() == null || g.getMeetingPlace().isBlank() ? "Meet-up place not set" : g.getMeetingPlace();
        JLabel meetLabel = Theme.iconText("users", "Meet-up: " + meet, Theme.TEXT_GRAY);
        meetLabel.setToolTipText("Meet-up: " + meet);
        JPanel meetRow = Theme.clear(new BorderLayout(8, 0));
        meetRow.add(meetLabel, BorderLayout.CENTER);                 // long names end with "..."
        if (km != null) meetRow.add(distanceLabel(km), BorderLayout.EAST);
        meetRow.setAlignmentX(LEFT_ALIGNMENT);
        meetRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        body.add(Theme.gap(6));
        body.add(meetRow);
        body.add(Theme.gap(16));

        RoundButton btn = new RoundButton(g.isOpenJoin() ? "Join" : "Request to join");
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

    /** "12 km away" in green, used for both the destination and the meet-up distance. */
    private static JLabel distanceLabel(double km) {
        String text = km < 1 ? "under 1 km" : String.format("%,d km", Math.round(km));
        JLabel l = Theme.label(text + " away", Font.BOLD, 12, Theme.GREEN);
        l.setToolTipText("Straight-line distance from your home pin");
        return l;
    }

    /**
     * Straight-line km from the traveler's home pin to the trip's DESTINATION pin.
     * null if either one has no pin (then nothing is shown). Only uses Backend.km(): no database call.
     */
    private static Double destinationKm(TravelGroup g) {
        sabaylakbay_1.model.User me = Backend.getCurrentUser();
        if (me == null || !me.hasLocation() || g.getLatitude() == null || g.getLongitude() == null) return null;
        return Backend.km(me.getLatitude(), me.getLongitude(), g.getLatitude(), g.getLongitude());
    }
}
