package sabaylakbay_1.backend;


import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MYSQL CONNECTION + small helpers so Backend.java stays short and easy to read.
 *
 * SETUP:
 *   1. Start MySQL (XAMPP: click Start on MySQL).
 *   2. Run database/sabaylakbay.sql once (phpMyAdmin > SQL tab > paste > Go).
 *   3. Add the MySQL driver to the project: right-click Libraries > Add JAR/Folder > mysql-connector-j-9.x.x.jar
 *   4. If your MySQL user or password is different, change USER / PASSWORD below.
 */
public final class Database {

    // ===== CHANGE THESE IF YOUR MYSQL IS DIFFERENT =====
    private static final String HOST = "localhost";
    private static final int PORT = 3306;
    private static final String DATABASE = "sabaylakbay";
    private static final String USER = "root";
    private static final String PASSWORD = "tjpogi1821!";      // XAMPP default is empty

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Manila&characterEncoding=UTF-8";

    private Database() { }

    /** Turns one row into an object (e.g. a User). */
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /** One connection kept open while the app runs (much faster than opening one per query). */
    private static Connection shared;

    /** Opens a new connection to MySQL. */
    public static Connection connect() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("No suitable driver"))
                throw new RuntimeException("MySQL driver not found. Add mysql-connector-j.jar to the project Libraries.", e);
            throw new RuntimeException("Can't connect to MySQL. Make sure MySQL is running and the database "
                    + "'sabaylakbay' exists.\n\n" + e.getMessage(), e);
        }
    }

    private static synchronized Connection conn() throws SQLException {
        if (shared == null || shared.isClosed()) shared = connect();
        return shared;
    }

    private interface Call<R> {
        R run(Connection c) throws SQLException;
    }

    /** Runs a query on the shared connection. If MySQL was restarted, reconnects once and tries again. */
    private static <R> R run(String sql, Call<R> call) {
        try {
            return call.run(conn());
        } catch (SQLException e) {
            String state = e.getSQLState();
            if (state != null && state.startsWith("08")) {          // 08xxx = connection problem
                shared = null;
                try {
                    return call.run(conn());
                } catch (SQLException again) {
                    throw error(sql, again);
                }
            }
            throw error(sql, e);
        }
    }

    /** SELECT that returns many rows. */
    public static <T> List<T> list(String sql, RowMapper<T> mapper, Object... params) {
        return run(sql, c -> {
            List<T> out = new ArrayList<>();
            try (PreparedStatement ps = prepare(c, sql, false, params); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapper.map(rs));
            }
            return out;
        });
    }

    /** SELECT that returns one row, or null if nothing was found. */
    public static <T> T one(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = list(sql, mapper, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** SELECT COUNT(*) / SUM / AVG ... : returns the first column of the first row as a number. */
    public static double number(String sql, Object... params) {
        Double d = one(sql, rs -> rs.getDouble(1), params);
        return d == null ? 0 : d;
    }

    public static int count(String sql, Object... params) {
        return (int) number(sql, params);
    }

    /** True if the SELECT returns at least one row. */
    public static boolean exists(String sql, Object... params) {
        return one(sql, rs -> true, params) != null;
    }

    /** INSERT / UPDATE / DELETE. Returns how many rows changed. */
    public static int update(String sql, Object... params) {
        return run(sql, c -> {
            try (PreparedStatement ps = prepare(c, sql, false, params)) {
                return ps.executeUpdate();
            }
        });
    }

    /** INSERT that returns the new AUTO_INCREMENT id. */
    public static int insert(String sql, Object... params) {
        return run(sql, c -> insert(c, sql, params));
    }

    /** Runs several statements as one: either all of them save, or none (used for creating a group). */
    public interface Work {
        void run(Connection c) throws SQLException;
    }

    public static void transaction(Work work) {
        run("transaction", c -> {
            c.setAutoCommit(false);
            try {
                work.run(c);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
            return null;
        });
    }

    /** INSERT using a given connection (inside a transaction), returns the new id. */
    public static int insert(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = prepare(c, sql, true, params)) {
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    // ===== reading values safely =====

    public static LocalDate date(ResultSet rs, String col) throws SQLException {
        Date d = rs.getDate(col);
        return d == null ? null : d.toLocalDate();
    }

    public static LocalTime time(ResultSet rs, String col) throws SQLException {
        Time t = rs.getTime(col);
        return t == null ? null : t.toLocalTime();
    }

    public static LocalDateTime dateTime(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }

    // ===== internal =====

    private static PreparedStatement prepare(Connection c, String sql, boolean keys, Object... params) throws SQLException {
        PreparedStatement ps = keys ? c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS) : c.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            int at = i + 1;
            if (p == null) ps.setNull(at, Types.NULL);
            else if (p instanceof LocalDate) ps.setDate(at, Date.valueOf((LocalDate) p));
            else if (p instanceof LocalTime) ps.setTime(at, Time.valueOf((LocalTime) p));
            else if (p instanceof LocalDateTime) ps.setTimestamp(at, Timestamp.valueOf((LocalDateTime) p));
            else if (p instanceof Enum) ps.setString(at, ((Enum<?>) p).name());
            else if (p instanceof Boolean) ps.setBoolean(at, (Boolean) p);
            else ps.setObject(at, p);
        }
        return ps;
    }

    private static RuntimeException error(String sql, SQLException e) {
        System.err.println("[Database] " + e.getMessage() + "\n  SQL: " + sql);
        return new RuntimeException("Database error: " + e.getMessage(), e);
    }
}