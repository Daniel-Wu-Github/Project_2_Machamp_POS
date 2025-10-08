import java.util.List;
import java.util.Map;

/**
 * Cashier subclass - focused on order submission responsibilities.
 * Real order logic should live in service classes; this class can carry
 * contextual helper methods or cashier-specific constraints.
 */
public class Cashier extends Employee {

    public Cashier(int id, String firstName, String lastName, String email) {
        super(id, firstName, lastName, email, EmployeeRole.CASHIER);
    }

    /**
     * Submit an order. Placeholder signature: items is a map of itemId -> quantity.
     * Returns a generated orderId (mock for now).
     */
    public int submitOrder(Map<Integer, Integer> items) {
        // TODO: Integrate with OrderService / DB layer.
        // Validate inventory, compute totals, persist order, decrement inventory.
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        // Temporary: return a fake order id derived from hash.
        return Math.abs(items.hashCode());
    }
    
    /**
     * Convenience overload using list of itemIds (quantity = 1 each).
     */
    public int submitOrder(List<Integer> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        java.util.Map<Integer, Integer> map = new java.util.HashMap<>();
        for (Integer id : itemIds) {
            map.merge(id, 1, Integer::sum);
        }
        return submitOrder(map);
    }
}
