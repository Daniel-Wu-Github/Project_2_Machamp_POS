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
}
