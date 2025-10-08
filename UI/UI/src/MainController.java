import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller class for the Main View of the Machamp POS System
 * This class handles all the UI interactions and business logic
 */
public class MainController implements Initializable {
    
    // FXML injected components
    @FXML
    private TextField productNameField;
    
    @FXML
    private TextField priceField;
    
    @FXML
    private Button addProductButton;
    
    @FXML
    private Button clearButton;
    
    @FXML
    private Label statusLabel;
    
    /**
     * Initialize method called when the FXML is loaded
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize any components or set default values here
    if (statusLabel != null) statusLabel.setText("System ready");
    System.out.println("[MainController] initialize: productNameField=" + (productNameField != null)
        + ", priceField=" + (priceField != null) + ", addButton=" + (addProductButton != null)
        + ", clearButton=" + (clearButton != null) + ", statusLabel=" + (statusLabel != null));
        
        // Add listeners or validation if needed
        setupValidation();
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

    // Handlers for the main view buttons (wired in out/MainView.fxml)
    @FXML
    private void handleOrder(ActionEvent event) {
        System.out.println("[MainController] Order button clicked");
    }

    @FXML
    private void handleManager(ActionEvent event) {
        System.out.println("[MainController] Manager button clicked");
    }

    @FXML
    private void handleDrinks(ActionEvent event) {
        System.out.println("[MainController] Drinks button clicked");
    }

    @FXML
    private void handleFood(ActionEvent event) {
        System.out.println("[MainController] Food button clicked");
    }

    @FXML
    private void handleMerch(ActionEvent event) {
        System.out.println("[MainController] Merch button clicked");
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
        statusLabel.setText(message);
        
        switch (type.toLowerCase()) {
            case "error":
                statusLabel.setStyle("-fx-text-fill: red;");
                break;
            case "success":
                statusLabel.setStyle("-fx-text-fill: green;");
                break;
            case "info":
                statusLabel.setStyle("-fx-text-fill: blue;");
                break;
            default:
                statusLabel.setStyle("-fx-text-fill: black;");
        }
    }
    
    /**
     * Set up field validation and formatting
     */
    private void setupValidation() {
        // Add price field validation to only accept numbers
        priceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                priceField.setText(oldValue);
            }
        });
    }
    
    /**
     * Add product to the system (placeholder for database integration)
     */
    private void addProduct(String name, double price) {
        // Attempt to insert into database
        boolean ok = DB.insertMenuItem(name, price);
        if (ok) {
            System.out.println("Added product to DB: " + name + " ($" + String.format("%.2f", price) + ")");
        } else {
            // Fallback behavior: still print but inform the user via status label
            System.out.println("Failed to add product to DB. See stderr for details.");
            updateStatus("(Local) Product '" + name + "' added (DB not configured)", "info");
        }
    }
}