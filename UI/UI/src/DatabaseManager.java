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
}