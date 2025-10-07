import java.util.List;
import java.util.Map;

/**
 * Manager subclass - has elevated capabilities for managing menu, inventory, employees, and reports.
 * Actual DB operations should be delegated to service/repository layers. These methods are placeholders
 * that define intended interactions.
 */
public class manager extends Employee {

    public manager(int id, String firstName, String lastName, String email) {
        super(id, firstName, lastName, email, EmployeeRole.MANAGER);
    }

    // ---- Menu Management ----
    public void addMenuItem(int itemId, String name, double price) {
        // TODO: Persist new item
        validatePrice(price);
    }

    public void updateMenuItemPrice(int itemId, double newPrice) {
        validatePrice(newPrice);
        // TODO: Update DB
    }

    public void updateMenuItemName(int itemId, String newName) {
        // TODO: Update DB
    }

    public List<String> viewMenu() {
        // TODO: Query DB for menu items
        return java.util.Collections.emptyList();
    }

    // ---- Inventory Management ----
    public void addInventoryItem(int sku, String name, int quantity) {
        validateQuantity(quantity);
        // TODO: Persist
    }

    public void updateInventoryQuantity(int sku, int newQuantity) {
        validateQuantity(newQuantity);
        // TODO: Update
    }

    public Map<Integer, Integer> viewInventory() {
        // TODO: Query DB for inventory sku -> quantity
        return java.util.Collections.emptyMap();
    }

    // ---- Employee Management ----
    public void addEmployee(Employee employee) {
        if (employee == null) throw new IllegalArgumentException("employee null");
        // TODO: Persist employee record.
    }

    public void deactivateEmployee(Employee employee) {
        if (employee == null) throw new IllegalArgumentException("employee null");
        employee.setActive(false);
        // TODO: Persist state change.
    }

    public List<Employee> listEmployees() {
        // TODO: Query DB for employees.
        return java.util.Collections.emptyList();
    }

    // ---- Reporting ----
    public String generateSalesReport() {
        // TODO: Pull order data & aggregate
        return "(sales report placeholder)";
    }

    public String generateInventoryReport() {
        // TODO: Pull inventory + low stock warnings
        return "(inventory report placeholder)";
    }

    // ---- Helpers ----
    private void validatePrice(double price) {
        if (price < 0) throw new IllegalArgumentException("Price cannot be negative");
    }

    private void validateQuantity(int qty) {
        if (qty < 0) throw new IllegalArgumentException("Quantity cannot be negative");
    }
}
