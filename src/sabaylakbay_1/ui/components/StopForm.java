package sabaylakbay_1.ui.components;

import sabaylakbay_1.model.Activity;
import sabaylakbay_1.ui.Dialogs;
import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * The popup for adding or editing ONE itinerary stop:
 * day + time, what you'll do, the place, details, a photo of the place and a map pin.
 * Used by Create / Edit Group (ItineraryEditor) and by My Trips.
 *
 *   StopForm.Result r = StopForm.open(this, stop, tripStart, tripEnd, areaLat, areaLng);
 *   if (r != null) { ... r.stop, r.newPhoto ... }
 */
public final class StopForm {

    /** What the organizer filled in. newPhoto is the picture chosen now (null = keep the old one, if any). */
    public static final class Result {
        public final Activity stop;
        public final File newPhoto;
        Result(Activity stop, File newPhoto) { this.stop = stop; this.newPhoto = newPhoto; }
    }

    private StopForm() { }

    /**
     * @param stop      the stop to edit (a copy is changed), or a new Activity with a suggested day and time
     * @param tripStart first day of the trip; tripEnd last day (the day can only be picked inside it)
     * @param areaLat   where the map starts when nothing is pinned (e.g. the trip's destination), may be null
     * @return what was filled in, or null when cancelled
     */
    public static Result open(Component parent, Activity stop, LocalDate tripStart, LocalDate tripEnd,
                              Double areaLat, Double areaLng) {
        boolean isNew = stop.getTitle() == null || stop.getTitle().isBlank();
        Activity a = stop.copy();

        // ----- fields -----
        DateField day = DateField.date();
        day.setRange(tripStart, tripEnd);
        day.setDate(a.getDate() != null ? a.getDate() : tripStart);
        TimeField time = new TimeField();
        time.setTime(a.getTime() != null ? a.getTime() : LocalTime.of(8, 0));
        RoundTextField title = new RoundTextField("e.g. Swim at Batlag Falls");
        title.setText(a.getTitle() == null ? "" : a.getTitle());
        RoundTextField place = new RoundTextField("e.g. Batlag Falls, Tanay");
        place.setText(a.getLocation() == null ? "" : a.getLocation());
        RoundTextField details = new RoundTextField("e.g. Bring a change of clothes");
        details.setText(a.getDetails() == null ? "" : a.getDetails());

        // photo
        File[] chosen = {null};
        String[] photoPath = {a.getPhotoPath()};
        PhotoPreview preview = new PhotoPreview(a.getPhotoPath());
        preview.setPreferredSize(new Dimension(128, 84));
        JLabel photoName = Theme.label(" ", Font.PLAIN, 12, Theme.TEXT_GRAY);
        RoundButton choose = new RoundButton("Choose photo", RoundButton.Style.OUTLINE).icon("image").small();
        JLabel[] removeRef = new JLabel[1];
        Runnable showPhoto = () -> {
            boolean has = photoPath[0] != null;
            photoName.setText(has ? new File(photoPath[0]).getName() : "Optional. JPG or PNG.");
            removeRef[0].setVisible(has);
            choose.setText(has ? "Change photo" : "Choose photo");
        };
        choose.addActionListener(e -> {
            File f = Dialogs.chooseImage(parent, "Choose a photo of this place");
            if (f != null) { chosen[0] = f; photoPath[0] = f.getAbsolutePath(); preview.setPath(photoPath[0]); showPhoto.run(); }
        });
        removeRef[0] = Theme.link("Remove photo", () -> {
            chosen[0] = null; photoPath[0] = null; preview.setPath(null); showPhoto.run();
        });
        showPhoto.run();

        // map pin (starts on the stop's pin, else on the trip's destination)
        MapPicker map = new MapPicker().mapSize(400, 250);
        map.setPin(a.getLatitude(), a.getLongitude());
        map.showArea(areaLat, areaLng, 10);
        map.setSearchText(place.getText().trim());
        place.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { map.setSearchText(place.getText().trim()); }
        });

        JLabel error = Theme.label(" ", Font.PLAIN, 12, Theme.RED);

        // ----- layout: fields on the left, map on the right -----
        JPanel left = Theme.vbox();
        JPanel when = Theme.clear(new GridLayout(1, 2, 12, 0));
        when.add(field("Day *", day));
        when.add(field("Time *", time));
        left.add(row(when));
        left.add(row(field("What will you do? *", title)));
        left.add(row(field("Place", place)));
        left.add(row(field("Details", details)));
        JPanel photoRow = Theme.clear(new BorderLayout(14, 0));
        photoRow.add(preview, BorderLayout.WEST);
        JPanel pb = Theme.vbox();
        pb.add(Theme.left(choose));
        pb.add(Theme.gap(8));
        pb.add(Theme.left(photoName));
        pb.add(Theme.gap(6));
        pb.add(Theme.left(removeRef[0]));
        photoRow.add(pb, BorderLayout.CENTER);
        left.add(row(field("Photo of the place", photoRow)));
        left.add(Theme.left(error));
        left.setPreferredSize(new Dimension(370, left.getPreferredSize().height));

        JPanel right = Theme.clear(new BorderLayout(0, 6));
        right.add(Theme.label("Pin the place on the map (optional)", Font.BOLD, 12, Theme.TEXT_DARK), BorderLayout.NORTH);
        right.add(map, BorderLayout.CENTER);

        JPanel body = Theme.clear(new BorderLayout(24, 0));
        body.add(left, BorderLayout.WEST);
        body.add(right, BorderLayout.CENTER);

        String head = isNew ? "Add a stop" : "Edit stop";
        String sub = "A photo and a map pin help joiners picture the plan.";
        boolean ok = Dialogs.form(parent, "map", head, sub, body, isNew ? "Add stop" : "Save stop", () -> {
            String err = null;
            if (title.getText().isBlank()) err = "Write what the group will do here.";
            else if (title.getText().trim().length() > 150) err = "Keep the name under 150 characters.";
            else if (day.getDate().isBefore(tripStart) || day.getDate().isAfter(tripEnd))
                err = "Pick a day within the trip (" + Theme.dateRange(tripStart, tripEnd) + ").";
            error.setText(err == null ? " " : err);
            return err == null;
        });
        if (!ok) return null;

        a.setDate(day.getDate());
        a.setTime(time.getTime());
        a.setTitle(title.getText().trim());
        String p = place.getText().trim();
        if (p.isEmpty() && map.getLat() != null && map.getPlaceName() != null) p = map.getPlaceName();   // pinned but not typed
        a.setLocation(p.length() > 150 ? p.substring(0, 150) : p);
        a.setDetails(details.getText().trim());
        a.setLatitude(map.getLat());
        a.setLongitude(map.getLng());
        a.setPhotoPath(photoPath[0]);     // a newly chosen photo is copied to uploads/itinerary when the stop is saved
        return new Result(a, chosen[0]);
    }

    private static JPanel field(String label, JComponent input) {
        JPanel f = Theme.clear(new BorderLayout(0, 5));
        f.add(Theme.label(label, Font.BOLD, 12, Theme.TEXT_DARK), BorderLayout.NORTH);
        if (!(input instanceof JPanel)) input.setPreferredSize(new Dimension(150, 38));
        f.add(input, BorderLayout.CENTER);
        return f;
    }

    private static JComponent row(JComponent c) {
        JPanel r = Theme.clear(new BorderLayout());
        r.add(c);
        r.setBorder(Theme.pad(0, 0, 12, 0));
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, r.getPreferredSize().height));
        return r;
    }

    /** The photo, or a plain box with an image icon when there is none (not a made-up picture). */
    private static final class PhotoPreview extends ImageBox {
        private String current;
        PhotoPreview(String path) { super(path, "stop", 10, true); current = path; }
        @Override public void setPath(String p) { current = p; super.setPath(p); }
        @Override protected void paintComponent(Graphics g) {
            if (current != null) { super.paintComponent(g); return; }
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            g2.setColor(Theme.GREEN_LIGHT);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.setColor(new Color(0xBFD9CC));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{4f, 4f}, 0f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            javax.swing.Icon ic = sabaylakbay_1.ui.Icons.get("image", 22, Theme.GREEN);
            ic.paintIcon(this, g2, (getWidth() - ic.getIconWidth()) / 2, (getHeight() - ic.getIconHeight()) / 2);
            g2.dispose();
        }
    }
}
