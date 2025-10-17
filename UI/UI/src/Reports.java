import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reports utility for managers.
 * Maintains a start and end date (inclusive) and can generate simple sales summaries
 * from the orderhistory table managed by DatabaseManager.
 *
 * Date/Time format in orderhistory:
 *  - order_datetime is stored as mmddyyyyhhmm (e.g. 092620240901 => 09/26/2024 09:01)
 *  - For date range filtering we normalize inside the SQL to yyyyMMdd using:
 *      year = substr(order_datetime,5,4)
 *      month = substr(order_datetime,1,2)
 *      day = substr(order_datetime,3,2)
 *    Then we compare (year||month||day) BETWEEN ? AND ? where parameters are yyyyMMdd.
 */
public class Reports {
    private LocalDate startDate; // inclusive
    private LocalDate endDate;   // inclusive

    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyyMMdd"); // used for normalized comparisons

    public Reports() {
        // default range: today to today
        LocalDate today = LocalDate.now();
        this.startDate = today;
        this.endDate = today;
    }

    public Reports(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) throw new IllegalArgumentException("End date cannot be before start date");
        this.startDate = start;
        this.endDate = end;
    }

    public void setStartDate(LocalDate start) {
        if (endDate != null && endDate.isBefore(start)) throw new IllegalArgumentException("Start after current end date");
        this.startDate = start;
    }

    public void setEndDate(LocalDate end) {
        if (startDate != null && end.isBefore(startDate)) throw new IllegalArgumentException("End before current start date");
        this.endDate = end;
    }

    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }

    /**
     * Generate a sales summary string containing:
     *  - Total sales (sum of total_price)
     *  - List of items sold with aggregate counts across all orders in range
     * The parsing logic expects menu_items stored as a JSON-like map: {"Drink": [a,b,c], ...}
     * We count each key once per occurrence (any quantity encoded inside the tuple/list is ignored for now
     * because the provided structure (a,b,c) meaning wasn't specified). You can adapt to interpret these numbers later.
     */
    public String generateSalesSummary(DatabaseManager db) throws SQLException {
        String startStr = startDate.format(DATE_ONLY);
        String endStr = endDate.format(DATE_ONLY);

    // Normalize mmddyyyyhhmm -> yyyyMMdd for BETWEEN comparison
    String sql = "SELECT menu_items, total_price FROM orderhistory WHERE " +
        "(substr(order_datetime,5,4)||substr(order_datetime,1,2)||substr(order_datetime,3,2)) BETWEEN ? AND ?";
        double total = 0.0;
        int orderCount = 0;
        Map<String,Integer> itemQty = new LinkedHashMap<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, startStr);
            ps.setString(2, endStr);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orderCount++;
                    total += rs.getDouble("total_price");
                    String raw = rs.getString("menu_items");
                    if (raw == null || raw.isBlank()) continue;
                    // Example raw (after normalization step in DB load): {"Mango Green Tea": [1, 0, 2], "Cappuccino Milk Tea": [0, 2, 0]}
                    // We'll extract each key and take the first integer inside its list as the quantity.
                    int i = 0; int len = raw.length();
                    while (i < len) {
                        int qs = raw.indexOf('"', i); if (qs < 0) break;
                        int qe = raw.indexOf('"', qs+1); if (qe < 0) break;
                        String name = raw.substring(qs+1, qe);
                        int listStart = raw.indexOf('[', qe);
                        if (listStart < 0) { i = qe + 1; continue; }
                        int listEnd = raw.indexOf(']', listStart);
                        if (listEnd < 0) { i = qe + 1; continue; }
                        String inside = raw.substring(listStart+1, listEnd).trim();
                        int qty = 1; // fallback
                        if (!inside.isEmpty()) {
                            int comma = inside.indexOf(',');
                            String firstNum = (comma>=0? inside.substring(0,comma) : inside).trim();
                            try { qty = Integer.parseInt(firstNum); } catch (NumberFormatException ignore) {}
                        }
                        itemQty.merge(name, qty, Integer::sum);
                        i = listEnd + 1;
                    }
                }
            }
        }
        // Sort items by quantity desc then name
        var sorted = itemQty.entrySet().stream()
                .sorted((a,b) -> {
                    int cmp = Integer.compare(b.getValue(), a.getValue());
                    if (cmp != 0) return cmp; return a.getKey().compareTo(b.getKey());
                })
                .collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        sb.append("Sales Summary (" + startStr + " to " + endStr + ")\n");
        sb.append(String.format("Orders: %d | Total Sales: $%.2f | Avg Order: $%.2f\n", orderCount, total, orderCount==0?0: total/orderCount));
        sb.append("Items Sold (aggregated quantities from first value in tuple):\n");
        for (var e : sorted) {
            sb.append(String.format(" - %s: %d\n", e.getKey(), e.getValue()));
        }
        return sb.toString();
    }

    /**
     * Generate an inventory usage report showing how much of each ingredient was used
     * during the specified time window based on orders.
     * 
     * This analyzes all orders in the date range, extracts the menu items,
     * looks up which ingredients are used in each drink, and aggregates the total usage.
     * 
     * @param db DatabaseManager instance to query order history and drink ingredients
     * @return A formatted string containing the inventory usage report
     * @throws SQLException if database access fails
     */
    public String generateInventoryUsageReport(DatabaseManager db) throws SQLException {
        String startStr = startDate.format(DATE_ONLY);
        String endStr = endDate.format(DATE_ONLY);

        // First, get all orders in the date range with their menu items
        String orderSql = "SELECT menu_items FROM orderhistory WHERE " +
            "(substr(order_datetime,5,4)||substr(order_datetime,1,2)||substr(order_datetime,3,2)) BETWEEN ? AND ?";
        
        // Map to track ingredient usage: ingredient name -> quantity used
        Map<String, Integer> ingredientUsage = new LinkedHashMap<>();
        int totalOrders = 0;
        
        try (PreparedStatement ps = db.getConnection().prepareStatement(orderSql)) {
            ps.setString(1, startStr);
            ps.setString(2, endStr);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    totalOrders++;
                    String raw = rs.getString("menu_items");
                    if (raw == null || raw.isBlank()) continue;
                    
                    // Parse menu items JSON: {"Drink Name": [qty, ...], ...}
                    // Extract drink names and their quantities
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
                            int comma = inside.indexOf(',');
                            String firstNum = (comma >= 0 ? inside.substring(0, comma) : inside).trim();
                            try {
                                qty = Integer.parseInt(firstNum);
                            } catch (NumberFormatException ignore) {
                            }
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
                        try (PreparedStatement drinkPs = db.getConnection().prepareStatement(drinkSql)) {
                            drinkPs.setString(1, drinkName);
                            try (ResultSet drinkRs = drinkPs.executeQuery()) {
                                if (drinkRs.next()) {
                                    String ingredientsStr = drinkRs.getString("ingredients");
                                    if (ingredientsStr != null && !ingredientsStr.isBlank()) {
                                        // Parse ingredients: {Water, Milk, Sugar, Tea}
                                        // Remove braces and split by comma
                                        ingredientsStr = ingredientsStr.replaceAll("[{}]", "").trim();
                                        String[] ingredients = ingredientsStr.split(",");
                                        
                                        for (String ingredient : ingredients) {
                                            ingredient = ingredient.trim();
                                            if (!ingredient.isEmpty()) {
                                                // Each drink uses the ingredient, multiply by drink quantity
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
        
        // Sort ingredients by usage (descending) then by name
        var sortedIngredients = ingredientUsage.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getValue(), a.getValue());
                    if (cmp != 0) return cmp;
                    return a.getKey().compareTo(b.getKey());
                })
                .collect(Collectors.toList());
        
        // Build the report
        StringBuilder sb = new StringBuilder();
        sb.append("Inventory Usage Report (").append(startStr).append(" to ").append(endStr).append(")\n");
        sb.append(String.format("Total Orders Analyzed: %d\n", totalOrders));
        sb.append("\nIngredient Usage (sorted by quantity used):\n");
        sb.append("-----------------------------------------------\n");
        
        if (sortedIngredients.isEmpty()) {
            sb.append("No ingredient usage data found for this period.\n");
        } else {
            for (var entry : sortedIngredients) {
                sb.append(String.format("%-30s : %4d units\n", entry.getKey(), entry.getValue()));
            }
        }
        
        sb.append("\n* Note: Usage is based on drinks sold. Each unit represents one drink containing that ingredient.\n");
        
        return sb.toString();
    }
}
