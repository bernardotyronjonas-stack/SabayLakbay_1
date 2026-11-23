package sabaylakbay_1.backend;

import sabaylakbay_1.model.*;
import sabaylakbay_1.ui.components.Toast;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * =====================================================================
 *  BACKEND: every screen reads and saves data ONLY through these methods.
 *  Data is stored in MySQL (connection settings: Database.java,
 *  tables: database/sabaylakbay.sql, based on the project ERD).
 * =====================================================================
 *  How the tables work together:
 *    - an "activity" is a travel group / trip
 *    - a "booking" is a request to join; APPROVED bookings are the members
 *      (the organizer is also a member, without a booking)
 *    - a review is written through the reviewer's booking
 *
 *  If something is not allowed (group full, wrong password...) the method throws
 *  new RuntimeException("Friendly message") and the screen shows it in a popup.
 *
 *  Sections:
 *    1. Account and ID verification   2. Main Menu   3. Create and Manage Groups   4. Join Groups
 *    5. Trip Planning   6. Communication   7. Safety and Reviews   8. Notifications   9. Admin
 * =====================================================================
 */
public final class Backend {

    private Backend() { }

    /** The logged-in user. Set by login(), cleared by logout(). */
    private static User currentUser;

    /** Folder (inside the project folder) where uploaded IDs and profile pictures are saved. */
    private static final String UPLOADS = "uploads";

    // ===== reusable SQL parts =====

    /** Number of members of activity a = the organizer + approved bookings. */
    private static final String MEMBER_COUNT =
            "(1 + (SELECT COUNT(*) FROM bookings mb WHERE mb.activity_id = a.activity_id AND mb.status = 'APPROVED'))";

    private static final String GROUP_SELECT =
            "SELECT a.*, u.full_name AS organizer_name, " + MEMBER_COUNT + " AS member_count "
                    + "FROM activities a JOIN users u ON u.user_id = a.user_id ";

    private static final String REQUEST_SELECT =
            "SELECT b.*, a.title, u.full_name FROM bookings b "
                    + "JOIN activities a ON a.activity_id = b.activity_id JOIN users u ON u.user_id = b.user_id ";

    private static final String MESSAGE_SELECT =
            "SELECT m.*, u.full_name AS sender_name FROM group_messages m JOIN users u ON u.user_id = m.user_id ";

    private static final String DM_SELECT =
            "SELECT d.*, u.full_name AS sender_name FROM direct_messages d JOIN users u ON u.user_id = d.user_id ";

    private static final String REVIEW_SELECT =
            "SELECT r.*, b.user_id, b.activity_id, u.full_name AS reviewer_name FROM reviews r "
                    + "JOIN bookings b ON b.booking_id = r.booking_id JOIN users u ON u.user_id = b.user_id ";

    private static final String REPORT_SELECT =
            "SELECT r.*, rep.full_name AS reporter_name, CASE "
                    + "  WHEN r.message_id IS NOT NULL THEN CONCAT('Message from ', COALESCE((SELECT su.full_name "
                    + "       FROM group_messages sm JOIN users su ON su.user_id = sm.user_id WHERE sm.message_id = r.message_id), 'someone')) "
                    + "  WHEN r.reported_user_id IS NOT NULL THEN (SELECT full_name FROM users WHERE user_id = r.reported_user_id) "
                    + "  WHEN r.activity_id IS NOT NULL THEN (SELECT title FROM activities WHERE activity_id = r.activity_id) "
                    + "  ELSE 'Deleted message' END AS target_name "
                    + "FROM reports r JOIN users rep ON rep.user_id = r.user_id ";

    // =================================================================
    // 1. ACCOUNT AND ID VERIFICATION
    // =================================================================

    /** REGISTER a traveler with a photo of a valid ID. The ID waits for an admin to check it. */
    public static void register(User u, String password, File idPhoto) {
        register(u, password, idPhoto, java.util.Collections.emptyList());
    }

    /** True when an account already uses this email (checked before the sign-up steps). */
    public static boolean emailTaken(String email) {
        ensureMapColumns();
        return Database.exists("SELECT 1 FROM users WHERE email = ?", email);
    }

    /**
     * REGISTER with the answers from the sign-up steps: interests (trip categories they like)
     * and home location (u.getLocation / getLatitude / getLongitude, can be empty).
     */
    public static void register(User u, String password, File idPhoto, List<String> interests) {
        register(u, password, idPhoto, null, null, interests);
    }

    /**
     * REGISTER (the full sign-up): account, personal details (birthday, gender, emergency contact are in u),
     * ID type + photo, optional profile picture, interests and home location. All saved together.
     */
    public static void register(User u, String password, File idPhoto, String idType, File avatar, List<String> interests) {
        register(u, password, idPhoto, idType, null, avatar, interests);
    }

    /** Full sign-up including the face photo taken with the camera (compared with the ID by an admin). */
    public static void register(User u, String password, File idPhoto, String idType, File facePhoto, File avatar, List<String> interests) {
        ensureMapColumns();
        if (idPhoto == null || !idPhoto.isFile()) throw new RuntimeException("Please upload a photo of a valid ID.");
        if (u.getBirthdate() != null && u.getBirthdate().plusYears(18).isAfter(java.time.LocalDate.now()))
            throw new RuntimeException("You must be at least 18 years old to join SabayLakbay.");
        if (Database.exists("SELECT 1 FROM users WHERE email = ?", u.getEmail()))
            throw new RuntimeException("An account with that email already exists.");
        Database.transaction(c -> {
            int id = Database.insert(c, "INSERT INTO users (full_name, email, password_hash, phone, "
                    + "birthdate, gender, emergency_contact_name, emergency_contact_phone, "
                    + "street, barangay, city, province, zip_code, "
                    + "latitude, longitude, role, account_status) "
                    + "VALUES (?, ?, SHA2(?, 256), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'TRAVELER', 'ACTIVE')",
                    u.getFullName(), u.getEmail(), password, u.getPhone(), u.getBirthdate(), u.getGender(),
                    u.getEmergencyName(), u.getEmergencyPhone(), u.getStreet(), u.getBarangay(), u.getCity(),
                    u.getProvince(), u.getZipCode(), u.getLatitude(), u.getLongitude());
            String path = saveUpload(idPhoto, "ids", "id-" + id);
            String face = facePhoto != null && facePhoto.isFile() ? saveUpload(facePhoto, "faces", "face-" + id) : null;
            Database.insert(c, "INSERT INTO id_verifications (user_id, id_type, id_photo_path, "
                    + "selfie_path) VALUES (?, ?, ?, ?)", id, idType, path, face);
            if (avatar != null && avatar.isFile()) {
                String pic = saveUpload(avatar, "avatars", "user-" + id);
                Database.insert(c, "UPDATE users SET profile_picture = ? WHERE user_id = ?", pic, id);
            }
            for (String i : interests)
                Database.insert(c, "INSERT INTO user_interests (user_id, category) VALUES (?, ?)", id, i);
        });
        Toast.show("Account created. You can log in while we check your ID.");
    }

    // ================= PERSONALIZATION (interests + home location) =================

    /** The interests a traveler can pick. Same names as the trip categories, so trips can be matched. */
    public static final String[] INTERESTS = {"Hiking", "Beach", "Island hopping", "Food trip", "Road trip",
            "Sightseeing", "Camping", "Adventure", "City tour"};

    public static List<String> getInterests(int userId) {
        return Database.list("SELECT category FROM user_interests WHERE user_id = ? "
                + "ORDER BY category", rs -> rs.getString(1), userId);
    }

    /** Replaces the traveler's interests. */
    public static void saveInterests(int userId, List<String> interests) {
        Database.update("DELETE FROM user_interests WHERE user_id = ?", userId);
        for (String i : interests)
            Database.update("INSERT INTO user_interests (user_id, category) VALUES (?, ?)", userId, i);
    }

    /** Saves the traveler's home address and (optional) map pin. a holds street, barangay, city, province, ZIP. */
    public static void updateAddress(int userId, User a, Double lat, Double lng) {
        if (a.getCity() == null || a.getCity().isBlank() || a.getProvince() == null)
            throw new RuntimeException("Please enter at least your city and province.");
        Database.update("UPDATE users SET street = ?, barangay = ?, city = ?, province = ?, zip_code = ?, "
                + "latitude = ?, longitude = ? WHERE user_id = ?",
                a.getStreet(), a.getBarangay(), a.getCity(), a.getProvince(), a.getZipCode(), lat, lng, userId);
        if (currentUser != null && currentUser.getId() == userId) currentUser = getUser(userId);
    }

    /**
     * Straight-line distance in km from the logged-in traveler's home to the trip's MEETING PLACE
     * (where the group meets up, not the destination). null if the traveler or the trip has no pin for it.
     */
    public static Double distanceKm(TravelGroup g) {
        User me = currentUser;
        if (me == null || !me.hasLocation() || g == null || !g.hasMeetingPin()) return null;
        return km(me.getLatitude(), me.getLongitude(), g.getMeetingLatitude(), g.getMeetingLongitude());
    }

