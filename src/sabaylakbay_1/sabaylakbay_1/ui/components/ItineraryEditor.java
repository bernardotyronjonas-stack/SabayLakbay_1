package sabaylakbay_1.ui.components;

import sabaylakbay_1.model.Activity;
import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * The "Itinerary" part of Create / Edit Group.
 * Stops are kept here as a draft and saved together with the trip (Backend.saveItinerary),
 * so Cancel on the form really cancels. Each stop can have a photo and a map pin (StopForm).
 */
public class ItineraryEditor extends JPanel {

    private final List<Activity> stops = new ArrayList<>();
    private final Map<Activity, File> newPhotos = new IdentityHashMap<>();
    private final List<Integer> removedIds = new ArrayList<>();
    private final ItineraryView list = new ItineraryView();
    private final JLabel count = Theme.label(" ", Font.PLAIN, 12, Theme.TEXT_GRAY);

    private final Supplier<LocalDate> tripStart, tripEnd;
    private final Supplier<Double[]> area;   // where the map starts: the destination pin, or null

    public ItineraryEditor(Supplier<LocalDate> tripStart, Supplier<LocalDate> tripEnd, Supplier<Double[]> area) {
        super(new BorderLayout(0, 12));
        setOpaque(false);
        this.tripStart = tripStart;
        this.tripEnd = tripEnd;
        this.area = area;

        RoundButton add = new RoundButton("Add a stop", RoundButton.Style.OUTLINE).icon("plus").small();
        add.addActionListener(e -> addStop());
        JPanel foot = Theme.clear(new BorderLayout());
        foot.add(add, BorderLayout.WEST);
        foot.add(count, BorderLayout.EAST);

        // the list sits in a soft box so it reads as one block inside the big form
        RoundPanel box = new RoundPanel(new BorderLayout(), 12, new Color(0xFAFBFC), Theme.BORDER);
        box.setBorder(Theme.pad(14, 16, 14, 16));
        box.add(list);
        add(box, BorderLayout.CENTER);
        add(foot, BorderLayout.SOUTH);
        refresh();
    }

    // ===== loading / reading =====

    /** Starts empty (new trip). */
    public void clear() {
        stops.clear();
        newPhotos.clear();
        removedIds.clear();
        refresh();
    }

    /** Starts with the trip's saved stops (editing). */
    public void load(List<Activity> saved) {
        clear();
        for (Activity a : saved) stops.add(a.copy());
        refresh();
    }

    public List<Activity> getStops() { return stops; }
    public Map<Activity, File> getNewPhotos() { return newPhotos; }
    public List<Integer> getRemovedIds() { return removedIds; }

    /** Stops whose day is outside the trip dates (e.g. after the dates were changed). */
    public List<Activity> outside(LocalDate start, LocalDate end) {
        List<Activity> out = new ArrayList<>();
        for (Activity a : stops) if (a.getDate().isBefore(start) || a.getDate().isAfter(end)) out.add(a);
        return out;
    }

    /** Call when the trip dates change, so "Day 1, Day 2..." stay right. */
    public void datesChanged() { refresh(); }

    // ===== editing =====

    private void addStop() {
        Activity s = new Activity();
        // suggest the day of the last stop, two hours later (or 8:00 AM on the first day)
        Activity last = stops.stream().max(Comparator.comparing(Activity::getDate).thenComparing(Activity::getTime)).orElse(null);
        LocalDate start = tripStart.get(), end = tripEnd.get();
        if (last != null && !last.getDate().isBefore(start) && !last.getDate().isAfter(end)) {
            s.setDate(last.getDate());
            s.setTime(last.getTime().isAfter(LocalTime.of(21, 0)) ? last.getTime() : last.getTime().plusHours(2));
        } else {
            s.setDate(start);
            s.setTime(LocalTime.of(8, 0));
        }
        StopForm.Result r = open(s);
        if (r == null) return;
        stops.add(r.stop);
        if (r.newPhoto != null) newPhotos.put(r.stop, r.newPhoto);
        refresh();
    }

    private void editStop(Activity a) {
        StopForm.Result r = open(a);
        if (r == null) return;
        int i = stops.indexOf(a);
        File oldNew = newPhotos.remove(a);
        stops.set(i, r.stop);
        File photo = r.newPhoto != null ? r.newPhoto : (r.stop.hasPhoto() ? oldNew : null);
        if (photo != null) newPhotos.put(r.stop, photo);
        refresh();
    }

    private void removeStop(Activity a) {
        if (!Theme.confirm(this, "Remove this stop?", "\"" + a.getTitle() + "\" will be taken off the itinerary.", "Remove", true)) return;
        stops.remove(a);
        newPhotos.remove(a);
        if (a.getId() > 0) removedIds.add(a.getId());   // deleted from the database when the trip is saved
        refresh();
    }

    private StopForm.Result open(Activity a) {
        Double[] pin = area.get();
        return StopForm.open(this, a, tripStart.get(), tripEnd.get(), pin == null ? null : pin[0], pin == null ? null : pin[1]);
    }

    private void refresh() {
        list.show(stops, tripStart.get(), new ItineraryView.Actions() {
            @Override public void edit(Activity a) { editStop(a); }
            @Override public void remove(Activity a) { removeStop(a); }
        }, "No stops yet. Add the places you'll visit, with a time, a photo and a map pin.");
        int n = stops.size();
        count.setText(n == 0 ? "Optional" : n + (n == 1 ? " stop" : " stops"));
        revalidate();
        repaint();
    }
}
