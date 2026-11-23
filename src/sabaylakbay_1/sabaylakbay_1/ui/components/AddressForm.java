package sabaylakbay_1.ui.components;

import sabaylakbay_1.model.User;
import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * Philippine home address + optional map pin (used in Register, Profile and the first-login setup).
 *   Province (list) | City / Municipality
 *   Barangay        | ZIP code
 *   House no., street, building (optional) - its street name is used to find the address on the map
 *   [Find on map]  map with the pin (optional, used for "trips near you")
 * The typed address is what gets saved, so places that aren't on the map still work.
 *
 *   AddressForm f = new AddressForm(true);   f.load(user);   if (f.validateAll()) f.applyTo(user);
 */
public class AddressForm extends JPanel {

    /** All provinces of the Philippines (82) plus Metro Manila. */
    public static final String[] PROVINCES = {"Metro Manila",
            "Abra", "Agusan del Norte", "Agusan del Sur", "Aklan", "Albay", "Antique", "Apayao", "Aurora",
            "Basilan", "Bataan", "Batanes", "Batangas", "Benguet", "Biliran", "Bohol", "Bukidnon", "Bulacan",
            "Cagayan", "Camarines Norte", "Camarines Sur", "Camiguin", "Capiz", "Catanduanes", "Cavite", "Cebu",
            "Cotabato", "Davao de Oro", "Davao del Norte", "Davao del Sur", "Davao Occidental", "Davao Oriental",
            "Dinagat Islands", "Eastern Samar", "Guimaras", "Ifugao", "Ilocos Norte", "Ilocos Sur", "Iloilo",
            "Isabela", "Kalinga", "La Union", "Laguna", "Lanao del Norte", "Lanao del Sur", "Leyte",
            "Maguindanao del Norte", "Maguindanao del Sur", "Marinduque", "Masbate", "Misamis Occidental",
            "Misamis Oriental", "Mountain Province", "Negros Occidental", "Negros Oriental", "Northern Samar",
            "Nueva Ecija", "Nueva Vizcaya", "Occidental Mindoro", "Oriental Mindoro", "Palawan", "Pampanga",
            "Pangasinan", "Quezon", "Quirino", "Rizal", "Romblon", "Samar", "Sarangani", "Siquijor", "Sorsogon",
            "South Cotabato", "Southern Leyte", "Sultan Kudarat", "Sulu", "Surigao del Norte", "Surigao del Sur",
            "Tarlac", "Tawi-Tawi", "Zambales", "Zamboanga del Norte", "Zamboanga del Sur", "Zamboanga Sibugay"};

    private final JComboBox<String> province = new JComboBox<>();
    private final RoundTextField city = new RoundTextField("e.g. Quezon City");
    private final RoundTextField barangay = new RoundTextField("e.g. Pinyahan");
    private final RoundTextField zip = new RoundTextField("e.g. 1100");
    private final RoundTextField street = new RoundTextField("e.g. 12 Mapagmahal St., Unit 4B");
    private final MapPicker map = new MapPicker().hideSearch();
    private final JLabel errProvince = err(), errCity = err(), errBarangay = err(), errZip = err();

