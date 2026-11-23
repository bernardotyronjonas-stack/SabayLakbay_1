package sabaylakbay_1;

import sabaylakbay_1.model.Difficulty;
import sabaylakbay_1.model.TravelGroup;
import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Page;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * CREATE GROUP FORM (also used to EDIT a group)
 * Opens from: sidebar "Create Group", or "Edit" on a group you created
 * Fields: trip name, destination, start/end date, category, difficulty, budget per person, max participants,
 *         transportation, meeting time, meeting place, itinerary (stops with photo + map pin),
 *         description, notes for joiners, cover photo
 * Backend: Backend.createGroup(), Backend.updateGroup(), Backend.getItinerary(), Backend.saveItinerary()
 */
public class CreateGroupForm extends Page {

    private static final String[] TRANSPORT = {"Bus", "Van", "Private car", "Boat / Ferry", "Plane", "Motorcycle", "Van + Boat", "Plane + Van"};

    private final RoundTextField title = new RoundTextField("e.g. Mt. Mayon Adventure");
    private final RoundTextField destination = new RoundTextField("e.g. Albay");
    private final DateField start = DateField.date();
    private final DateField end = DateField.date();
    private final RoundTextField budget = new RoundTextField("e.g. 1500");
    private final JSpinner maxMembers = new JSpinner(new SpinnerNumberModel(8, 2, 100, 1));
    // how travelers join: the organizer approves each one (default), or verified travelers join right away
    private final OptionCard requestJoin = new OptionCard("users", "Request to join", "You approve each traveler before they join.");
    private final OptionCard openJoin = new OptionCard("check", "Open join", "Verified travelers join right away. Good for big, easy trips.");
    private final JComboBox<String> transport = new JComboBox<>(TRANSPORT);
    private static final String[] CATEGORIES = {"Hiking", "Beach", "Island hopping", "Food trip", "Road trip", "Sightseeing",
            "Camping", "Adventure", "City tour", "Other"};
    private final JComboBox<String> category = new JComboBox<>(CATEGORIES);
    private final JComboBox<Difficulty> difficulty = new JComboBox<>(Difficulty.values());
    private final RoundTextField meetingPlace = new RoundTextField("e.g. Cubao Bus Terminal, Quezon City");
    private final DateField meetingTime = DateField.time();
    private final TextAreaBox description = new TextAreaBox("What will the group do? Places, activities, what's included...", 5);
    private final TextAreaBox notes = new TextAreaBox("Notes for joiners: what to bring, rules, reminders...", 4);
    private final ImageBox preview = new ImageBox(null, "new", 12, true);
    private final MapPicker mapPicker = new MapPicker();   // where the trip goes (optional pin)
    private final MapPicker meetPicker = new MapPicker();  // where the group meets (used for "Groups near you")
    private final JLabel imageName = Theme.label("No photo chosen", Font.PLAIN, 12, Theme.TEXT_GRAY);
    // stops of the trip; new stop maps start on the destination pin
    private final ItineraryEditor itinerary = new ItineraryEditor(start::getDate, end::getDate,
            () -> mapPicker.getLat() == null ? null : new Double[]{mapPicker.getLat(), mapPicker.getLng()});

    private final JLabel pageTitle = Theme.title("Create Travel Group");
    private final JLabel pageSub = Theme.subtitle("Fill in the trip details. Travelers can send a request to join.");
    private final RoundButton save = new RoundButton("Create Group");

    private TravelGroup editing;
    private String imagePath;

    public CreateGroupForm(MainDashboard app) {
        super(app);
        JPanel box = new Theme.ScrollableBox(null);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(Theme.pad(28, 32, 32, 32));

        box.add(Theme.left(pageTitle));
        box.add(Theme.gap(4));
        box.add(Theme.left(pageSub));
        box.add(Theme.gap(20));

        RoundPanel card = RoundPanel.card(new GridBagLayout());
        card.setBorder(Theme.pad(24, 26, 24, 26));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 16, 18);

