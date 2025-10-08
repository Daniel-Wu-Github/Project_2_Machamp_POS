// javac --module-path lib --add-modules javafx.controls,javafx.fxml -d out src/*.java
// java --module-path "lib" --add-modules javafx.controls,javafx.fxml -cp out App
// RUN COMMAND FOR WINDOWS: java "-Djava.library.path=lib" --module-path "lib" --add-modules javafx.controls,javafx.fxml -cp out App

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.io.File;
import java.net.URL;

/**
 * Main Application class for the Machamp POS System
 * This class extends JavaFX Application and serves as the entry point
 */
public class App extends Application {
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showOrdersPage(); // Start with Orders Page (will attempt FXML first)
        stage.setTitle("Machamp POS");
        stage.show();
    }

    public void showOrdersPage() {
        // Try loading the FXML view first (out/MainView.fxml). If loading fails, fall back
        // to the programmatic OrdersPage so the app still works.
        try {
            System.out.println("[App] Attempting to load FXML 'out/MainView.fxml' (classpath then file)");
            URL fxmlUrl = getClass().getResource("/out/MainView.fxml");
            Parent root;
            FXMLLoader loader = null;
            if (fxmlUrl != null) {
                System.out.println("[App] Found on classpath: " + fxmlUrl);
                loader = new FXMLLoader(fxmlUrl);
                // Ensure controller can be instantiated even if default instantiation is blocked
                loader.setControllerFactory(clazz -> {
                    try {
                        return clazz.getDeclaredConstructor().newInstance();
                    } catch (Exception ex) {
                        throw new RuntimeException("Controller instantiation failed: " + clazz, ex);
                    }
                });
                root = loader.load();
            } else {
                // Fallback: load from file system relative to working dir (avoid deprecated URL(String))
                File f = new File("out/MainView.fxml");
                URL fileUrl = f.toURI().toURL();
                System.out.println("[App] Classpath lookup failed, trying file URL: " + fileUrl);
                loader = new FXMLLoader(fileUrl);
                // Ensure controller can be instantiated even if default instantiation is blocked
                loader.setControllerFactory(clazz -> {
                    try {
                        return clazz.getDeclaredConstructor().newInstance();
                    } catch (Exception ex) {
                        throw new RuntimeException("Controller instantiation failed: " + clazz, ex);
                    }
                });
                root = loader.load();
            }
            Scene scene = new Scene(root, 900, 600);
            primaryStage.setScene(scene);
            if (loader != null) {
                Object controller = loader.getController();
                System.out.println("[App] FXMLLoader controller: " + (controller != null ? controller.getClass().getName() : "null"));
            }
        } catch (IOException | RuntimeException e) {
            // Fallback to programmatically created OrdersPage
            OrdersPage ordersPage = new OrdersPage(this);
            Scene scene = new Scene(ordersPage.getRoot(), 900, 600);
            primaryStage.setScene(scene);
            System.err.println("[App] Unable to load FXML, using programmatic OrdersPage: " + e.getMessage());
        }
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
