import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller class for the Main View of the Machamp POS System
 * This class handles all the UI interactions and business logic
 */
public class MainController implements Initializable {

    // Root stack to layer panes
    @FXML private StackPane rootStack;

    // Panes
    @FXML private BorderPane ordersPane;
    @FXML private BorderPane customizationPane;
    @FXML private BorderPane managerPane;

    // Orders page controls
    @FXML private FlowPane drinksGrid; // existing drink items
    @FXML private Button managerNavBtn;
    @FXML private Button orderNavBtn;
    @FXML private Button drinksTabBtn;
    @FXML private Button foodTabBtn;
    @FXML private Button merchTabBtn;

    // Customization page controls
    @FXML private Label customizationDrinkTitle;
    @FXML private Button backFromCustomizationBtn;
    @FXML private Button sizeSmallBtn, sizeMediumBtn, sizeLargeBtn;
    @FXML private Button sugarNoneBtn, sugarHalfBtn, sugarNormalBtn;
    @FXML private Button toppingBobaBtn, toppingLycheeBtn, toppingPuddingBtn;
    @FXML private Button continueCustomizationBtn;

    // Manager page controls
    @FXML private Button backFromManagerBtn;
    @FXML private Label dailyEarningsLabel, operatingCostLabel, popularItemLabel;
    @FXML private LineChart<String, Number> salesLineChart;

    // Legacy product form (kept if needed for future admin input) - optional null if removed from FXML
    @FXML private TextField productNameField;
    @FXML private TextField priceField;
    @FXML private Button addProductButton;
    @FXML private Button clearButton;
    @FXML private Label statusLabel;

    // State for customization
    private String selectedDrinkName;
    
    /**
     * Initialize method called when the FXML is loaded
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (statusLabel != null) {
            statusLabel.setText("System ready");
        }

        setupValidation();

        // Setup dynamic click handlers for drink items
        setupDrinkItemHandlers();

        // Navigation buttons
        if (managerNavBtn != null) managerNavBtn.setOnAction(e -> showManagerPortal());
        if (orderNavBtn != null) orderNavBtn.setOnAction(e -> showOrdersPage());
        if (backFromCustomizationBtn != null) {
            backFromCustomizationBtn.setOnAction(e -> showOrdersPage());
        }
        if (backFromManagerBtn != null) {
            backFromManagerBtn.setOnAction(e -> showOrdersPage());
        }

        if (drinksTabBtn != null) drinksTabBtn.setOnAction(e -> filterCategory("Drinks"));
        if (foodTabBtn != null) foodTabBtn.setOnAction(e -> filterCategory("Food"));
        if (merchTabBtn != null) merchTabBtn.setOnAction(e -> filterCategory("Merch"));

        // Sample actions for customization buttons
        if (continueCustomizationBtn != null) {
            continueCustomizationBtn.setOnAction(e -> {
                Alert a = new Alert(AlertType.INFORMATION, "Added '" + selectedDrinkName + "' to order (demo)");
                a.show();
                showOrdersPage();
            });
        }

        // Populate chart data programmatically (avoids FXML load coercion issues)
        if (salesLineChart != null) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Sales ($)");
            series.getData().add(new XYChart.Data<>("8:00", 12000));
            series.getData().add(new XYChart.Data<>("12:00", 30000));
            series.getData().add(new XYChart.Data<>("16:00", 50000));
            series.getData().add(new XYChart.Data<>("20:00", 90000));
            salesLineChart.getData().add(series);
        }
    }
    
    /**
     * Handle the Add Product button click
     */
    @FXML
    private void handleAddProduct(ActionEvent event) {
        String productName = productNameField.getText().trim();
        String priceText = priceField.getText().trim();
        
        // Validate input
        if (productName.isEmpty()) {
            updateStatus("Please enter a product name", "error");
            return;
        }
        
        if (priceText.isEmpty()) {
            updateStatus("Please enter a price", "error");
            return;
        }
        
        try {
            double price = Double.parseDouble(priceText);
            if (price <= 0) {
                updateStatus("Price must be greater than 0", "error");
                return;
            }
            
            // Process the product addition (you can add database logic here)
            addProduct(productName, price);
            
            // Clear fields after successful addition
            clearFields();
            updateStatus("Product '" + productName + "' added successfully!", "success");
            
        } catch (NumberFormatException e) {
            updateStatus("Please enter a valid price", "error");
        }
    }
    
    /**
     * Handle the Clear button click
     */
    @FXML
    private void handleClear(ActionEvent event) {
        clearFields();
        updateStatus("Fields cleared", "info");
    }
    
    /**
     * Clear all input fields
     */
    private void clearFields() {
        productNameField.clear();
        priceField.clear();
        productNameField.requestFocus();
    }
    
    /**
     * Update the status label with different styles
     */
    private void updateStatus(String message, String type) {
        if (statusLabel == null) return; // optional if form removed
        statusLabel.setText(message);
        switch (type.toLowerCase()) {
            case "error" -> statusLabel.setStyle("-fx-text-fill: red;");
            case "success" -> statusLabel.setStyle("-fx-text-fill: green;");
            case "info" -> statusLabel.setStyle("-fx-text-fill: blue;");
            default -> statusLabel.setStyle("-fx-text-fill: black;");
        }
    }
    
    /**
     * Set up field validation and formatting
     */
    private void setupValidation() {
        if (priceField != null) {
            priceField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*(\\.\\d*)?")) {
                    priceField.setText(oldValue);
                }
            });
        }
    }
    
    /**
     * Add product to the system (placeholder for database integration)
     */
    private void addProduct(String name, double price) {
        System.out.println("Adding product: " + name + " with price: $" + String.format("%.2f", price));
    }

    // ---------- New Navigation Logic ----------
    private void showPane(BorderPane pane) {
        for (Node child : rootStack.getChildren()) {
            if (child instanceof BorderPane bp) {
                bp.setVisible(false);
                bp.setManaged(false);
            }
        }
        pane.setVisible(true);
        pane.setManaged(true);
    }

    public void showOrdersPage() {
        showPane(ordersPane);
    }

    public void showCustomizationPage(String drinkName) {
        selectedDrinkName = drinkName;
        customizationDrinkTitle.setText(drinkName);
        showPane(customizationPane);
    }

    public void showManagerPortal() {
        showPane(managerPane);
    }

    private void setupDrinkItemHandlers() {
        if (drinksGrid == null) return;
        for (Node node : drinksGrid.getChildren()) {
            if (node instanceof VBox vb) {
                // Expect last child to be label
                for (Node child : vb.getChildren()) {
                    if (child instanceof Label lbl) {
                        vb.setOnMouseClicked(e -> showCustomizationPage(lbl.getText()));
                    }
                }
            }
        }
    }

    private void filterCategory(String category) {
        // Placeholder: implement filtering logic when categories beyond Drinks are added
        System.out.println("Category selected: " + category);
    }
}