import java.io.BufferedReader;
import java.io.FileReader;
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
 */
public class DayReports {

	// Store hours configuration: open inclusive, close exclusive (e.g., 9 -> 21 means 09:00 through 20:59)
	private static final int STORE_OPEN_HOUR = 9;  // 09:00
	private static final int STORE_CLOSE_HOUR = 21; // 21:00 (exclusive)

	public static class XReportResult {
		public final LocalDate date;
		public final Map<Integer, HourBucket> hourly; // hour -> metrics
		public final double totalSales;
		public final int txnCount;

		XReportResult(LocalDate date, Map<Integer, HourBucket> hourly, double totalSales, int txnCount) {
			this.date = date;
			this.hourly = hourly;
			this.totalSales = totalSales;
			this.txnCount = txnCount;
		}

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

	public static class HourBucket {
		double sales = 0.0;
		int transactions = 0;
	}

	private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("MMddyyyyHHmm");

	/**
	 * Generate X report for the given date using the orders.csv file.
	 * @param baseDir a directory to search for the CSV (tries typical locations if null or empty).
	 * @param date LocalDate to report on (use LocalDate.now() for today)
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

	// Attempt to locate orders.csv across likely project paths.
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

	private LocalDateTime parseDateTime(String s) {
		try {
			return LocalDateTime.parse(s, DATE_TIME_FMT);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private double parseAmount(String s) {
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	private static String money(double v) {
		return String.format(Locale.US, "$%.2f", v);
	}

	private static String repeat(char ch, int count) {
		StringBuilder sb = new StringBuilder(count);
		for (int i = 0; i < count; i++) sb.append(ch);
		return sb.toString();
	}

	// Compute percentile (0.0 - 1.0) for a list of values. Simple nearest-rank approach.
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

	// Minimal CSV parser that respects quotes and commas inside quotes
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