    /** @param bigMap true = taller map (sign-up page), false = smaller (inside a box) */
    public AddressForm(boolean bigMap) {
        super(new BorderLayout(0, 0));
        setOpaque(false);
        province.addItem("Select province");
        for (String p : PROVINCES) province.addItem(p);
        province.setMaximumRowCount(12);
        province.setPreferredSize(new Dimension(260, 44));
        province.addActionListener(e -> errProvince.setText(" "));
        onType(city, () -> errCity.setText(" "));
        onType(barangay, () -> errBarangay.setText(" "));
        // search the map by itself after typing the street or barangay (no need to press Find on map)
        java.awt.event.FocusAdapter autoFind = new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (!city.getText().isBlank() || province.getSelectedIndex() > 0) findOnMap();
            }
        };
        street.addFocusListener(autoFind);
        barangay.addFocusListener(autoFind);
        onType(zip, () -> errZip.setText(" "));

        JPanel fields = new JPanel(new GridLayout(0, 2, 14, 0));
        fields.setOpaque(false);
        fields.add(field("Province", province, errProvince));
        fields.add(field("City / Municipality", city, errCity));
        fields.add(field("Barangay", barangay, errBarangay));
        fields.add(field("ZIP code (optional)", zip, errZip));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(fields, BorderLayout.NORTH);
        top.add(field("House no., street, building (optional)", street, null), BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // map pin (optional)
        JPanel pin = new JPanel(new BorderLayout(0, 8));
        pin.setOpaque(false);
        pin.setBorder(new EmptyBorder(6, 0, 0, 0));
        JPanel ph = new JPanel(new BorderLayout());
        ph.setOpaque(false);
        JPanel words = Theme.vbox();
        words.add(Theme.left(Theme.label("Pin on the map (optional)", Theme.MEDIUM, 13, new Color(0x344054))));
        words.add(Theme.gap(2));
        words.add(Theme.left(Theme.label("Found from your street, barangay and city. Click the map to fine-tune.", Font.PLAIN, 12, Theme.TEXT_GRAY)));
        ph.add(words, BorderLayout.WEST);
        RoundButton find = new RoundButton("Find on map", RoundButton.Style.OUTLINE).icon("pin").small();
        find.addActionListener(e -> { lastSearch = null; findOnMap(); });
        JPanel fw = new JPanel(new GridBagLayout());
        fw.setOpaque(false);
        fw.add(find);
        ph.add(fw, BorderLayout.EAST);
        pin.add(ph, BorderLayout.NORTH);
        map.setPreferredSize(new Dimension(560, bigMap ? 300 : 230));
        pin.add(map, BorderLayout.CENTER);
        add(pin, BorderLayout.CENTER);
    }

    /** Fills the fields from a user (Profile edit / first-login setup). */
    public void load(User u) {
        if (u == null) return;
        if (u.getProvince() != null) province.setSelectedItem(u.getProvince());
        city.setText(n(u.getCity()));
        barangay.setText(n(u.getBarangay()));
        zip.setText(n(u.getZipCode()));
        street.setText(n(u.getStreet()));
        map.setPin(u.getLatitude(), u.getLongitude());
    }

    /** Checks the fields and shows mistakes under them. */
    public boolean validateAll() {
        boolean ok = true;
        if (province.getSelectedIndex() <= 0) { errProvince.setText("Choose your province."); ok = false; }
        if (city.getText().isBlank()) { errCity.setText("Enter your city or municipality."); ok = false; }
        if (barangay.getText().isBlank()) { errBarangay.setText("Enter your barangay."); ok = false; }
        String z = zip.getText().trim();
        if (!z.isEmpty() && !z.matches("\\d{4}")) { errZip.setText("ZIP codes have 4 digits."); ok = false; }
        return ok;
    }

    /** Copies the typed address and the pin into the user. */
    public void applyTo(User u) {
        u.setProvince(province.getSelectedIndex() > 0 ? (String) province.getSelectedItem() : null);
        u.setCity(clean(city.getText()));
        u.setBarangay(clean(barangay.getText()).replaceFirst("(?i)^(brgy\\.?|barangay)\\s+", ""));
        u.setZipCode(clean(zip.getText()));
        u.setStreet(clean(street.getText()));
        u.setLatitude(map.getLat());
        u.setLongitude(map.getLng());
    }

    public Double getLat() { return map.getLat(); }
    public Double getLng() { return map.getLng(); }

    /**
     * Looks for the typed address on the map, most exact first:
     *   street + barangay + city  ->  street + city  ->  barangay + city  ->  city  ->  province.
     * The street is cleaned first (house number, unit, block / lot removed), because the map knows
     * street names but not house numbers. Zooms in close when the street is found.
     */
    private void findOnMap() {
        String p = province.getSelectedIndex() > 0 ? (String) province.getSelectedItem() : "";
        String c = city.getText().trim(), b = barangay.getText().trim();
        if (c.isEmpty() && p.isEmpty()) { errCity.setText("Type your city first, then press Find on map."); return; }
        String st = streetForMap(street.getText());
        String key = join(st, b, c, p);
        if (key == null || key.equals(lastSearch)) return;      // same address as last time: keep the pin
        lastSearch = key;
        boolean hasStreet = st != null;
        String clickHouse = " Click your house on the map to place the pin exactly.";
        map.findAddress(
                new String[]{hasStreet ? join(st, b, c, p) : null, hasStreet ? join(st, c, p) : null,
                             join(b, c, p), join(c, p), join(p)},
                new int[]{17, 17, 15, 13, 10},
                new String[]{null, null,
                        hasStreet ? "Couldn't find your street on the map, so the pin is at your barangay." + clickHouse : null,
                        "Couldn't find your " + (hasStreet ? "street or " : "") + "barangay, so the pin is at your city." + clickHouse,
                        "Couldn't find your city, so the pin is at your province." + clickHouse});
    }

    private String lastSearch;

    /**
     * "#12-A Mapagmahal St., Unit 4B"  ->  "Mapagmahal Street"
     * "Blk 3 Lot 5, Phase 2, Sampaguita St."  ->  "Sampaguita Street"
     * Returns null when nothing street-like is left.
     */
    static String streetForMap(String raw) {
        if (raw == null || raw.isBlank()) return null;
        StringBuilder out = new StringBuilder();
        for (String part : raw.split("[,;]")) {
            String x = part.trim();
            // parts that are only a unit, floor, building, block, lot or phase
            if (x.matches("(?i)^(unit|rm\\.?|room|apt\\.?|apartment|bldg\\.?|building|tower|floor|flr\\.?|\\d+(st|nd|rd|th)\\s+(floor|flr)|blk\\.?|block|lot|phase|ph\\.?|purok|zone|sitio)\\b.*")) continue;
            x = x.replaceAll("(?i)\\b(blk|block|lot|phase|ph)\\.?\\s*\\d+[a-z]?\\b", " ");   // "Blk 3 Lot 5" inside a part
            x = x.replaceAll("^#?\\s*\\d+(-?[A-Za-z]\\b(?!\\.))?[-\\s]*", "");                     // leading house number
            x = x.replaceAll("(?i)\\bst\\.?$", "Street").replaceAll("(?i)\\bst\\.\\s", "Street ")
                 .replaceAll("(?i)\\bave?\\.?$", "Avenue").replaceAll("(?i)\\bave?\\.\\s", "Avenue ")
                 .replaceAll("(?i)\\brd\\.?$", "Road").replaceAll("(?i)\\bblvd\\.?$", "Boulevard")
                 .replaceAll("(?i)\\bext\\.?$", "Extension")
                 .replaceAll("\\s+", " ").trim();
            if (x.length() >= 3 && x.matches(".*[A-Za-z]{3}.*")) { out.append(x); break; }   // first real street name
        }
        return out.length() == 0 ? null : out.toString();
    }

    private static String join(String... parts) {
        StringBuilder s = new StringBuilder();
        for (String x : parts) if (x != null && !x.isBlank()) s.append(s.length() > 0 ? ", " : "").append(x.trim());
        return s.length() == 0 ? null : s.toString();
    }

    private static String clean(String s) { return s == null || s.isBlank() ? null : s.trim().replaceAll("\\s+", " "); }
    private static String n(String s) { return s == null ? "" : s; }

    private static JComponent field(String label, JComponent input, JLabel error) {
        JPanel f = Theme.vbox();
        f.add(Theme.left(Theme.label(label, Theme.MEDIUM, 13, new Color(0x344054))));
        f.add(Theme.gap(7));
        input.setPreferredSize(new Dimension(240, 44));
        input.setAlignmentX(LEFT_ALIGNMENT);
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        f.add(input);
        f.add(Theme.gap(4));
        f.add(Theme.left(error != null ? error : Theme.label(" ", Font.PLAIN, 12, Theme.RED)));
        f.setBorder(new EmptyBorder(0, 0, 8, 0));
        return f;
    }

    private static JLabel err() { return Theme.label(" ", Font.PLAIN, 12, Theme.RED); }

    private static void onType(JTextField t, Runnable r) {
        t.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }
}