    /** Haversine distance between two map points, in km. */
    public static double km(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * GROUPS NEAR YOU (like Marketplace): open, upcoming trips whose meeting place is within radiusKm
     * of the traveler's home, closest first. Trips the traveler organizes are left out.
     */
    public static List<TravelGroup> getGroupsNearMe(double radiusKm) {
        User me = currentUser;
        List<TravelGroup> out = new ArrayList<>();
        if (me == null || !me.hasLocation()) return out;
        GroupFilter f = new GroupFilter();
        f.setSort(SortOption.UPCOMING);
        for (TravelGroup g : searchGroups(f)) {
            if (g.getCreatorId() == me.getId()) continue;
            Double d = distanceKm(g);
            if (d != null && d <= radiusKm) out.add(g);
        }
        out.sort(java.util.Comparator.comparingDouble(Backend::distanceKm));
        return out;
    }

    /**
     * "PICKED FOR YOU" on Home: open trips the traveler hasn't joined, matching their interests
     * and/or close to home. Best matches first.
     */
    public static List<Recommendation> getRecommendations(int limit) {
        User me = currentUser;
        if (me == null) return new ArrayList<>();
        java.util.Set<String> likes = new java.util.HashSet<>();
        for (String i : getInterests(me.getId())) likes.add(i.toLowerCase());
        GroupFilter f = new GroupFilter();
        f.setSort(SortOption.UPCOMING);
        List<Recommendation> out = new ArrayList<>();
        for (TravelGroup g : searchGroups(f)) {
            if (g.getCreatorId() == me.getId() || g.isFull()) continue;
            JoinRequest r = getRequest(g.getId(), me.getId());
            if (r != null && (r.getStatus() == RequestStatus.PENDING || r.getStatus() == RequestStatus.APPROVED)) continue;
            boolean liked = g.getCategory() != null && likes.contains(g.getCategory().toLowerCase());
            Double km = distanceKm(g);
            double score = liked ? 3 : 0;
            if (km != null) score += km < 60 ? 3 : km < 150 ? 2 : km < 400 ? 1 : 0;
            if (score <= 0) continue;
            String near = km == null ? null : km < 1 ? "Meet-up under 1 km from you" : "Meet-up " + Math.round(km) + " km from you";
            String reason = liked && km != null && km < 400 ? g.getCategory() + " \u2022 " + near
                    : liked ? "Because you like " + g.getCategory()
                    : "Near you \u2022 " + near;
            out.add(new Recommendation(g, reason, score - g.getStartDate().toEpochDay() / 1e6));
        }
        out.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
    }

    /** Map pins of the sample trips' meeting places (for databases made before meeting pins). */
    private static final Object[][] SAMPLE_MEETING_PINS = {
            {"Cubao Bus Terminal, Quezon City", 14.619500, 121.053200},
            {"Naga City Plaza", 13.624800, 123.185900},
            {"Starmall EDSA Shaw, Mandaluyong", 14.581000, 121.053800},
            {"NAIA Terminal 3", 14.520300, 121.014400},
            {"NAIA Terminal 2", 14.511400, 121.019300},
            {"SM North EDSA, The Block entrance", 14.656600, 121.029600},
            {"NLEX Balintawak Toll Plaza", 14.657800, 121.000300}};

    /** Map pins of the sample itinerary stops (for databases made before itinerary photos and pins). */
    private static final Object[][] SAMPLE_STOP_PINS = {
            {"Starmall EDSA Shaw", 14.581000, 121.053800},
            {"Tanay Public Market", 14.497800, 121.284500},
            {"Daranak Falls", 14.543700, 121.335700},
            {"Batlag Falls", 14.546300, 121.333300},
            {"SM North EDSA", 14.656600, 121.029600},
            {"Baguio City", 16.402300, 120.596000},
            {"La Trinidad", 16.455000, 120.587500},
            {"Harrison Road", 16.411200, 120.597300},
            {"Coron Island", 11.959100, 120.229300}};

    /** Every member has read every group message so far (used once when upgrading). */
    private static void markAllGroupChatsRead() {
        String last = "COALESCE((SELECT MAX(m.message_id) FROM group_messages m WHERE m.activity_id = a.activity_id), 0)";
        Database.update("INSERT INTO chat_reads (user_id, activity_id, last_read_message_id) "
                + "SELECT a.user_id, a.activity_id, " + last + " FROM activities a");
        Database.update("INSERT INTO chat_reads (user_id, activity_id, last_read_message_id) "
                + "SELECT b.user_id, a.activity_id, " + last + " FROM bookings b "
                + "JOIN activities a ON a.activity_id = b.activity_id WHERE b.status = 'APPROVED' "
                + "AND b.user_id <> a.user_id");
    }

    /** LOG IN with email and password. Suspended and removed accounts can't log in. */
    // ================= MAP PIN UPGRADE =================
    private static boolean mapColumnsChecked;

    /**
     * Databases made before the map update don't have latitude / longitude.
     * This adds them once (keeping all data) and pins the sample trips. Same as database/add-map-pins.sql.
     */
    /**
     * Foreign keys now use the same name as the primary key they point to (user_id, activity_id, booking_id, message_id).
     * {table, old name, new name}. Same as database/rename-foreign-keys.sql.
     */
    private static final String[][] FK_RENAMES = {
            {"user_interests", "user_interest_user_id", "user_id"},
            {"id_verifications", "verification_user_id", "user_id"},
            {"activities", "activity_organizer_id", "user_id"},
            {"itinerary", "itinerary_activity_id", "activity_id"},
            {"activity_expenses", "expense_activity_id", "activity_id"},
            {"bookings", "booking_user_id", "user_id"},
            {"bookings", "booking_activity_id", "activity_id"},
            {"group_messages", "message_activity_id", "activity_id"},
            {"group_messages", "message_sender_id", "user_id"},
            {"direct_messages", "dm_sender_id", "user_id"},
            {"chat_reads", "read_user_id", "user_id"},
            {"chat_reads", "read_activity_id", "activity_id"},
            {"reports", "report_reporter_id", "user_id"},
            {"reports", "report_activity_id", "activity_id"},
            {"reports", "report_message_id", "message_id"},
            {"reviews", "review_booking_id", "booking_id"},
            {"blocked_users", "block_blocker_id", "user_id"},
            {"notifications", "notification_user_id", "user_id"},
            {"notifications", "notification_activity_id", "activity_id"},
            {"admin_logs", "log_admin_id", "user_id"},
            // second link to users: a short role name
            {"blocked_users", "block_blocked_user_id", "blocked_user_id"},
            {"direct_messages", "dm_receiver_id", "receiver_id"},
            {"reports", "report_reported_user_id", "reported_user_id"},
            {"reports", "report_reviewed_by", "reviewed_by"},
            {"id_verifications", "verification_reviewed_by", "reviewed_by"},
            {"reviews", "review_reviewed_user_id", "reviewed_user_id"}};

    /**
     * Columns without the table name in front (workplace style): users.user_full_name is now users.full_name.
     * {table, old name, new name}. Same as database/rename-columns.sql.
     */
    private static final String[][] COLUMN_RENAMES = {
            {"users", "user_full_name", "full_name"},
            {"users", "user_email", "email"},
            {"users", "user_password", "password_hash"},
            {"users", "user_phone", "phone"},
            {"users", "user_profile_picture", "profile_picture"},
            {"users", "user_birthdate", "birthdate"},
            {"users", "user_gender", "gender"},
            {"users", "user_emergency_name", "emergency_contact_name"},
            {"users", "user_emergency_phone", "emergency_contact_phone"},
            {"users", "user_street", "street"},
            {"users", "user_barangay", "barangay"},
            {"users", "user_city", "city"},
            {"users", "user_province", "province"},
            {"users", "user_zip_code", "zip_code"},
            {"users", "user_latitude", "latitude"},
            {"users", "user_longitude", "longitude"},
            {"users", "user_role", "role"},
            {"users", "user_account_status", "account_status"},
            {"users", "user_created_at", "created_at"},
            {"user_interests", "user_interest_category", "category"},
            {"id_verifications", "verification_id_type", "id_type"},
            {"id_verifications", "verification_id_file_path", "id_photo_path"},
            {"id_verifications", "verification_selfie_path", "selfie_path"},
            {"id_verifications", "verification_status", "status"},
            {"id_verifications", "verification_reviewed_at", "reviewed_at"},
            {"activities", "activity_title", "title"},
            {"activities", "activity_category", "category"},
            {"activities", "activity_description", "description"},
            {"activities", "activity_destination", "destination"},
            {"activities", "activity_start_date", "start_date"},
            {"activities", "activity_end_date", "end_date"},
            {"activities", "activity_difficulty", "difficulty"},
            {"activities", "activity_price_per_person", "price_per_person"},
            {"activities", "activity_max_participants", "max_participants"},
            {"activities", "activity_status", "status"},
            {"activities", "activity_meeting_place", "meeting_place"},
            {"activities", "activity_meeting_time", "meeting_time"},
            {"activities", "activity_transport_mode", "transport_mode"},
            {"activities", "activity_notes", "notes"},
            {"activities", "activity_image_path", "image_path"},
            {"activities", "activity_latitude", "latitude"},
            {"activities", "activity_longitude", "longitude"},
            {"activities", "activity_meeting_latitude", "meeting_latitude"},
            {"activities", "activity_meeting_longitude", "meeting_longitude"},
            {"activities", "activity_created_at", "created_at"},
            {"itinerary", "itinerary_activity_name", "activity_name"},
            {"itinerary", "itinerary_location", "location"},
            {"itinerary", "itinerary_scheduled_time", "scheduled_time"},
            {"itinerary", "itinerary_details", "details"},
            {"activity_expenses", "expense_name", "name"},
            {"activity_expenses", "expense_estimated_amount", "estimated_amount"},
            {"bookings", "booking_request_message", "request_message"},
            {"bookings", "booking_status", "status"},
            {"bookings", "booking_requested_at", "requested_at"},
            {"group_messages", "message_content", "content"},
            {"group_messages", "message_is_announcement", "is_announcement"},
            {"group_messages", "message_sent_at", "sent_at"},
            {"direct_messages", "dm_content", "content"},
            {"direct_messages", "dm_sent_at", "sent_at"},
            {"direct_messages", "dm_is_read", "is_read"},
            {"chat_reads", "read_last_message_id", "last_read_message_id"},
            {"reports", "report_reason", "reason"},
            {"reports", "report_details", "details"},
            {"reports", "report_status", "status"},
            {"reports", "report_action_taken", "action_taken"},
            {"reports", "report_reviewed_at", "reviewed_at"},
            {"reports", "report_created_at", "created_at"},
            {"reviews", "review_rating", "rating"},
            {"reviews", "review_comment", "comment"},
            {"reviews", "review_created_at", "created_at"},
            {"blocked_users", "block_created_at", "created_at"},
            {"notifications", "notification_message", "message"},
            {"notifications", "notification_is_read", "is_read"},
            {"notifications", "notification_created_at", "created_at"},
            {"admin_logs", "log_action", "action"},
            {"admin_logs", "log_target", "target"},
            {"admin_logs", "log_details", "details"},
            {"admin_logs", "log_created_at", "created_at"}};

    /** Renames old key and column names in a database made before these updates. Data is kept. */
    private static void renameOldForeignKeys() {
        java.util.List<String[]> all = new java.util.ArrayList<>(java.util.Arrays.asList(FK_RENAMES));
        all.addAll(java.util.Arrays.asList(COLUMN_RENAMES));
        java.util.List<String[]> todo = new java.util.ArrayList<>();
        java.util.Set<String> columns = existingColumns();
        for (String[] r : all) {
            if (columns != null) {
                if (columns.contains((r[0] + "." + r[1]).toLowerCase())) todo.add(r);   // still has the old name
                continue;
            }
            try {
                Database.exists("SELECT " + r[1] + " FROM " + r[0] + " LIMIT 1");
                todo.add(r);
            } catch (RuntimeException alreadyNew) {
                // renamed already, or the table doesn't exist yet
            }
        }
        if (todo.isEmpty()) return;
        // MySQL won't rename a column a CHECK rule uses, so those rules are removed first and added back after
        java.util.Set<String> tables = new java.util.HashSet<>();
        for (String[] r : todo) tables.add(r[0]);
        boolean activities = tables.contains("activities") && dropChecks("activities");
        boolean reviews = tables.contains("reviews") && dropChecks("reviews");
        for (String[] r : todo) Database.update("ALTER TABLE " + r[0] + " RENAME COLUMN " + r[1] + " TO " + r[2]);
        if (activities) {
            Database.update("ALTER TABLE activities ADD CONSTRAINT chk_activities_dates CHECK (end_date >= start_date)");
            Database.update("ALTER TABLE activities ADD CONSTRAINT chk_activities_participants CHECK (max_participants >= 2)");
            Database.update("ALTER TABLE activities ADD CONSTRAINT chk_activities_price CHECK (price_per_person > 0)");
        }
        if (reviews) Database.update("ALTER TABLE reviews ADD CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)");
    }

    /** Every "table.column" in the database (MySQL), read once. null if the database can't list them. */
    private static java.util.Set<String> existingColumns() {
        try {
            java.util.Set<String> out = new java.util.HashSet<>();
            for (String c : Database.list("SELECT CONCAT(TABLE_NAME, '.', COLUMN_NAME) FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE()", rs -> rs.getString(1)))
                out.add(c.toLowerCase());
            return out.isEmpty() ? null : out;
        } catch (RuntimeException notMySql) {
            return null;
        }
    }

    /** Removes a table's CHECK rules (MySQL). Returns true if any were removed, so they can be added back. */
    private static boolean dropChecks(String table) {
        List<String> names;
        try {
            names = Database.list("SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CONSTRAINT_TYPE = 'CHECK'",
                    rs -> rs.getString(1), table);
        } catch (RuntimeException notMySql) {
            return false;                         // other databases rename CHECK rules by themselves
        }
        for (String n : names) Database.update("ALTER TABLE " + table + " DROP CHECK `" + n + "`");
        return !names.isEmpty();
    }

    private static void ensureMapColumns() {
        if (mapColumnsChecked) return;
        renameOldForeignKeys();                   // first, so everything below uses the new names
        try {
            Database.exists("SELECT latitude FROM activities LIMIT 1");
        } catch (RuntimeException missing) {
            Database.update("ALTER TABLE activities ADD COLUMN latitude DECIMAL(9,6) NULL");
            Database.update("ALTER TABLE activities ADD COLUMN longitude DECIMAL(9,6) NULL");
            Object[][] pins = {
                {"Albay", 13.257200, 123.685600},
                {"Palawan", 11.998600, 120.204300},
                {"Surigao del Norte", 9.784000, 126.157700},
                {"Benguet", 16.402300, 120.596000},
                {"Tanay, Rizal", 14.519700, 121.304400},
                {"Cebu", 9.947500, 123.367500},
                {"Batanes", 20.448700, 121.970200},
                {"Bohol", 9.829700, 124.139700},
                {"Camarines Sur", 13.770600, 123.863300},
                {"Ilocos Norte", 18.561700, 120.785400}
            };
            for (Object[] p : pins)
                Database.update("UPDATE activities SET latitude = ?, longitude = ? "
                        + "WHERE destination = ? AND latitude IS NULL", p[1], p[2], p[0]);
        }
        // home location + interests (same as database/add-personalization.sql)
        try {
            Database.exists("SELECT latitude FROM users LIMIT 1");
        } catch (RuntimeException missing) {
            Database.update("ALTER TABLE users ADD COLUMN latitude DECIMAL(9,6) NULL");
            Database.update("ALTER TABLE users ADD COLUMN longitude DECIMAL(9,6) NULL");
        }
        // sign-up details (same as database/add-signup-details.sql)
        try {
            Database.exists("SELECT birthdate FROM users LIMIT 1");
        } catch (RuntimeException missing) {
            Database.update("ALTER TABLE users ADD COLUMN birthdate DATE NULL");
            Database.update("ALTER TABLE users ADD COLUMN gender VARCHAR(20) NULL");
            Database.update("ALTER TABLE users ADD COLUMN emergency_contact_name VARCHAR(100) NULL");
            Database.update("ALTER TABLE users ADD COLUMN emergency_contact_phone VARCHAR(20) NULL");
        }
        try {
            Database.exists("SELECT id_type FROM id_verifications LIMIT 1");
        } catch (RuntimeException missing) {
            Database.update("ALTER TABLE id_verifications ADD COLUMN id_type VARCHAR(40) NULL");
        }
        try {   // face photo (same as database/add-face-check.sql)
            Database.exists("SELECT selfie_path FROM id_verifications LIMIT 1");
        } catch (RuntimeException missing) {
            Database.update("ALTER TABLE id_verifications ADD COLUMN selfie_path VARCHAR(255) NULL");
        }
        // full home address (same as database/add-address.sql)
        try {
            Database.exists("SELECT city FROM users LIMIT 1");
        } catch (RuntimeException missing) {
            Database.update("ALTER TABLE users ADD COLUMN street VARCHAR(150) NULL");
            Database.update("ALTER TABLE users ADD COLUMN barangay VARCHAR(100) NULL");
            Database.update("ALTER TABLE users ADD COLUMN city VARCHAR(100) NULL");
            Database.update("ALTER TABLE users ADD COLUMN province VARCHAR(100) NULL");
            Database.update("ALTER TABLE users ADD COLUMN zip_code VARCHAR(4) NULL");
            try {   // older accounts keep their place name as the city
                Database.update("UPDATE users SET city = user_location WHERE city IS NULL AND user_location IS NOT NULL");
            } catch (RuntimeException noOldColumn) { /* database never had user_location */ }
        }
        Database.update("CREATE TABLE IF NOT EXISTS user_interests ("
                + "user_id INT NOT NULL, category VARCHAR(40) NOT NULL, "
                + "PRIMARY KEY (user_id, category), "
                + "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE)");
        // unread messages (same as database/add-message-alerts.sql)
        try {
            Database.exists("SELECT is_read FROM direct_messages LIMIT 1");
        } catch (RuntimeException missing) {
            Database.update("ALTER TABLE direct_messages ADD COLUMN is_read BOOLEAN NOT NULL DEFAULT FALSE");
            Database.update("UPDATE direct_messages SET is_read = TRUE");       // old messages count as read
        }
        try {
            Database.exists("SELECT 1 FROM chat_reads LIMIT 1");
        } catch (RuntimeException missing) {
            Database.update("CREATE TABLE chat_reads (user_id INT NOT NULL, activity_id INT NOT NULL, "
                    + "last_read_message_id INT NOT NULL DEFAULT 0, PRIMARY KEY (user_id, activity_id), "
                    + "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (activity_id) REFERENCES activities(activity_id) ON DELETE CASCADE)");
            markAllGroupChatsRead();                                                // old messages count as read
        }
        // meeting place pins (same as database/add-meeting-pins.sql)
        try {
            Database.exists("SELECT meeting_latitude FROM activities LIMIT 1");
        } catch (RuntimeException missing) {
            Database.update("ALTER TABLE activities ADD COLUMN meeting_latitude DECIMAL(9,6) NULL");
            Database.update("ALTER TABLE activities ADD COLUMN meeting_longitude DECIMAL(9,6) NULL");
            for (Object[] p : SAMPLE_MEETING_PINS)
                Database.update("UPDATE activities SET meeting_latitude = ?, meeting_longitude = ? "
                        + "WHERE meeting_place = ? AND meeting_latitude IS NULL", p[1], p[2], p[0]);
        }
        // itinerary photos and map pins (same as database/add-itinerary-photos.sql)
        try {
            Database.exists("SELECT photo_path FROM itinerary LIMIT 1");
        } catch (RuntimeException missing) {
            Database.update("ALTER TABLE itinerary ADD COLUMN photo_path VARCHAR(255) NULL");
            Database.update("ALTER TABLE itinerary ADD COLUMN latitude DECIMAL(9,6) NULL");
            Database.update("ALTER TABLE itinerary ADD COLUMN longitude DECIMAL(9,6) NULL");
            for (Object[] p : SAMPLE_STOP_PINS)
                Database.update("UPDATE itinerary SET latitude = ?, longitude = ? "
                        + "WHERE location = ? AND latitude IS NULL", p[1], p[2], p[0]);
        }
        // old password hashes, for Forgot password (same as database/add-password-history.sql)
        Database.update("CREATE TABLE IF NOT EXISTS password_history ("
                + "history_id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, password_hash CHAR(64) NOT NULL, "
                + "changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE)");
        // admin activity log (same as database/add-admin-tools.sql)
        Database.update("CREATE TABLE IF NOT EXISTS admin_logs ("
                + "log_id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NULL, action VARCHAR(40) NOT NULL, "
                + "target VARCHAR(150), details VARCHAR(500), created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL)");
        mapColumnsChecked = true;
    }

    public static User login(String email, String password) {
        ensureMapColumns();
        User u = Database.one("SELECT * FROM users WHERE email = ? AND password_hash = SHA2(?, 256)",
                Backend::mapUser, email, password);
        if (u == null) throw new RuntimeException("Wrong email or password.");
        if (u.getStatus() == AccountStatus.SUSPENDED)
            throw new RuntimeException("Your account is suspended. Please contact the SabayLakbay team.");
        if (u.getStatus() == AccountStatus.REMOVED) throw new RuntimeException("This account was removed.");
        currentUser = u;
        return u;
    }

    // ===== FORGOT PASSWORD =====
    // Simple reset: the traveler types their email, a RECENT password (the current one or any
    // password they used before on this account) and a new password. Old passwords are kept only
    // as SHA-256 hashes in password_history. After 5 wrong tries the email waits 15 minutes.

    private static final Map<String, int[]> resetFails = new HashMap<>();       // email -> {fails, lockedUntilMinute}

    public static void resetPassword(String email, String recentPassword, String newPassword) {
        ensureMapColumns();
        String key = email == null ? "" : email.trim().toLowerCase();
        long minute = System.currentTimeMillis() / 60000;
        int[] f = resetFails.getOrDefault(key, new int[]{0, 0});
        if (f[1] > minute)
            throw new RuntimeException("Too many wrong tries. Please wait " + (f[1] - minute) + " minutes and try again.");
        checkNewPassword(newPassword);
        if (newPassword.equals(recentPassword)) throw new RuntimeException("Choose a new password that's different from the recent one.");

        // the current password, or one this account used before
        User u = Database.one("SELECT * FROM users WHERE email = ? AND (password_hash = SHA2(?, 256) "
                        + "OR EXISTS (SELECT 1 FROM password_history h WHERE h.user_id = users.user_id AND h.password_hash = SHA2(?, 256)))",
                Backend::mapUser, key, recentPassword, recentPassword);
        if (u == null) {
            f[0]++;
            if (f[0] >= 5) { f[0] = 0; f[1] = (int) (minute + 15); }
            resetFails.put(key, f);
            throw new RuntimeException("That email and recent password don't match.");
        }
        if (u.getStatus() == AccountStatus.REMOVED) throw new RuntimeException("This account was removed.");
        if (u.getStatus() == AccountStatus.SUSPENDED)
            throw new RuntimeException("Your account is suspended. Please contact the SabayLakbay team.");
        resetFails.remove(key);

        // keep the old password's hash, then set the new one
        Database.update("INSERT INTO password_history (user_id, password_hash) SELECT user_id, password_hash FROM users WHERE user_id = ?", u.getId());
        Database.update("UPDATE users SET password_hash = SHA2(?, 256) WHERE user_id = ?", newPassword, u.getId());
        notify(u.getId(), "Your password was changed with Forgot password. If this wasn't you, contact the SabayLakbay team.", 0);
        Toast.show("Password changed. You can log in now.");
    }

    /** Same rules as sign-up: at least 8 characters, a number and an uppercase letter. */
    private static void checkNewPassword(String p) {
        if (p == null || p.length() < 8 || !p.matches(".*\\d.*") || !p.matches(".*[A-Z].*"))
            throw new RuntimeException("Use at least 8 characters with a number and an uppercase letter.");
    }

    public static void logout() {
        currentUser = null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static int currentUserId() {
        return currentUser == null ? 0 : currentUser.getId();
    }

    public static User getUser(int userId) {
        return Database.one("SELECT * FROM users WHERE user_id = ?", Backend::mapUser, userId);
    }

    /** EDIT PROFILE: name, phone and (optional) new profile picture. */
    public static void updateProfile(User u, File newPicture) {
        String picture = newPicture == null ? null : saveUpload(newPicture, "avatars", "user-" + u.getId());
        // gender and emergency contact can change anytime
        Database.update("UPDATE users SET gender = ?, emergency_contact_name = ?, emergency_contact_phone = ? WHERE user_id = ?",
                u.getGender(), u.getEmergencyName(), u.getEmergencyPhone(), u.getId());
        // the birthday is checked against the ID by an admin, so it can only be set once (older accounts)
        if (u.getBirthdate() != null) {
            if (u.getBirthdate().plusYears(18).isAfter(java.time.LocalDate.now()))
                throw new RuntimeException("You must be at least 18 years old.");
            Database.update("UPDATE users SET birthdate = ? WHERE user_id = ? AND birthdate IS NULL", u.getBirthdate(), u.getId());
        }
        if (picture == null) {
            Database.update("UPDATE users SET full_name = ?, phone = ? WHERE user_id = ?",
                    u.getFullName(), u.getPhone(), u.getId());
        } else {
            Database.update("UPDATE users SET full_name = ?, phone = ?, profile_picture = ? WHERE user_id = ?",
                    u.getFullName(), u.getPhone(), picture, u.getId());
        }
        if (currentUser != null && currentUser.getId() == u.getId()) currentUser = getUser(u.getId());
        Toast.show("Profile updated");
    }

    /** Latest ID check of a user. NONE if they never uploaded one. */
    public static VerificationStatus getVerificationStatus(int userId) {
        String s = Database.one("SELECT status FROM id_verifications WHERE user_id = ? "
                + "ORDER BY verification_id DESC LIMIT 1", rs -> rs.getString(1), userId);
        return s == null ? VerificationStatus.NONE : VerificationStatus.valueOf(s);
    }

    /** Upload a new ID photo (after a rejection, or if none was uploaded). */
    public static void resubmitId(int userId, File idPhoto) {
        resubmitId(userId, null, idPhoto, null);
    }

    /** Upload a new ID with its type and a face photo (after a rejection, or if none was uploaded). */
    public static void resubmitId(int userId, String idType, File idPhoto, File facePhoto) {
        if (getVerificationStatus(userId) == VerificationStatus.PENDING)
            throw new RuntimeException("Your ID is already waiting to be checked.");
        if (idPhoto == null || !idPhoto.isFile()) throw new RuntimeException("Please add a photo of your ID.");
        String path = saveUpload(idPhoto, "ids", "id-" + userId);
        String face = facePhoto != null && facePhoto.isFile() ? saveUpload(facePhoto, "faces", "face-" + userId) : null;
        Database.insert("INSERT INTO id_verifications (user_id, id_type, id_photo_path, "
                + "selfie_path) VALUES (?, ?, ?, ?)", userId, idType, path, face);
        Toast.show("ID uploaded. We'll let you know once it's checked.");
    }

    // =================================================================
    // 2. MAIN MENU (browse, search, filter, sort, view details)
    // =================================================================

    /** HOME PAGE LIST: open trips that are not finished yet, with search, filters and sort. */
    public static List<TravelGroup> searchGroups(GroupFilter f) {
        StringBuilder sql = new StringBuilder(GROUP_SELECT + "WHERE a.status = 'OPEN' AND a.end_date >= CURDATE() ");
        List<Object> p = new ArrayList<>();
        // hide trips of people I blocked or who blocked me
        sql.append("AND NOT EXISTS (SELECT 1 FROM blocked_users bl WHERE (bl.user_id = ? AND bl.blocked_user_id = a.user_id) "
                + "OR (bl.user_id = a.user_id AND bl.blocked_user_id = ?)) ");
        p.add(currentUserId());
        p.add(currentUserId());
        if (f.getKeyword() != null && !f.getKeyword().isBlank()) {
            sql.append("AND (a.title LIKE ? OR a.destination LIKE ? OR a.category LIKE ? OR u.full_name LIKE ?) ");
            String like = "%" + f.getKeyword().trim() + "%";
            p.add(like); p.add(like); p.add(like); p.add(like);
        }
        if (f.getDateFrom() != null) { sql.append("AND a.start_date >= ? "); p.add(f.getDateFrom()); }
        if (f.getDateTo() != null) { sql.append("AND a.start_date <= ? "); p.add(f.getDateTo()); }
        if (f.getMaxBudget() != null) { sql.append("AND a.price_per_person <= ? "); p.add(f.getMaxBudget()); }
        if (f.getMinSlots() > 0) { sql.append("AND a.max_participants - " + MEMBER_COUNT + " >= ? "); p.add(f.getMinSlots()); }
        sql.append(f.getSort() == SortOption.NEWEST ? "ORDER BY a.created_at DESC, a.activity_id DESC" : "ORDER BY a.start_date ASC");
        List<TravelGroup> list = Database.list(sql.toString(), Backend::mapGroup, p.toArray());
        if (f.getSort() == SortOption.NEAREST) {
            // closest to the traveler's home first; trips without a pin go last
            list.sort(java.util.Comparator.comparingDouble(g -> { Double d = distanceKm(g); return d == null ? Double.MAX_VALUE : d; }));
        }
        return list;
    }

    /** GROUP DETAILS. */
    public static TravelGroup getGroup(int groupId) {
        TravelGroup g = Database.one(GROUP_SELECT + "WHERE a.activity_id = ?", Backend::mapGroup, groupId);
        if (g == null) throw new RuntimeException("This trip no longer exists.");
        return g;
    }

    /** MEMBERS: the organizer first, then everyone with an approved booking. */
    public static List<User> getMembers(int groupId) {
        return Database.list("SELECT u.*, 0 AS sort_order, NULL AS joined FROM activities a JOIN users u ON u.user_id = a.user_id "
                        + "WHERE a.activity_id = ? "
                        + "UNION ALL "
                        + "SELECT u.*, 1 AS sort_order, b.requested_at AS joined FROM bookings b JOIN users u ON u.user_id = b.user_id "
                        + "WHERE b.activity_id = ? AND b.status = 'APPROVED' "
                        + "ORDER BY sort_order, joined",
                Backend::mapUser, groupId, groupId);
    }

    /** Organizer or approved booking. */
    public static boolean isMember(int groupId, int userId) {
        return Database.exists("SELECT 1 FROM activities WHERE activity_id = ? AND user_id = ? "
                        + "UNION ALL SELECT 1 FROM bookings WHERE activity_id = ? AND user_id = ? AND status = 'APPROVED'",
                groupId, userId, groupId, userId);
    }

    // =================================================================
    // 3. CREATE AND MANAGE TRAVEL GROUPS
    // =================================================================

    /** CREATE GROUP. Needs an approved ID. The organizer is a member automatically. */
    public static int createGroup(TravelGroup g) {
        requireVerifiedId(g.getCreatorId());
        validateGroup(g, true);
        int id = Database.insert("INSERT INTO activities (user_id, title, category, description, "
                        + "destination, start_date, end_date, difficulty, price_per_person, "
                        + "max_participants, meeting_place, meeting_time, transport_mode, "
                        + "notes, image_path, latitude, longitude, "
                        + "meeting_latitude, meeting_longitude) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                g.getCreatorId(), g.getTitle(), g.getCategory(), g.getDescription(), g.getDestination(), g.getStartDate(),
                g.getEndDate(), g.getDifficulty(), g.getBudgetPerPerson(), g.getMaxMembers(), g.getMeetingPlace(),
                g.getMeetingTime(), g.getTransportMode(), g.getCreatorNotes(), g.getImagePath(),
                g.getLatitude(), g.getLongitude(), g.getMeetingLatitude(), g.getMeetingLongitude());
        g.setId(id);
        Toast.show("Your trip \"" + g.getTitle() + "\" is now live!");
        return id;
    }

    /** EDIT GROUP. Max participants can't go below the current number of members. */
    public static void updateGroup(TravelGroup g) {
        validateGroup(g, false);
        int members = getGroup(g.getId()).getCurrentMembers();
        if (g.getMaxMembers() < members)
            throw new RuntimeException("Max participants can't be lower than the current " + members + " members.");
        Database.update("UPDATE activities SET title = ?, category = ?, description = ?, "
                        + "destination = ?, start_date = ?, end_date = ?, difficulty = ?, "
                        + "price_per_person = ?, max_participants = ?, meeting_place = ?, "
                        + "meeting_time = ?, transport_mode = ?, notes = ?, image_path = ?, "
                        + "latitude = ?, longitude = ?, meeting_latitude = ?, "
                        + "meeting_longitude = ? WHERE activity_id = ?",
                g.getTitle(), g.getCategory(), g.getDescription(), g.getDestination(), g.getStartDate(), g.getEndDate(),
                g.getDifficulty(), g.getBudgetPerPerson(), g.getMaxMembers(), g.getMeetingPlace(), g.getMeetingTime(),
                g.getTransportMode(), g.getCreatorNotes(), g.getImagePath(), g.getLatitude(), g.getLongitude(),
                g.getMeetingLatitude(), g.getMeetingLongitude(), g.getId());
        notifyMembers(g.getId(), currentUserId(), "\"" + g.getTitle() + "\" details were updated by the organizer.");
        Toast.show("Changes saved");
    }

    /** CANCEL GROUP. Pending requests are cancelled and members are notified. */
    public static void cancelGroup(int groupId) {
        TravelGroup g = getGroup(groupId);
        Database.update("UPDATE activities SET status = 'CANCELLED' WHERE activity_id = ?", groupId);
        Database.update("UPDATE bookings SET status = 'CANCELLED' WHERE activity_id = ? AND status = 'PENDING'", groupId);
        notifyMembers(groupId, g.getCreatorId(), "The trip \"" + g.getTitle() + "\" was cancelled by the organizer.");
        Toast.show("Trip cancelled");
    }

    /** MY GROUPS > Created. */
    public static List<TravelGroup> getGroupsCreatedBy(int userId) {
        return Database.list(GROUP_SELECT + "WHERE a.user_id = ? ORDER BY a.start_date", Backend::mapGroup, userId);
    }

    /** JOIN REQUESTS an organizer must review. */
    public static List<JoinRequest> getPendingRequestsForGroup(int groupId) {
        return Database.list(REQUEST_SELECT + "WHERE b.activity_id = ? AND b.status = 'PENDING' ORDER BY b.requested_at",
                Backend::mapRequest, groupId);
    }

    /** APPROVE a join request: the booking becomes APPROVED, so the traveler is now a member. */
    public static void approveRequest(int requestId) {
        JoinRequest r = findRequest(requestId);
        TravelGroup g = getGroup(r.getGroupId());
        if (g.isFull()) throw new RuntimeException("The trip is already full. Increase max participants first.");
        Database.update("UPDATE bookings SET status = 'APPROVED' WHERE booking_id = ?", requestId);
        notify(r.getUserId(), "Your request to join \"" + g.getTitle() + "\" was approved!", g.getId());
        Toast.show(r.getUserName() + " was added to the trip");
    }

    public static void rejectRequest(int requestId) {
        JoinRequest r = findRequest(requestId);
        Database.update("UPDATE bookings SET status = 'REJECTED' WHERE booking_id = ?", requestId);
        notify(r.getUserId(), "Your request to join \"" + r.getGroupTitle() + "\" was not approved.", r.getGroupId());
        Toast.show("Request rejected");
    }

    // =================================================================
    // 4. JOIN TRAVEL GROUPS
    // =================================================================

    /** SEND JOIN REQUEST (creates a PENDING booking). Needs an approved ID. */
    public static void sendJoinRequest(int groupId, int userId, String message) {
        requireVerifiedId(userId);
        TravelGroup g = getGroup(groupId);
        if (g.isCancelled()) throw new RuntimeException("This trip was cancelled.");
        if (g.isFull()) throw new RuntimeException("This trip is already full.");
        if (isMember(groupId, userId)) throw new RuntimeException("You are already a member of this trip.");
        if (isBlocked(g.getCreatorId(), userId)) throw new RuntimeException("You can't join this trip.");
        JoinRequest last = getRequest(groupId, userId);
        if (last != null && last.getStatus() == RequestStatus.PENDING) throw new RuntimeException("You already have a pending request.");
        Database.insert("INSERT INTO bookings (user_id, activity_id, request_message) VALUES (?, ?, ?)",
                userId, groupId, message);
        notify(g.getCreatorId(), getUser(userId).getFullName() + " wants to join \"" + g.getTitle() + "\".", groupId);
        Toast.show("Join request sent");
    }

    /** CANCEL a pending request. */
    public static void cancelJoinRequest(int requestId) {
        int changed = Database.update("UPDATE bookings SET status = 'CANCELLED' WHERE booking_id = ? AND status = 'PENDING'", requestId);
        if (changed == 0) throw new RuntimeException("Only pending requests can be cancelled.");
        Toast.show("Request cancelled");
    }

    /** The latest booking of a user for a trip (null if none). */
    public static JoinRequest getRequest(int groupId, int userId) {
        return Database.one(REQUEST_SELECT + "WHERE b.activity_id = ? AND b.user_id = ? "
                + "ORDER BY b.requested_at DESC, b.booking_id DESC LIMIT 1", Backend::mapRequest, groupId, userId);
    }

    /** MY GROUPS > My Requests. */
    public static List<JoinRequest> getRequestsByUser(int userId) {
        return Database.list(REQUEST_SELECT + "WHERE b.user_id = ? ORDER BY b.requested_at DESC, b.booking_id DESC",
                Backend::mapRequest, userId);
    }

    /** MY GROUPS > Joined: trips with an approved booking. */
    public static List<TravelGroup> getGroupsJoinedBy(int userId) {
        return Database.list(GROUP_SELECT + "JOIN bookings jb ON jb.activity_id = a.activity_id "
                + "WHERE jb.user_id = ? AND jb.status = 'APPROVED' ORDER BY a.start_date", Backend::mapGroup, userId);
    }

    /** LEAVE GROUP: the booking becomes LEFT. The organizer can't leave (they cancel the trip instead). */
    public static void leaveGroup(int groupId, int userId) {
        TravelGroup g = getGroup(groupId);
        if (g.getCreatorId() == userId) throw new RuntimeException("The organizer can't leave. Cancel the trip instead.");
        Database.update("UPDATE bookings SET status = 'LEFT' WHERE activity_id = ? AND user_id = ? "
                + "AND status = 'APPROVED'", groupId, userId);
        notify(g.getCreatorId(), getUser(userId).getFullName() + " left \"" + g.getTitle() + "\".", groupId);
        Toast.show("You left \"" + g.getTitle() + "\"");
    }

    // =================================================================
    // 5. TRIP PLANNING
    // =================================================================

    public static List<Activity> getItinerary(int groupId) {
        return Database.list("SELECT * FROM itinerary WHERE activity_id = ? ORDER BY scheduled_time, itinerary_id",
                Backend::mapItinerary, groupId);
    }

    /** ADD TO ITINERARY (organizer only). Members are notified. */
    public static void addActivity(Activity a) { addActivity(a, null); }

    /** ADD TO ITINERARY with an optional photo of the place (copied into uploads/itinerary). Members are notified. */
    public static void addActivity(Activity a, File photo) {
        TravelGroup g = getGroup(a.getGroupId());
        requireOrganizer(g);
        insertStop(g, a, photo);
        notifyMembers(g.getId(), currentUserId(), "New activity added to \"" + g.getTitle() + "\": " + a.getTitle());
        Toast.show("Added to the itinerary");
    }

    /** EDIT AN ITINERARY STOP. newPhoto = a new picture, or null to keep the current one (or none). */
    public static void updateActivity(Activity a, File newPhoto) {
        TravelGroup g = getGroup(a.getGroupId());
        requireOrganizer(g);
        updateStop(g, a, newPhoto);
        notifyMembers(g.getId(), currentUserId(), "The itinerary of \"" + g.getTitle() + "\" was updated: " + a.getTitle());
        Toast.show("Itinerary updated");
    }

    public static void deleteActivity(int itineraryId) {
        Database.update("DELETE FROM itinerary WHERE itinerary_id = ?", itineraryId);
        Toast.show("Removed from the itinerary");
    }

    /**
     * SAVE THE WHOLE ITINERARY from the Create / Edit Group form in one go:
     * stops without an id are added, stops with an id are updated, removedIds are deleted.
     * newPhotos = the picture chosen for a stop in the form (only for stops that got a new one).
     * Members are told once (not once per stop).
     */
    public static void saveItinerary(int groupId, List<Activity> stops, Map<Activity, File> newPhotos, List<Integer> removedIds) {
        TravelGroup g = getGroup(groupId);
        requireOrganizer(g);
        for (Activity a : stops) checkStop(g, a);              // check everything first, so nothing is half saved
        for (int id : removedIds)
            Database.update("DELETE FROM itinerary WHERE itinerary_id = ? AND activity_id = ?", id, groupId);
        for (Activity a : stops) {
            a.setGroupId(groupId);
            File photo = newPhotos == null ? null : newPhotos.get(a);
            if (a.getId() == 0) insertStop(g, a, photo);
            else updateStop(g, a, photo);
        }
    }

    private static void requireOrganizer(TravelGroup g) {
        if (g == null) throw new RuntimeException("This trip no longer exists.");
        if (g.getCreatorId() != currentUserId()) throw new RuntimeException("Only the organizer can change the itinerary.");
    }

    /** The rules for one stop: a name, a date inside the trip, and a time. */
    private static void checkStop(TravelGroup g, Activity a) {
        if (a.getTitle() == null || a.getTitle().isBlank()) throw new RuntimeException("Every itinerary stop needs a name.");
        if (a.getDate() == null || a.getTime() == null) throw new RuntimeException("\"" + a.getTitle() + "\" needs a day and a time.");
        if (a.getDate().isBefore(g.getStartDate()) || a.getDate().isAfter(g.getEndDate()))
        {
            DateTimeFormatter f = DateTimeFormatter.ofPattern("MMM d");
            throw new RuntimeException("\"" + a.getTitle() + "\" is on " + f.format(a.getDate()) + ", outside the trip ("
                    + f.format(g.getStartDate()) + " - " + f.format(g.getEndDate()) + "). Move it or remove it.");
        }
    }

    private static void insertStop(TravelGroup g, Activity a, File photo) {
        checkStop(g, a);
        if (photo != null) a.setPhotoPath(saveUpload(photo, "itinerary", "trip-" + g.getId()));
        int id = Database.insert("INSERT INTO itinerary (activity_id, activity_name, location, scheduled_time, details, "
                        + "photo_path, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                g.getId(), a.getTitle().trim(), blankToNull(a.getLocation()), LocalDateTime.of(a.getDate(), a.getTime()),
                blankToNull(a.getDetails()), a.getPhotoPath(), a.getLatitude(), a.getLongitude());
        a.setId(id);
    }

    private static void updateStop(TravelGroup g, Activity a, File newPhoto) {
        checkStop(g, a);
        if (newPhoto != null) a.setPhotoPath(saveUpload(newPhoto, "itinerary", "trip-" + g.getId()));
        Database.update("UPDATE itinerary SET activity_name = ?, location = ?, scheduled_time = ?, details = ?, "
                        + "photo_path = ?, latitude = ?, longitude = ? WHERE itinerary_id = ? AND activity_id = ?",
                a.getTitle().trim(), blankToNull(a.getLocation()), LocalDateTime.of(a.getDate(), a.getTime()),
                blankToNull(a.getDetails()), a.getPhotoPath(), a.getLatitude(), a.getLongitude(), a.getId(), g.getId());
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    /** ESTIMATED EXPENSES per person. */
    public static List<Expense> getExpenses(int groupId) {
        return Database.list("SELECT * FROM activity_expenses WHERE activity_id = ? ORDER BY expense_id", Backend::mapExpense, groupId);
    }

    public static void addExpense(Expense e) {
        if (e.getLabel() == null || e.getLabel().isBlank()) throw new RuntimeException("Expense name is required.");
        Database.insert("INSERT INTO activity_expenses (activity_id, name, estimated_amount) VALUES (?, ?, ?)",
                e.getGroupId(), e.getLabel(), e.getAmount());
        Toast.show("Expense added");
    }

    public static void deleteExpense(int expenseId) {
        Database.update("DELETE FROM activity_expenses WHERE expense_id = ?", expenseId);
        Toast.show("Expense removed");
    }

    /** TRIP UPDATES = announcements, newest first. */
    public static List<Message> getAnnouncements(int groupId) {
        return Database.list(MESSAGE_SELECT + "WHERE m.activity_id = ? AND m.is_announcement = TRUE "
                + "ORDER BY m.sent_at DESC, m.message_id DESC", Backend::mapGroupMessage, groupId);
    }

    // =================================================================
    // 6. COMMUNICATION
    // =================================================================

    public static List<Message> getGroupMessages(int groupId) {
        return Database.list(MESSAGE_SELECT + "WHERE m.activity_id = ? ORDER BY m.sent_at, m.message_id",
                Backend::mapGroupMessage, groupId);
    }

    /** SEND GROUP MESSAGE. Only members can send; only the organizer can post announcements. */
    public static void sendGroupMessage(int groupId, int senderId, String content, boolean announcement) {
        if (content == null || content.isBlank()) throw new RuntimeException("Message is empty.");
        if (!isMember(groupId, senderId)) throw new RuntimeException("Only trip members can send messages.");
        TravelGroup g = getGroup(groupId);
        if (announcement && g.getCreatorId() != senderId) throw new RuntimeException("Only the organizer can post announcements.");
        Database.insert("INSERT INTO group_messages (activity_id, user_id, content, is_announcement) "
                + "VALUES (?, ?, ?, ?)", groupId, senderId, content.trim(), announcement);
        if (announcement) notifyMembers(groupId, senderId, "Trip update in \"" + g.getTitle() + "\": " + content.trim());
    }

    /** DIRECT MESSAGES between two users, oldest first. */
    public static List<Message> getDirectMessages(int userA, int userB) {
        return Database.list(DM_SELECT + "WHERE (d.user_id = ? AND d.receiver_id = ?) OR (d.user_id = ? AND d.receiver_id = ?) "
                + "ORDER BY d.sent_at, d.dm_id", Backend::mapDirectMessage, userA, userB, userB, userA);
    }

    /** People this user has direct messages with, most recent first (blocked users hidden). */
    public static List<User> getDirectContacts(int userId) {
        return Database.list("SELECT u.* FROM users u JOIN ("
                        + "  SELECT CASE WHEN user_id = ? THEN receiver_id ELSE user_id END AS other_id, MAX(sent_at) AS last_sent "
                        + "  FROM direct_messages WHERE user_id = ? OR receiver_id = ? GROUP BY other_id) x ON x.other_id = u.user_id "
                        + "WHERE u.user_id NOT IN (SELECT blocked_user_id FROM blocked_users WHERE user_id = ?) "
                        + "ORDER BY x.last_sent DESC",
                Backend::mapUser, userId, userId, userId, userId);
    }

    /** SEND DIRECT MESSAGE. Not allowed if either user blocked the other. */
    public static void sendDirectMessage(int senderId, int recipientId, String content) {
        if (content == null || content.isBlank()) throw new RuntimeException("Message is empty.");
        if (isBlocked(senderId, recipientId)) throw new RuntimeException("You blocked this user. Unblock them first.");
        if (isBlocked(recipientId, senderId)) throw new RuntimeException("You can't message this user.");
        Database.insert("INSERT INTO direct_messages (user_id, receiver_id, content) VALUES (?, ?, ?)", senderId, recipientId, content.trim());
    }

    // ----- 6.5 Unread messages and new-message alerts -----

    /** The group message m (joined with activities a) belongs to a chat this user is in, and came after they joined. */
    private static final String IN_MY_CHAT = "(a.user_id = ? OR EXISTS (SELECT 1 FROM bookings b "
            + "WHERE b.activity_id = a.activity_id AND b.user_id = ? AND b.status = 'APPROVED' "
            + "AND m.sent_at >= b.requested_at)) ";
    /** The last group message this user has seen in chat a. */
    private static final String LAST_READ = "COALESCE((SELECT r.last_read_message_id FROM chat_reads r "
            + "WHERE r.user_id = ? AND r.activity_id = a.activity_id), 0) ";

    /** Unread messages per group chat: group id -> count. */
    public static java.util.Map<Integer, Integer> getUnreadGroupCounts(int userId) {
        java.util.Map<Integer, Integer> out = new java.util.HashMap<>();
        for (int[] r : Database.list("SELECT m.activity_id AS gid, COUNT(*) AS n FROM group_messages m "
                + "JOIN activities a ON a.activity_id = m.activity_id "
                + "WHERE a.status <> 'CANCELLED' AND m.user_id <> ? AND " + IN_MY_CHAT
                + "AND m.message_id > " + LAST_READ + "GROUP BY m.activity_id",
                rs -> new int[]{rs.getInt("gid"), rs.getInt("n")}, userId, userId, userId, userId))
            out.put(r[0], r[1]);
        return out;
    }

    /** Unread direct messages per person: sender id -> count (blocked people left out). */
    public static java.util.Map<Integer, Integer> getUnreadDirectCounts(int userId) {
        java.util.Map<Integer, Integer> out = new java.util.HashMap<>();
        for (int[] r : Database.list("SELECT user_id AS sid, COUNT(*) AS n FROM direct_messages "
                + "WHERE receiver_id = ? AND is_read = FALSE AND user_id NOT IN "
                + "(SELECT blocked_user_id FROM blocked_users WHERE user_id = ?) GROUP BY user_id",
                rs -> new int[]{rs.getInt("sid"), rs.getInt("n")}, userId, userId))
            out.put(r[0], r[1]);
        return out;
    }

    /** All unread messages (group chats + direct). Shown as the badge on "Messages". */
    public static int getUnreadMessageCount(int userId) {
        int n = 0;
        for (int c : getUnreadGroupCounts(userId).values()) n += c;
        for (int c : getUnreadDirectCounts(userId).values()) n += c;
        return n;
    }

    /** The user opened this group chat: everything in it is now read. */
    public static void markGroupRead(int userId, int groupId) {
        int last = Database.count("SELECT COALESCE(MAX(message_id), 0) FROM group_messages WHERE activity_id = ?", groupId);
        int changed = Database.update("UPDATE chat_reads SET last_read_message_id = ? WHERE user_id = ? AND activity_id = ?",
                last, userId, groupId);
        if (changed == 0)
            Database.update("INSERT INTO chat_reads (user_id, activity_id, last_read_message_id) VALUES (?, ?, ?)",
                    userId, groupId, last);
    }

    /** The user opened the chat with this person: their messages are now read. */
    public static void markDirectRead(int userId, int otherId) {
        Database.update("UPDATE direct_messages SET is_read = TRUE WHERE receiver_id = ? AND user_id = ? "
                + "AND is_read = FALSE", userId, otherId);
    }

    /** Newest direct message id and group message id right now (the starting point for new-message alerts). */
    public static int[] getLatestMessageIds() {
        return new int[]{Database.count("SELECT COALESCE(MAX(dm_id), 0) FROM direct_messages"),
                Database.count("SELECT COALESCE(MAX(message_id), 0) FROM group_messages")};
    }

    /**
     * NEW MESSAGES FOR THIS USER since the given ids (direct messages to them and messages in their group chats),
     * newest first. Used by the main window every few seconds to pop up "New message from ...".
     * Direct messages have groupId 0.
     */
    public static List<Message> getNewMessages(int userId, int afterDmId, int afterGroupMessageId) {
        List<Message> out = new ArrayList<>(Database.list(DM_SELECT + "WHERE d.receiver_id = ? AND d.dm_id > ? "
                + "AND d.user_id NOT IN (SELECT blocked_user_id FROM blocked_users WHERE user_id = ?) "
                + "ORDER BY d.dm_id DESC", Backend::mapDirectMessage, userId, afterDmId, userId));
        out.addAll(Database.list(MESSAGE_SELECT + "JOIN activities a ON a.activity_id = m.activity_id "
                + "WHERE m.message_id > ? AND m.user_id <> ? AND a.status <> 'CANCELLED' AND " + IN_MY_CHAT
                + "ORDER BY m.message_id DESC", Backend::mapGroupMessage, afterGroupMessageId, userId, userId, userId));
        out.sort((x, y) -> {
            if (x.getSentAt() == null || y.getSentAt() == null) return 0;
            return y.getSentAt().compareTo(x.getSentAt());
        });
        return out;
    }

    // =================================================================
    // 7. SAFETY AND REVIEWS
    // =================================================================

    /** REPORT a user, a trip or one group message. */
    public static void report(Report r) {
        if (r.getReason() == null || r.getReason().isBlank()) throw new RuntimeException("Please choose a reason.");
        Integer user = r.getTargetType() == ReportTarget.USER ? r.getTargetId() : null;
        Integer activity = r.getTargetType() == ReportTarget.GROUP ? r.getTargetId() : null;
        Integer message = r.getTargetType() == ReportTarget.MESSAGE ? r.getTargetId() : null;
        Database.insert("INSERT INTO reports (user_id, reported_user_id, activity_id, message_id, "
                + "reason, details) VALUES (?, ?, ?, ?, ?, ?)", r.getReporterId(), user, activity, message, r.getReason(), r.getDetails());
        Toast.show("Report sent. Thank you, our team will review it.");
    }

    public static void blockUser(int blockerId, int blockedId) {
        if (blockerId == blockedId) throw new RuntimeException("You can't block yourself.");
        Database.update("INSERT IGNORE INTO blocked_users (user_id, blocked_user_id) VALUES (?, ?)", blockerId, blockedId);
        Toast.show("User blocked");
    }

    public static void unblockUser(int blockerId, int blockedId) {
        Database.update("DELETE FROM blocked_users WHERE user_id = ? AND blocked_user_id = ?", blockerId, blockedId);
        Toast.show("User unblocked");
    }

    public static List<User> getBlockedUsers(int userId) {
        return Database.list("SELECT u.* FROM blocked_users bl JOIN users u ON u.user_id = bl.blocked_user_id "
                + "WHERE bl.user_id = ? ORDER BY bl.created_at DESC", Backend::mapUser, userId);
    }

    public static boolean isBlocked(int blockerId, int blockedId) {
        return Database.exists("SELECT 1 FROM blocked_users WHERE user_id = ? AND blocked_user_id = ?", blockerId, blockedId);
    }

    /**
     * RATE A COMPANION (revieweeId = that user) or REVIEW THE TRIP (revieweeId = 0).
     * Written through the reviewer's approved booking, so only travelers who joined can review (organizers get reviewed).
     */
    public static void addReview(Review r) {
        TravelGroup g = getGroup(r.getGroupId());
        if (!g.isCompleted()) throw new RuntimeException("You can only review after the trip is completed.");
        if (r.getRating() < 1 || r.getRating() > 5) throw new RuntimeException("Please give 1 to 5 stars.");
        int bookingId = bookingOf(r.getGroupId(), r.getReviewerId());
        if (bookingId == 0) throw new RuntimeException("Only travelers who joined this trip can write reviews.");
        Database.insert("INSERT INTO reviews (booking_id, reviewed_user_id, rating, comment) VALUES (?, ?, ?, ?)",
                bookingId, r.getRevieweeId() == 0 ? null : r.getRevieweeId(), r.getRating(), r.getComment());
    }

    /** True if this user joined the trip through a booking (only they can write reviews). */
    public static boolean canReview(int groupId, int userId) {
        return bookingOf(groupId, userId) != 0;
    }

    /** Ratings other travelers gave this user, newest first. */
    public static List<Review> getReviewsForUser(int userId) {
        return Database.list(REVIEW_SELECT + "WHERE r.reviewed_user_id = ? ORDER BY r.created_at DESC", Backend::mapReview, userId);
    }

    /** Reviews of the trip itself. */
    public static List<Review> getTripReviews(int groupId) {
        return Database.list(REVIEW_SELECT + "WHERE b.activity_id = ? AND r.reviewed_user_id IS NULL "
                + "ORDER BY r.created_at DESC", Backend::mapReview, groupId);
    }

    public static double getAverageRating(int userId) {
        return Database.number("SELECT COALESCE(AVG(rating), 0) FROM reviews WHERE reviewed_user_id = ?", userId);
    }

    public static boolean hasReviewedTrip(int groupId, int reviewerId) {
        return Database.exists("SELECT 1 FROM reviews r JOIN bookings b ON b.booking_id = r.booking_id "
                + "WHERE b.activity_id = ? AND b.user_id = ? AND r.reviewed_user_id IS NULL", groupId, reviewerId);
    }

    public static boolean hasRatedCompanions(int groupId, int reviewerId) {
        return Database.exists("SELECT 1 FROM reviews r JOIN bookings b ON b.booking_id = r.booking_id "
                + "WHERE b.activity_id = ? AND b.user_id = ? AND r.reviewed_user_id IS NOT NULL", groupId, reviewerId);
    }

    // =================================================================
    // 8. NOTIFICATIONS
    // =================================================================

    public static List<Notification> getNotifications(int userId) {
        return Database.list("SELECT * FROM notifications WHERE user_id = ? "
                + "ORDER BY created_at DESC, notification_id DESC", Backend::mapNotification, userId);
    }

    public static int getUnreadCount(int userId) {
        return Database.count("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE", userId);
    }

    public static void markAllRead(int userId) {
        Database.update("UPDATE notifications SET is_read = TRUE WHERE user_id = ?", userId);
    }

    // =================================================================
    // 9. ADMIN (every method checks that the logged-in user is an admin)
    // =================================================================

    // ----- 9.1 Dashboard overview -----

    public static AdminStats getAdminStats() {
        requireAdmin();
        AdminStats a = new AdminStats();
        a.setTotalUsers(Database.count("SELECT COUNT(*) FROM users WHERE role = 'TRAVELER' AND account_status <> 'REMOVED'"));
        a.setTotalGroups(Database.count("SELECT COUNT(*) FROM activities"));
        a.setActiveTrips(Database.count("SELECT COUNT(*) FROM activities WHERE status = 'OPEN' AND end_date >= CURDATE()"));
        a.setCompletedTrips(Database.count("SELECT COUNT(*) FROM activities WHERE status = 'OPEN' AND end_date < CURDATE()"));
        a.setPendingReports(Database.count("SELECT COUNT(*) FROM reports WHERE status IN ('PENDING', 'UNDER_REVIEW')"));
        a.setPendingVerifications(Database.count("SELECT COUNT(*) FROM id_verifications WHERE status = 'PENDING'"));
        return a;
    }

    /** MOST VISITED PLACES / POPULAR DESTINATIONS: travelers (members) per destination. */
    public static List<ChartPoint> getMostVisitedPlaces(int limit) {
        return Database.list("SELECT a.destination, SUM" + MEMBER_COUNT + " AS travelers FROM activities a "
                        + "WHERE a.status = 'OPEN' GROUP BY a.destination ORDER BY travelers DESC, a.destination LIMIT ?",
                rs -> new ChartPoint(rs.getString("destination"), rs.getInt("travelers")), limit);
    }

    /** TOP PERFORMING CREATORS: most completed trips, then best rating. */
    public static List<CreatorStat> getTopCreators(int limit) {
        return Database.list("SELECT u.*, x.groups_created, x.completed_trips, "
                        + "(SELECT COUNT(*) FROM bookings hb JOIN activities ha ON ha.activity_id = hb.activity_id "
                        + "   WHERE ha.user_id = u.user_id AND hb.status = 'APPROVED') AS hosted, "
                        + "(SELECT COALESCE(AVG(rv.rating), 0) FROM reviews rv WHERE rv.reviewed_user_id = u.user_id) AS rating "
                        + "FROM users u JOIN ("
                        + "  SELECT user_id, COUNT(*) AS groups_created, "
                        + "         SUM(CASE WHEN end_date < CURDATE() THEN 1 ELSE 0 END) AS completed_trips "
                        + "  FROM activities WHERE status = 'OPEN' GROUP BY user_id) x ON x.user_id = u.user_id "
                        + "ORDER BY x.completed_trips DESC, rating DESC, x.groups_created DESC LIMIT ?",
                rs -> {
                    CreatorStat c = new CreatorStat();
                    c.setUser(mapUser(rs));
                    c.setGroupsCreated(rs.getInt("groups_created"));
                    c.setCompletedTrips(rs.getInt("completed_trips"));
                    c.setTravelersHosted(rs.getInt("hosted"));
                    c.setRating(rs.getDouble("rating"));
                    return c;
                }, limit);
    }

    // ----- 9.2 User management -----

    /** ALL TRAVELERS. keyword matches name or email (empty = all). status null = all. */
    public static List<User> getAllUsers(String keyword, AccountStatus status) {
        requireAdmin();
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE role = 'TRAVELER' ");
        List<Object> p = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append("AND (full_name LIKE ? OR email LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            p.add(like); p.add(like);
        }
        if (status != null) { sql.append("AND account_status = ? "); p.add(status); }
        sql.append("ORDER BY created_at DESC, user_id DESC");
        return Database.list(sql.toString(), Backend::mapUser, p.toArray());
    }

    public static int countReportsAgainstUser(int userId) {
        return Database.count("SELECT COUNT(*) FROM reports WHERE reported_user_id = ?", userId);
    }

    public static List<Report> getReportsAgainstUser(int userId) {
        return Database.list(REPORT_SELECT + "WHERE r.reported_user_id = ? ORDER BY r.created_at DESC", Backend::mapReport, userId);
    }

    /** Number of trips a user is in (organized + joined). */
    public static int countGroupsOf(int userId) {
        return Database.count("SELECT (SELECT COUNT(*) FROM activities WHERE user_id = ?) "
                + "+ (SELECT COUNT(*) FROM bookings WHERE user_id = ? AND status = 'APPROVED')", userId, userId);
    }

    /** SUSPEND: the user can't log in until reactivated. */
    public static void suspendUser(int userId, String reason) {
        requireAdmin();
        Database.update("UPDATE users SET account_status = 'SUSPENDED' WHERE user_id = ?", userId);
        notify(userId, "Your account was suspended: " + reason, 0);
        log(LOG_SUSPEND, userName(userId), reason);
        Toast.show("Account suspended");
    }

    public static void reactivateUser(int userId) {
        requireAdmin();
        Database.update("UPDATE users SET account_status = 'ACTIVE' WHERE user_id = ?", userId);
        notify(userId, "Your account was reactivated. Welcome back!", 0);
        log(LOG_REACTIVATE, userName(userId), null);
        Toast.show("Account reactivated");
    }

    /** REMOVE: the row stays for records, but the account can't be used anymore. */
    public static void removeUser(int userId, String reason) {
        requireAdmin();
        Database.update("UPDATE users SET account_status = 'REMOVED' WHERE user_id = ?", userId);
        log(LOG_REMOVE, userName(userId), reason);
        Toast.show("Account removed");
    }

    // ----- 9.3 ID verifications -----

    /** Uploaded IDs. status null = all. */
    public static List<IdVerification> getVerifications(VerificationStatus status) {
        requireAdmin();
        String sql = "SELECT v.*, u.full_name, u.email, u.created_at, u.birthdate FROM id_verifications v "
                + "JOIN users u ON u.user_id = v.user_id ";
        if (status == null) return Database.list(sql + "ORDER BY v.verification_id DESC", Backend::mapVerification);
        return Database.list(sql + "WHERE v.status = ? ORDER BY v.verification_id DESC", Backend::mapVerification, status);
    }

    public static void approveVerification(int verificationId) {
        int userId = reviewVerification(verificationId, VerificationStatus.APPROVED,
                "Your ID was verified. You can now join and create trips!");
        log(LOG_APPROVE_ID, userName(userId), null);
        Toast.show("ID approved");
    }

    public static void rejectVerification(int verificationId) {
        rejectVerification(verificationId, null);
    }

    /** Reasons an admin can pick when rejecting an ID. */
    public static final String[] REJECT_REASONS = {
            "The ID photo is blurry or cut off",
            "The face photo doesn't match the ID",
            "The name doesn't match the account",
            "The birthday doesn't match the account",
            "The ID is expired or not a valid government ID",
            "The face photo is missing or unclear"};

    /** REJECT with a reason the traveler can act on. */
    public static void rejectVerification(int verificationId, String reason) {
        String why = reason == null || reason.isBlank() ? "" : " Reason: " + reason.trim() + ".";
        int userId = reviewVerification(verificationId, VerificationStatus.REJECTED,
                "We couldn't verify your ID." + why + " Please upload a new ID photo and face photo in your Profile.");
        log(LOG_REJECT_ID, userName(userId), reason);
        Toast.show("ID rejected");
    }

    private static int reviewVerification(int verificationId, VerificationStatus status, String message) {
        requireAdmin();
        Integer userId = Database.one("SELECT user_id FROM id_verifications WHERE verification_id = ?",
                rs -> rs.getInt(1), verificationId);
        if (userId == null) throw new RuntimeException("ID check not found.");
        Database.update("UPDATE id_verifications SET status = ?, reviewed_by = ?, "
                + "reviewed_at = NOW() WHERE verification_id = ?", status, currentUserId(), verificationId);
        notify(userId, message, 0);
        return userId;
    }

    // ----- 9.4 Reports and complaints -----

    /** REPORTS LIST. status null = all. */
    public static List<Report> getReports(ReportStatus status) {
        requireAdmin();
        if (status == null) return Database.list(REPORT_SELECT + "ORDER BY r.created_at DESC, r.report_id DESC", Backend::mapReport);
        return Database.list(REPORT_SELECT + "WHERE r.status = ? ORDER BY r.created_at DESC, r.report_id DESC",
                Backend::mapReport, status);
    }

    /** THE ONE REPORTED MESSAGE. Admins never see the rest of the conversation. */
    public static Message getReportedMessage(int messageId) {
        requireAdmin();
        return Database.one(MESSAGE_SELECT + "WHERE m.message_id = ?", Backend::mapGroupMessage, messageId);
    }

    /** SAVE THE ADMIN'S DECISION. Optionally notifies the affected user. The reporter is told when it's closed. */
    public static void updateReport(int reportId, ReportStatus status, String actionTaken, boolean notifyUser) {
        requireAdmin();
        Report r = Database.one(REPORT_SELECT + "WHERE r.report_id = ?", Backend::mapReport, reportId);
        if (r == null) throw new RuntimeException("Report not found.");
        Database.update("UPDATE reports SET status = ?, action_taken = ?, reviewed_by = ?, reviewed_at = NOW() "
                + "WHERE report_id = ?", status, actionTaken, currentUserId(), reportId);
        if (notifyUser) {
            int affected = affectedUser(r);
            if (affected > 0) notify(affected, "A report about your " + targetWord(r.getTargetType())
                    + " was reviewed by the SabayLakbay team. Action: " + actionTaken + ".", 0);
        }
        if (status == ReportStatus.RESOLVED || status == ReportStatus.DISMISSED)
            notify(r.getReporterId(), "Your report (" + r.getReason() + ") was reviewed. Thank you for helping keep SabayLakbay safe.", 0);
        log(LOG_REPORT, r.getReason() + " - " + r.getTargetName(), AdminDashboardText.status(status)
                + (actionTaken == null || actionTaken.isBlank() ? "" : ": " + actionTaken));
        Toast.show("Decision saved");
    }

    private static String targetWord(ReportTarget t) {
        return t == ReportTarget.USER ? "account" : t == ReportTarget.GROUP ? "trip" : "message";
    }

    private static int affectedUser(Report r) {
        switch (r.getTargetType()) {
            case USER: return r.getTargetId();
            case GROUP: return Database.count("SELECT user_id FROM activities WHERE activity_id = ?", r.getTargetId());
            default: return Database.count("SELECT user_id FROM group_messages WHERE message_id = ?", r.getTargetId());
        }
    }

    // ----- 9.5 Reports and analytics -----

    /** NEW REGISTRATIONS per month (last N months). */
    public static List<ChartPoint> getRegistrationsPerMonth(int months) {
        Map<String, Integer> counts = perMonth("SELECT DATE_FORMAT(created_at, '%Y-%m') AS ym, COUNT(*) AS n FROM users "
                + "WHERE role = 'TRAVELER' AND created_at >= ? GROUP BY ym", months);
        List<ChartPoint> out = new ArrayList<>();
        for (YearMonth ym : lastMonths(months)) out.add(new ChartPoint(label(ym), counts.getOrDefault(ym.toString(), 0)));
        return out;
    }

    /** TRIPS CREATED (first) and COMPLETED (second) per month. */
    public static List<MonthlyCount> getTripsPerMonth(int months) {
        return monthly(months,
                "SELECT DATE_FORMAT(created_at, '%Y-%m') AS ym, COUNT(*) AS n FROM activities WHERE created_at >= ? GROUP BY ym",
                "SELECT DATE_FORMAT(end_date, '%Y-%m') AS ym, COUNT(*) AS n FROM activities "
                        + "WHERE status = 'OPEN' AND end_date < CURDATE() AND end_date >= ? GROUP BY ym");
    }

    /** REPORTS FILED (first) and RESOLVED (second) per month. */
    public static List<MonthlyCount> getReportsPerMonth(int months) {
        return monthly(months,
                "SELECT DATE_FORMAT(created_at, '%Y-%m') AS ym, COUNT(*) AS n FROM reports WHERE created_at >= ? GROUP BY ym",
                "SELECT DATE_FORMAT(reviewed_at, '%Y-%m') AS ym, COUNT(*) AS n FROM reports "
                        + "WHERE status = 'RESOLVED' AND reviewed_at >= ? GROUP BY ym");
    }

    /** EXPORT SUMMARY REPORT as a CSV file (opens in Excel). */
    public static void exportSummaryReport(int months, File file) {
        requireAdmin();
        File f = file.getName().toLowerCase().endsWith(".csv") ? file : new File(file.getPath() + ".csv");
        AdminStats s = getAdminStats();
        try (PrintWriter w = new PrintWriter(f, StandardCharsets.UTF_8)) {
            w.println("SabayLakbay summary report,Generated " + LocalDate.now() + ",Last " + months + " months");
            w.println();
            w.println("Totals");
            w.println("Registered travelers," + s.getTotalUsers());
            w.println("Trips," + s.getTotalGroups());
            w.println("Active trips," + s.getActiveTrips());
            w.println("Completed trips," + s.getCompletedTrips());
            w.println("Pending reports," + s.getPendingReports());
            w.println("Pending ID checks," + s.getPendingVerifications());
            w.println();
            w.println("Month,New registrations,Trips created,Trips completed,Reports filed,Reports resolved");
            List<ChartPoint> regs = getRegistrationsPerMonth(months);
            List<MonthlyCount> trips = getTripsPerMonth(months);
            List<MonthlyCount> reps = getReportsPerMonth(months);
            for (int i = 0; i < regs.size(); i++)
                w.println(regs.get(i).getLabel() + "," + (int) regs.get(i).getValue() + "," + trips.get(i).getFirst() + ","
                        + trips.get(i).getSecond() + "," + reps.get(i).getFirst() + "," + reps.get(i).getSecond());
            w.println();
            w.println("Popular destinations,Travelers");
            for (ChartPoint p : getMostVisitedPlaces(10)) w.println(csv(p.getLabel()) + "," + (int) p.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Could not save the file: " + e.getMessage(), e);
        }
        Toast.show("Summary saved to " + f.getName());
    }

    // ----- 9.6 Trips (admin) -----

    /** ALL TRIPS for admins. tab: 0 upcoming, 1 happening now, 2 completed, 3 cancelled, 4 all. */
    public static List<TravelGroup> getAllTrips(String keyword, int tab) {
        requireAdmin();
        StringBuilder sql = new StringBuilder(GROUP_SELECT + "WHERE 1 = 1 ");
        List<Object> p = new ArrayList<>();
        switch (tab) {
            case 0: sql.append("AND a.status = 'OPEN' AND a.start_date > CURDATE() "); break;
            case 1: sql.append("AND a.status = 'OPEN' AND a.start_date <= CURDATE() AND a.end_date >= CURDATE() "); break;
            case 2: sql.append("AND a.status = 'OPEN' AND a.end_date < CURDATE() "); break;
            case 3: sql.append("AND a.status = 'CANCELLED' "); break;
            default: break;
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append("AND (a.title LIKE ? OR a.destination LIKE ? OR u.full_name LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            p.add(like); p.add(like); p.add(like);
        }
        sql.append(tab == 2 || tab == 3 ? "ORDER BY a.end_date DESC, a.activity_id DESC" : "ORDER BY a.start_date ASC, a.activity_id ASC");
        return Database.list(sql.toString(), Backend::mapGroup, p.toArray());
    }

    public static int countReportsAgainstTrip(int groupId) {
        return Database.count("SELECT COUNT(*) FROM reports WHERE activity_id = ? AND message_id IS NULL", groupId);
    }

    public static List<Report> getReportsAgainstTrip(int groupId) {
        requireAdmin();
        return Database.list(REPORT_SELECT + "WHERE r.activity_id = ? AND r.message_id IS NULL "
                + "ORDER BY r.created_at DESC", Backend::mapReport, groupId);
    }

    /** CANCEL A TRIP AS ADMIN (unsafe, fake, or breaks the rules). The organizer and members are told why. */
    public static void adminCancelTrip(int groupId, String reason) {
        requireAdmin();
        TravelGroup g = getGroup(groupId);
        if (g.isCancelled()) throw new RuntimeException("This trip is already cancelled.");
        Database.update("UPDATE activities SET status = 'CANCELLED' WHERE activity_id = ?", groupId);
        Database.update("UPDATE bookings SET status = 'CANCELLED' WHERE activity_id = ? AND status = 'PENDING'", groupId);
        notifyMembers(groupId, 0, "The trip \"" + g.getTitle() + "\" was cancelled by the SabayLakbay team. Reason: " + reason);
        log(LOG_CANCEL_TRIP, g.getTitle(), reason);
        Toast.show("Trip cancelled");
    }

    // ----- 9.7 Announcements -----

    /** SEND TO EVERY ACTIVE TRAVELER (or only verified ones). Returns how many got it. */
    public static int sendAnnouncement(String message, boolean verifiedOnly) {
        requireAdmin();
        String text = message == null ? "" : message.trim();
        if (text.isEmpty()) throw new RuntimeException("Write the announcement first.");
        if (text.length() > 450) throw new RuntimeException("Keep the announcement under 450 characters.");
        String sql = "SELECT * FROM users u WHERE u.role = 'TRAVELER' AND u.account_status = 'ACTIVE' "
                + (verifiedOnly ? "AND EXISTS (SELECT 1 FROM id_verifications v WHERE v.user_id = u.user_id "
                + "AND v.status = 'APPROVED') " : "");
        List<User> to = Database.list(sql, Backend::mapUser);
        for (User u : to) notify(u.getId(), "Announcement: " + text, 0);
        log(LOG_ANNOUNCE, (verifiedOnly ? "Verified travelers" : "All travelers") + " (" + to.size() + ")", text);
        Toast.show("Announcement sent to " + to.size() + (to.size() == 1 ? " traveler" : " travelers"));
        return to.size();
    }

    public static List<AdminLog> getAnnouncements() {
        return getAdminLogs(LOG_ANNOUNCE, null);
    }

    // ----- 9.8 Account corrections and privacy requests -----

    /** FIX A BIRTHDAY after checking it against the traveler's ID (travelers can't change it themselves). */
    public static void correctBirthdate(int userId, LocalDate birthdate, String note) {
        requireAdmin();
        if (birthdate == null) throw new RuntimeException("Pick the birthday shown on the ID.");
        if (birthdate.isAfter(LocalDate.now())) throw new RuntimeException("The birthday can't be in the future.");
        User u = getUser(userId);
        Database.update("UPDATE users SET birthdate = ? WHERE user_id = ?", birthdate, userId);
        notify(userId, "Your birthday was corrected to " + birthdate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                + " after checking your ID.", 0);
        log(LOG_BIRTHDAY, u.getFullName(), (u.getBirthdate() == null ? "none" : u.getBirthdate().toString()) + " to " + birthdate
                + (note == null || note.isBlank() ? "" : " (" + note.trim() + ")"));
        Toast.show("Birthday corrected");
    }

    /**
     * ERASE PERSONAL DATA of a removed account (Data Privacy Act: right to erasure).
     * The row stays so trips, reviews and reports still make sense, but nothing personal is left.
     */
    public static void erasePersonalData(int userId, String reason) {
        requireAdmin();
        User u = getUser(userId);
        if (u.getStatus() != AccountStatus.REMOVED)
            throw new RuntimeException("Remove the account first. Personal data is only erased from closed accounts.");
        if (u.getEmail() != null && u.getEmail().endsWith("@erased.sabaylakbay"))
            throw new RuntimeException("This account's data was already erased.");
        // delete the photo files on this computer
        List<String> files = new ArrayList<>();
        if (u.getAvatarPath() != null) files.add(u.getAvatarPath());
        for (String[] f : Database.list("SELECT id_photo_path, selfie_path FROM id_verifications "
                + "WHERE user_id = ?", rs -> new String[]{rs.getString(1), rs.getString(2)}, userId)) {
            if (f[0] != null) files.add(f[0]);
            if (f[1] != null) files.add(f[1]);
        }
        for (String f : files) {
            try { Files.deleteIfExists(Path.of(f)); } catch (Exception ignore) { /* already gone */ }
        }
        Database.update("UPDATE users SET full_name = 'Deleted user', email = ?, password_hash = ?, "
                    + "phone = NULL, profile_picture = NULL, birthdate = NULL, gender = NULL, "
                    + "emergency_contact_name = NULL, emergency_contact_phone = NULL, street = NULL, barangay = NULL, "
                    + "city = NULL, province = NULL, zip_code = NULL, latitude = NULL, longitude = NULL "
                + "WHERE user_id = ?", "user" + userId + "@erased.sabaylakbay", UUID.randomUUID().toString().replace("-", ""), userId);
        Database.update("DELETE FROM user_interests WHERE user_id = ?", userId);
        Database.update("UPDATE id_verifications SET id_photo_path = 'erased', selfie_path = NULL "
                + "WHERE user_id = ?", userId);
        log(LOG_ERASE, "User #" + userId, reason);
        Toast.show("Personal data erased");
    }

    // ----- 9.9 Activity log -----

    public static final String LOG_APPROVE_ID = "Approved ID", LOG_REJECT_ID = "Rejected ID",
            LOG_SUSPEND = "Suspended account", LOG_REACTIVATE = "Reactivated account", LOG_REMOVE = "Removed account",
            LOG_ERASE = "Erased personal data", LOG_BIRTHDAY = "Corrected birthday", LOG_CANCEL_TRIP = "Cancelled trip",
            LOG_ANNOUNCE = "Sent announcement", LOG_REPORT = "Report decision";
    public static final String[] LOG_ACTIONS = {LOG_APPROVE_ID, LOG_REJECT_ID, LOG_SUSPEND, LOG_REACTIVATE, LOG_REMOVE,
            LOG_ERASE, LOG_BIRTHDAY, LOG_CANCEL_TRIP, LOG_ANNOUNCE, LOG_REPORT};

    /** What admins did, newest first. action null = all. keyword matches the target, details or admin. */
    public static List<AdminLog> getAdminLogs(String action, String keyword) {
        requireAdmin();
        ensureMapColumns();
        StringBuilder sql = new StringBuilder("SELECT l.*, COALESCE(u.full_name, 'Admin') AS admin_name FROM admin_logs l "
                + "LEFT JOIN users u ON u.user_id = l.user_id WHERE 1 = 1 ");
        List<Object> p = new ArrayList<>();
        if (action != null) { sql.append("AND l.action = ? "); p.add(action); }
        if (keyword != null && !keyword.isBlank()) {
            sql.append("AND (l.target LIKE ? OR l.details LIKE ? OR u.full_name LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            p.add(like); p.add(like); p.add(like);
        }
        sql.append("ORDER BY l.created_at DESC, l.log_id DESC LIMIT 500");
        return Database.list(sql.toString(), rs -> {
            AdminLog l = new AdminLog();
            l.setId(rs.getInt("log_id"));
            l.setAdminName(rs.getString("admin_name"));
            l.setAction(rs.getString("action"));
            l.setTarget(rs.getString("target"));
            l.setDetails(rs.getString("details"));
            l.setCreatedAt(Database.dateTime(rs, "created_at"));
            return l;
        }, p.toArray());
    }

    /** EXPORT THE LOG as a CSV file (opens in Excel). */
    public static void exportAdminLogs(String action, String keyword, File file) {
        File f = file.getName().toLowerCase().endsWith(".csv") ? file : new File(file.getPath() + ".csv");
        try (PrintWriter w = new PrintWriter(f, StandardCharsets.UTF_8)) {
            w.println("Date and time,Admin,Action,About,Details");
            for (AdminLog l : getAdminLogs(action, keyword))
                w.println(l.getCreatedAt() + "," + csv(l.getAdminName()) + "," + csv(l.getAction()) + ","
                        + csv(l.getTarget() == null ? "" : l.getTarget()) + "," + csv(l.getDetails() == null ? "" : l.getDetails()));
        } catch (IOException e) {
            throw new RuntimeException("Could not save the file: " + e.getMessage(), e);
        }
        Toast.show("Log saved to " + f.getName());
    }

    private static void log(String action, String target, String details) {
        ensureMapColumns();
        Database.insert("INSERT INTO admin_logs (user_id, action, target, details) VALUES (?, ?, ?, ?)",
                currentUserId(), action, cut(target, 150), cut(details, 500));
    }

    private static String cut(String s, int max) {
        return s == null ? null : s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private static String userName(int userId) {
        String n = Database.one("SELECT full_name FROM users WHERE user_id = ?", rs -> rs.getString(1), userId);
        return n == null ? "User #" + userId : n;
    }

    /** Report status words for the log. */
    private static final class AdminDashboardText {
        static String status(ReportStatus s) {
            switch (s) {
                case PENDING: return "Pending";
                case UNDER_REVIEW: return "Under review";
                case RESOLVED: return "Resolved";
                default: return "Dismissed";
            }
        }
    }

    // =================================================================
    // helpers
    // =================================================================

    /** Small note at the bottom of the window. */
    public static void showConnected(String message) {
        Toast.show(message);
    }

    private static void requireAdmin() {
        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN)
            throw new RuntimeException("Only admins can do this.");
    }

    private static void requireVerifiedId(int userId) {
        switch (getVerificationStatus(userId)) {
            case APPROVED: return;
            case PENDING: throw new RuntimeException("Your ID is still being checked. You can join and create trips once it's approved.");
            default: throw new RuntimeException("Please upload a valid ID first (Profile > ID verification).");
        }
    }

    /** The user's approved booking for a trip, or 0. */
    private static int bookingOf(int groupId, int userId) {
        Integer id = Database.one("SELECT booking_id FROM bookings WHERE activity_id = ? AND user_id = ? "
                + "AND status = 'APPROVED' ORDER BY booking_id DESC LIMIT 1", rs -> rs.getInt(1), groupId, userId);
        return id == null ? 0 : id;
    }

    /** Copies an uploaded file into uploads/<folder>/ and returns the saved path. */
    private static String saveUpload(File source, String folder, String name) {
        try {
            String n = source.getName();
            String ext = n.contains(".") ? n.substring(n.lastIndexOf('.')).toLowerCase() : ".jpg";
            Path dir = Path.of(UPLOADS, folder);
            Files.createDirectories(dir);
            Path target = dir.resolve(name + "-" + System.currentTimeMillis() + ext);
            Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString().replace('\\', '/');
        } catch (IOException e) {
            throw new RuntimeException("Could not save the file: " + e.getMessage(), e);
        }
    }

    private static void notify(int userId, String message, int groupId) {
        Database.insert("INSERT INTO notifications (user_id, message, activity_id) VALUES (?, ?, ?)",
                userId, message, groupId == 0 ? null : groupId);
    }

    private static void notifyMembers(int groupId, int exceptUserId, String message) {
        for (User u : getMembers(groupId)) if (u.getId() != exceptUserId) notify(u.getId(), message, groupId);
    }

    private static JoinRequest findRequest(int requestId) {
        JoinRequest r = Database.one(REQUEST_SELECT + "WHERE b.booking_id = ?", Backend::mapRequest, requestId);
        if (r == null) throw new RuntimeException("Request not found.");
        return r;
    }

    private static void validateGroup(TravelGroup g, boolean creating) {
        if (g.getTitle() == null || g.getTitle().isBlank()) throw new RuntimeException("Trip name is required.");
        if (g.getDestination() == null || g.getDestination().isBlank()) throw new RuntimeException("Destination is required.");
        if (g.getCategory() == null || g.getCategory().isBlank()) throw new RuntimeException("Category is required.");
        if (g.getStartDate() == null || g.getEndDate() == null) throw new RuntimeException("Travel dates are required.");
        if (g.getEndDate().isBefore(g.getStartDate())) throw new RuntimeException("End date can't be before the start date.");
        if (creating && g.getStartDate().isBefore(LocalDate.now())) throw new RuntimeException("Start date can't be in the past.");
        if (g.getBudgetPerPerson() <= 0) throw new RuntimeException("Budget per person must be more than 0.");
        if (g.getMaxMembers() < 2) throw new RuntimeException("Max participants must be at least 2.");
    }

    // ----- months for the charts -----

    private static List<YearMonth> lastMonths(int months) {
        List<YearMonth> out = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) out.add(now.minusMonths(i));
        return out;
    }

    private static String label(YearMonth ym) {
        return ym.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH));
    }

    /** Runs a "GROUP BY month" query. The query has one ? for the first day of the period. */
    private static Map<String, Integer> perMonth(String sql, int months) {
        LocalDate from = YearMonth.now().minusMonths(months - 1).atDay(1);
        Map<String, Integer> out = new HashMap<>();
        for (Object[] row : Database.list(sql, rs -> new Object[]{rs.getString("ym"), rs.getInt("n")}, from))
            out.put((String) row[0], (Integer) row[1]);
        return out;
    }

    private static List<MonthlyCount> monthly(int months, String firstSql, String secondSql) {
        Map<String, Integer> a = perMonth(firstSql, months), b = perMonth(secondSql, months);
        List<MonthlyCount> out = new ArrayList<>();
        for (YearMonth ym : lastMonths(months))
            out.add(new MonthlyCount(label(ym), a.getOrDefault(ym.toString(), 0), b.getOrDefault(ym.toString(), 0)));
        return out;
    }

    private static String csv(String s) {
        return s.contains(",") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    // ----- turning rows into objects -----

    private static User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("user_id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setAvatarPath(rs.getString("profile_picture"));
        u.setBirthdate(Database.date(rs, "birthdate"));
        u.setGender(rs.getString("gender"));
        u.setEmergencyName(rs.getString("emergency_contact_name"));
        u.setEmergencyPhone(rs.getString("emergency_contact_phone"));
        u.setStreet(rs.getString("street"));
        u.setBarangay(rs.getString("barangay"));
        u.setCity(rs.getString("city"));
        u.setProvince(rs.getString("province"));
        u.setZipCode(rs.getString("zip_code"));
        double lat = rs.getDouble("latitude");
        boolean noLat = rs.wasNull();
        double lng = rs.getDouble("longitude");
        if (!noLat && !rs.wasNull()) { u.setLatitude(lat); u.setLongitude(lng); }
        u.setRole(UserRole.valueOf(rs.getString("role")));
        u.setStatus(AccountStatus.valueOf(rs.getString("account_status")));
        u.setCreatedAt(Database.dateTime(rs, "created_at"));
        return u;
    }

    private static TravelGroup mapGroup(ResultSet rs) throws SQLException {
        TravelGroup g = new TravelGroup();
        g.setId(rs.getInt("activity_id"));
        g.setCreatorId(rs.getInt("user_id"));
        g.setCreatorName(rs.getString("organizer_name"));
        g.setTitle(rs.getString("title"));
        g.setCategory(rs.getString("category"));
        g.setDescription(rs.getString("description"));
        g.setDestination(rs.getString("destination"));
        g.setStartDate(Database.date(rs, "start_date"));
        g.setEndDate(Database.date(rs, "end_date"));
        g.setDifficulty(Difficulty.valueOf(rs.getString("difficulty")));
        g.setBudgetPerPerson(rs.getDouble("price_per_person"));
        g.setMaxMembers(rs.getInt("max_participants"));
        g.setCurrentMembers(rs.getInt("member_count"));
        g.setStatus(GroupStatus.valueOf(rs.getString("status")));
        g.setMeetingPlace(rs.getString("meeting_place"));
        g.setMeetingTime(Database.time(rs, "meeting_time"));
        g.setTransportMode(rs.getString("transport_mode"));
        g.setCreatorNotes(rs.getString("notes"));
        String cover = rs.getString("image_path");
        // no cover photo (or a missing sample picture): show a real photo of the destination
        if (cover == null || cover.isBlank() || cover.startsWith("/images/"))
            cover = sabaylakbay_1.model.Photos.forTrip(rs.getString("title"), rs.getString("destination"),
                    rs.getString("category"));
        g.setImagePath(cover);
        double lat = rs.getDouble("latitude");
        boolean noLat = rs.wasNull();
        double lng = rs.getDouble("longitude");
        if (!noLat && !rs.wasNull()) { g.setLatitude(lat); g.setLongitude(lng); }
        double mLat = rs.getDouble("meeting_latitude");
        boolean noMLat = rs.wasNull();
        double mLng = rs.getDouble("meeting_longitude");
        if (!noMLat && !rs.wasNull()) { g.setMeetingLatitude(mLat); g.setMeetingLongitude(mLng); }
        g.setCreatedAt(Database.dateTime(rs, "created_at"));
        return g;
    }

    private static JoinRequest mapRequest(ResultSet rs) throws SQLException {
        JoinRequest r = new JoinRequest();
        r.setId(rs.getInt("booking_id"));
        r.setUserId(rs.getInt("user_id"));
        r.setUserName(rs.getString("full_name"));
        r.setGroupId(rs.getInt("activity_id"));
        r.setGroupTitle(rs.getString("title"));
        r.setMessage(rs.getString("request_message"));
        r.setStatus(RequestStatus.valueOf(rs.getString("status")));
        r.setCreatedAt(Database.dateTime(rs, "requested_at"));
        return r;
    }

    private static Activity mapItinerary(ResultSet rs) throws SQLException {
        Activity a = new Activity();
        a.setId(rs.getInt("itinerary_id"));
        a.setGroupId(rs.getInt("activity_id"));
        a.setTitle(rs.getString("activity_name"));
        a.setLocation(rs.getString("location"));
        LocalDateTime when = Database.dateTime(rs, "scheduled_time");
        a.setDate(when.toLocalDate());
        a.setTime(when.toLocalTime());
        a.setDetails(rs.getString("details"));
        a.setPhotoPath(rs.getString("photo_path"));
        double lat = rs.getDouble("latitude");
        boolean noLat = rs.wasNull();
        double lng = rs.getDouble("longitude");
        if (!noLat && !rs.wasNull()) { a.setLatitude(lat); a.setLongitude(lng); }
        return a;
    }

    private static Expense mapExpense(ResultSet rs) throws SQLException {
        Expense e = new Expense();
        e.setId(rs.getInt("expense_id"));
        e.setGroupId(rs.getInt("activity_id"));
        e.setLabel(rs.getString("name"));
        e.setAmount(rs.getDouble("estimated_amount"));
        return e;
    }

    private static Message mapGroupMessage(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getInt("message_id"));
        m.setGroupId(rs.getInt("activity_id"));
        m.setSenderId(rs.getInt("user_id"));
        m.setSenderName(rs.getString("sender_name"));
        m.setContent(rs.getString("content"));
        m.setAnnouncement(rs.getBoolean("is_announcement"));
        m.setSentAt(Database.dateTime(rs, "sent_at"));
        return m;
    }

    private static Message mapDirectMessage(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getInt("dm_id"));
        m.setSenderId(rs.getInt("user_id"));
        m.setSenderName(rs.getString("sender_name"));
        m.setRecipientId(rs.getInt("receiver_id"));
        m.setContent(rs.getString("content"));
        m.setSentAt(Database.dateTime(rs, "sent_at"));
        return m;
    }

    private static Review mapReview(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setId(rs.getInt("review_id"));
        r.setGroupId(rs.getInt("activity_id"));
        r.setReviewerId(rs.getInt("user_id"));
        r.setReviewerName(rs.getString("reviewer_name"));
        r.setRevieweeId(rs.getInt("reviewed_user_id"));   // NULL becomes 0 = review of the trip
        r.setRating(rs.getInt("rating"));
        r.setComment(rs.getString("comment"));
        r.setCreatedAt(Database.dateTime(rs, "created_at"));
        return r;
    }

    private static Report mapReport(ResultSet rs) throws SQLException {
        Report r = new Report();
        r.setId(rs.getInt("report_id"));
        r.setReporterId(rs.getInt("user_id"));
        r.setReporterName(rs.getString("reporter_name"));
        int user = rs.getInt("reported_user_id"), activity = rs.getInt("activity_id"), message = rs.getInt("message_id");
        if (message > 0) { r.setTargetType(ReportTarget.MESSAGE); r.setTargetId(message); }
        else if (user > 0) { r.setTargetType(ReportTarget.USER); r.setTargetId(user); }
        else if (activity > 0) { r.setTargetType(ReportTarget.GROUP); r.setTargetId(activity); }
        else { r.setTargetType(ReportTarget.MESSAGE); r.setTargetId(0); }   // the reported message was deleted
        r.setTargetName(rs.getString("target_name"));
        r.setReason(rs.getString("reason"));
        r.setDetails(rs.getString("details"));
        r.setStatus(ReportStatus.valueOf(rs.getString("status")));
        r.setActionTaken(rs.getString("action_taken"));
        r.setReviewedAt(Database.dateTime(rs, "reviewed_at"));
        r.setCreatedAt(Database.dateTime(rs, "created_at"));
        return r;
    }

    private static Notification mapNotification(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("notification_id"));
        n.setUserId(rs.getInt("user_id"));
        n.setMessage(rs.getString("message"));
        n.setRelatedGroupId(rs.getInt("activity_id"));
        n.setRead(rs.getBoolean("is_read"));
        n.setCreatedAt(Database.dateTime(rs, "created_at"));
        return n;
    }

    private static IdVerification mapVerification(ResultSet rs) throws SQLException {
        IdVerification v = new IdVerification();
        v.setId(rs.getInt("verification_id"));
        v.setUserId(rs.getInt("user_id"));
        v.setUserName(rs.getString("full_name"));
        v.setUserEmail(rs.getString("email"));
        v.setIdType(rs.getString("id_type"));
        v.setFilePath(rs.getString("id_photo_path"));
        v.setSelfiePath(rs.getString("selfie_path"));
        v.setUserBirthdate(Database.date(rs, "birthdate"));
        v.setStatus(VerificationStatus.valueOf(rs.getString("status")));
        v.setUserCreatedAt(Database.dateTime(rs, "created_at"));
        v.setReviewedAt(Database.dateTime(rs, "reviewed_at"));
        return v;
    }
}
