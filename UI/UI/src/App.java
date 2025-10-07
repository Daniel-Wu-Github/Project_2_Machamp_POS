// javac --module-path lib --add-modules javafx.controls,javafx.fxml -d out src/*.java
// java --module-path "lib" --add-modules javafx.controls,javafx.fxml -cp out App
// RUN COMMAND FOR WINDOWS: java "-Djava.library.path=lib" --module-path "lib" --add-modules javafx.controls,javafx.fxml -cp out App

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main Application class for the Machamp POS System
 * This class extends JavaFX Application and serves as the entry point
 */
public class App extends Application {
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showOrdersPage(); // Start with Orders Page
        stage.setTitle("Machamp POS");
        stage.show();
    }

    public void showOrdersPage() {
        OrdersPage ordersPage = new OrdersPage(this);
        Scene scene = new Scene(ordersPage.getRoot(), 900, 600);
        primaryStage.setScene(scene);
    }

    public void showManagerPortal() {
        ManagerPortalPage managerPage = new ManagerPortalPage(this);
        Scene scene = new Scene(managerPage.getRoot(), 900, 600);
        primaryStage.setScene(scene);
    }

    public void showCustomizationPage(String drinkName) {
        CustomizationPage customPage = new CustomizationPage(this, drinkName);
        Scene scene = new Scene(customPage.getRoot(), 900, 600);
        primaryStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
