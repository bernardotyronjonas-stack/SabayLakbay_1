package sabaylakbay_1.backend;

import sabaylakbay_1.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * =====================================================================
 *  BACKEND CONNECTION POINTS (front end only for now)
 * =====================================================================
 *  Every screen gets its data and sends its button actions ONLY through the methods below.
 *
 *  Right now there is NO database:
 *    - "GET" methods return sample data from SampleData.java so the screens have something to show.
 *    - Action methods (save, approve, send...) do nothing. They show a small note at the bottom
 *      of the window and print the action in NetBeans' Output window.
 *
 *  Later, the backend replaces the body of each method. The screens don't need to change.
 *  In the screen files, look for the comments that start with  // BACKEND
 *
 *  Sections:
 *    1. Account   2. Main Menu   3. Create and Manage Groups   4. Join Groups
 *    5. Trip Planning   6. Communication   7. Safety and Reviews   8. Notifications   9. Admin
 * =====================================================================
 */
public final class Backend {

    private Backend() { }

    /** The logged-in user. Set by login(). */
    private static User currentUser;

    // =================================================================
    // 1. ACCOUNT
    // =================================================================

    /**
     * REGISTER: create a new account.
     * Rules: username and email must be unique.
     */
    public static void register(User newUser, String password) {
        notConnected("register", newUser.getFullName(), newUser.getUsername(), newUser.getEmail(), newUser.getPhone());
    }

    /**
     * LOG IN: check the username/email and password. Return the user if correct.
     * Throw new RuntimeException("Wrong email/username or password.") if not.
     * Also refuse SUSPENDED / REMOVED accounts: throw new RuntimeException("Your account is suspended.")
     * The user's role (TRAVELER / ADMIN) decides which dashboard opens.
     */
    public static User login(String usernameOrEmail, String password) {
        log("login", usernameOrEmail);
        // FRONT END ONLY: "admin" opens the admin side, anything else logs in as the sample traveler
        currentUser = usernameOrEmail.equalsIgnoreCase("admin") ? SampleData.USERS.get(99) : SampleData.USERS.get(1);
        return currentUser;
    }

    /** FORGOT PASSWORD: send a reset link/code to this email. */
    public static void sendPasswordReset(String email) {
        notConnected("sendPasswordReset", email);
    }

    public static void logout() {
        log("logout");
        currentUser = null;
    }

    public static User getCurrentUser() {
        return currentUser != null ? currentUser : SampleData.USERS.get(1);
    }

    public static int currentUserId() {
        return getCurrentUser().getId();
    }

    public static User getUser(int userId) {
        return SampleData.USERS.get(userId);
    }

    /**
     * EDIT PROFILE: save name, phone and bio.
     */
    public static void updateProfile(User user) {
        notConnected("updateProfile", user.getId(), user.getFullName(), user.getPhone(), user.getBio());
    }

    // =================================================================
    // 2. MAIN MENU (browse, search, filter, sort, view details)
    // =================================================================

    /**
     * HOME PAGE LIST: open groups that are not finished yet, using the search/filter/sort options.
     */
    public static List<TravelGroup> searchGroups(GroupFilter filter) {
        log("searchGroups", "keyword=" + filter.getKeyword(), "from=" + filter.getDateFrom(), "to=" + filter.getDateTo(),
                "maxBudget=" + filter.getMaxBudget(), "minSlots=" + filter.getMinSlots(), "sort=" + filter.getSort());
        List<TravelGroup> list = SampleData.groups(g -> !g.isCancelled() && !g.isCompleted());
        list.sort(Comparator.comparing(TravelGroup::getCreatedAt).reversed());
        return list;   // FRONT END ONLY: same sample list every time (filters not applied)
    }

    /** GROUP DETAILS. */
    public static TravelGroup getGroup(int groupId) {
        return SampleData.group(groupId);
    }

    /**
     * MEMBERS of a group (approved members, including the creator).
     */
    public static List<User> getMembers(int groupId) {
        return SampleData.memberIds(groupId).stream().map(SampleData.USERS::get).collect(Collectors.toList());
    }

    public static boolean isMember(int groupId, int userId) {
        return SampleData.memberIds(groupId).contains(userId);
    }

    /** HEART BUTTON. */
    public static boolean isSaved(int groupId, int userId) {
        return false;
    }

