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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private String selectedSize = "Medium"; // default
    private String selectedSugar = "Normal"; // default
    private final List<String> selectedToppings = new ArrayList<>();
    private final List<String> orderItems = new ArrayList<>(); // stored orders
    
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

        setupSelectionHandlers();
        setupContinueHandler();

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
        resetSelections();
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

    // ---------------- Ordering Mechanic ----------------
    private void setupSelectionHandlers() {
        if (sizeSmallBtn != null) sizeSmallBtn.setOnAction(e -> selectSize("Small"));
        if (sizeMediumBtn != null) sizeMediumBtn.setOnAction(e -> selectSize("Medium"));
        if (sizeLargeBtn != null) sizeLargeBtn.setOnAction(e -> selectSize("Large"));

        if (sugarNoneBtn != null) sugarNoneBtn.setOnAction(e -> selectSugar("No Sugar"));
        if (sugarHalfBtn != null) sugarHalfBtn.setOnAction(e -> selectSugar("Half Sugar"));
        if (sugarNormalBtn != null) sugarNormalBtn.setOnAction(e -> selectSugar("Normal"));

        if (toppingBobaBtn != null) toppingBobaBtn.setOnAction(e -> toggleTopping("Boba", toppingBobaBtn));
        if (toppingLycheeBtn != null) toppingLycheeBtn.setOnAction(e -> toggleTopping("Lychee Jelly", toppingLycheeBtn));
        if (toppingPuddingBtn != null) toppingPuddingBtn.setOnAction(e -> toggleTopping("Pudding", toppingPuddingBtn));
    }

    private void setupContinueHandler() {
        if (continueCustomizationBtn == null) return;
        continueCustomizationBtn.setOnAction(e -> {
            String orderStr = buildOrderString();
            orderItems.add(orderStr);
            Alert a = new Alert(AlertType.INFORMATION, orderStr);
            a.setHeaderText("Order Added");
            a.show();
            showOrdersPage();
            System.out.println("Current Orders: " + orderItems);
        });
    }

    private String buildOrderString() {
        String toppingsPart = selectedToppings.isEmpty() ? "No Toppings" : selectedToppings.stream().collect(Collectors.joining(", "));
        return String.format("%s | Size: %s | Sugar: %s | Toppings: %s", selectedDrinkName, selectedSize, selectedSugar, toppingsPart);
    }

    private void selectSize(String size) {
        selectedSize = size;
        highlightSizeButtons();
    }

    private void selectSugar(String sugar) {
        selectedSugar = sugar;
        highlightSugarButtons();
    }

    private void toggleTopping(String topping, Button btn) {
        if (selectedToppings.contains(topping)) {
            selectedToppings.remove(topping);
            btn.setStyle("");
        } else {
            selectedToppings.add(topping);
            btn.setStyle("-fx-background-color: #b3e5fc;");
        }
    }

    private void highlightSizeButtons() {
        if (sizeSmallBtn != null) sizeSmallBtn.setStyle(selectedSize.equals("Small")? "-fx-background-color: #c5e1a5;" : "");
        if (sizeMediumBtn != null) sizeMediumBtn.setStyle(selectedSize.equals("Medium")? "-fx-background-color: #c5e1a5;" : "");
        if (sizeLargeBtn != null) sizeLargeBtn.setStyle(selectedSize.equals("Large")? "-fx-background-color: #c5e1a5;" : "");
    }

    private void highlightSugarButtons() {
        if (sugarNoneBtn != null) sugarNoneBtn.setStyle(selectedSugar.equals("No Sugar")? "-fx-background-color: #ffd54f;" : "");
        if (sugarHalfBtn != null) sugarHalfBtn.setStyle(selectedSugar.equals("Half Sugar")? "-fx-background-color: #ffd54f;" : "");
        if (sugarNormalBtn != null) sugarNormalBtn.setStyle(selectedSugar.equals("Normal")? "-fx-background-color: #ffd54f;" : "");
    }

    private void resetSelections() {
        selectedSize = "Medium";
        selectedSugar = "Normal";
        selectedToppings.clear();
        // clear styles
        if (toppingBobaBtn != null) toppingBobaBtn.setStyle("");
        if (toppingLycheeBtn != null) toppingLycheeBtn.setStyle("");
        if (toppingPuddingBtn != null) toppingPuddingBtn.setStyle("");
        highlightSizeButtons();
        highlightSugarButtons();
    }
}