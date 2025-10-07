# Project_2_Machamp_POS
The repository for project 2

## Employee Class Hierarchy

Implemented base `Employee` (abstract) with subclasses `Cashier` and `Manager` located in `backend/`.

### Classes
* `Employee` – common fields: id, firstName, lastName, email, role, active flag.
* `Cashier` – adds order submission helpers (`submitOrder`).
* `Manager` – placeholder methods for menu, inventory, employee, and reporting management.
* `EmployeeRole` – enum with `CASHIER`, `MANAGER`.

### Example Usage
```java
import backend.*;

public class Demo {
    public static void main(String[] args) {
        Cashier c = new Cashier(101, "Alice", "Nguyen", "alice@example.com");
        Manager m = new Manager(1, "Bob", "Smith", "bob@example.com");

        int orderId = c.submitOrder(java.util.Map.of(10, 2, 25, 1));
        System.out.println("Created order id: " + orderId);

        m.updateMenuItemPrice(10, 7.99); // placeholder call
        System.out.println("Manager: " + m.getFullName());
    }
}
```

### Notes / Next Steps
* Replace placeholder TODOs with real service / DAO implementations (database access layer).
* Add authentication layer later if needed (password logic removed for now).
* Add validation & exception hierarchy for domain errors.
* Introduce interfaces/services for reporting, inventory, and menu to decouple from UI.