    /**
     * HEART BUTTON clicked.
     */
    public static void setSaved(int groupId, int userId, boolean saved) {
        log("setSaved", groupId, userId, saved);
    }

    // =================================================================
    // 3. CREATE AND MANAGE TRAVEL GROUPS
    // =================================================================

    /**
     * CREATE GROUP.
     * Rules: start date not in the past, end >= start, budget > 0, max members >= 2.
     *        The creator becomes the first member.
     */
    public static void createGroup(TravelGroup g) {
        notConnected("createGroup", g.getTitle(), g.getDestination(), g.getStartDate(), g.getEndDate(),
                g.getBudgetPerPerson(), g.getMaxMembers());
    }

    /**
     * EDIT GROUP. Rule: max members can't go below the current number of members.
     */
    public static void updateGroup(TravelGroup g) {
        notConnected("updateGroup", g.getId(), g.getTitle());
    }

    /**
     * CANCEL GROUP.
     * Then notify all members.
     */
    public static void cancelGroup(int groupId) {
        notConnected("cancelGroup", groupId);
    }

    /** MY GROUPS > Created. */
    public static List<TravelGroup> getGroupsCreatedBy(int userId) {
        return SampleData.groups(g -> g.getCreatorId() == userId);
    }

    /** JOIN REQUESTS a creator must review. */
    public static List<JoinRequest> getPendingRequestsForGroup(int groupId) {
        return SampleData.REQUESTS.stream()
                .filter(r -> r.getGroupId() == groupId && r.getStatus() == RequestStatus.PENDING)
                .collect(Collectors.toList());
    }

    /**
     * APPROVE a join request. Rule: the group must not be full.
     */
    public static void approveRequest(int requestId) {
        notConnected("approveRequest", requestId);
    }

    /**
     * REJECT a join request.
     */
    public static void rejectRequest(int requestId) {
        notConnected("rejectRequest", requestId);
    }

    // =================================================================
    // 4. JOIN TRAVEL GROUPS
    // =================================================================

    /**
     * SEND JOIN REQUEST.
     * Rules: not allowed if the group is full or cancelled, if already a member, or if there is already a PENDING request.
     */
    public static void sendJoinRequest(int groupId, int userId, String message) {
        notConnected("sendJoinRequest", groupId, userId, message);
    }

    /** CANCEL a pending request. */
    public static void cancelJoinRequest(int requestId) {
        notConnected("cancelJoinRequest", requestId);
    }

    /** The latest request of a user for a group (null if none). */
    public static JoinRequest getRequest(int groupId, int userId) {
        JoinRequest last = null;
        for (JoinRequest r : SampleData.REQUESTS) if (r.getGroupId() == groupId && r.getUserId() == userId) last = r;
        return last;
    }

    /** MY GROUPS > My Requests (request status). */
    public static List<JoinRequest> getRequestsByUser(int userId) {
        return SampleData.REQUESTS.stream().filter(r -> r.getUserId() == userId)
                .sorted(Comparator.comparing(JoinRequest::getCreatedAt).reversed()).collect(Collectors.toList());
    }

    /**
     * MY GROUPS > Joined (and joined trips).
     */
    public static List<TravelGroup> getGroupsJoinedBy(int userId) {
        return SampleData.groups(g -> g.getCreatorId() != userId && SampleData.memberIds(g.getId()).contains(userId));
    }

    /** LEAVE GROUP. Rule: the creator can't leave. */
    public static void leaveGroup(int groupId, int userId) {
        notConnected("leaveGroup", groupId, userId);
    }

    // =================================================================
    // 5. TRIP PLANNING
    // =================================================================

    /** ITINERARY. */
    public static List<Activity> getItinerary(int groupId) {
        return SampleData.ACTIVITIES.stream().filter(a -> a.getGroupId() == groupId)
                .sorted(Comparator.comparing(Activity::getDate).thenComparing(Activity::getTime)).collect(Collectors.toList());
    }

    /**
     * ADD ACTIVITY (creator only).
     */
    public static void addActivity(Activity a) {
        notConnected("addActivity", a.getGroupId(), a.getDate(), a.getTime(), a.getTitle(), a.getLocation(), a.getEstimatedCost());
    }

    public static void deleteActivity(int activityId) {
        notConnected("deleteActivity", activityId);
    }

