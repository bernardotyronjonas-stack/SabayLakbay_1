package sabaylakbay_1.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DATABASE CONNECTION (for the backend).
 *
 * 1. Run database/schema.sql in MySQL (XAMPP / phpMyAdmin / Workbench).
 * 2. Add the MySQL driver: right-click Libraries > Add JAR/Folder > mysql-connector-j-x.x.x.jar
 * 3. Change URL / USER / PASSWORD below if yours are different.
 *
 * Example use inside Backend.java:
 *
 *   public static List<Expense> getExpenses(int groupId) {
 *       List<Expense> list = new ArrayList<>();
 *       String sql = "SELECT * FROM expenses WHERE group_id = ?";
 *       try (Connection con = Database.get(); PreparedStatement ps = con.prepareStatement(sql)) {
 *           ps.setInt(1, groupId);
 *           ResultSet rs = ps.executeQuery();
 *           while (rs.next()) {
 *               Expense e = new Expense();
 *               e.setId(rs.getInt("id"));
 *               e.setGroupId(rs.getInt("group_id"));
 *               e.setLabel(rs.getString("label"));
 *               e.setAmount(rs.getDouble("amount"));
 *               list.add(e);
 *           }
 *       } catch (SQLException ex) {
 *           throw new RuntimeException("Could not load expenses.", ex);
 *       }
 *       return list;
 *   }
 */
public final class Database {

    private static final String URL = "jdbc:mysql://localhost:3306/sabaylakbay";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private Database() { }

    public static Connection get() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException("Can't connect to the database. Is MySQL running?", e);
        }
    }
}
