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
        try {
            initializeDatabase();
            createTables();
            // insertSampleData();
            insertSampleIngredients();
            insertSampleDrinks();
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
                category TEXT NOT NULL,
                description TEXT,
                available BOOLEAN DEFAULT 1,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
        String insertIngredients = """
            INSERT INTO ingredients (id, name, cost, quantity) VALUES
            (1, 'Water', 0.001, 50),
            (2, 'Milk', 0.008, 50),
            (3, 'Sugar', 0.002, 50),
            (4, 'Tea', 0.15, 50),
            (5, 'Black Tea', 0.18, 50),
            (6, 'Oolong Tea', 0.25, 50),
            (7, 'Green Tea', 0.20, 50),
            (8, 'Coffee', 0.12, 50),
            (9, 'Cream', 0.015, 50),
            (10, 'Coconut', 0.10, 50),
            (11, 'Ube Powder', 0.35, 50),
            (12, 'Protein Powder', 0.40, 50),
            (13, 'Winter Melon', 0.08, 50),
            (14, 'Passion Fruit', 0.12, 50),
            (15, 'Mango', 0.09, 50),
            (16, 'Strawberry Lemonade', 0.07, 50),
            (17, 'Strawberry', 0.08, 50),
            (18, 'Peach', 0.09, 50),
            (19, 'Matcha', 0.45, 50)
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
        // Insert drinks
        String insertDrinks = """
            INSERT INTO drinks (name, base_price, category, description) VALUES
            ('Vanilla Latte', 4.50, 'Coffee', 'Classic latte with vanilla syrup'),
        """;
        
        try (Statement insertStmt = connection.createStatement()) {
            insertStmt.execute(insertDrinks);
            System.out.println("Sample drinks inserted!");
        }
        
    }

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