    /** ESTIMATED EXPENSES per person. */
    public static List<Expense> getExpenses(int groupId) {
        return SampleData.EXPENSES.stream().filter(e -> e.getGroupId() == groupId).collect(Collectors.toList());
    }

    public static void addExpense(Expense e) {
        notConnected("addExpense", e.getGroupId(), e.getLabel(), e.getAmount());
    }

    public static void deleteExpense(int expenseId) {
        notConnected("deleteExpense", expenseId);
    }

    /** TRIP UPDATES. */
    public static List<Message> getAnnouncements(int groupId) {
        return SampleData.MESSAGES.stream().filter(m -> m.getGroupId() == groupId && m.isAnnouncement())
                .sorted(Comparator.comparing(Message::getSentAt).reversed()).collect(Collectors.toList());
    }

    // =================================================================
    // 6. COMMUNICATION
    // =================================================================

    /** GROUP CHAT history. */
    public static List<Message> getGroupMessages(int groupId) {
        return SampleData.MESSAGES.stream().filter(m -> m.getGroupId() == groupId)
                .sorted(Comparator.comparing(Message::getSentAt)).collect(Collectors.toList());
    }

    /**
     * SEND GROUP MESSAGE. Rules: only members can send; announcement = true only for the creator.
     * If announcement: notify all members.
     */
    public static void sendGroupMessage(int groupId, int senderId, String content, boolean announcement) {
        notConnected("sendGroupMessage", groupId, senderId, content, announcement);
    }

    /**
     * DIRECT MESSAGES between two users (e.g. contacting the group creator).
     */
    public static List<Message> getDirectMessages(int userA, int userB) {
        return SampleData.MESSAGES.stream().filter(m -> m.getGroupId() == 0 &&
                        ((m.getSenderId() == userA && m.getRecipientId() == userB) || (m.getSenderId() == userB && m.getRecipientId() == userA)))
                .sorted(Comparator.comparing(Message::getSentAt)).collect(Collectors.toList());
    }

    /** People the user has direct messages with (left list of the Messages page). */
    public static List<User> getDirectContacts(int userId) {
        List<User> out = new ArrayList<>();
        for (Message m : SampleData.MESSAGES) {
            if (m.getGroupId() != 0) continue;
            int other = m.getSenderId() == userId ? m.getRecipientId() : m.getRecipientId() == userId ? m.getSenderId() : 0;
            User u = SampleData.USERS.get(other);
            if (u != null && !out.contains(u)) out.add(u);
        }
        return out;
    }

    /**
     * SEND DIRECT MESSAGE. Rule: not allowed if either user blocked the other.
     */
    public static void sendDirectMessage(int senderId, int recipientId, String content) {
        notConnected("sendDirectMessage", senderId, recipientId, content);
    }

    // =================================================================
    // 7. SAFETY AND REVIEWS
    // =================================================================

    /** REPORT a user or group. */
    public static void report(Report r) {
        notConnected("report", r.getTargetType(), r.getTargetId(), r.getReason(), r.getDetails());
    }

    /** BLOCK. */
    public static void blockUser(int blockerId, int blockedId) {
        notConnected("blockUser", blockerId, blockedId);
    }

    /** UNBLOCK. */
    public static void unblockUser(int blockerId, int blockedId) {
        notConnected("unblockUser", blockerId, blockedId);
    }

    public static List<User> getBlockedUsers(int userId) {
        return SampleData.BLOCKED_BY_ME.stream().map(SampleData.USERS::get).collect(Collectors.toList());
    }

    public static boolean isBlocked(int blockerId, int blockedId) {
        return blockerId == currentUserId() && SampleData.BLOCKED_BY_ME.contains(blockedId);
    }

    /**
     * RATE A COMPANION (revieweeId = that user) or REVIEW THE TRIP (revieweeId = 0).
     * Rules: only after the trip ended, only by members, rating 1 to 5.
     */
    public static void addReview(Review r) {
        log("addReview", r.getGroupId(), r.getRevieweeId(), r.getRating(), r.getComment());
    }

