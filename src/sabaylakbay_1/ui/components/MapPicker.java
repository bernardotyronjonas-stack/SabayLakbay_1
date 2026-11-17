package sabaylakbay_1.ui.components;

import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Search box + map for choosing where a trip goes (used in Create / Edit trip).
 * Type a place and press Search, or just click the map to drop the pin.
 *
 *   MapPicker p = new MapPicker();   p.setPin(lat, lng);   p.getLat();  p.getLng();  (null = no pin)
 *
 * Place search uses the free OpenStreetMap search (Nominatim), limited to the Philippines.
 */
public class MapPicker extends JPanel {

    private final MapView map = new MapView(true);
    private final RoundTextField search = new RoundTextField("Search a place, e.g. Mayon Volcano, Albay").withIcon("search");
    private final RoundButton find = new RoundButton("Search", RoundButton.Style.OUTLINE).icon("search");
    private final JLabel status = Theme.label(" ", Font.PLAIN, 12, Theme.TEXT_GRAY);
    private final JLabel removePin;
    private String placeName;              // e.g. "Quezon City, Metro Manila" (from search or the map)
    private boolean fromSearch;            // true while a search result is being pinned
    private int lookupId;                  // ignores old place-name answers

    public MapPicker() {
        super(new BorderLayout(0, 10));
        setOpaque(false);

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        search.setPreferredSize(new Dimension(300, 40));
        top.add(search, BorderLayout.CENTER);
        find.setPreferredSize(new Dimension(128, 40));
        top.add(find, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        map.setPreferredSize(new Dimension(600, 340));
        add(map, BorderLayout.CENTER);

        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        foot.add(status, BorderLayout.WEST);
        removePin = Theme.link("Remove pin", map::clearPin);
        foot.add(removePin, BorderLayout.EAST);
        add(foot, BorderLayout.SOUTH);

        find.addActionListener(e -> runSearch());
        search.addActionListener(e -> runSearch());
        map.onPinChange(() -> {
            if (!fromSearch) {
                placeName = null;
                if (map.hasPin()) lookupName(map.getLat(), map.getLng());
            }
            showStatus();
        });
        showStatus();
    }

    /** Hides the search row (when the address is typed in fields instead). */
    public MapPicker hideSearch() {
        ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.NORTH).setVisible(false);
        return this;
    }

    /**
     * Looks up an address on the map and drops the pin there. Tries each text in order
     * (e.g. barangay + city, then just the city, then the province) and stops at the first match.
     */
    public void findAddress(String... queries) {
        status.setText("Looking for the address on the map...");
        status.setForeground(Theme.TEXT_GRAY);
        new SwingWorker<Place, Void>() {
            @Override protected Place doInBackground() throws Exception {
                for (String q : queries) {
                    if (q == null || q.isBlank()) continue;
                    List<Place> found = lookup(q);
                    if (!found.isEmpty()) return found.get(0);
                }
                return null;
            }
            @Override protected void done() {
                try {
                    Place p = get();
                    if (p == null) {
                        status.setText("This address isn't on the map. Click the map to drop the pin yourself.");
                        status.setForeground(Theme.RED);
                        return;
                    }
                    fromSearch = true;
                    lookupId++;
                    placeName = shortName(p.name);
                    map.setView(p.lat, p.lng, 14);
                    map.setPin(p.lat, p.lng);
                    fromSearch = false;
                    showStatus();
                } catch (Exception ex) {
                    fromSearch = false;
                    status.setText("The map needs internet. Your address is still saved without a pin.");
                    status.setForeground(Theme.RED);
                }
            }
        }.execute();
    }

    /** Short name of the pinned place, or null when it isn't known yet. */
    public String getPlaceName() { return placeName; }

    public void setPlaceName(String name) { placeName = name; showStatus(); }

