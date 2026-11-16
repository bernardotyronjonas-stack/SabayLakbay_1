package sabaylakbay_1.backend;

import sabaylakbay_1.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FAKE DATA so the screens have something to show.
 *
 * Edit the names, places and numbers here to change what the screens show.
 */
final class SampleData {

    private SampleData() { }

    // ===== sample lists =====
    static final Map<Integer, User> USERS = new LinkedHashMap<>();
    static final Map<Integer, TravelGroup> GROUPS = new LinkedHashMap<>();
    static final Map<Integer, Set<Integer>> MEMBERS = new HashMap<>();
    static final List<JoinRequest> REQUESTS = new ArrayList<>();
    static final List<Activity> ACTIVITIES = new ArrayList<>();
    static final List<Expense> EXPENSES = new ArrayList<>();
    static final List<Message> MESSAGES = new ArrayList<>();
    static final List<Review> REVIEWS = new ArrayList<>();
    static final List<Notification> NOTIFICATIONS = new ArrayList<>();
    static final Set<Integer> BLOCKED_BY_ME = new HashSet<>();
    static final List<Report> REPORTS = new ArrayList<>();

    // ===== numbers for the admin charts (sample only) =====
    static final String[] MONTHS = {"Dec", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov"};
    static final int[] NEW_USERS = {38, 52, 47, 66, 81, 74, 92, 118, 105, 131, 146, 163};
    static final int[] TRIPS_CREATED = {9, 12, 11, 16, 21, 19, 24, 31, 28, 33, 37, 41};
    static final int[] TRIPS_COMPLETED = {6, 8, 10, 12, 15, 17, 18, 22, 25, 27, 30, 26};
    static final int[] REPORTS_FILED = {2, 3, 2, 4, 5, 3, 6, 7, 5, 6, 8, 9};
    static final int[] REPORTS_RESOLVED = {2, 2, 2, 3, 5, 3, 5, 6, 5, 5, 7, 6};
    static final String[] TOP_PLACES = {"Albay", "Palawan", "Siargao", "Benguet", "Batanes", "Camarines Sur", "Rizal", "Ilocos Norte"};
    static final int[] TOP_PLACES_TRIPS = {64, 58, 47, 41, 29, 26, 22, 17};

    private static int nextId = 1000;

    // ===== small helpers used by Backend.java =====
    static Set<Integer> memberIds(int groupId) {
        return MEMBERS.computeIfAbsent(groupId, k -> new LinkedHashSet<>());
    }

    static TravelGroup group(int id) {
        TravelGroup g = GROUPS.get(id);
        g.setCurrentMembers(memberIds(id).size());
        g.setCreatorName(USERS.get(g.getCreatorId()).getFullName());
        return g.copy();
    }

    static List<TravelGroup> groups(java.util.function.Predicate<TravelGroup> keep) {
        return GROUPS.keySet().stream().map(SampleData::group).filter(keep)
                .sorted(Comparator.comparing(TravelGroup::getStartDate)).collect(Collectors.toList());
    }

    // ===== the sample data itself =====
    static {
        user(1, "Juan Dela Cruz", "juan", "Traveler. Beach first, mountains second.");
        user(2, "Mark Santos", "mark", "Weekend hiker from QC.");
        user(3, "Ana Reyes", "ana", "Island hopper and food lover.");
        user(4, "Carlo Mendoza", "carlo", "Trail runner, waterfall chaser.");
        user(5, "Bea Lim", "bea", "Surfer. Always down for a road trip.");
        user(6, "Paolo Cruz", "paolo", "Photographer.");
        user(7, "Mika Tan", "mika", "Travel planner by day.");
        user(8, "Jerome Villanueva", "jerome", "Road trips and coffee.");
        user(9, "Lara Gomez", "lara", "Backpacker on a budget.");
        user(10, "Nico Ramos", "nico", "Mountains every month.");
        user(11, "Trish Castillo", "trish", "Beach bum.");
        user(12, "Ken Abad", "ken", "Says he organizes cheap tours.");
        user(13, "Dan Ortega", "dan", "Weekend traveler.");
        user(14, "Ella Navarro", "ella", "First time solo traveler.");
        USERS.get(12).setStatus(AccountStatus.SUSPENDED);

        // admin account (LoginForm sends admins to AdminDashboard)
        user(99, "Admin Team", "admin", "SabayLakbay administrator.");
        USERS.get(99).setRole(UserRole.ADMIN);
        USERS.get(99).setEmail("admin@sabaylakbay.com");

        LocalDate t = LocalDate.now();
        LocalDateTime n = LocalDateTime.now();

        group(1, "Mt. Mayon Adventure", "Albay", t.plusDays(10), t.plusDays(12), 1600, 10, "Bus",
                "Cubao Bus Terminal, Quezon City", LocalTime.of(20, 0),
                "Three days around Mayon: Cagsawa Ruins, Lignon Hill, ATV lava trail and Bicol food trip.",
                "Bring a light jacket and comfy shoes. Budget covers bus, ATV and lodging. Food is separate.",
                "/images/mayon.jpg", 2, n.minusDays(3), 2, 5, 6, 7);
        group(2, "Island Hopping - Caramoan", "Camarines Sur", t.plusDays(19), t.plusDays(22), 1200, 8, "Van + Boat",
                "Naga City Plaza", LocalTime.of(5, 0),
                "Hop around Caramoan's islands: Matukad, Lahos and Sabitang Laya. Snorkeling and beach camping.",
                "Waterproof bag is a must. Bring your own snorkel if you have one.",
                "/images/caramoan.jpg", 3, n.minusDays(1), 3, 4, 5, 6, 7, 8, 12);
        group(3, "Trekking + Falls", "Tanay, Rizal", t.plusDays(6), t.plusDays(6), 900, 6, "Van",
                "Starmall EDSA Shaw, Mandaluyong", LocalTime.of(4, 30),
                "Day hike to Daranak and Batlag Falls with a short trek. Beginner friendly.",
                "Wear trekking sandals. We leave on time, so please be early.",
                "/images/falls.jpg", 4, n.minusDays(5), 4, 1, 6);
        group(4, "Baguio Food Trip", "Benguet", t.plusDays(25), t.plusDays(27), 2500, 8, "Private car",
                "SM North EDSA, The Block entrance", LocalTime.of(6, 0),
                "Cafes, Good Shepherd, night market, strawberry farm. Chill trip, lots of eating.",
                "Two cars, we split gas and toll. Bring a jacket, it gets cold at night.",
                "/images/baguio.jpg", 1, n.minusDays(2), 1, 5);
        group(5, "Siargao Surf Week", "Surigao del Norte", t.plusDays(40), t.plusDays(45), 8500, 6, "Plane + Van",
                "NAIA Terminal 3", LocalTime.of(5, 30),
                "Surf lessons at Cloud 9, island hopping and Sugba Lagoon.",
                "Flights not included in the budget. Book your own ticket after approval.",
                "/images/siargao.jpg", 5, n.minusDays(8), 5, 2, 3, 6, 7, 8);
        group(6, "Batanes Escape", "Batanes", t.plusDays(60), t.plusDays(64), 15000, 10, "Plane",
                "NAIA Terminal 2", LocalTime.of(4, 0),
                "North and South Batan tour, Sabtang Island and Marlboro Hills.",
                "Weather can delay flights. Keep your schedule flexible.",
                "/images/batanes.jpg", 6, n.minusDays(10), 6, 3, 8);
        group(7, "Coron Weekend", "Palawan", t.minusDays(20), t.minusDays(17), 5200, 8, "Plane + Boat",
                "NAIA Terminal 3", LocalTime.of(5, 0),
                "Kayangan Lake, Twin Lagoon and the shipwreck snorkel tour.",
                "Done! Please leave a review for the group.",
                "/images/coron.jpg", 7, n.minusDays(40), 7, 1, 3, 4);
        group(8, "Pagudpud Road Trip", "Ilocos Norte", t.plusDays(14), t.plusDays(16), 3000, 5, "Private car",
                "NLEX Balintawak Toll Plaza", LocalTime.of(3, 0),
                "Bangui windmills, Kapurpurawan, Blue Lagoon and empanada stops.",
                "We split gas and toll equally.",
                "/images/pagudpud.jpg", 8, n.minusHours(20), 8, 2);

        request(3, 1, "Hi! First time to join, excited!", RequestStatus.APPROVED, n.minusDays(4));
        request(8, 1, "Can I join? I can help drive.", RequestStatus.PENDING, n.minusHours(10));
        request(6, 1, "Hello!", RequestStatus.REJECTED, n.minusDays(6));
        request(4, 8, "Hi, I'm from QC too. Can I join?", RequestStatus.PENDING, n.minusHours(5));
        request(4, 7, "Looks fun! I love cafes.", RequestStatus.PENDING, n.minusHours(2));

        activity(3, t.plusDays(6), 4, 30, "Meet up and leave Manila", "Starmall EDSA Shaw", 0);
        activity(3, t.plusDays(6), 7, 0, "Breakfast", "Tanay Public Market", 120);
        activity(3, t.plusDays(6), 8, 30, "Trek to Daranak Falls", "Daranak Falls", 100);
        activity(3, t.plusDays(6), 11, 0, "Swim at Batlag Falls", "Batlag Falls", 80);
        activity(3, t.plusDays(6), 13, 0, "Lunch", "Tanay", 200);
        activity(3, t.plusDays(6), 15, 0, "Head back to Manila", "", 0);
        activity(4, t.plusDays(25), 6, 0, "Leave Manila", "SM North EDSA", 0);
        activity(4, t.plusDays(25), 13, 0, "Lunch at Good Taste", "Baguio City", 300);
        activity(4, t.plusDays(26), 7, 0, "Strawberry farm", "La Trinidad", 150);
        activity(4, t.plusDays(26), 18, 0, "Night market", "Harrison Road", 300);
        activity(7, t.minusDays(20), 9, 0, "Kayangan Lake tour", "Coron Island", 1500);

        expense(3, "Van rental (shared)", 450);
        expense(3, "Entrance fees", 180);
        expense(3, "Food", 270);
        expense(4, "Gas and toll (shared)", 900);
        expense(4, "Lodging (2 nights)", 1200);
        expense(4, "Food", 400);
        expense(7, "Flights", 3500);
        expense(7, "Tours and boat", 1700);

        msg(3, 4, 0, "Hi everyone! Welcome to Trekking + Falls.", false, n.minusDays(4));
        msg(3, 1, 0, "Hello! Excited for this.", false, n.minusDays(4).plusHours(1));
        msg(3, 6, 0, "Same here. Should I bring a dry bag?", false, n.minusDays(3));
        msg(3, 4, 0, "Yes please, bring one for your phone.", false, n.minusDays(3).plusMinutes(20));
        msg(3, 4, 0, "Reminder: meet at Starmall Shaw at 4:30 AM sharp. Van leaves at 4:45.", true, n.minusDays(1));
        msg(4, 1, 0, "Welcome Bea! Planning to leave at 6 AM.", false, n.minusDays(1));
        msg(4, 5, 0, "Perfect, I'll bring snacks for the road.", false, n.minusHours(20));
        msg(7, 7, 0, "Thanks for joining Coron, guys! Please leave a review.", true, n.minusDays(16));
        msg(0, 2, 1, "Hi Juan! Saw your request on Batanes. Want to join Mayon instead? We still have slots.", false, n.minusDays(2));
        msg(0, 1, 2, "Oh nice, I'll check it out. Thanks Mark!", false, n.minusDays(2).plusHours(2));

        review(7, 7, 1, 5, "Super chill and always on time. Would travel again!", n.minusDays(15));
        review(7, 3, 1, 4, "Fun to be with, good photos.", n.minusDays(15));
        review(7, 4, 0, 5, "Well planned trip, Mika did a great job.", n.minusDays(16));
        review(5, 2, 5, 5, "Great surf buddy and super friendly.", n.minusDays(30));
        review(1, 6, 2, 5, "Mark plans everything well.", n.minusDays(50));
        review(1, 5, 2, 4, "Good trip lead, a bit strict with time but worth it.", n.minusDays(50));
        review(2, 4, 3, 5, "Ana knows the best spots.", n.minusDays(45));
        review(2, 1, 4, 5, "Carlo is a great guide on trails.", n.minusDays(60));

        int scamMsg = msg(2, 12, 0, "Pay me first through GCash so I can lock your slot. Don't tell the organizer.", false, n.minusDays(2));

        report(1, 3, ReportTarget.USER, 12, "Ken Abad", "Scam or fraud",
                "He asked members to send money to his own GCash before the trip.", ReportStatus.PENDING, null, n.minusHours(6));
        report(2, 5, ReportTarget.MESSAGE, scamMsg, "Message from Ken Abad", "Scam or fraud",
                "This message in the Caramoan group chat asks for payment outside the group.", ReportStatus.UNDER_REVIEW, null, n.minusDays(1));
        report(3, 9, ReportTarget.GROUP, 6, "Batanes Escape", "Fake profile or group",
                "Price looks too low for Batanes, not sure if this is real.", ReportStatus.PENDING, null, n.minusDays(2));
        report(4, 14, ReportTarget.USER, 13, "Dan Ortega", "Harassment or bullying",
                "Kept sending rude messages after I left the group.", ReportStatus.RESOLVED, "Warning sent", n.minusDays(9));
        report(5, 10, ReportTarget.GROUP, 8, "Pagudpud Road Trip", "Safety concern",
                "Leaving at 3 AM with only one driver.", ReportStatus.DISMISSED, "No action needed", n.minusDays(12));
        report(6, 7, ReportTarget.USER, 12, "Ken Abad", "Scam or fraud",
                "Same person collected money in another group and disappeared.", ReportStatus.RESOLVED, "Account suspended", n.minusDays(20));
        report(7, 11, ReportTarget.USER, 6, "Paolo Cruz", "Inappropriate content",
                "Posted photos of members without asking.", ReportStatus.PENDING, null, n.minusHours(30));

        notification(1, "Your request to join \"Trekking + Falls\" was approved!", 3, n.minusDays(4), true);
        notification(1, "Your request to join \"Batanes Escape\" was not approved.", 6, n.minusDays(5), true);
        notification(1, "Trip update in \"Trekking + Falls\": Reminder: meet at Starmall Shaw at 4:30 AM sharp.", 3, n.minusDays(1), false);
        notification(1, "Jerome Villanueva wants to join \"Baguio Food Trip\".", 4, n.minusHours(5), false);
        notification(1, "Mika Tan wants to join \"Baguio Food Trip\".", 4, n.minusHours(2), false);
    }

    private static void user(int id, String name, String username, String bio) {
        User u = new User();
        u.setId(id);
        u.setFullName(name);
        u.setUsername(username);
        u.setEmail(username + "@sabaylakbay.com");
        u.setPhone("0917 000 00" + String.format("%02d", id));
        u.setBio(bio);
        u.setRole(UserRole.TRAVELER);
        u.setStatus(AccountStatus.ACTIVE);
        u.setCreatedAt(LocalDateTime.now().minusDays(id * 17L + 3));
        USERS.put(id, u);
    }

    private static void group(int id, String title, String dest, LocalDate start, LocalDate end, double budget, int max,
                              String transport, String place, LocalTime time, String desc, String notes, String img,
                              int creator, LocalDateTime created, int... memberIds) {
        TravelGroup g = new TravelGroup();
        g.setId(id); g.setTitle(title); g.setDestination(dest); g.setStartDate(start); g.setEndDate(end);
        g.setBudgetPerPerson(budget); g.setMaxMembers(max); g.setTransportMode(transport);
        g.setMeetingPlace(place); g.setMeetingTime(time); g.setDescription(desc); g.setCreatorNotes(notes);
        g.setImagePath(img); g.setCreatorId(creator); g.setStatus(GroupStatus.OPEN); g.setCreatedAt(created);
        GROUPS.put(id, g);
        for (int m : memberIds) memberIds(id).add(m);
    }

    private static void request(int groupId, int userId, String text, RequestStatus st, LocalDateTime at) {
        JoinRequest r = new JoinRequest();
        r.setId(nextId++); r.setGroupId(groupId); r.setGroupTitle(GROUPS.get(groupId).getTitle());
        r.setUserId(userId); r.setUserName(USERS.get(userId).getFullName());
        r.setMessage(text); r.setStatus(st); r.setCreatedAt(at);
        REQUESTS.add(r);
    }

    private static void activity(int groupId, LocalDate d, int h, int m, String title, String loc, double cost) {
        Activity a = new Activity();
        a.setId(nextId++); a.setGroupId(groupId); a.setDate(d); a.setTime(LocalTime.of(h, m));
        a.setTitle(title); a.setLocation(loc); a.setEstimatedCost(cost);
        ACTIVITIES.add(a);
    }

    private static void expense(int groupId, String label, double amount) {
        Expense e = new Expense();
        e.setId(nextId++); e.setGroupId(groupId); e.setLabel(label); e.setAmount(amount);
        EXPENSES.add(e);
    }

    private static int msg(int groupId, int sender, int recipient, String text, boolean ann, LocalDateTime at) {
        Message m = new Message();
        m.setId(nextId++); m.setGroupId(groupId); m.setSenderId(sender); m.setRecipientId(recipient);
        m.setSenderName(USERS.get(sender).getFullName()); m.setContent(text); m.setAnnouncement(ann); m.setSentAt(at);
        MESSAGES.add(m);
        return m.getId();
    }

    private static void report(int id, int reporter, ReportTarget type, int targetId, String targetName, String reason,
                               String details, ReportStatus status, String action, LocalDateTime at) {
        Report r = new Report();
        r.setId(id); r.setReporterId(reporter); r.setReporterName(USERS.get(reporter).getFullName());
        r.setTargetType(type); r.setTargetId(targetId); r.setTargetName(targetName);
        r.setReason(reason); r.setDetails(details); r.setStatus(status); r.setActionTaken(action); r.setCreatedAt(at);
        if (status != ReportStatus.PENDING) r.setReviewedAt(at.plusHours(5));
        REPORTS.add(r);
    }

    private static void review(int groupId, int reviewer, int reviewee, int rating, String comment, LocalDateTime at) {
        Review r = new Review();
        r.setId(nextId++); r.setGroupId(groupId); r.setReviewerId(reviewer);
        r.setReviewerName(USERS.get(reviewer).getFullName()); r.setRevieweeId(reviewee);
        r.setRating(rating); r.setComment(comment); r.setCreatedAt(at);
        REVIEWS.add(r);
    }

    private static void notification(int userId, String text, int groupId, LocalDateTime at, boolean read) {
        Notification x = new Notification();
        x.setId(nextId++); x.setUserId(userId); x.setMessage(text); x.setRelatedGroupId(groupId);
        x.setCreatedAt(at); x.setRead(read);
        NOTIFICATIONS.add(x);
    }
}