    /** REVIEWS ABOUT A USER. */
    public static List<Review> getReviewsForUser(int userId) {
        return SampleData.REVIEWS.stream().filter(r -> r.getRevieweeId() == userId)
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed()).collect(Collectors.toList());
    }

    /** REVIEWS OF A TRIP. */
    public static List<Review> getTripReviews(int groupId) {
        return SampleData.REVIEWS.stream().filter(r -> r.getGroupId() == groupId && r.getRevieweeId() == 0)
                .collect(Collectors.toList());
    }

    public static double getAverageRating(int userId) {
        return getReviewsForUser(userId).stream().mapToInt(Review::getRating).average().orElse(0);
    }

    public static boolean hasReviewedTrip(int groupId, int reviewerId) {
        return SampleData.REVIEWS.stream().anyMatch(r -> r.getGroupId() == groupId && r.getReviewerId() == reviewerId && r.getRevieweeId() == 0);
    }

    public static boolean hasRatedCompanions(int groupId, int reviewerId) {
        return SampleData.REVIEWS.stream().anyMatch(r -> r.getGroupId() == groupId && r.getReviewerId() == reviewerId && r.getRevieweeId() != 0);
    }

    // =================================================================
    // 8. NOTIFICATIONS
    // =================================================================

    public static List<Notification> getNotifications(int userId) {
        return SampleData.NOTIFICATIONS.stream().filter(n -> n.getUserId() == userId)
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed()).collect(Collectors.toList());
    }

    public static int getUnreadCount(int userId) {
        return (int) getNotifications(userId).stream().filter(n -> !n.isRead()).count();
    }

    public static void markAllRead(int userId) {
        notConnected("markAllRead", userId);
    }


    // =================================================================
    // 9. ADMIN
    // Every admin method must check that the logged-in user has role = 'ADMIN'.
    // =================================================================

    // ----- 9.1 Dashboard overview -----

    /**
     * OVERVIEW CARDS.
     */
    public static AdminStats getAdminStats() {
        AdminStats a = new AdminStats();
        a.setTotalUsers(1247);       // FRONT END ONLY: sample numbers
        a.setTotalGroups(312);
        a.setActiveTrips(86);
        a.setCompletedTrips(214);
        a.setPendingReports((int) SampleData.REPORTS.stream()
                .filter(r -> r.getStatus() == ReportStatus.PENDING || r.getStatus() == ReportStatus.UNDER_REVIEW).count());
        return a;
    }

    /**
     * MOST VISITED PLACES (graph) and POPULAR DESTINATIONS (analytics).
     */
    public static List<ChartPoint> getMostVisitedPlaces(int limit) {
        List<ChartPoint> out = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, SampleData.TOP_PLACES.length); i++)
            out.add(new ChartPoint(SampleData.TOP_PLACES[i], SampleData.TOP_PLACES_TRIPS[i]));
        return out;
    }

    /**
     * TOP PERFORMING CREATORS.
     */
    public static List<CreatorStat> getTopCreators(int limit) {
        int[][] sample = {{2, 14, 11, 132}, {3, 12, 10, 118}, {4, 9, 8, 74}, {7, 8, 7, 61}, {5, 6, 4, 39}};
        List<CreatorStat> out = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, sample.length); i++) {
            CreatorStat c = new CreatorStat();
            c.setUser(SampleData.USERS.get(sample[i][0]));
            c.setGroupsCreated(sample[i][1]);
            c.setCompletedTrips(sample[i][2]);
            c.setTravelersHosted(sample[i][3]);
            c.setRating(getAverageRating(sample[i][0]));
            out.add(c);
        }
        return out;
    }

    // ----- 9.2 User management -----

    /**
     * ALL TRAVELERS, with search and status filter.
     * keyword: matches name or email (null/empty = all). status: null = all.
     */
    public static List<User> getAllUsers(String keyword, AccountStatus status) {
        log("getAllUsers", keyword, status);
        return SampleData.USERS.values().stream()
                .filter(u -> u.getRole() == UserRole.TRAVELER)
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .collect(Collectors.toList());   // FRONT END ONLY: search and filter not applied
    }

    public static int countReportsAgainstUser(int userId) {
        return (int) SampleData.REPORTS.stream()
                .filter(r -> r.getTargetType() == ReportTarget.USER && r.getTargetId() == userId).count();
    }

    /** Reports about one user (shown in the admin's user details). */
    public static List<Report> getReportsAgainstUser(int userId) {
        return SampleData.REPORTS.stream()
                .filter(r -> r.getTargetType() == ReportTarget.USER && r.getTargetId() == userId)
                .collect(Collectors.toList());
    }

    /** Number of groups a user created and joined (admin user list). */
    public static int countGroupsOf(int userId) {
        return (int) SampleData.GROUPS.keySet().stream().filter(id -> SampleData.memberIds(id).contains(userId)).count();
    }

    /**
     * SUSPEND an account (can't log in until reactivated).
     */
    public static void suspendUser(int userId, String reason) {
        notConnected("suspendUser", userId, reason);
    }

    /** REACTIVATE. */
    public static void reactivateUser(int userId) {
        notConnected("reactivateUser", userId);
    }

    /**
     * REMOVE an account (only when justified under the platform rules).
     */
    public static void removeUser(int userId, String reason) {
        notConnected("removeUser", userId, reason);
    }

    // ----- 9.3 Reports and complaints -----

    /**
     * REPORTS LIST. status = null means all.
     * targetName: the user's name, the group title, or "Message from <sender>".
     */
    public static List<Report> getReports(ReportStatus status) {
        return SampleData.REPORTS.stream()
                .filter(r -> status == null || r.getStatus() == status)
                .sorted(Comparator.comparing(Report::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    /**
     * THE ONE REPORTED MESSAGE (when a message was reported).
     * PRIVACY RULE: return only this message, never the rest of the conversation.
     */
    public static Message getReportedMessage(int messageId) {
        log("getReportedMessage", messageId);
        return SampleData.MESSAGES.stream().filter(m -> m.getId() == messageId).findFirst().orElse(null);
    }

    /**
     * SAVE THE ADMIN'S DECISION on a report.
     */
    public static void updateReport(int reportId, ReportStatus status, String actionTaken, String notes, boolean notifyUser) {
        notConnected("updateReport", reportId, status, actionTaken, notes, notifyUser);
    }

    // ----- 9.4 Reports and analytics -----

    /**
     * NEW REGISTRATIONS per month (last N months).
     */
    public static List<ChartPoint> getRegistrationsPerMonth(int months) {
        List<ChartPoint> out = new ArrayList<>();
        int start = SampleData.MONTHS.length - Math.min(months, SampleData.MONTHS.length);
        for (int i = start; i < SampleData.MONTHS.length; i++) out.add(new ChartPoint(SampleData.MONTHS[i], SampleData.NEW_USERS[i]));
        return out;
    }

    /**
     * TRIPS CREATED (first) and COMPLETED (second) per month.
     */
    public static List<MonthlyCount> getTripsPerMonth(int months) {
        return monthly(months, SampleData.TRIPS_CREATED, SampleData.TRIPS_COMPLETED);
    }

    /**
     * REPORTS FILED (first) and RESOLVED (second) per month.
     */
    public static List<MonthlyCount> getReportsPerMonth(int months) {
        return monthly(months, SampleData.REPORTS_FILED, SampleData.REPORTS_RESOLVED);
    }

    /**
     * EXPORT SUMMARY REPORT to a file (CSV or PDF) chosen by the admin.
     * Backend: collect the numbers above and write them to the file.
     */
    public static void exportSummaryReport(int months, java.io.File file) {
        notConnected("exportSummaryReport", months, file.getName());
    }

    private static List<MonthlyCount> monthly(int months, int[] a, int[] b) {
        List<MonthlyCount> out = new ArrayList<>();
        int start = SampleData.MONTHS.length - Math.min(months, SampleData.MONTHS.length);
        for (int i = start; i < SampleData.MONTHS.length; i++) out.add(new MonthlyCount(SampleData.MONTHS[i], a[i], b[i]));
        return out;
    }

    // =================================================================
    // helpers (FRONT END ONLY, delete when everything is connected)
    // =================================================================

    /** Prints the call in NetBeans' Output window. */
    private static void log(String method, Object... values) {
        System.out.println("[Backend] " + method + Arrays.toString(values));
    }

    /** Prints the action and shows a small "preview only" note at the bottom of the window. */
    private static void notConnected(String method, Object... values) {
        log(method, values);
        showConnected(method);
    }

    /** Small note at the bottom of the window, e.g. "Approve request: preview only, nothing is saved". */
    public static void showConnected(String method) {
        sabaylakbay_1.ui.components.Toast.show(readable(method) + ": preview only, nothing is saved");
    }

    /** "approveRequest" -> "Approve request" */
    private static String readable(String method) {
        String words = method.replaceAll("([A-Z])", " $1").toLowerCase().trim();
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
