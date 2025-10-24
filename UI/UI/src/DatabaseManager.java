import java.io.BufferedReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Database Manager for Machamp POS System using SQLite.
 * Handles database connections, table creation, and CRUD operations
 * for drinks, ingredients, employees, customers, and order history.
 * 
 * @author Juan Elias
 */
public class DatabaseManager {
    private static final String DATABASE_URL = "jdbc:sqlite:machamp_pos.db";
    private Connection connection;

    
    /**
     * Default constructor - initializes database connection and creates tables.
     * Does not reset existing data.
     */
    public DatabaseManager() {
        this(false);
    }

    /**
     * Constructor with optional reset functionality.
     * Initializes database connection, optionally drops and recreates tables,
     * and loads sample data.
     * 
     * @param reset if true, existing tables are dropped and recreated fresh each startup
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
     * Initializes the database connection and enables foreign key constraints.
     * 
     * @throws SQLException if connection to database fails
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
     * Creates necessary tables for the POS system if they don't already exist.
     * Tables created: drinks, orderhistory, ingredients, customers, employees.
     * 
     * @throws SQLException if table creation fails
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

    /**
     * Imports order history from CSV if the orderhistory table is empty.
     * 
     * @param csvPath the path to the CSV file containing order history
     */
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
     * which is normalized to a JSON-like string: {"Drink": [a,b,c], ...}
     * 
     * @param path the file path to the CSV file
     * @throws Exception if the file cannot be read or import fails
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
     * Internal helper class to hold parsed order line data.
     * 
     * @author Ayad Masud
     */
    private static class ParsedOrder {
        String dateTime; String orderId; String customerId; String menuItemsJson; String totalPrice;
    }

    /**
     * Parses a single CSV line from the order history file.
     * Avoids heavy CSV libraries; relies on fixed column count and quoting on menu_items.
     * 
     * @param line the CSV line to parse
     * @return ParsedOrder object containing the parsed data, or null if parsing fails
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
     * Drops all known tables in the proper order to avoid foreign key issues.
     * 
     * @throws SQLException if table dropping fails
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

    /**
     * Inserts sample employee data if the employees table is empty.
     * 
     * @throws SQLException if insertion fails
     */
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

