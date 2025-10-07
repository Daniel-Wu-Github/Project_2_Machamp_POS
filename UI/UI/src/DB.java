import java.sql.*;

/**
 * Minimal DB helper that obtains a JDBC Connection from environment variables
 * or from a dbSetupExample class if available. Provides small helper methods
 * used by the UI to insert menu items and orders.
 *
 * Usage:
 *  - Set environment variables: JDBC_URL, DB_USER, DB_PASS
 *  - Or create a class named dbSetup with public fields `user` and `pswd` and
 *    reference a default JDBC URL in code (not included here for security).
 */
public final class DB {
    // Try to read from environment first
    private static String jdbcUrl = System.getenv("JDBC_URL");
    private static String dbUser = System.getenv("DB_USER");
    private static String dbPass = System.getenv("DB_PASS");

    static {
        // If env vars not set, try to fallback to dbSetupExample/dbSetup class if present
        if (jdbcUrl == null || dbUser == null || dbPass == null) {
            try {
                // Look for a helper class that stores credentials (not committed to repo)
                Class<?> cfg = Class.forName("dbSetup");
                Object instance = cfg.getDeclaredConstructor().newInstance();
                java.lang.reflect.Field u = cfg.getField("user");
                java.lang.reflect.Field p = cfg.getField("pswd");
                dbUser = (String) u.get(null);
                dbPass = (String) p.get(null);
                // If JDBC_URL is not provided, try csce-315 default used in examples
                if (jdbcUrl == null) {
                    jdbcUrl = System.getenv("JDBC_URL");
                }
            } catch (ClassNotFoundException cnfe) {
                // no dbSetup class available; keep env values as-is (likely null)
            } catch (Exception e) {
                // ignore other reflection errors but print to stderr for debugging
                System.err.println("DB fallback init error: " + e.getMessage());
            }
        }

        // If JDBC_URL still null, set a helpful default placeholder (won't connect)
        if (jdbcUrl == null) {
            jdbcUrl = System.getenv("JDBC_URL");
        }
    }

    private DB() {}

    public static Connection getConnection() throws SQLException {
        if (jdbcUrl == null || dbUser == null || dbPass == null) {
            throw new SQLException("Database credentials not configured. Set JDBC_URL, DB_USER, DB_PASS env vars or provide a dbSetup class.");
        }
        return DriverManager.getConnection(jdbcUrl, dbUser, dbPass);
    }

    /**
     * Insert a menu item into a menu_items table.
     * Assumes a schema like: menu_items(id serial primary key, name text, price numeric)
     */
    public static boolean insertMenuItem(String name, double price) {
        String sql = "INSERT INTO menu_items (name, price) VALUES (?, ?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setDouble(2, price);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("insertMenuItem error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Insert a simple order into an orders table. This is intentionally minimal.
     * Assumes a schema like: orders(id serial primary key, item_name text, size text, sugar text, toppings text, total numeric)
     */
    public static boolean insertOrder(String itemName, String size, String sugar, String toppings, double total) {
        String sql = "INSERT INTO orders (item_name, size, sugar, toppings, total) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, itemName);
            ps.setString(2, size);
            ps.setString(3, sugar);
            ps.setString(4, toppings);
            ps.setDouble(5, total);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("insertOrder error: " + e.getMessage());
            return false;
        }
    }
}
