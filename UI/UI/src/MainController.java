import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;
import javafx.event.ActionEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.io.InputStream;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.geometry.Insets;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.LinkedHashMap;
import java.sql.SQLException;

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
    @FXML private BorderPane orderViewPane;

    // Orders page controls
    @FXML private FlowPane drinksGrid; // existing drink items
    @FXML private Button managerNavBtn;
    @FXML private Button orderNavBtn;
    @FXML private Button viewCurrentOrderBtn;
    @FXML private Button drinksTabBtn;
    @FXML private Button foodTabBtn;
    @FXML private Button merchTabBtn;
    
    // Order View page controls
    @FXML private Button backFromOrderViewBtn;
    @FXML private Button clearOrderBtn;
    @FXML private VBox orderItemsContainer;
    @FXML private Label orderTotalLabel;

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
    
    
    // Management buttons
    @FXML private Button viewMenuBtn, addMenuItemBtn, updateMenuItemBtn;
    @FXML private Button viewInventoryBtn, addIngredientBtn, updateInventoryBtn;
    @FXML private Button viewEmployeesBtn, addEmployeeBtn, updateEmployeeBtn;
    @FXML private Button generateReportBtn;
    @FXML private Button generateInventoryReportBtn;
    
    // Management UI components
    @FXML private VBox managementSection;
    @FXML private Label managementTitle;
    @FXML private ScrollPane displayPane;
    @FXML private TextArea displayArea;
    @FXML private VBox formSection;
    @FXML private TextField idField, field1, field2, field3, field4;
    @FXML private Label field1Label, field2Label, field3Label, field4Label;
    @FXML private HBox field4Container;
    @FXML private HBox dateRangeContainer;
    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private Button submitBtn, cancelBtn;
    @FXML private Label statusLabel;
    @FXML private Button generateXReportBtn;
    @FXML private Button generateZReportBtn;

    // Legacy product form (kept if needed for future admin input) - optional null if removed from FXML
    @FXML private TextField productNameField;
    @FXML private TextField priceField;
    @FXML private Button addProductButton;
    @FXML private Button clearButton;

    // State for customization
    private String selectedDrinkName;
    
    // Database manager instance
    private DatabaseManager dbManager;

    private final DayReports dayReports = new DayReports();
    
    // Current management operation
    private String currentOperation = "";
    private String selectedSize = "Medium"; // default
    private String selectedSugar = "Normal"; // default
    private final List<String> selectedToppings = new ArrayList<>();
    private final List<String> orderItems = new ArrayList<>(); // stored orders
    @FXML
    private void handleGenerateXReport() {
        try {
            java.time.LocalDate picked = showDatePickerPopup(java.time.LocalDate.now());
            if (picked == null) {
                updateStatus("X Report canceled.", "info");
                return;
            }

            showManagementSection("X Report (" + picked + ")");
            hideFormSection();
            showDisplayPane();

            DayReports dr = new DayReports();
            DayReports.XReportResult result = dr.generateXReport(null, picked);
            displayArea.setText(result.toString());
            // Use monospaced font so the table columns line up nicely
            if (displayArea != null) {
                displayArea.setStyle("-fx-font-family: 'monospace'; -fx-font-size: 12px;");
            }
            updateStatus("X Report generated for " + picked + ".", "success");
        } catch (Exception ex) {
            updateStatus("Failed to generate X Report: " + ex.getMessage(), "error");
            ex.printStackTrace();
        }
    }
    @FXML
    private void handleGenerateZReport() {
        try {
            java.time.LocalDate picked = showDatePickerPopup(java.time.LocalDate.now());
            if (picked == null) {
                updateStatus("Z Report canceled.", "info");
                return;
            }

            showManagementSection("Z Report (" + picked + ")");
            hideFormSection();
            showDisplayPane();

            String result = dayReports.generateZReport(null, picked);
            displayArea.setText(result);
            // Use monospaced font so the table columns line up nicely
            if (displayArea != null) {
                displayArea.setStyle("-fx-font-family: 'monospace'; -fx-font-size: 12px;");
            }
            updateStatus("Z Report generated for " + picked + ".", "success");
        } catch (Exception ex) {
            updateStatus("Failed to generate Z Report: " + ex.getMessage(), "error");
            ex.printStackTrace();
        }
    }

    // Show a modal DatePicker in a popup dialog and return the chosen date, or null if canceled
    private java.time.LocalDate showDatePickerPopup(java.time.LocalDate defaultDate) {
        Dialog<java.time.LocalDate> dialog = new Dialog<>();
        dialog.setTitle("Select Date for X Report");
        dialog.setHeaderText("Choose a date to generate the X Report");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        javafx.scene.control.DatePicker datePicker = new javafx.scene.control.DatePicker(defaultDate);
        javafx.scene.control.Label label = new javafx.scene.control.Label("Date:");
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10, label, datePicker);
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(12, row);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? datePicker.getValue() : null);
        java.util.Optional<java.time.LocalDate> result = dialog.showAndWait();
        return result.orElse(null);
    }
    
    /**
     * Initialize method called when the FXML is loaded
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize database manager
        dbManager = new DatabaseManager();
        
        // Initialize database manager
        dbManager = new DatabaseManager();
        
        if (statusLabel != null) {
            statusLabel.setText("System ready");
        }

        setupValidation();

        // Setup dynamic click handlers for drink items
        setupDrinkItemHandlers();

        // Navigation buttons
        if (managerNavBtn != null) managerNavBtn.setOnAction(e -> showManagerPortal());
        if (orderNavBtn != null) orderNavBtn.setOnAction(e -> showOrdersPage());
        if (viewCurrentOrderBtn != null) viewCurrentOrderBtn.setOnAction(e -> showOrderView());
        if (backFromCustomizationBtn != null) {
            backFromCustomizationBtn.setOnAction(e -> showOrdersPage());
        }
        if (backFromManagerBtn != null) {
            backFromManagerBtn.setOnAction(e -> showOrdersPage());
        }
        if (backFromOrderViewBtn != null) {
            backFromOrderViewBtn.setOnAction(e -> showOrdersPage());
        }
        if (clearOrderBtn != null) {
            clearOrderBtn.setOnAction(e -> handleClearOrder());
        }
        
        // Management buttons - Menu
        if (viewMenuBtn != null) viewMenuBtn.setOnAction(e -> handleViewMenu());
        if (addMenuItemBtn != null) addMenuItemBtn.setOnAction(e -> handleAddMenuItem());
        if (updateMenuItemBtn != null) updateMenuItemBtn.setOnAction(e -> handleUpdateMenuItem());
        
        // Management buttons - Inventory
        if (viewInventoryBtn != null) viewInventoryBtn.setOnAction(e -> handleViewInventory());
        if (addIngredientBtn != null) addIngredientBtn.setOnAction(e -> handleAddIngredient());
        if (updateInventoryBtn != null) updateInventoryBtn.setOnAction(e -> handleUpdateInventory());
        
        // Management buttons - Employees
        if (viewEmployeesBtn != null) viewEmployeesBtn.setOnAction(e -> handleViewEmployees());
        if (addEmployeeBtn != null) addEmployeeBtn.setOnAction(e -> handleAddEmployee());
        if (updateEmployeeBtn != null) updateEmployeeBtn.setOnAction(e -> handleUpdateEmployee());
        
    // Reports buttons
    if (generateReportBtn != null) generateReportBtn.setOnAction(e -> handleGenerateReport());
    if (generateXReportBtn != null) generateXReportBtn.setOnAction(e -> handleGenerateXReport());
    if (generateZReportBtn != null) generateZReportBtn.setOnAction(e -> handleGenerateZReport());
        
        // Form buttons
        if (submitBtn != null) submitBtn.setOnAction(e -> handleSubmit());
        if (cancelBtn != null) cancelBtn.setOnAction(e -> handleCancel());
        
        // Management buttons - Menu
        if (viewMenuBtn != null) viewMenuBtn.setOnAction(e -> handleViewMenu());
        if (addMenuItemBtn != null) addMenuItemBtn.setOnAction(e -> handleAddMenuItem());
        if (updateMenuItemBtn != null) updateMenuItemBtn.setOnAction(e -> handleUpdateMenuItem());
        
        // Management buttons - Inventory
        if (viewInventoryBtn != null) viewInventoryBtn.setOnAction(e -> handleViewInventory());
        if (addIngredientBtn != null) addIngredientBtn.setOnAction(e -> handleAddIngredient());
        if (updateInventoryBtn != null) updateInventoryBtn.setOnAction(e -> handleUpdateInventory());
        
        // Management buttons - Employees
        if (viewEmployeesBtn != null) viewEmployeesBtn.setOnAction(e -> handleViewEmployees());
        if (addEmployeeBtn != null) addEmployeeBtn.setOnAction(e -> handleAddEmployee());
        if (updateEmployeeBtn != null) updateEmployeeBtn.setOnAction(e -> handleUpdateEmployee());
        
    // Reports buttons (dup safe)
    if (generateReportBtn != null) generateReportBtn.setOnAction(e -> handleGenerateReport());
    if (generateXReportBtn != null) generateXReportBtn.setOnAction(e -> handleGenerateXReport());
    if (generateZReportBtn != null) generateZReportBtn.setOnAction(e -> handleGenerateZReport());
    if (generateInventoryReportBtn != null) generateInventoryReportBtn.setOnAction(e -> handleGenerateInventoryReport());

    // Form buttons
    if (submitBtn != null) submitBtn.setOnAction(e -> handleSubmit());
    if (cancelBtn != null) cancelBtn.setOnAction(e -> handleCancel());

        if (drinksTabBtn != null) drinksTabBtn.setOnAction(e -> filterCategory("Drinks"));
        if (foodTabBtn != null) foodTabBtn.setOnAction(e -> filterCategory("Food"));
        if (merchTabBtn != null) merchTabBtn.setOnAction(e -> filterCategory("Merch"));

        setupSelectionHandlers();
        setupContinueHandler();
        
        // Populate default drinks for the menu
        populateDefaultDrinks();
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

    public void showOrderView() {
        updateOrderView();
        showPane(orderViewPane);
    }
    
    private void updateOrderView() {
        if (orderItemsContainer == null) return;
        
        // Clear existing items
        orderItemsContainer.getChildren().clear();
        
        if (orderItems.isEmpty()) {
            Label emptyLabel = new Label("No items in order yet.");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
            orderItemsContainer.getChildren().add(emptyLabel);
        } else {
            // Add each order item as a card
            for (int i = 0; i < orderItems.size(); i++) {
                String item = orderItems.get(i);
                VBox itemCard = createOrderItemCard(i + 1, item);
                orderItemsContainer.getChildren().add(itemCard);
            }
        }
        
        // Update total label
        if (orderTotalLabel != null) {
            orderTotalLabel.setText("Total Items: " + orderItems.size());
        }
    }
    
    private VBox createOrderItemCard(int itemNumber, String itemDetails) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 10; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        Label numberLabel = new Label("Item #" + itemNumber);
        numberLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        
        Label detailsLabel = new Label(itemDetails);
        detailsLabel.setStyle("-fx-font-size: 13px;");
        detailsLabel.setWrapText(true);
        
        card.getChildren().addAll(numberLabel, detailsLabel);
        return card;
    }
    
    private void handleClearOrder() {
        if (orderItems.isEmpty()) {
            Alert alert = new Alert(AlertType.INFORMATION, "Order is already empty.");
            alert.setHeaderText("No Items");
            alert.show();
            return;
        }
        
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION, "Are you sure you want to clear all items from the order?");
        confirmAlert.setHeaderText("Clear Order");
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                orderItems.clear();
                updateOrderView();
                Alert successAlert = new Alert(AlertType.INFORMATION, "Order has been cleared.");
                successAlert.setHeaderText("Order Cleared");
                successAlert.show();
            }
        });
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

    // ================== MANAGEMENT METHODS ==================
    
    // Menu Management Methods
    private void handleViewMenu() {
        currentOperation = "view_menu";
        showManagementSection("Menu Items");
        hideFormSection();
        showDisplayPane();
        
        try {
            java.util.List<String> drinks = dbManager.listDrinks();
            StringBuilder sb = new StringBuilder();
            sb.append("Current Menu Items:\n");
            sb.append("ID | Name | Price\n");
            sb.append("------------------------\n");
            
            for (String drink : drinks) {
                sb.append(drink).append("\n");
            }
            
            if (drinks.isEmpty()) {
                sb.append("No menu items found.");
            }
            
            displayArea.setText(sb.toString());
            updateStatus("Displaying all menu items.", "info");
            
        } catch (Exception e) {
            updateStatus("Error loading menu: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }
    
    private void handleAddMenuItem() {
        currentOperation = "add_menu";
        showManagementSection("Add Menu Item");
        hideDisplayPane();
        showFormSection();
        
        // Setup form for adding menu item
        idField.setVisible(false);
        idField.setManaged(false);
        field1Label.setText("Name:");
        field2Label.setText("Price:");
        field3Label.setText("Ingredients:");
        field4Container.setVisible(false);
        field4Container.setManaged(false);
        
        clearForm();
        field1.setPromptText("Enter drink name");
        field2.setPromptText("Enter price (e.g., 5.99)");
        field3.setPromptText("Enter ingredients (e.g., {Water, Milk, Sugar, Tea})");
        
        updateStatus("Enter details for new menu item.", "info");
    }
    
    private void handleUpdateMenuItem() {
        currentOperation = "update_menu";
        showManagementSection("Update Menu Item");
        hideDisplayPane();
        showFormSection();
        
        // Setup form for updating menu item
        idField.setVisible(true);
        idField.setManaged(true);
        field1Label.setText("Name:");
        field2Label.setText("Price:");
        field3Label.setText("Ingredients:");
        field4Container.setVisible(false);
        field4Container.setManaged(false);
        
        clearForm();
        idField.setPromptText("Enter menu item ID");
        field1.setPromptText("Enter new name");
        field2.setPromptText("Enter new price");
        field3.setPromptText("Enter new ingredients");
        
        updateStatus("Enter ID and new details for menu item.", "info");
    }
    
    // Inventory Management Methods
    private void handleViewInventory() {
        currentOperation = "view_inventory";
        showManagementSection("Inventory");
        hideFormSection();
        showDisplayPane();
        
        try {
            java.util.List<String> ingredients = dbManager.listIngredients();
            StringBuilder sb = new StringBuilder();
            sb.append("Current Inventory:\n");
            sb.append("ID | Name | Cost | Quantity\n");
            sb.append("----------------------------------\n");
            
            for (String ingredient : ingredients) {
                sb.append(ingredient).append("\n");
            }
            
            if (ingredients.isEmpty()) {
                sb.append("No ingredients found.");
            }
            
            displayArea.setText(sb.toString());
            updateStatus("Displaying all inventory items.", "info");
            
        } catch (Exception e) {
            updateStatus("Error loading inventory: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }
    
    private void handleAddIngredient() {
        currentOperation = "add_ingredient";
        showManagementSection("Add Ingredient");
        hideDisplayPane();
        showFormSection();
        
        // Setup form for adding ingredient
        idField.setVisible(false);
        idField.setManaged(false);
        field1Label.setText("Name:");
        field2Label.setText("Cost:");
        field3Label.setText("Quantity:");
        field4Container.setVisible(false);
        field4Container.setManaged(false);
        
        clearForm();
        field1.setPromptText("Enter ingredient name");
        field2.setPromptText("Enter cost per unit");
        field3.setPromptText("Enter initial quantity");
        
        updateStatus("Enter details for new ingredient.", "info");
    }
    
    private void handleUpdateInventory() {
        currentOperation = "update_inventory";
        showManagementSection("Update Inventory");
        hideDisplayPane();
        showFormSection();
        
        // Setup form for updating inventory
        idField.setVisible(true);
        idField.setManaged(true);
        field1Label.setText("Cost:");
        field2Label.setText("Quantity:");
        field3Label.setText("Adjustment:");
        field4Container.setVisible(false);
        field4Container.setManaged(false);
        
        clearForm();
        idField.setPromptText("Enter ingredient ID");
        field1.setPromptText("New cost (leave empty to keep current)");
        field2.setPromptText("New quantity (leave empty to keep current)");
        field3.setPromptText("Quantity adjustment (+/- amount)");
        
        updateStatus("Enter ID and updates for inventory item.", "info");
    }
    
    // Employee Management Methods
    private void handleViewEmployees() {
        currentOperation = "view_employees";
        showManagementSection("Employees");
        hideFormSection();
        showDisplayPane();
        
        try {
            java.util.List<String> employees = dbManager.listEmployees();
            StringBuilder sb = new StringBuilder();
            sb.append("Current Employees:\n");
            sb.append("ID | Name | Email | Role | Active\n");
            sb.append("----------------------------------------\n");
            
            for (String employee : employees) {
                sb.append(employee).append("\n");
            }
            
            if (employees.isEmpty()) {
                sb.append("No employees found.");
            }
            
            displayArea.setText(sb.toString());
            updateStatus("Displaying all employees.", "info");
            
        } catch (Exception e) {
            updateStatus("Error loading employees: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }
    
    private void handleAddEmployee() {
        currentOperation = "add_employee";
        showManagementSection("Add Employee");
        hideDisplayPane();
        showFormSection();
        
        // Setup form for adding employee
        idField.setVisible(false);
        idField.setManaged(false);
        field1Label.setText("First Name:");
        field2Label.setText("Last Name:");
        field3Label.setText("Email:");
        field4Label.setText("Role:");
        field4Container.setVisible(true);
        field4Container.setManaged(true);
        
        clearForm();
        field1.setPromptText("Enter first name");
        field2.setPromptText("Enter last name");
        field3.setPromptText("Enter email address");
        field4.setPromptText("Enter role (CASHIER or MANAGER)");
        
        updateStatus("Enter details for new employee.", "info");
    }
    
    private void handleUpdateEmployee() {
        currentOperation = "update_employee";
        showManagementSection("Update Employee");
        hideDisplayPane();
        showFormSection();
        
        // Setup form for updating employee
        idField.setVisible(true);
        idField.setManaged(true);
        field1Label.setText("First Name:");
        field2Label.setText("Last Name:");
        field3Label.setText("Email:");
        field4Label.setText("Role:");
        field4Container.setVisible(true);
        field4Container.setManaged(true);
        
        clearForm();
        idField.setPromptText("Enter employee ID");
        field1.setPromptText("New first name (leave empty to keep)");
        field2.setPromptText("New last name (leave empty to keep)");
        field3.setPromptText("New email (leave empty to keep)");
        field4.setPromptText("New role (leave empty to keep)");
        
        updateStatus("Enter ID and updates for employee.", "info");
    }
    
    // Reports Management Method
    private void handleGenerateReport() {
        currentOperation = "generate_report";
        showManagementSection("Generate Sales Report");
        hideDisplayPane();
        showFormSection();
        
        // Setup form for date range selection
        idField.setVisible(false);
        idField.setManaged(false);
        field1Label.setText("");
        field2Label.setText("");
        field3Label.setText("");
        field4Container.setVisible(false);
        field4Container.setManaged(false);
        field1.setVisible(false);
        field1.setManaged(false);
        field2.setVisible(false);
        field2.setManaged(false);
        field3.setVisible(false);
        field3.setManaged(false);
        
        // Show date range container
        dateRangeContainer.setVisible(true);
        dateRangeContainer.setManaged(true);
        
        // Set default dates to today
        java.time.LocalDate today = java.time.LocalDate.now();
        startDatePicker.setValue(today);
        endDatePicker.setValue(today);
        
        updateStatus("Select date range for sales report.", "info");
    }
    
    private void handleGenerateInventoryReport() {
        currentOperation = "generate_inventory_report";
        showManagementSection("Generate Inventory Report");
        hideDisplayPane();
        showFormSection();
        
        // Setup form for date range selection
        idField.setVisible(false);
        idField.setManaged(false);
        field1Label.setText("");
        field2Label.setText("");
        field3Label.setText("");
        field4Container.setVisible(false);
        field4Container.setManaged(false);
        field1.setVisible(false);
        field1.setManaged(false);
        field2.setVisible(false);
        field2.setManaged(false);
        field3.setVisible(false);
        field3.setManaged(false);
        
        // Show date range container
        dateRangeContainer.setVisible(true);
        dateRangeContainer.setManaged(true);
        
        // Set default dates to today
        java.time.LocalDate today = java.time.LocalDate.now();
        startDatePicker.setValue(today);
        endDatePicker.setValue(today);
        
        updateStatus("Select date range for inventory report.", "info");
    }
    
    // Form handling methods
    private void handleSubmit() {
        try {
            switch (currentOperation) {
                case "add_menu" -> submitAddMenuItem();
                case "update_menu" -> submitUpdateMenuItem();
                case "add_ingredient" -> submitAddIngredient();
                case "update_inventory" -> submitUpdateInventory();
                case "add_employee" -> submitAddEmployee();
                case "update_employee" -> submitUpdateEmployee();
                case "generate_report" -> submitGenerateReport();
                case "generate_inventory_report" -> submitGenerateInventoryReport();
                default -> updateStatus("Unknown operation", "error");
            }
        } catch (Exception e) {
            updateStatus("Error: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }
    
    private void handleCancel() {
        hideManagementSection();
        clearForm();
        hideDateRangeContainer();
        updateStatus("", "info");
        currentOperation = "";
    }
    
    // Submit implementations
    private void submitAddMenuItem() throws Exception {
        String name = field1.getText().trim();
        String priceText = field2.getText().trim();
        String ingredients = field3.getText().trim();
        
        if (name.isEmpty() || priceText.isEmpty()) {
            updateStatus("Name and price are required.", "error");
            return;
        }
        
        double price = Double.parseDouble(priceText);
        if (ingredients.isEmpty()) ingredients = "{Water}";
        
        int id = dbManager.addDrink(name, price, ingredients);
        if (id > 0) {
            updateStatus("Menu item '" + name + "' added successfully with ID: " + id, "success");
            clearForm();
            // Also add a visual tile for the new drink to the orders page so it appears immediately
            try {
                addDrinkTile(name, price, id, ingredients);
            } catch (Exception e) {
                // Non-fatal: log and continue
                e.printStackTrace();
            }
        } else {
            updateStatus("Failed to add menu item.", "error");
        }
    }

    /**
     * Create a visual tile for a drink and add it to the drinksGrid FlowPane.
     * The tile will be clickable and will open the customization page for that drink.
     */
    private void addDrinkTile(String name, double price, int id, String ingredients) {
        if (drinksGrid == null) return;

        VBox tile = new VBox();
        tile.setSpacing(6);
        tile.setStyle("-fx-alignment: center; -fx-padding: 8; -fx-border-color: #d0d0d0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: linear-gradient(#ffffff, #f7f7f7);");

        // Try to load an image for the drink. Prefer classpath /images/, then project folder "bin/Drink Pics/", then "Drink Pics/"
        String fileName = slugify(name) + ".png";
        Image img = null;
        try {
            InputStream is = getClass().getResourceAsStream("/images/" + fileName);
            if (is != null) {
                // Use InputStream overload: (InputStream, requestedWidth, requestedHeight, preserveRatio, smooth)
                img = new Image(is, 150, 100, true, true);
            } else {
                File f = new File("bin/Drink Pics/" + fileName);
                if (!f.exists()) f = new File("Drink Pics/" + fileName);
                if (f.exists()) {
                    img = new Image(f.toURI().toString(), 150, 100, true, true);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        ImageView thumbView = new ImageView();
        if (img != null) {
            thumbView.setImage(img);
        } else {
            // fallback visual when no image found
            Rectangle placeholder = new Rectangle(150, 100);
            placeholder.setArcWidth(8);
            placeholder.setArcHeight(8);
            placeholder.setStyle("-fx-fill: #f0f0f0; -fx-stroke: #cccccc;");
            // add name and price with placeholder
            Label nameLbl = new Label(name);
            Label priceLbl = new Label(String.format("$%.2f", price));
            tile.getChildren().addAll(placeholder, nameLbl, priceLbl);
            // Attach tooltip showing ingredients if available
            if (ingredients != null && !ingredients.isEmpty()) {
                Tooltip tooltip = new Tooltip("Ingredients: " + ingredients);
                Tooltip.install(tile, tooltip);
            }
            drinksGrid.getChildren().add(tile);
            return;
        }

        thumbView.setFitWidth(150);
        thumbView.setPreserveRatio(true);
        thumbView.setSmooth(true);

        Label nameLbl = new Label(name);
        Label priceLbl = new Label(String.format("$%.2f", price));

        // Attach a tooltip showing ingredients if available
        if (ingredients != null && !ingredients.isEmpty()) {
            Tooltip tooltip = new Tooltip("Ingredients: " + ingredients);
            Tooltip.install(thumbView, tooltip);
        }

        tile.getChildren().addAll(thumbView, nameLbl, priceLbl);

        // Make the tile open the customization page when clicked
        tile.setOnMouseClicked(e -> showCustomizationPage(name));

        drinksGrid.getChildren().add(tile);
    }

    /**
     * Populate a curated list of default drinks into the drinksGrid.
     */
    private void populateDefaultDrinks() {
        String[][] defaults = new String[][] {
            {"Original Milk Tea", "5.25", "{Water, Milk, Sugar, Tea}"},
            {"Black Milk Tea", "5.25", "{Water, Milk, Sugar, Black Tea}"},
            {"Oolong Milk Tea", "5.25", "{Water, Milk, Sugar, Oolong Tea}"},
            {"Green Milk Tea", "5.25", "{Water, Milk, Sugar, Green Tea}"},
            {"Capuccino Milk Tea", "6.25", "{Water, Milk, Sugar, Coffee, Cream}"},
            {"Coconut Milk Tea", "7.25", "{Water, Milk, Sugar, Tea, Coconut}"},
            {"Ube Milk Tea", "7.25", "{Water, Milk, Sugar, Ube Powder}"},
            {"Protein Shake Milk Tea", "9.75", "{Water, Milk, Sugar, Tea, Protein Powder}"},
            {"Ice Blend Latte", "6.25", "{Water, Milk, Sugar, Tea, Protein Powder}"},
            {"Winter Melon Green Tea", "8.25", "{Water, Sugar, Green Tea, Winter Melon}"},
            {"Passionfruit Green Tea", "7.25", "{Water, Sugar, Passionfruit, Green Tea}"},
            {"Mango Green Tea", "3.25", "{Water, Sugar, Green Tea, Mango}"},
            {"Strawberry Lemonade Tea", "3.25", "{Water, Sugar, Green Tea, Strawberry Lemonade}"},
            {"Strawberry Matcha", "7.25", "{Water, Sugar, Green Tea, Strawberry}"},
            {"Peach Oolong Tea", "7.25", "{Water, Sugar, Oolong Tea, Peach}"},
            {"Secret Matcha", "69.25", "{Water, Matcha}"},
            {"Free Drink", "0.00", "{Water, Milk, Sugar, Tea}"}
        };

        for (String[] item : defaults) {
            String name = item[0];
            double price = 0.0;
            try {
                price = Double.parseDouble(item[1]);
            } catch (NumberFormatException ex) {
                // leave price as 0.0
            }
            String ingredients = item.length > 2 ? item[2] : "";
            addDrinkTile(name, price, -1, ingredients);
        }
    }

    /** Create a filesystem/classpath-safe filename from a display name */
    private String slugify(String name) {
        if (name == null) return "";
        String s = name.toLowerCase();
        s = s.replaceAll("[^a-z0-9\\s-]", "");
        s = s.trim().replaceAll("\\s+", "_");
        return s;
    }
    
    private void submitUpdateMenuItem() throws Exception {
        String idText = idField.getText().trim();
        String name = field1.getText().trim();
        String priceText = field2.getText().trim();
        String ingredients = field3.getText().trim();
        
        if (idText.isEmpty()) {
            updateStatus("ID is required for updates.", "error");
            return;
        }
        
        int id = Integer.parseInt(idText);
        boolean updated = false;
        
        if (!name.isEmpty() && !ingredients.isEmpty()) {
            updated = dbManager.updateDrink(id, name, ingredients);
        }
        
        if (!priceText.isEmpty()) {
            double price = Double.parseDouble(priceText);
            updated = dbManager.updateDrinkPrice(id, price) || updated;
        }
        
        if (updated) {
            updateStatus("Menu item ID " + id + " updated successfully.", "success");
            clearForm();
        } else {
            updateStatus("Failed to update menu item.", "error");
        }
    }
    
    private void submitAddIngredient() throws Exception {
        String name = field1.getText().trim();
        String costText = field2.getText().trim();
        String quantityText = field3.getText().trim();
        
        if (name.isEmpty() || costText.isEmpty() || quantityText.isEmpty()) {
            updateStatus("All fields are required.", "error");
            return;
        }
        
        double cost = Double.parseDouble(costText);
        double quantity = Double.parseDouble(quantityText);
        
        int id = dbManager.addIngredient(name, cost, quantity);
        if (id > 0) {
            updateStatus("Ingredient '" + name + "' added successfully with ID: " + id, "success");
            clearForm();
        } else {
            updateStatus("Failed to add ingredient.", "error");
        }
    }
    
    private void submitUpdateInventory() throws Exception {
        String idText = idField.getText().trim();
        String costText = field1.getText().trim();
        String quantityText = field2.getText().trim();
        String adjustmentText = field3.getText().trim();
        
        if (idText.isEmpty()) {
            updateStatus("ID is required for updates.", "error");
            return;
        }
        
        int id = Integer.parseInt(idText);
        boolean updated = false;
        
        if (!costText.isEmpty()) {
            double cost = Double.parseDouble(costText);
            updated = dbManager.updateIngredientCost(id, cost);
        }
        
        if (!quantityText.isEmpty()) {
            double quantity = Double.parseDouble(quantityText);
            updated = dbManager.updateIngredientQuantity(id, quantity) || updated;
        }
        
        if (!adjustmentText.isEmpty()) {
            double adjustment = Double.parseDouble(adjustmentText);
            updated = dbManager.adjustIngredientQuantity(id, adjustment) || updated;
        }
        
        if (updated) {
            updateStatus("Inventory item ID " + id + " updated successfully.", "success");
            clearForm();
        } else {
            updateStatus("Failed to update inventory item.", "error");
        }
    }
    
    private void submitAddEmployee() throws Exception {
        String firstName = field1.getText().trim();
        String lastName = field2.getText().trim();
        String email = field3.getText().trim();
        String role = field4.getText().trim().toUpperCase();
        
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || role.isEmpty()) {
            updateStatus("All fields are required.", "error");
            return;
        }
        
        if (!role.equals("CASHIER") && !role.equals("MANAGER")) {
            updateStatus("Role must be CASHIER or MANAGER.", "error");
            return;
        }
        
        int id = dbManager.addEmployee(firstName, lastName, email, role);
        if (id > 0) {
            updateStatus("Employee '" + firstName + " " + lastName + "' added successfully with ID: " + id, "success");
            clearForm();
        } else {
            updateStatus("Failed to add employee.", "error");
        }
    }
    
    private void submitUpdateEmployee() throws Exception {
        String idText = idField.getText().trim();
        String firstName = field1.getText().trim();
        String lastName = field2.getText().trim();
        String email = field3.getText().trim();
        String role = field4.getText().trim().toUpperCase();
        
        if (idText.isEmpty()) {
            updateStatus("ID is required for updates.", "error");
            return;
        }
        
        int id = Integer.parseInt(idText);
        
        // Convert empty strings to null
        String firstNameUpdate = firstName.isEmpty() ? null : firstName;
        String lastNameUpdate = lastName.isEmpty() ? null : lastName;
        String emailUpdate = email.isEmpty() ? null : email;
        String roleUpdate = role.isEmpty() ? null : role;
        
        if (roleUpdate != null && !roleUpdate.equals("CASHIER") && !roleUpdate.equals("MANAGER")) {
            updateStatus("Role must be CASHIER or MANAGER.", "error");
            return;
        }
        
        boolean updated = dbManager.updateEmployee(id, firstNameUpdate, lastNameUpdate, emailUpdate, roleUpdate, null);
        
        if (updated) {
            updateStatus("Employee ID " + id + " updated successfully.", "success");
            clearForm();
        } else {
            updateStatus("Failed to update employee or ID not found.", "error");
        }
    }
    
    private void submitGenerateReport() throws Exception {
        java.time.LocalDate startDate = startDatePicker.getValue();
        java.time.LocalDate endDate = endDatePicker.getValue();
        
        if (startDate == null || endDate == null) {
            updateStatus("Please select both start and end dates.", "error");
            return;
        }
        
        if (endDate.isBefore(startDate)) {
            updateStatus("End date cannot be before start date.", "error");
            return;
        }
        
        // Generate the report with selected date range
        Reports reports = new Reports(startDate, endDate);
        String reportContent = reports.generateSalesSummary(dbManager);
        
        // Hide form and show display area with report
        hideFormSection();
        showDisplayPane();
        displayArea.setText(reportContent);
        
        updateStatus("Sales report generated successfully for " + startDate + " to " + endDate, "success");
    }
    
    private void submitGenerateInventoryReport() throws Exception {
        java.time.LocalDate startDate = startDatePicker.getValue();
        java.time.LocalDate endDate = endDatePicker.getValue();
        
        if (startDate == null || endDate == null) {
            updateStatus("Please select both start and end dates.", "error");
            return;
        }
        
        if (endDate.isBefore(startDate)) {
            updateStatus("End date cannot be before start date.", "error");
            return;
        }
        
        // Get the inventory usage data
        Map<String, Integer> ingredientUsage = getIngredientUsageData(startDate, endDate);
        
        // Hide form section
        hideFormSection();
        
        if (ingredientUsage.isEmpty()) {
            // If no data, show a message in the text area
            showDisplayPane();
            displayArea.setText("No ingredient usage data found for the selected date range.\n" +
                              "Date Range: " + startDate + " to " + endDate);
            updateStatus("No inventory data found for the selected period.", "info");
            return;
        }
        
        // Create a bar chart for inventory usage
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Ingredients");
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Units Used");
        
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Inventory Usage Report (" + startDate + " to " + endDate + ")");
        barChart.setLegendVisible(false);
        
        // Create data series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Ingredient Usage");
        
        // Add data to the series
        for (Map.Entry<String, Integer> entry : ingredientUsage.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        
        barChart.getData().add(series);
        
        // Set chart size
        barChart.setPrefHeight(500);
        barChart.setPrefWidth(700);
        
        // Clear the display pane and add the chart
        if (displayPane != null) {
            displayPane.setContent(barChart);
            displayPane.setVisible(true);
            displayPane.setManaged(true);
        }
        
        updateStatus("Inventory report chart generated successfully for " + startDate + " to " + endDate, "success");
    }
    
    /**
     * Helper method to get ingredient usage data from the database
     * Returns a map of ingredient names to quantities used, sorted by usage (descending)
     */
    private Map<String, Integer> getIngredientUsageData(java.time.LocalDate startDate, java.time.LocalDate endDate) throws SQLException {
        java.time.format.DateTimeFormatter DATE_ONLY = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");
        String startStr = startDate.format(DATE_ONLY);
        String endStr = endDate.format(DATE_ONLY);
        
        // First, get all orders in the date range with their menu items
        String orderSql = "SELECT menu_items FROM orderhistory WHERE " +
            "(substr(order_datetime,5,4)||substr(order_datetime,1,2)||substr(order_datetime,3,2)) BETWEEN ? AND ?";
        
        // Map to track ingredient usage: ingredient name -> quantity used
        Map<String, Integer> ingredientUsage = new LinkedHashMap<>();
        
        try (java.sql.PreparedStatement ps = dbManager.getConnection().prepareStatement(orderSql)) {
            ps.setString(1, startStr);
            ps.setString(2, endStr);
            
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String raw = rs.getString("menu_items");
                    if (raw == null || raw.isBlank()) continue;
                    
                    // Parse menu items JSON: {"Drink Name": [qty, ...], ...}
                    Map<String, Integer> drinksInOrder = new LinkedHashMap<>();
                    int i = 0;
                    int len = raw.length();
                    while (i < len) {
                        int qs = raw.indexOf('"', i);
                        if (qs < 0) break;
                        int qe = raw.indexOf('"', qs + 1);
                        if (qe < 0) break;
                        String drinkName = raw.substring(qs + 1, qe);
                        
                        int listStart = raw.indexOf('[', qe);
                        if (listStart < 0) {
                            i = qe + 1;
                            continue;
                        }
                        int listEnd = raw.indexOf(']', listStart);
                        if (listEnd < 0) {
                            i = qe + 1;
                            continue;
                        }
                        
                        String inside = raw.substring(listStart + 1, listEnd).trim();
                        int qty = 1; // default quantity
                        if (!inside.isEmpty()) {
                            // Parse the quantities: [small, medium, large]
                            String[] quantities = inside.split(",");
                            int totalQty = 0;
                            for (String q : quantities) {
                                try {
                                    totalQty += Integer.parseInt(q.trim());
                                } catch (NumberFormatException ignore) {}
                            }
                            qty = Math.max(1, totalQty);
                        }
                        
                        drinksInOrder.put(drinkName, qty);
                        i = listEnd + 1;
                    }
                    
                    // For each drink in the order, look up its ingredients and add to usage
                    for (Map.Entry<String, Integer> entry : drinksInOrder.entrySet()) {
                        String drinkName = entry.getKey();
                        int drinkQty = entry.getValue();
                        
                        // Query the drinks table to get ingredients for this drink
                        String drinkSql = "SELECT ingredients FROM drinks WHERE name = ?";
                        try (java.sql.PreparedStatement drinkPs = dbManager.getConnection().prepareStatement(drinkSql)) {
                            drinkPs.setString(1, drinkName);
                            try (java.sql.ResultSet drinkRs = drinkPs.executeQuery()) {
                                if (drinkRs.next()) {
                                    String ingredientsStr = drinkRs.getString("ingredients");
                                    if (ingredientsStr != null && !ingredientsStr.isBlank()) {
                                        // Parse ingredients: {Water, Milk, Sugar, Tea}
                                        ingredientsStr = ingredientsStr.replaceAll("[{}]", "").trim();
                                        String[] ingredients = ingredientsStr.split(",");
                                        
                                        for (String ingredient : ingredients) {
                                            ingredient = ingredient.trim();
                                            if (!ingredient.isEmpty()) {
                                                ingredientUsage.merge(ingredient, drinkQty, Integer::sum);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Sort ingredients by usage (descending) then by name and return as LinkedHashMap
        return ingredientUsage.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getValue(), a.getValue());
                    if (cmp != 0) return cmp;
                    return a.getKey().compareTo(b.getKey());
                })
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));
    }
    
    // UI Helper Methods
    private void showManagementSection(String title) {
        if (managementSection != null) {
            managementSection.setVisible(true);
            managementSection.setManaged(true);
        }
        if (managementTitle != null) {
            managementTitle.setText(title);
        }
    }
    
    private void hideManagementSection() {
        if (managementSection != null) {
            managementSection.setVisible(false);
            managementSection.setManaged(false);
        }
        hideDisplayPane();
        hideFormSection();
    }
    
    private void showDisplayPane() {
        if (displayPane != null) {
            displayPane.setVisible(true);
            displayPane.setManaged(true);
        }
    }
    
    private void hideDisplayPane() {
        if (displayPane != null) {
            displayPane.setVisible(false);
            displayPane.setManaged(false);
        }
    }
    
    private void showFormSection() {
        if (formSection != null) {
            formSection.setVisible(true);
            formSection.setManaged(true);
        }
    }
    
    private void hideFormSection() {
        if (formSection != null) {
            formSection.setVisible(false);
            formSection.setManaged(false);
        }
    }
    
    private void clearForm() {
        if (idField != null) idField.clear();
        if (field1 != null) field1.clear();
        if (field2 != null) field2.clear();
        if (field3 != null) field3.clear();
        if (field4 != null) field4.clear();
    }
    
    private void hideDateRangeContainer() {
        if (dateRangeContainer != null) {
            dateRangeContainer.setVisible(false);
            dateRangeContainer.setManaged(false);
        }
    }
    
    private void updateStatus(String message, String type) {
        if (statusLabel == null) return;
        statusLabel.setText(message);
        switch (type.toLowerCase()) {
            case "error" -> statusLabel.setStyle("-fx-text-fill: red;");
            case "success" -> statusLabel.setStyle("-fx-text-fill: green;");
            case "info" -> statusLabel.setStyle("-fx-text-fill: blue;");
            default -> statusLabel.setStyle("-fx-text-fill: black;");
        }
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