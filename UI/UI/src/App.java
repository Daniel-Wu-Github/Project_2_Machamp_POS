// javac --module-path lib --add-modules javafx.controls,javafx.fxml -d out src/*.java
// java --module-path "lib" --add-modules javafx.controls,javafx.fxml -cp out App
// RUN COMMAND FOR WINDOWS: java "-Djava.library.path=lib" --module-path "lib" --add-modules javafx.controls,javafx.fxml -cp out App

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.util.List;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Main Application class for the Machamp POS System
 * This class extends JavaFX Application and serves as the entry point
 * for the point-of-sale system user interface.
 * 
 * @author Juan Elias
 */
public class App extends Application {
    private Stage primaryStage;

    /**
     * Starts the JavaFX application and initializes the primary stage.
     * Loads the main FXML view and sets up the initial scene.
     * 
     * @param stage the primary stage for this application
     */
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../out/MainView.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 900, 600);
            stage.setScene(scene);
            stage.setTitle("Machamp POS");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Main entry point for the application.
     * Initializes the database manager and launches the JavaFX application.
     * 
     * @param args command line arguments passed to the application
     */
    public static void main(String[] args) {
        // Initialize database manager BEFORE launching UI so test output appears immediately
        DatabaseManager dbManager = new DatabaseManager(true); // true => reset for clean test

        // TESTING CODE ---------------------------------------------------------------------
        /*try {
            System.out.println("--- Ingredients ---");
            for (String ingredient : dbManager.listIngredients()) {
                System.out.println(ingredient);
            }

            int newDrinkId = dbManager.addDrink("Test Drink", 4.99, "{Milk, Sugar, Tea}");
            System.out.println("Added new drink with ID: " + newDrinkId);

            System.out.println("--- Drinks ---");
            for (String drink : dbManager.listDrinks()) {
                System.out.println(drink);
            }
        } catch (SQLException e) {
            System.err.println("Test code DB error: " + e.getMessage());
        }

        // sample reports
        String reportSummary = null;
        try {
            Reports reports = new Reports();
            reports.setStartDate(LocalDate.of(2024, 9, 26));
            reports.setEndDate(LocalDate.of(2024, 9, 30));
            reportSummary = reports.generateSalesSummary(dbManager);
        } catch (SQLException e) {
            System.err.println("Report generation error: " + e.getMessage());
        }
        if (reportSummary != null) {
            System.out.println(reportSummary);
        }*/
        // Launch JavaFX application (blocking until window closed)
        launch(args);
    }
}
