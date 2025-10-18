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

    /**
     * Generate an "X-report" for the current startDate (assumed single day):
     * - For each hour (00..23) compute totals: sales (sum of total_price), count of orders
     *   and optionally separate counts/sums for order_type values like RETURN, VOID, DISCARD
     * - Also provide a breakdown by payment_method when that column exists
     *
     * This method is defensive: it detects whether the optional columns exist and falls
     * back to treating all rows as SALES if not present.
     */
    public String generateXReport(DatabaseManager db) throws SQLException {
        // We'll use the startDate (if range spans multiple days, limit to startDate only)
        String dayMmddyyyy = startDate.format(DateTimeFormatter.ofPattern("MMddyyyy"));

        // Detect optional columns in orderhistory table
        boolean hasOrderType = false;
        boolean hasPayment = false;
        try (Statement s = db.getConnection().createStatement(); ResultSet rs = s.executeQuery("PRAGMA table_info('orderhistory')")) {
            while (rs.next()) {
                String col = rs.getString("name");
                if ("order_type".equalsIgnoreCase(col)) hasOrderType = true;
                if ("payment_method".equalsIgnoreCase(col)) hasPayment = true;
            }
        }

        // Base query: select hour, total_price, order_type, payment_method when available
        StringBuilder sbSql = new StringBuilder("SELECT substr(order_datetime,9,2) AS hour");
        sbSql.append(", SUM(total_price) AS total_sales, COUNT(*) AS cnt");
        if (hasOrderType) sbSql.append(", order_type");
        if (hasPayment) sbSql.append(", payment_method");
        sbSql.append(" FROM orderhistory WHERE substr(order_datetime,1,8)=? GROUP BY hour");
        if (hasOrderType) sbSql.append(", order_type");
        if (hasPayment) sbSql.append(", payment_method");

        // We'll collect per-hour aggregates and also payment-method totals
        Map<Integer, Double> hourSales = new java.util.TreeMap<>();
        Map<Integer, Integer> hourCount = new java.util.TreeMap<>();
        Map<Integer, Map<String, Double>> hourByTypeSales = new java.util.HashMap<>();
        Map<Integer, Map<String, Integer>> hourByTypeCount = new java.util.HashMap<>();
        Map<String, Double> paymentTotals = new java.util.HashMap<>();

        try (PreparedStatement ps = db.getConnection().prepareStatement(sbSql.toString())) {
            ps.setString(1, dayMmddyyyy);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String hourStr = rs.getString("hour");
                    int h = 0;
                    try { h = Integer.parseInt(hourStr); } catch (Exception ex) { /* default 0 */ }
                    double sales = rs.getDouble("total_sales");
                    int cnt = rs.getInt("cnt");
                    hourSales.put(h, hourSales.getOrDefault(h, 0.0) + sales);
                    hourCount.put(h, hourCount.getOrDefault(h, 0) + cnt);
                    if (hasOrderType) {
                        String type = rs.getString("order_type");
                        if (type == null) type = "SALE";
                        hourByTypeSales.computeIfAbsent(h, k->new java.util.HashMap<>()).merge(type, sales, Double::sum);
                        hourByTypeCount.computeIfAbsent(h, k->new java.util.HashMap<>()).merge(type, cnt, Integer::sum);
                    }
                    if (hasPayment) {
                        String pm = rs.getString("payment_method");
                        if (pm == null) pm = "UNKNOWN";
                        paymentTotals.merge(pm, sales, Double::sum);
                    }
                }
            }
        }

        

        // Build output for hours 00..23
        StringBuilder out = new StringBuilder();
        out.append("X-Report for ").append(startDate.format(DateTimeFormatter.ISO_DATE)).append("\n");
        out.append(String.format("%3s | %10s | %8s", "HR", "Sales", "Orders")).append("\n");
        out.append("--------------------------------\n");
        double dayTotal = 0.0; int dayOrders = 0;
        for (int hour = 0; hour < 24; hour++) {
            double sVal = hourSales.getOrDefault(hour, 0.0);
            int cVal = hourCount.getOrDefault(hour, 0);
            dayTotal += sVal; dayOrders += cVal;
            out.append(String.format("%02d   | $%9.2f | %8d\n", hour, sVal, cVal));
            if (hasOrderType && hourByTypeSales.containsKey(hour)) {
                var map = hourByTypeSales.get(hour);
                for (var e : map.entrySet()) {
                    out.append(String.format("     - %s: $%.2f (%d)\n", e.getKey(), e.getValue(), hourByTypeCount.get(hour).getOrDefault(e.getKey(), 0)));
                }
            }
        }
        out.append("--------------------------------\n");
        out.append(String.format("DAILY TOTAL | $%.2f | %d orders\n", dayTotal, dayOrders));
        if (hasPayment) {
            out.append("\nPayment method breakdown:\n");
            for (var e : paymentTotals.entrySet()) {
                out.append(String.format(" - %s: $%.2f\n", e.getKey(), e.getValue()));
            }
        }
        return out.toString();
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
        // Footer note
        sb.append("\n* Note: Usage is based on drinks sold. Each unit represents one drink containing that ingredient.\n");
        
        return sb.toString();
    }
}
