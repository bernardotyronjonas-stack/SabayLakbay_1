package sabaylakbay_1.ui.components;

import sabaylakbay_1.model.Activity;
import sabaylakbay_1.ui.Dialogs;
import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The itinerary as a list grouped by day: "Day 1 - Sat, Dec 6", then each stop with its time,
 * a photo (click to see it big), the place with a "Map" link, and details.
 * Read-only on the trip page; with Edit / Remove buttons for the organizer (pass Actions).
 */
public class ItineraryView extends JPanel {

    /** What the Edit and Remove buttons do. Pass null for a read-only list. */
    public interface Actions {
        void edit(Activity a);
        void remove(Activity a);
    }

    public ItineraryView() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(LEFT_ALIGNMENT);
    }

    /** Shows the stops (sorted by day and time). emptyText is shown when there are none. */
    public void show(List<Activity> stops, LocalDate tripStart, Actions actions, String emptyText) {
        removeAll();
        List<Activity> sorted = new ArrayList<>(stops);
        sorted.sort(Comparator.comparing(Activity::getDate).thenComparing(Activity::getTime));
        if (sorted.isEmpty() && emptyText != null)
            add(Theme.left(Theme.label(emptyText, Font.PLAIN, 13, Theme.TEXT_GRAY)));
        LocalDate day = null;
        for (Activity a : sorted) {
            if (!a.getDate().equals(day)) {
                day = a.getDate();
                long n = tripStart == null ? 0 : ChronoUnit.DAYS.between(tripStart, day) + 1;
                if (getComponentCount() > 0) add(Theme.gap(12));
                add(Theme.left(Theme.label((n > 0 ? "Day " + n + "  •  " : "") + Theme.day(day), Font.BOLD, 13, Theme.GREEN)));
                add(Theme.gap(4));
            }
            add(stopRow(a, actions));
        }
        revalidate();
        repaint();
    }

    private JComponent stopRow(Activity a, Actions actions) {
        JPanel r = Theme.clear(new BorderLayout(14, 0));
        r.setBorder(Theme.pad(8, 0, 8, 0));

        // time + photo on the left
        JLabel time = Theme.label(Theme.time(a.getTime()), Font.BOLD, 12, Theme.TEXT_DARK);
        time.setPreferredSize(new Dimension(64, 18));
        time.setVerticalAlignment(SwingConstants.TOP);
        JPanel west = Theme.clear(new BorderLayout(10, 0));
        west.add(time, BorderLayout.WEST);
        west.add(thumb(a), BorderLayout.CENTER);
        r.add(west, BorderLayout.WEST);

        // name, place (+ Map link), details
        JPanel t = Theme.vbox();
        t.add(Theme.left(Theme.label(a.getTitle(), Font.BOLD, 13, Theme.TEXT_DARK)));
        boolean hasPlace = a.getLocation() != null && !a.getLocation().isBlank();
        if (hasPlace || a.hasPin()) {
            t.add(Theme.gap(3));
            JLabel place = Theme.iconText("pin", hasPlace ? a.getLocation() : "Pinned on the map", Theme.TEXT_GRAY);
            JPanel pr = Theme.row(10, place);
            if (a.hasPin()) {
                JLabel map = Theme.link("Map", () -> MapView.openInGoogleMaps(this, a.getLatitude(), a.getLongitude()));
                map.setToolTipText("Open this place in Google Maps");
                pr.add(map);
            }
            t.add(Theme.left(pr));
        }
        if (a.getDetails() != null && !a.getDetails().isBlank()) {
            t.add(Theme.gap(3));
            t.add(Theme.left(Theme.wrapLabel(a.getDetails(), 420, Font.PLAIN, 12, Theme.TEXT_GRAY)));
        }
        r.add(t, BorderLayout.CENTER);

        // organizer buttons
        if (actions != null) {
            RoundButton edit = iconButton("edit", "Edit this stop", Theme.GREEN);
            edit.addActionListener(e -> actions.edit(a));
            RoundButton del = iconButton("trash", "Remove this stop", Theme.RED);
            del.addActionListener(e -> actions.remove(a));
            JPanel b = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            b.add(edit);
            b.add(del);
            r.add(b, BorderLayout.EAST);
        }
        r.setAlignmentX(LEFT_ALIGNMENT);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, r.getPreferredSize().height));
        return r;
    }

    /** 72x52 photo of the place (click = see it big), or a soft square with a pin when there is no photo. */
    private JComponent thumb(Activity a) {
        if (!a.hasPhoto()) {
            JComponent empty = new JComponent() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.aa(g2);
                    g2.setColor(Theme.GREEN_LIGHT);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    Icon ic = Icons.get(a.hasPin() ? "pin" : "map", 18, Theme.GREEN);
                    ic.paintIcon(this, g2, (getWidth() - ic.getIconWidth()) / 2, (getHeight() - ic.getIconHeight()) / 2);
                    g2.dispose();
                }
            };
            empty.setPreferredSize(new Dimension(72, 52));
            return top(empty);
        }
        ImageBox img = new ImageBox(a.getPhotoPath(), a.getTitle(), 10, true);
        img.setPreferredSize(new Dimension(72, 52));
        img.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        img.setToolTipText("See the photo");
        img.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                ImageBox big = new ImageBox(a.getPhotoPath(), a.getTitle(), 12, true);
                big.setPreferredSize(new Dimension(620, 400));
                Dialogs.view(ItineraryView.this, "image", a.getTitle(),
                        a.getLocation() == null || a.getLocation().isBlank() ? null : a.getLocation(), big);
            }
        });
        return top(img);
    }

    private static JComponent top(JComponent c) {
        JPanel p = Theme.clear(new BorderLayout());
        p.add(c, BorderLayout.NORTH);
        return p;
    }

    private static RoundButton iconButton(String icon, String tip, Color color) {
        RoundButton b = new RoundButton("", RoundButton.Style.GHOST);
        b.setIcon(Icons.get(icon, 15, color));
        b.setToolTipText(tip);
        b.getAccessibleContext().setAccessibleName(tip);
        b.setMargin(new Insets(4, 4, 4, 4));
        b.setPreferredSize(new Dimension(34, 34));
        return b;
    }
}