    // ===== place name for a dropped pin (OpenStreetMap reverse search) =====
    private void lookupName(double lat, double lng) {
        int id = ++lookupId;
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                String url = System.getProperty("sabaylakbay.reverse", "https://nominatim.openstreetmap.org/reverse")
                        + "?format=json&zoom=10&accept-language=en&lat=" + lat + "&lon=" + lng;
                String json = fetch(url);
                Matcher m = Pattern.compile("\"display_name\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
                return m.find() ? shortName(unescape(m.group(1))) : null;
            }
            @Override protected void done() {
                if (id != lookupId) return;
                try { placeName = get(); } catch (Exception ignored) { placeName = null; }
                showStatus();
            }
        }.execute();
    }

    private static String fetch(String url) throws Exception {
        URLConnection c = URI.create(url).toURL().openConnection();
        c.setRequestProperty("User-Agent", "SabayLakbay/1.0 (student travel app)");
        c.setConnectTimeout(8000);
        c.setReadTimeout(10000);
        try (InputStream in = c.getInputStream()) { return new String(in.readAllBytes(), StandardCharsets.UTF_8); }
    }

    /** "Quezon City, Eastern Manila District, Metro Manila, 1100, Philippines" -> "Quezon City, Metro Manila". */
    static String shortName(String full) {
        if (full == null) return null;
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (String p : full.split(",")) {
            String t = p.trim();
            if (t.isEmpty() || t.equals("Philippines") || t.matches("\\d+") || t.endsWith("District")) continue;
            parts.add(t);
        }
        if (parts.isEmpty()) return null;
        return parts.size() == 1 ? parts.get(0) : parts.get(0) + ", " + parts.get(parts.size() - 1);
    }

    // ===== values =====
    public Double getLat() { return map.getLat(); }
    public Double getLng() { return map.getLng(); }

    /** Shows an existing pin (or the whole Philippines when null). */
    public void setPin(Double lat, Double lng) {
        if (lat != null && lng != null) {
            fromSearch = true;
            map.setView(lat, lng, 11);
            map.setPin(lat, lng);
            fromSearch = false;
            showStatus();
        } else {
            placeName = null;
            map.clearPin();
            map.setView(12.3, 122.6, 5);
        }
    }

    /** Puts text in the search box (e.g. the destination the user typed). */
    public void setSearchText(String s) { if (search.getText().isBlank()) search.setText(s); }

    private void showStatus() {
        if (map.hasPin()) {
            status.setText(placeName != null ? "Pinned: " + placeName : String.format("Pinned at %.5f, %.5f", map.getLat(), map.getLng()));
            status.setForeground(Theme.GREEN);
        } else {
            status.setText("Click the map to drop a pin. Drag to move, scroll to zoom.");
            status.setForeground(Theme.TEXT_GRAY);
        }
        removePin.setVisible(map.hasPin());
    }

    // ===== place search =====
    private void runSearch() {
        String q = search.getText().trim();
        if (q.isEmpty()) { search.requestFocusInWindow(); return; }
        find.setEnabled(false);
        find.setText("Searching");
        new SwingWorker<List<Place>, Void>() {
            @Override protected List<Place> doInBackground() throws Exception { return lookup(q); }
            @Override protected void done() {
                find.setEnabled(true);
                find.setText("Search");
                try {
                    List<Place> places = get();
                    if (places.isEmpty()) {
                        status.setText("No place found for \"" + q + "\". Try another name, or click the map.");
                        status.setForeground(Theme.RED);
                    } else if (places.size() == 1) {
                        go(places.get(0));
                    } else {
                        choose(places);
                    }
                } catch (Exception ex) {
                    status.setText("Search needs internet. You can still click the map to drop the pin.");
                    status.setForeground(Theme.RED);
                }
            }
        }.execute();
    }

    private void go(Place p) {
        fromSearch = true;
        lookupId++;
        placeName = shortName(p.name);
        map.setView(p.lat, p.lng, 12);
        map.setPin(p.lat, p.lng);
        fromSearch = false;
        showStatus();
    }

    /** Drop-down list of the places found. */
    private void choose(List<Place> places) {
        JPopupMenu menu = new JPopupMenu();
        for (Place p : places) {
            String[] parts = p.name.split(",", 2);
            JMenuItem item = new JMenuItem("<html><b>" + Theme.escape(parts[0].trim()) + "</b>"
                    + (parts.length > 1 ? "<br><span style='color:#6B7280'>" + Theme.escape(shorten(parts[1].trim(), 70)) + "</span>" : "")
                    + "</html>");
            item.setIcon(sabaylakbay_1.ui.Icons.get("pin", 16, Theme.ORANGE));
            item.setIconTextGap(10);
            item.addActionListener(e -> go(p));
            menu.add(item);
        }
        menu.show(search, 0, search.getHeight() + 4);
    }

    private static String shorten(String s, int max) { return s.length() <= max ? s : s.substring(0, max - 1) + "\u2026"; }

    static final class Place {
        final double lat, lng;
        final String name;
        Place(double lat, double lng, String name) { this.lat = lat; this.lng = lng; this.name = name; }
    }

    private static final Pattern ITEM = Pattern.compile(
            "\"lat\"\\s*:\\s*\"(-?[0-9.]+)\"\\s*,\\s*\"lon\"\\s*:\\s*\"(-?[0-9.]+)\".*?\"display_name\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"",
            Pattern.DOTALL);

    /** Asks OpenStreetMap's place search. Runs in the background (not on the screen thread). */
    static List<Place> lookup(String q) throws Exception {
        String url = System.getProperty("sabaylakbay.search", "https://nominatim.openstreetmap.org/search")
                + "?format=json&limit=6&countrycodes=ph&accept-language=en&q=" + URLEncoder.encode(q, StandardCharsets.UTF_8);
        URLConnection c = URI.create(url).toURL().openConnection();
        c.setRequestProperty("User-Agent", "SabayLakbay/1.0 (student travel app)");
        c.setConnectTimeout(8000);
        c.setReadTimeout(10000);
        String json;
        try (InputStream in = c.getInputStream()) { json = new String(in.readAllBytes(), StandardCharsets.UTF_8); }
        List<Place> out = new ArrayList<>();
        for (String obj : json.split("\\}\\s*,\\s*\\{")) {
            Matcher m = ITEM.matcher(obj);
            if (m.find()) out.add(new Place(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)), unescape(m.group(3))));
        }
        return out;
    }

    private static String unescape(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                if (n == 'u' && i + 4 < s.length()) { b.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16)); i += 4; }
                else b.append(n == 'n' ? '\n' : n);
            } else b.append(ch);
        }
        return b.toString();
    }

}
