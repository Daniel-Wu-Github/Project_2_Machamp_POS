import java.io.BufferedReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Database Manager for Machamp POS System using SQLite
 * Handles database connections, table creation
 */
public class DatabaseManager {
    private static final String DATABASE_URL = "jdbc:sqlite:machamp_pos.db";
    private Connection connection;

    
    /**
     * Constructor - initializes database connection and creates tables
     */
    public DatabaseManager() {
        this(false);
    }

    /**
     * @param reset If true, existing tables are dropped and recreated fresh each startup.
     */
    public DatabaseManager(boolean reset) {
        try {
            initializeDatabase();
            if (reset) {
                dropAllTables();
            }
            createTables();
            // Import historical orders if present (only once if table empty)
            importOrderHistoryIfEmpty("src/orders.csv");
            // insertSampleData();
            insertSampleIngredients();
            insertSampleDrinks();
            insertSampleEmployees();
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialize database connection
     */
    private void initializeDatabase() throws SQLException {
        connection = DriverManager.getConnection(DATABASE_URL);
        // Enforce foreign keys for future relational tables
        try (Statement fk = connection.createStatement()) {
            fk.execute("PRAGMA foreign_keys = ON");
        }
        System.out.println("Connected to SQLite database successfully!");
    }

    /**
     * Create necessary tables for the POS system
     */
    private void createTables() throws SQLException {
        String[] createTableQueries = {
            // Drinks table
            """
            CREATE TABLE IF NOT EXISTS drinks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                base_price DECIMAL(10,2) NOT NULL,
                ingredients TEXT NOT NULL,
                sugar_level TEXT,
                size TEXT,
                toppings TEXT,
                ice_level TEXT
            )
            """,
            // Order history table (imported from CSV)
            """
            CREATE TABLE IF NOT EXISTS orderhistory (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_datetime TEXT NOT NULL,
                order_id TEXT NOT NULL UNIQUE,
                customer_id TEXT NOT NULL,
                menu_items TEXT NOT NULL, -- stored as normalized JSON-like string
                total_price DECIMAL(10,2) NOT NULL
            )
            """,
            
            // Ingredients table
            """
            CREATE TABLE IF NOT EXISTS ingredients (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                cost DECIMAL(10,3) NOT NULL,
                quantity DECIMAL(10,3) NOT NULL
            )
            """,
            
            
            // Customers table
            """
            CREATE TABLE IF NOT EXISTS customers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                phone TEXT,
                email TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            // Employees table
            """
            CREATE TABLE IF NOT EXISTS employees (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                first_name TEXT NOT NULL,
                last_name  TEXT NOT NULL,
                email      TEXT NOT NULL UNIQUE,
                role       TEXT NOT NULL CHECK(role IN ('CASHIER','MANAGER')),
                active     INTEGER NOT NULL DEFAULT 1,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """
        };

        for (String query : createTableQueries) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(query);
            }
        }
        
        System.out.println("Database tables created successfully!");
    }

    // -------- Order History Import --------
    private void importOrderHistoryIfEmpty(String csvPath) {
        try {
            if (!isTableEmpty("orderhistory")) return;
        } catch (SQLException e) {
            System.err.println("Could not check orderhistory table: " + e.getMessage());
            return;
        }
        try {
            importOrderHistoryFromCSV(csvPath);
        } catch (Exception e) {
            System.err.println("Order history import failed: " + e.getMessage());
        }
    }

    /**
     * Imports orders from a CSV file with headers:
     * DateTime,Order ID,Customer ID,Menu Items,Total Price
     * Menu Items column contains a Python dict-like string: {'Drink': (a,b,c), ...}
     * We normalize it to a JSON-like string: {"Drink": [a,b,c], ...}
     */
    public void importOrderHistoryFromCSV(String path) throws Exception {
        java.nio.file.Path p = java.nio.file.Paths.get(path);
        if (!java.nio.file.Files.exists(p)) {
            System.err.println("Order CSV not found at " + path);
            return;
        }
        long start = System.currentTimeMillis();
        int imported = 0;
        try (BufferedReader br = java.nio.file.Files.newBufferedReader(p);
             PreparedStatement ps = connection.prepareStatement(
                     "INSERT OR IGNORE INTO orderhistory (order_datetime, order_id, customer_id, menu_items, total_price) VALUES (?,?,?,?,?)")) {
            connection.setAutoCommit(false);
            String header = br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                ParsedOrder po = parseOrderLine(line);
                if (po == null) continue; // skip malformed
                ps.setString(1, po.dateTime);
                ps.setString(2, po.orderId);
                ps.setString(3, po.customerId);
                ps.setString(4, po.menuItemsJson);
                ps.setBigDecimal(5, new java.math.BigDecimal(po.totalPrice));
                ps.addBatch();
                imported++;
                if (imported % 500 == 0) ps.executeBatch();
            }
            ps.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);
        } catch (Exception e) {
            try { connection.rollback(); } catch (SQLException ignore) {}
            throw e;
        }
        System.out.println("Imported " + imported + " order history rows in " + (System.currentTimeMillis()-start) + "ms");
    }

    /**
     * Internal helper to hold parsed order line.
     */
    private static class ParsedOrder {
        String dateTime; String orderId; String customerId; String menuItemsJson; String totalPrice;
    }

    /**
     * Parses a single CSV line. Avoids heavy CSV libs; relies on fixed column count and quoting on menu_items.
     */
    private ParsedOrder parseOrderLine(String line) {
        try {
            // Find first three commas
            int c1 = line.indexOf(',');
            int c2 = line.indexOf(',', c1+1);
            int c3 = line.indexOf(',', c2+1);
            if (c1<0||c2<0||c3<0) return null;
            // Last comma (before total price)
            int lastComma = line.lastIndexOf(',');
            if (lastComma <= c3) return null;
            String dateTime = line.substring(0,c1).trim();
            String orderId = line.substring(c1+1,c2).trim();
            String customerId = line.substring(c2+1,c3).trim();
            String itemsRaw = line.substring(c3+1,lastComma).trim();
            if (itemsRaw.startsWith("\"")) itemsRaw = itemsRaw.substring(1);
            if (itemsRaw.endsWith("\"")) itemsRaw = itemsRaw.substring(0, itemsRaw.length()-1);
            String total = line.substring(lastComma+1).trim();
            // Normalize itemsRaw: change single quotes to double quotes, tuple parens to brackets
            String jsonLike = itemsRaw
                .replace("'", "\"")
                .replace('(', '[')
                .replace(')', ']');
            ParsedOrder po = new ParsedOrder();
            po.dateTime = dateTime;
            po.orderId = orderId;
            po.customerId = customerId;
            po.menuItemsJson = jsonLike;
            po.totalPrice = total;
            return po;
        } catch (Exception e) {
            System.err.println("Failed to parse line: " + line);
            return null;
        }
    }

    /**
     * Drops all known tables (order chosen to avoid FK issues if added later).
     */
    private void dropAllTables() throws SQLException {
        String[] tables = {"employees", "drinks", "ingredients", "customers"};
        try (Statement st = connection.createStatement()) {
            for (String t : tables) {
                st.executeUpdate("DROP TABLE IF EXISTS " + t);
            }
        }
        System.out.println("All tables dropped (reset mode).");
    }

    // ---------- Employee helpers ----------
    private void insertSampleEmployees() throws SQLException {
        if (isTableEmpty("employees")) {
            String sql = """
                INSERT INTO employees (first_name,last_name,email,role,active) VALUES
                ('Alice','Nguyen','alice@example.com','CASHIER',1),
                ('Bob','Smith','bob@example.com','MANAGER',1)
            """;
            try (Statement st = connection.createStatement()) {
                st.execute(sql);
                System.out.println("Sample employees inserted!");
            }
        }
    }

    private boolean isTableEmpty(String table) throws SQLException {
        String q = "SELECT COUNT(*) FROM " + table;
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(q)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    public int addEmployee(String firstName, String lastName, String email, String role) throws SQLException {
        String sql = "INSERT INTO employees (first_name,last_name,email,role,active) VALUES (?,?,?,?,1)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.setString(4, role);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public List<String> listEmployees() throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT id, first_name, last_name, email, role, active FROM employees ORDER BY id";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(String.format("%d | %s %s | %s | %s | %s",
                        rs.getInt("id"), rs.getString("first_name"), rs.getString("last_name"),
                        rs.getString("email"), rs.getString("role"), rs.getInt("active") == 1 ? "ACTIVE" : "INACTIVE"));
            }
        }
        return list;
    }

    public boolean updateEmployeeRole(int id, String newRole) throws SQLException {
        String sql = "UPDATE employees SET role=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newRole);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean setEmployeeActive(int id, boolean active) throws SQLException {
        String sql = "UPDATE employees SET active=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    /** Retrieve a single employee record */
    public String getEmployee(int id) throws SQLException {
        String sql = "SELECT id, first_name, last_name, email, role, active FROM employees WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return String.format("%d | %s %s | %s | %s | %s", rs.getInt(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getString(5), rs.getInt(6)==1?"ACTIVE":"INACTIVE");
                }
            }
        }
        return null;
    }

    /** Update employee basic fields (except id & created_at). Null parameters keep existing values. */
    public boolean updateEmployee(int id, String firstName, String lastName, String email, String role, Boolean active) throws SQLException {
        // Build dynamic update based on provided non-null values
        StringBuilder sb = new StringBuilder("UPDATE employees SET ");
        List<Object> params = new ArrayList<>();
        if (firstName != null) { sb.append("first_name=?,"); params.add(firstName); }
        if (lastName != null)  { sb.append("last_name=?,");  params.add(lastName); }
        if (email != null)     { sb.append("email=?,");      params.add(email); }
        if (role != null)      { sb.append("role=?,");       params.add(role); }
        if (active != null)    { sb.append("active=?,");     params.add(active?1:0); }
        if (params.isEmpty()) return false; // nothing to update
        sb.setLength(sb.length()-1); // remove trailing comma
        sb.append(" WHERE id=?");
        params.add(id);
        try (PreparedStatement ps = connection.prepareStatement(sb.toString())) {
            for (int i=0;i<params.size();i++) ps.setObject(i+1, params.get(i));
            return ps.executeUpdate() == 1;
        }
    }

    /** List only active employees */
    public List<String> listActiveEmployees() throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT id, first_name, last_name, email, role FROM employees WHERE active=1 ORDER BY id";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(String.format("%d | %s %s | %s | %s", rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
            }
        }
        return list;
    }

    /**
     * Insert sample data if tables are empty
     */
    // private void insertSampleData() throws SQLException {
    //     // Check if ingredients table has data
    //     String checkIngredientsQuery = "SELECT COUNT(*) FROM ingredients";
    //     try (Statement stmt = connection.createStatement();
    //          ResultSet rs = stmt.executeQuery(checkIngredientsQuery)) {
            
    //         if (rs.next() && rs.getInt(1) == 0) {
    //             insertSampleIngredients();
    //         }
    //     }
        
    //     // Check if drinks table has data
    //     String checkQuery = "SELECT COUNT(*) FROM drinks";
    //     try (Statement stmt = connection.createStatement();
    //          ResultSet rs = stmt.executeQuery(checkQuery)) {
            
    //         if (rs.next() && rs.getInt(1) == 0) {
    //             insertSampleDrinks();
    //         }
    //     }
    // }
    
    /**
     * Insert sample ingredients
     */
    private void insertSampleIngredients() throws SQLException {
        if (!isTableEmpty("ingredients")) return;
        String insertIngredients = """
            INSERT INTO ingredients (name, cost, quantity) VALUES
            ('Sparkling Water', 0.001, 50),
            ('Milk', 0.008, 50),
            ('Sugar', 0.002, 50),
            ('Tea', 0.15, 50),
            ('Black Tea', 0.18, 50),
            ('Oolong Tea', 0.25, 50),
            ('Green Tea', 0.20, 50),
            ('Coffee', 0.12, 50),
            ('Cream', 0.015, 50),
            ('Coconut', 0.10, 50),
            ('Ube Powder', 0.35, 50),
            ('Protein Powder', 0.40, 50),
            ('Winter Melon', 0.08, 50),
            ('Passion Fruit', 0.12, 50),
            ('Mango', 0.09, 50),
            ('Strawberry Lemonade', 0.07, 50),
            ('Strawberry', 0.08, 50),
            ('Peach', 0.09, 50),
            ('Matcha', 0.45, 50)
        """;
        try (Statement insertStmt = connection.createStatement()) {
            insertStmt.execute(insertIngredients);
            System.out.println("Sample ingredients inserted!");
        }
        
    }
    
    /**
     * Insert sample drinks with ingredients
     */
    private void insertSampleDrinks() throws SQLException {
        if (!isTableEmpty("drinks")) return;
        String insertDrinks = """
            INSERT INTO drinks (name, base_price, ingredients, sugar_level, size, toppings, ice_level) VALUES
            ('Original Milk Tea', 5.25, '{Water, Milk, Sugar, Tea}', NULL, NULL, NULL, NULL),
            ('Black Milk Tea', 5.25, '{Water, Milk, Sugar, Black Tea}', NULL, NULL, NULL, NULL),
            ('Oolong Milk Tea', 5.25, '{Water, Milk, Sugar, Oolong Tea}', NULL, NULL, NULL, NULL),
            ('Green Milk Tea', 5.25, '{Water, Milk, Sugar, Green Tea}', NULL, NULL, NULL, NULL),
            ('Capuccino Milk Tea', 6.25, '{Water, Milk, Sugar, Coffee, Cream}', NULL, NULL, NULL, NULL),
            ('Coconut Milk Tea', 7.25, '{Water, Milk, Sugar, Tea, Coconut}', NULL, NULL, NULL, NULL),
            ('Ube Milk Tea', 7.25, '{Water, Milk, Sugar, Ube Powder}', NULL, NULL, NULL, NULL),
            ('Protein Shake Milk Tea', 9.75, '{Water, Milk, Sugar, Tea, Protein Powder}', NULL, NULL, NULL, NULL),
            ('Ice Blend Latte', 6.25, '{Water, Milk, Sugar, Tea, Protein Powder}', NULL, NULL, NULL, NULL),
            ('Winter Melon Green Tea', 8.25, '{Water, Sugar, Green Tea, Winter Melon}', NULL, NULL, NULL, NULL),
            ('Passionfruit Green Tea', 7.25, '{Water, Sugar, Passionfruit, Green Tea}', NULL, NULL, NULL, NULL),
            ('Mango Green Tea', 3.25, '{Water, Sugar, Green Tea, Mango}', NULL, NULL, NULL, NULL),
            ('Strawberry Lemonade Tea', 3.25, '{Water, Sugar, Green Tea, Strawberry Lemonade}', NULL, NULL, NULL, NULL),
            ('Strawberry Matcha', 7.25, '{Water, Sugar, Green Tea, Strawberry}', NULL, NULL, NULL, NULL),
            ('Peach Oolong Tea', 7.25, '{Water, Sugar, Oolong Tea, Peach}', NULL, NULL, NULL, NULL),
            ('Secret Matcha', 69.25, '{Water, Matcha}', NULL, NULL, NULL, NULL),
            ('Free Drink', 0.00, '{Water, Milk, Sugar, Tea}', NULL, NULL, NULL, NULL)
        """;
        try (Statement insertStmt = connection.createStatement()) {
            insertStmt.execute(insertDrinks);
            System.out.println("Sample drinks inserted!");
        }
        
    }

    // Optional: recreate only the employees table if schema adjustments are needed without full reset
    public void recreateEmployeesTable() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("DROP TABLE IF EXISTS employees");
        }
        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS employees (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    first_name TEXT NOT NULL,
                    last_name  TEXT NOT NULL,
                    email      TEXT NOT NULL UNIQUE,
                    role       TEXT NOT NULL CHECK(role IN ('CASHIER','MANAGER')),
                    active     INTEGER NOT NULL DEFAULT 1,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        }
        System.out.println("Employees table recreated.");
    }
    //moved locations
    //public List<Drink> getAllDrinks() {
        // TODO: Implement database query to fetch drinks
        //List<Drink> drinks = new ArrayList<>();
        //return drinks;
    //}

    /**
     * Close database connection
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }

    /**
     * Get a single connection for advanced operations
     */
    public Connection getConnection() {
        return connection;
    }

    // ================== DRINK (MENU) MANAGEMENT ==================
    /** Returns list of drinks with id, name, price */
    public List<String> listDrinks() throws SQLException {
        List<String> drinks = new ArrayList<>();
        String sql = "SELECT id, name, base_price FROM drinks ORDER BY id";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                drinks.add(String.format("%d | %s | $%.2f", rs.getInt(1), rs.getString(2), rs.getDouble(3)));
            }
        }
        return drinks;
    }

    /** Inserts a new drink (ingredients and customization columns optional) */
    public int addDrink(String name, double basePrice, String ingredients) throws SQLException {
        String sql = "INSERT INTO drinks (name, base_price, ingredients) VALUES (?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setDouble(2, basePrice);
            ps.setString(3, ingredients);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) return rs.getInt(1); }
        }
        return -1;
    }

    /** Update only base price for a drink */
    public boolean updateDrinkPrice(int drinkId, double newPrice) throws SQLException {
        String sql = "UPDATE drinks SET base_price=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, newPrice);
            ps.setInt(2, drinkId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Generic update of name and ingredients by id */
    public boolean updateDrink(int drinkId, String newName, String newIngredients) throws SQLException {
        String sql = "UPDATE drinks SET name=?, ingredients=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setString(2, newIngredients);
            ps.setInt(3, drinkId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Delete drink (hard delete) */
    public boolean deleteDrink(int drinkId) throws SQLException {
        String sql = "DELETE FROM drinks WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, drinkId);
            return ps.executeUpdate() == 1;
        }
    }

    // ================== INGREDIENT (INVENTORY) MANAGEMENT ==================
    /** List ingredients id | name | cost | quantity */
    public List<String> listIngredients() throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT id,name,cost,quantity FROM ingredients ORDER BY id";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(String.format("%d | %s | cost=%.3f | qty=%.3f",
                        rs.getInt(1), rs.getString(2), rs.getDouble(3), rs.getDouble(4)));
            }
        }
        return list;
    }

    /** Add a new ingredient */
    public int addIngredient(String name, double cost, double quantity) throws SQLException {
        String sql = "INSERT INTO ingredients (name,cost,quantity) VALUES (?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setDouble(2, cost);
            ps.setDouble(3, quantity);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) return rs.getInt(1); }
        }
        return -1;
    }

    /** Set absolute quantity */
    public boolean updateIngredientQuantity(int ingredientId, double newQuantity) throws SQLException {
        String sql = "UPDATE ingredients SET quantity=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, newQuantity);
            ps.setInt(2, ingredientId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Increment/decrement quantity by delta (can be negative) */
    public boolean adjustIngredientQuantity(int ingredientId, double delta) throws SQLException {
        String sql = "UPDATE ingredients SET quantity = quantity + ? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, delta);
            ps.setInt(2, ingredientId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Update ingredient cost */
    public boolean updateIngredientCost(int ingredientId, double newCost) throws SQLException {
        String sql = "UPDATE ingredients SET cost=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, newCost);
            ps.setInt(2, ingredientId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Delete ingredient */
    public boolean deleteIngredient(int ingredientId) throws SQLException {
        String sql = "DELETE FROM ingredients WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            return ps.executeUpdate() == 1;
        }
    }
}