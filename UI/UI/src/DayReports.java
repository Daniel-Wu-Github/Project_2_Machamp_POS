import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * DayReports provides utilities to generate operational reports.
 * X-Report: Sales activities per hour for the current day of operation.
 *
 * Input data source: UI/src/orders.csv (CSV headers match sample provided)
 * Columns: DateTime,Order ID,Customer ID,Menu Items,Total Price
 * - DateTime format: MMddyyyyHHmm (e.g., 092620240901)
 * - Total Price: numeric
 *
 * NOTE: CSV currently only includes sales; returns/voids/discards/payment method
 * are not tracked. This X Report focuses on hourly sales, transaction counts,
 * average sale per transaction, and marks rush hours.
 * 
 * @author Ayad Masud
 */
public class DayReports {

	// Store hours configuration: open inclusive, close exclusive (e.g., 9 -> 21 means 09:00 through 20:59)
	private static final int STORE_OPEN_HOUR = 9;  // 09:00
	private static final int STORE_CLOSE_HOUR = 21; // 21:00 (exclusive)

	/**
	 * Encapsulates the results of an X Report generation.
	 * Contains the date, hourly breakdown of sales metrics, and totals.
	 * 
	 * @author Daniel Wu
	 */
	public static class XReportResult {
		public final LocalDate date;
		public final Map<Integer, HourBucket> hourly; // hour -> metrics
		public final double totalSales;
		public final int txnCount;

		/**
		 * Constructs an XReportResult with the specified parameters.
		 * 
		 * @param date the date of the report
		 * @param hourly map of hour (0-23) to HourBucket containing sales metrics
		 * @param totalSales the total sales amount for the day
		 * @param txnCount the total number of transactions for the day
		 */
		XReportResult(LocalDate date, Map<Integer, HourBucket> hourly, double totalSales, int txnCount) {
			this.date = date;
			this.hourly = hourly;
			this.totalSales = totalSales;
			this.txnCount = txnCount;
		}

		/**
		 * Generates a formatted string representation of the X Report.
		 * Includes summary statistics and hourly breakdown with rush hour indicators.
		 * 
		 * @return formatted X Report as a string
		 */
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			// Compute within store hours only
			List<Double> hourlySalesOpen = new ArrayList<>();
			int openHours = Math.max(0, STORE_CLOSE_HOUR - STORE_OPEN_HOUR);
			for (int h = STORE_OPEN_HOUR; h < STORE_CLOSE_HOUR; h++) {
				HourBucket b = hourly.get(h);
				hourlySalesOpen.add(b != null ? b.sales : 0.0);
			}
			double rushThreshold = computePercentile(hourlySalesOpen, 0.75);

			// Peak hour in store hours
			int peakHour = STORE_OPEN_HOUR;
			double peakSales = -1;
			for (int h = STORE_OPEN_HOUR; h < STORE_CLOSE_HOUR; h++) {
				HourBucket b = hourly.get(h);
				double s = (b != null ? b.sales : 0.0);
				if (s > peakSales) { peakSales = s; peakHour = h; }
			}

			double overallAvgOrder = txnCount > 0 ? (totalSales / txnCount) : 0.0;
			double avgPerHour = openHours > 0 ? (totalSales / openHours) : 0.0;

			// Header and totals first
			sb.append("X Report for ").append(date).append('\n');
			sb.append(String.format(Locale.US,
				"TOTAL, $%.2f, %d Orders, Avg/Order: $%.2f, Avg/Hour: $%.2f, Peak: %02d:00 ($%.2f)\n\n",
				totalSales, txnCount, overallAvgOrder, avgPerHour, peakHour, peakSales));

			// Table header with fixed-width columns using monospaced formatting
			String header = String.format("%-13s | %12s | %8s | %10s | %4s", "Hour", "Sales", "Orders", "Avg Sale", "Rush");
			String sep = repeat('-', header.length());
			sb.append(header).append('\n').append(sep).append('\n');

