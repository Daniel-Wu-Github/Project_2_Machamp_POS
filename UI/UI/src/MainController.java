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
        statusLabel.setText("System ready");
        
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
        // TODO: Add database integration here
        // For now, just print to console
        System.out.println("Adding product: " + name + " with price: $" + String.format("%.2f", price));
        
        // Here you would typically:
        // 1. Connect to your database
        // 2. Insert the product into your products table
        // 3. Handle any database exceptions
        // 4. Update any product lists or displays
    }
}