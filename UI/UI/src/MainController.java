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
    
    
    // Management buttons
    @FXML private Button viewMenuBtn, addMenuItemBtn, updateMenuItemBtn;
    @FXML private Button viewInventoryBtn, addIngredientBtn, updateInventoryBtn;
    @FXML private Button viewEmployeesBtn, addEmployeeBtn, updateEmployeeBtn;
    @FXML private Button generateReportBtn;
    
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

    // Legacy product form (kept if needed for future admin input) - optional null if removed from FXML
    @FXML private TextField productNameField;
    @FXML private TextField priceField;
    @FXML private Button addProductButton;
    @FXML private Button clearButton;

    // State for customization
    private String selectedDrinkName;
    
    // Database manager instance
    private DatabaseManager dbManager;
    
    // Current management operation
    private String currentOperation = "";
    private String selectedSize = "Medium"; // default
    private String selectedSugar = "Normal"; // default
    private final List<String> selectedToppings = new ArrayList<>();
    private final List<String> orderItems = new ArrayList<>(); // stored orders
    
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
        if (backFromCustomizationBtn != null) {
            backFromCustomizationBtn.setOnAction(e -> showOrdersPage());
        }
        if (backFromManagerBtn != null) {
            backFromManagerBtn.setOnAction(e -> showOrdersPage());
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
        
        // Reports button
        if (generateReportBtn != null) generateReportBtn.setOnAction(e -> handleGenerateReport());
        
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
        
        // Reports button
        if (generateReportBtn != null) generateReportBtn.setOnAction(e -> handleGenerateReport());
        
        // Form buttons
        if (submitBtn != null) submitBtn.setOnAction(e -> handleSubmit());
        if (cancelBtn != null) cancelBtn.setOnAction(e -> handleCancel());

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
        } else {
            updateStatus("Failed to add menu item.", "error");
        }
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