			for (int h = STORE_OPEN_HOUR; h < STORE_CLOSE_HOUR; h++) {
				HourBucket b = hourly.get(h);
				if (b == null) b = new HourBucket();
				double avg = b.transactions > 0 ? (b.sales / b.transactions) : 0.0;
				boolean rush = b.sales > 0 && b.sales >= rushThreshold && b.transactions > 0;
				String hourLabel = String.format("%02d:00 - %02d:59", h, h);
				String row = String.format(Locale.US, "%-13s | %12s | %8d | %10s | %4s",
						hourLabel,
						money(b.sales),
						b.transactions,
						money(avg),
						rush ? "Yes" : "No");
				sb.append(row).append('\n');
			}

			return sb.toString();
		}
	}

	/**
	 * Holds sales metrics for a single hour.
	 * Tracks total sales amount and number of transactions.
	 * 
	 * @author Sarang Cheler
	 */
	public static class HourBucket {
		double sales = 0.0;
		int transactions = 0;
	}

	private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("MMddyyyyHHmm");

	/**
	 * Generates an X report for the given date using the orders.csv file.
	 * The report includes hourly sales breakdown, transaction counts, and rush hour identification.
	 * 
	 * @param baseDir a directory to search for the CSV (tries typical locations if null or empty)
	 * @param date LocalDate to report on (use LocalDate.now() for today)
	 * @return XReportResult containing the complete report data
	 * @throws IOException if orders.csv cannot be found or read
	 */
	public XReportResult generateXReport(String baseDir, LocalDate date) throws IOException {
		Path csvPath = resolveOrdersCsv(baseDir);
		if (csvPath == null || !Files.exists(csvPath)) {
			throw new IOException("orders.csv not found");
		}

		// Initialize 0-23 hour buckets
		Map<Integer, HourBucket> buckets = new TreeMap<>();
		for (int h = 0; h < 24; h++) buckets.put(h, new HourBucket());

		int totalTxns = 0;
		double totalSales = 0.0;

		try (BufferedReader reader = new BufferedReader(new FileReader(csvPath.toFile())))
		{
			// Read header
			String line = reader.readLine();
			if (line == null) return new XReportResult(date, buckets, 0.0, 0);

			while ((line = reader.readLine()) != null) {
				List<String> cols = parseCsvLine(line);
				if (cols.size() < 5) continue;

				String dtStr = cols.get(0).trim();
				String totalStr = cols.get(4).trim();

				LocalDateTime ts = parseDateTime(dtStr);
				if (ts == null) continue;
				if (!ts.toLocalDate().equals(date)) continue; // only today's rows

				double amount = parseAmount(totalStr);

				int hour = ts.getHour();
				HourBucket b = buckets.get(hour);
				b.sales += amount;
				b.transactions += 1;

				totalSales += amount;
				totalTxns += 1;

				// If you later add columns indicating returns/voids/discards/payment method,
				// update bucket counters accordingly here.
			}
		}

		return new XReportResult(date, buckets, totalSales, totalTxns);
	}
    
	/**
	 * Generates a Z report (end of day report) for the given date.
	 * This is based on the X report but marks the end of the business day.
	 * The report is saved to the reports/ directory.
	 * 
	 * @param baseDir a directory to search for the CSV (tries typical locations if null or empty)
	 * @param date LocalDate to report on
	 * @return formatted Z Report string with file save confirmation
	 * @throws IOException if orders.csv cannot be found or read, or if report file cannot be written
	 */
    public String generateZReport(String baseDir, LocalDate date) throws IOException {
        XReportResult result = generateXReport(baseDir, date);
        String zText = result.toString().replace("X Report", "Z Report (End of Day)");

        // Write Z report file under /reports/
        Path reportsDir = Paths.get("reports").toAbsolutePath();
        if (!Files.exists(reportsDir)) {
            Files.createDirectories(reportsDir);
        }

        Path zFile = reportsDir.resolve(String.format("ZReport_%s.txt", date));
        try (FileWriter writer = new FileWriter(zFile.toFile())) {
            writer.write(zText);
        }

        // Optionally record last Z report date to prevent re-run (placeholder)
        Path flagFile = reportsDir.resolve("last_z_report_date.txt");
        Files.writeString(flagFile, date.toString());

        return zText + "\n\n[Z Report saved to: " + zFile.toAbsolutePath() + "]";
    }

	/**
	 * Attempts to locate orders.csv across likely project paths.
	 * Searches in the provided baseDir and several typical relative locations.
	 * 
	 * @param baseDir optional directory hint to search first
	 * @return Path to orders.csv if found, null otherwise
	 */
	private Path resolveOrdersCsv(String baseDir) {
		List<String> candidates = new ArrayList<>();
		if (baseDir != null && !baseDir.isEmpty()) {
			candidates.add(Paths.get(baseDir, "orders.csv").toString());
		}
		// Typical locations relative to this class file
		candidates.add("src/orders.csv");
		candidates.add("UI/src/orders.csv");
		candidates.add("UI/UI/src/orders.csv");
		candidates.add("../src/orders.csv");
		candidates.add("../../src/orders.csv");

		for (String c : candidates) {
			Path p = Paths.get(c).toAbsolutePath().normalize();
			if (Files.exists(p)) return p;
		}
		return null;
	}

	/**
	 * Parses a date-time string in MMddyyyyHHmm format.
	 * 
	 * @param s the date-time string to parse
	 * @return LocalDateTime object if parsing succeeds, null otherwise
	 */
	private LocalDateTime parseDateTime(String s) {
		try {
			return LocalDateTime.parse(s, DATE_TIME_FMT);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	/**
	 * Parses a numeric amount string to a double value.
	 * 
	 * @param s the amount string to parse
	 * @return the parsed amount, or 0.0 if parsing fails
	 */
	private double parseAmount(String s) {
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	/**
	 * Formats a double value as a currency string.
	 * 
	 * @param v the value to format
	 * @return formatted currency string (e.g., "$12.34")
	 */
	private static String money(double v) {
		return String.format(Locale.US, "$%.2f", v);
	}

	/**
	 * Creates a string consisting of a character repeated a specified number of times.
	 * 
	 * @param ch the character to repeat
	 * @param count the number of times to repeat the character
	 * @return a string with the character repeated count times
	 */
	private static String repeat(char ch, int count) {
		StringBuilder sb = new StringBuilder(count);
		for (int i = 0; i < count; i++) sb.append(ch);
		return sb.toString();
	}

	/**
	 * Computes the percentile value for a list of values using a simple nearest-rank approach.
	 * Uses linear interpolation between values when the rank falls between indices.
	 * 
	 * @param values the list of values to compute percentile from
	 * @param percentile the percentile to compute (0.0 to 1.0, where 0.75 is 75th percentile)
	 * @return the computed percentile value, or 0.0 if values is null or empty
	 */
	private static double computePercentile(List<Double> values, double percentile) {
		if (values == null || values.isEmpty()) return 0.0;
		List<Double> copy = new ArrayList<>(values);
		copy.sort(Double::compareTo);
		if (percentile <= 0) return copy.get(0);
		if (percentile >= 1) return copy.get(copy.size()-1);
		double rank = percentile * (copy.size() - 1);
		int low = (int)Math.floor(rank);
		int high = (int)Math.ceil(rank);
		if (low == high) return copy.get(low);
		double weight = rank - low;
		return copy.get(low) * (1 - weight) + copy.get(high) * weight;
	}

	/**
	 * Parses a CSV line respecting quotes and commas inside quoted fields.
	 * Handles quoted fields that may contain commas as part of the data.
	 * 
	 * @param line the CSV line to parse
	 * @return list of field values extracted from the CSV line
	 */
	private List<String> parseCsvLine(String line) {
		List<String> result = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		boolean inQuotes = false;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '"') {
				inQuotes = !inQuotes;
				continue;
			}
			if (c == ',' && !inQuotes) {
				result.add(sb.toString());
				sb.setLength(0);
			} else {
				sb.append(c);
			}
		}
		result.add(sb.toString());
		return result;
	}
}