        for (JComboBox<?> cb : new JComboBox<?>[]{category, difficulty}) {
            cb.setFont(Theme.font(Font.PLAIN, 13));
            cb.setPreferredSize(new Dimension(150, 40));
        }
        category.setEditable(true);
        transport.setEditable(true);
        transport.setFont(Theme.font(Font.PLAIN, 13));
        transport.setPreferredSize(new Dimension(150, 40));
        // trips can't start in the past, and the end date follows the start date
        start.setRange(LocalDate.now(), null);
        end.setRange(LocalDate.now(), null);
        start.onChange(() -> {
            end.setRange(start.getDate(), null);
            if (end.getDate().isBefore(start.getDate())) end.setDate(start.getDate());
            itinerary.datesChanged();
        });
        end.onChange(itinerary::datesChanged);
        maxMembers.setFont(Theme.font(Font.PLAIN, 13));
        maxMembers.setPreferredSize(new Dimension(100, 40));

        int y = 0;
        c.gridy = y++; c.gridx = 0; c.gridwidth = 2; card.add(section("Trip basics"), c);
        c.gridwidth = 1;
        c.gridy = y; c.gridx = 0; card.add(field("Trip name *", title), c);
        c.gridx = 1; card.add(field("Destination *", destination), c);
        c.gridy = ++y; c.gridx = 0; card.add(field("Start date *", start), c);
        c.gridx = 1; card.add(field("End date *", end), c);
        c.gridy = ++y; c.gridx = 0; card.add(field("Category *", category), c);
        c.gridx = 1; card.add(field("Difficulty *", difficulty), c);
        c.gridy = ++y; c.gridx = 0; card.add(field("Estimated budget per person (\u20B1) *", budget), c);
        c.gridx = 1; card.add(field("Maximum participants *", maxMembers), c);
        ButtonGroup joinGroup = new ButtonGroup();
        joinGroup.add(requestJoin);
        joinGroup.add(openJoin);
        requestJoin.setSelected(true);
        JPanel joinRow = Theme.clear(new GridLayout(1, 2, 16, 0));
        joinRow.add(requestJoin);
        joinRow.add(openJoin);
        c.gridy = ++y; c.gridx = 0; c.gridwidth = 2; card.add(field("How do travelers join? *", joinRow), c);
        c.gridwidth = 1;