    /**
     * Checks if a table is empty (has no rows).
     * 
     * @param table the name of the table to check
     * @return true if the table has no rows, false otherwise
     * @throws SQLException if the query fails
     */
    private boolean isTableEmpty(String table) throws SQLException {
        String q = "SELECT COUNT(*) FROM " + table;
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(q)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    /**
     * Adds a new employee to the database.
     * 
     * @param firstName the employee's first name
     * @param lastName the employee's last name
     * @param email the employee's email address (must be unique)
     * @param role the employee's role (CASHIER or MANAGER)
     * @return the generated employee ID, or -1 if insertion fails
     * @throws SQLException if insertion fails
     */
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

    /**
     * Lists all employees in the database with their details.
     * 
     * @return a list of formatted strings containing employee information
     * @throws SQLException if the query fails
     */
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

    /**
     * Updates an employee's role.
     * 
     * @param id the employee ID
     * @param newRole the new role (CASHIER or MANAGER)
     * @return true if exactly one row was updated, false otherwise
     * @throws SQLException if the update fails
     */
    public boolean updateEmployeeRole(int id, String newRole) throws SQLException {
        String sql = "UPDATE employees SET role=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newRole);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Sets an employee's active status.
     * 
     * @param id the employee ID
     * @param active true to set as active, false to set as inactive
     * @return true if exactly one row was updated, false otherwise
     * @throws SQLException if the update fails
     */
    public boolean setEmployeeActive(int id, boolean active) throws SQLException {
        String sql = "UPDATE employees SET active=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Retrieves a single employee record by ID.
     * 
     * @param id the employee ID
     * @return formatted string containing employee information, or null if not found
     * @throws SQLException if the query fails
     */
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

    /**
     * Updates employee basic fields (except id and created_at).
     * Null parameters keep existing values unchanged.
     * 
     * @param id the employee ID
     * @param firstName the new first name, or null to keep current value
     * @param lastName the new last name, or null to keep current value
     * @param email the new email, or null to keep current value
     * @param role the new role, or null to keep current value
     * @param active the new active status, or null to keep current value
     * @return true if exactly one row was updated, false if no changes were made
     * @throws SQLException if the update fails
     */
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

    /**
     * Lists only active employees.
     * 
     * @return a list of formatted strings containing active employee information
     * @throws SQLException if the query fails
     */
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
     * Inserts sample ingredients into the database if the ingredients table is empty.
     * 
     * @throws SQLException if insertion fails
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
     * Inserts sample drinks with ingredients into the database if the drinks table is empty.
     * 
     * @throws SQLException if insertion fails
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

    /**
     * Recreates only the employees table if schema adjustments are needed without full reset.
     * Drops and recreates the employees table with the current schema.
     * 
     * @throws SQLException if table recreation fails
     */
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
     * Gets the database connection for advanced operations.
     * 
     * @return the active database connection
     */
    public Connection getConnection() {
        return connection;
    }

    // ================== DRINK (MENU) MANAGEMENT ==================
    /**
     * Returns a list of drinks with id, name, and price.
     * 
     * @return a list of formatted strings containing drink information
     * @throws SQLException if the query fails
     */
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

    /**
     * Inserts a new drink into the database.
     * Ingredients and customization columns are optional.
     * 
     * @param name the drink name (must be unique)
     * @param basePrice the base price of the drink
     * @param ingredients the ingredients list (e.g., "{Water, Milk, Sugar, Tea}")
     * @return the generated drink ID, or -1 if insertion fails
     * @throws SQLException if insertion fails
     */
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

    /**
     * Updates only the base price for a drink.
     * 
     * @param drinkId the drink ID
     * @param newPrice the new base price
     * @return true if exactly one row was updated, false otherwise
     * @throws SQLException if the update fails
     */
    public boolean updateDrinkPrice(int drinkId, double newPrice) throws SQLException {
        String sql = "UPDATE drinks SET base_price=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, newPrice);
            ps.setInt(2, drinkId);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Generic update of name and ingredients by drink ID.
     * 
     * @param drinkId the drink ID
     * @param newName the new drink name
     * @param newIngredients the new ingredients list
     * @return true if exactly one row was updated, false otherwise
     * @throws SQLException if the update fails
     */
    public boolean updateDrink(int drinkId, String newName, String newIngredients) throws SQLException {
        String sql = "UPDATE drinks SET name=?, ingredients=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setString(2, newIngredients);
            ps.setInt(3, drinkId);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Deletes a drink from the database (hard delete).
     * 
     * @param drinkId the drink ID to delete
     * @return true if exactly one row was deleted, false otherwise
     * @throws SQLException if the deletion fails
     */
    public boolean deleteDrink(int drinkId) throws SQLException {
        String sql = "DELETE FROM drinks WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, drinkId);
            return ps.executeUpdate() == 1;
        }
    }

    // ================== INGREDIENT (INVENTORY) MANAGEMENT ==================
    /**
     * Lists all ingredients with id, name, cost, and quantity.
     * 
     * @return a list of formatted strings containing ingredient information
     * @throws SQLException if the query fails
     */
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

    /**
     * Adds a new ingredient to the database.
     * 
     * @param name the ingredient name (must be unique)
     * @param cost the cost per unit of the ingredient
     * @param quantity the initial quantity in stock
     * @return the generated ingredient ID, or -1 if insertion fails
     * @throws SQLException if insertion fails
     */
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

    /**
     * Sets the absolute quantity for an ingredient.
     * 
     * @param ingredientId the ingredient ID
     * @param newQuantity the new absolute quantity
     * @return true if exactly one row was updated, false otherwise
     * @throws SQLException if the update fails
     */
    public boolean updateIngredientQuantity(int ingredientId, double newQuantity) throws SQLException {
        String sql = "UPDATE ingredients SET quantity=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, newQuantity);
            ps.setInt(2, ingredientId);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Increments or decrements ingredient quantity by a delta value.
     * 
     * @param ingredientId the ingredient ID
     * @param delta the amount to add (positive) or subtract (negative)
     * @return true if exactly one row was updated, false otherwise
     * @throws SQLException if the update fails
     */
    public boolean adjustIngredientQuantity(int ingredientId, double delta) throws SQLException {
        String sql = "UPDATE ingredients SET quantity = quantity + ? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, delta);
            ps.setInt(2, ingredientId);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Updates the cost of an ingredient.
     * 
     * @param ingredientId the ingredient ID
     * @param newCost the new cost per unit
     * @return true if exactly one row was updated, false otherwise
     * @throws SQLException if the update fails
     */
    public boolean updateIngredientCost(int ingredientId, double newCost) throws SQLException {
        String sql = "UPDATE ingredients SET cost=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, newCost);
            ps.setInt(2, ingredientId);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Deletes an ingredient from the database.
     * 
     * @param ingredientId the ingredient ID to delete
     * @return true if exactly one row was deleted, false otherwise
     * @throws SQLException if the deletion fails
     */
    public boolean deleteIngredient(int ingredientId) throws SQLException {
        String sql = "DELETE FROM ingredients WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            return ps.executeUpdate() == 1;
        }
    }
}