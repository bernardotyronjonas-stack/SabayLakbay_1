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
import java.awt.geom.Ellipse2D;

/** One travel group card on the Home page. */
public class GroupCard extends RoundPanel {

    public GroupCard(MainDashboard app, TravelGroup g) {
        super(new BorderLayout(), 16, Color.WHITE, Theme.BORDER);
        int me = Backend.currentUserId();
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { app.openGroup(g.getId()); }
            @Override public void mouseEntered(MouseEvent e) { setStroke(new Color(0xBFD8CC)); }
            @Override public void mouseExited(MouseEvent e) { setStroke(Theme.BORDER); }
        });

        // ----- photo with heart button -----
        // BACKEND: Backend.isSaved() (sample data for now)
        HeartButton heart = new HeartButton(Backend.isSaved(g.getId(), me));
        // BACKEND: Backend.setSaved() (button only for now)
        heart.onToggle(on -> Theme.run(this, () -> Backend.setSaved(g.getId(), me, on)));
        ImageBox photo = new ImageBox(g.getImagePath(), g.getTitle(), 16, false) {
            @Override public void doLayout() { heart.setBounds(getWidth() - 44, 12, 32, 32); }
        };
        photo.add(heart);
        photo.setPreferredSize(new Dimension(300, 150));
        add(photo, BorderLayout.NORTH);

        // ----- text -----
        JPanel body = Theme.vbox();
        body.setBorder(Theme.pad(16, 18, 18, 18));

        JLabel title = Theme.label(g.getTitle(), Font.BOLD, 16, Theme.TEXT_DARK);
        body.add(Theme.left(title));
        body.add(Theme.gap(8));

        JPanel creator = Theme.clear(new FlowLayout(FlowLayout.LEFT, 6, 0));
        // BACKEND: Backend.getUser() (sample data for now)
        creator.add(new Avatar(Backend.getUser(g.getCreatorId()), 20));
        creator.add(Theme.label(g.getCreatorName(), Font.PLAIN, 12, Theme.TEXT_GRAY));
        body.add(Theme.left(creator));
        body.add(Theme.gap(10));

        JLabel price = Theme.label(Theme.peso(g.getBudgetPerPerson()) + " /person", Font.BOLD, 19, Theme.ORANGE);
        body.add(Theme.left(price));
        body.add(Theme.gap(10));

        JPanel row = Theme.clear(new BorderLayout());
        row.add(Theme.iconText("calendar", Theme.date(g.getStartDate()), Theme.TEXT_GRAY), BorderLayout.WEST);
        JLabel slots = Theme.iconText("users", g.getCurrentMembers() + "/" + g.getMaxMembers() + " slots",
                g.isFull() ? Theme.RED : Theme.TEXT_GRAY);
        row.add(slots, BorderLayout.EAST);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        body.add(row);
        body.add(Theme.gap(8));
        body.add(Theme.left(Theme.iconText("pin", g.getDestination(), Theme.TEXT_GRAY)));
        body.add(Theme.gap(16));

        RoundButton btn = new RoundButton("Join");
        // BACKEND: Backend.getRequest() (sample data for now)
        JoinRequest req = Backend.getRequest(g.getId(), me);
        if (g.getCreatorId() == me) { btn.setText("Manage"); btn.setStyle(RoundButton.Style.OUTLINE); }
        // BACKEND: Backend.isMember() (sample data for now)
        else if (Backend.isMember(g.getId(), me)) { btn.setText("Joined \u2713"); btn.setStyle(RoundButton.Style.LIGHT); }
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

    /** White round heart (save) button. */
    static class HeartButton extends JComponent {
        private boolean on;
        private java.util.function.Consumer<Boolean> listener;

        HeartButton(boolean on) {
            this.on = on;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Save");
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    HeartButton.this.on = !HeartButton.this.on;
                    repaint();
                    if (listener != null) listener.accept(HeartButton.this.on);
                    e.consume();
                }
            });
        }

        void onToggle(java.util.function.Consumer<Boolean> l) { listener = l; }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Float(0, 0, getWidth(), getHeight()));
            Icons.get(on ? "heartFilled" : "heart", 18, on ? Theme.RED : Theme.TEXT_DARK)
                    .paintIcon(this, g2, (getWidth() - 18) / 2, (getHeight() - 18) / 2 + 1);
            g2.dispose();
        }
    }
}