        c.gridy = ++y; c.gridx = 0; c.gridwidth = 2; card.add(section("Pin the destination"), c);
        c.gridy = ++y; card.add(mapPicker, c);
        c.gridwidth = 1;
        // the destination typed above is used as the map search text
        destination.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { mapPicker.setSearchText(destination.getText().trim()); }
        });

        c.gridy = ++y; c.gridx = 0; c.gridwidth = 2; card.add(section("Meet up"), c);
        c.gridwidth = 1;
        c.gridy = ++y; c.gridx = 0; card.add(field("Mode of transportation", transport), c);
        c.gridx = 1; card.add(field("Meeting time", meetingTime), c);
        c.gridy = ++y; c.gridx = 0; c.gridwidth = 2; card.add(field("Meeting place", meetingPlace), c);
        c.gridy = ++y; card.add(field("Pin the meeting place (travelers near it will see your trip)", meetPicker), c);
        meetingPlace.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { meetPicker.setSearchText(meetingPlace.getText().trim()); }
        });

        c.gridy = ++y; card.add(section("Itinerary"), c);
        c.gridy = ++y; card.add(Theme.label("Plan the trip stop by stop. Joiners see it on the trip page before they ask to join.",
                Font.PLAIN, 12, Theme.TEXT_GRAY), c);
        c.gridy = ++y; card.add(itinerary, c);

        c.gridy = ++y; card.add(section("Details"), c);
        c.gridy = ++y; card.add(field("Trip description", description), c);
        c.gridy = ++y; card.add(field("Notes for joiners", notes), c);

        // photo picker
        preview.setPreferredSize(new Dimension(180, 110));
        RoundButton choose = new RoundButton("Choose photo", RoundButton.Style.OUTLINE).icon("image").small();
        choose.addActionListener(e -> choosePhoto());
        JPanel ph = Theme.clear(new BorderLayout(16, 0));
        ph.add(preview, BorderLayout.WEST);
        JPanel pr = Theme.vbox();
        pr.add(Theme.left(choose));
        pr.add(Theme.gap(8));
        pr.add(Theme.left(imageName));
        pr.add(Theme.gap(4));
        pr.add(Theme.left(Theme.label("JPG or PNG, landscape looks best. No photo? We'll show one of the destination.",
                Font.PLAIN, 11, Theme.TEXT_GRAY)));
        ph.add(pr);
        c.gridy = ++y; card.add(field("Cover photo", ph), c);

        // payment note
        RoundPanel note = new RoundPanel(new BorderLayout(10, 0), 12, Theme.BLUE_LIGHT, null);
        note.setBorder(Theme.pad(12, 14, 12, 14));
        note.add(new JLabel(Icons.get("info", 18, Theme.BLUE)), BorderLayout.WEST);
        note.add(Theme.label("The budget is only an estimate. Payments are settled within the group, not through the app.",
                Font.PLAIN, 12, Theme.BLUE));
        c.gridy = ++y; c.insets = new Insets(4, 0, 0, 18); card.add(note, c);

        card.setAlignmentX(LEFT_ALIGNMENT);
        box.add(card);
        box.add(Theme.gap(18));

        RoundButton cancel = new RoundButton("Cancel", RoundButton.Style.GHOST);
        cancel.addActionListener(e -> app.showPage(editing == null ? "HOME" : "GROUPS"));
        save.addActionListener(e -> save());
        JPanel btns = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.add(cancel);
        btns.add(save);
        btns.setAlignmentX(LEFT_ALIGNMENT);
        btns.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        box.add(btns);

        add(Theme.scroll(box));
        startCreate();
    }

    private static JComponent section(String text) {
        JLabel l = Theme.label(text, Font.BOLD, 15, Theme.GREEN);
        l.setBorder(Theme.pad(4, 0, 0, 0));
        return l;
    }

    private static JPanel field(String label, JComponent input) {
        JPanel f = Theme.clear(new BorderLayout(0, 6));
        f.add(Theme.label(label, Font.BOLD, 12, Theme.TEXT_DARK), BorderLayout.NORTH);
        f.add(input, BorderLayout.CENTER);
        return f;
    }

    /** Empty form for a new group. */
    public void startCreate() {
        editing = null;
        pageTitle.setText("Create Travel Group");
        pageSub.setText("Fill in the trip details. Travelers can send a request to join.");
        save.setText("Create Group");
        title.setText("");
        destination.setText("");
        start.setDate(LocalDate.now().plusDays(14));
        end.setDate(LocalDate.now().plusDays(16));
        budget.setText("");
        maxMembers.setValue(8);
        requestJoin.setSelected(true);
        transport.setSelectedIndex(0);
        category.setSelectedIndex(0);
        difficulty.setSelectedItem(Difficulty.EASY);
        meetingPlace.setText("");
        meetingTime.setTime(LocalTime.of(6, 0));
        description.setText("");
        notes.setText("");
        setImage(null);
        mapPicker.setPin(null, null);
        meetPicker.setPin(null, null);
        itinerary.clear();
    }

    /** Form filled with an existing group. */
    public void startEdit(TravelGroup g) {
        editing = g.copy();
        pageTitle.setText("Edit Travel Group");
        pageSub.setText("Members will be notified about the changes.");
        save.setText("Save Changes");
        title.setText(g.getTitle());
        destination.setText(g.getDestination());
        mapPicker.setPin(g.getLatitude(), g.getLongitude());
        start.setDate(g.getStartDate());
        end.setDate(g.getEndDate());
        budget.setText(String.valueOf((long) g.getBudgetPerPerson()));
        maxMembers.setValue(g.getMaxMembers());
        (g.isOpenJoin() ? openJoin : requestJoin).setSelected(true);
        transport.setSelectedItem(g.getTransportMode());
        category.setSelectedItem(g.getCategory());
        difficulty.setSelectedItem(g.getDifficulty());
        meetingPlace.setText(g.getMeetingPlace());
        meetPicker.setPin(g.getMeetingLatitude(), g.getMeetingLongitude());
        meetingTime.setTime(g.getMeetingTime());
        description.setText(g.getDescription());
        notes.setText(g.getCreatorNotes());
        setImage(g.getImagePath());
        // BACKEND: Backend.getItinerary() reads from MySQL
        itinerary.load(Backend.getItinerary(g.getId()));
    }

    private void setImage(String path) {
        imagePath = path;
        preview.setPath(path);
        imageName.setText(path == null ? "No photo chosen"
                : sabaylakbay_1.model.Photos.isStockPhoto(path) ? "Destination photo (Wikimedia Commons)" : new File(path).getName());
    }

    private void choosePhoto() {
        File f = sabaylakbay_1.ui.Dialogs.chooseImage(this, "Choose a cover photo for the trip");
        // BACKEND: upload/copy the file to your server or images folder and store that path instead.
        if (f != null) setImage(f.getAbsolutePath());
    }

    private void save() {
        double b;
        try {
            b = Double.parseDouble(budget.getText().replace(",", "").trim());
        } catch (NumberFormatException ex) {
            Theme.error(this, "Budget must be a number, e.g. 1500.");
            return;
        }
        TravelGroup g = editing != null ? editing : new TravelGroup();
        g.setTitle(title.getText().trim());
        g.setDestination(destination.getText().trim());
        g.setLatitude(mapPicker.getLat());          // null when no pin was dropped
        g.setLongitude(mapPicker.getLng());
        g.setStartDate(start.getDate());
        g.setEndDate(end.getDate());
        g.setBudgetPerPerson(b);
        g.setMaxMembers((Integer) maxMembers.getValue());
        g.setOpenJoin(openJoin.isSelected());
        Object cat = category.getSelectedItem();
        g.setCategory(cat == null ? "" : cat.toString().trim());
        g.setDifficulty((Difficulty) difficulty.getSelectedItem());
        Object t = transport.getSelectedItem();
        g.setTransportMode(t == null ? "" : t.toString().trim());
        g.setMeetingPlace(meetingPlace.getText().trim());
        g.setMeetingLatitude(meetPicker.getLat());   // null when no pin was dropped
        g.setMeetingLongitude(meetPicker.getLng());
        g.setMeetingTime(meetingTime.getTime());
        g.setDescription(description.getText().trim());
        g.setCreatorNotes(notes.getText().trim());
        // a destination photo picked by the app isn't saved, so it follows the destination if it changes
        g.setImagePath(sabaylakbay_1.model.Photos.isStockPhoto(imagePath) ? null : imagePath);

        if (editing == null) {
            g.setCreatorId(Backend.currentUserId());
            if (!validateForm(g)) return;
            // BACKEND: Backend.createGroup() then Backend.saveItinerary() save to MySQL
            if (!Theme.run(this, () -> Backend.createGroup(g))) return;
            if (!saveItinerary(g.getId())) { app.openMyGroups(0); return; }   // the trip exists; the stops can be fixed in Edit
            app.openMyGroups(0);
        } else {
            if (!validateForm(g)) return;
            // BACKEND: Backend.updateGroup() then Backend.saveItinerary() save to MySQL
            if (Theme.run(this, () -> Backend.updateGroup(g)) && saveItinerary(g.getId())) {
                editing = null;
                app.openMyGroups(0);
            }
        }
    }

    /** Saves the stops added, changed or removed in the Itinerary section. */
    private boolean saveItinerary(int groupId) {
        if (itinerary.getStops().isEmpty() && itinerary.getRemovedIds().isEmpty()) return true;
        return Theme.run(this, () -> Backend.saveItinerary(groupId, itinerary.getStops(),
                itinerary.getNewPhotos(), itinerary.getRemovedIds()));
    }

    /** Simple checks done by the front end before calling the backend. */
    private boolean validateForm(TravelGroup g) {
        String error = null;
        if (g.getTitle().isEmpty()) error = "Trip name is required.";
        else if (g.getCategory().isEmpty()) error = "Category is required.";
        else if (g.getDestination().isEmpty()) error = "Destination is required.";
        else if (g.getEndDate().isBefore(g.getStartDate())) error = "End date can't be before the start date.";
        else if (editing == null && g.getStartDate().isBefore(LocalDate.now())) error = "Start date can't be in the past.";
        else if (g.getBudgetPerPerson() <= 0) error = "Budget per person must be more than 0.";
        else if (!itinerary.outside(g.getStartDate(), g.getEndDate()).isEmpty()) {
            java.util.List<sabaylakbay_1.model.Activity> out = itinerary.outside(g.getStartDate(), g.getEndDate());
            error = (out.size() == 1 ? "The stop \"" + out.get(0).getTitle() + "\" is" : out.size() + " itinerary stops are")
                    + " outside the new trip dates. Edit or remove " + (out.size() == 1 ? "it" : "them") + " in the Itinerary section.";
        }
        if (error != null) Theme.error(this, error);
        return error == null;
    }

    @Override
    public void refresh() { /* form keeps what was typed; startCreate/startEdit reset it */ }
}
