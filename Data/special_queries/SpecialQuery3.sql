-- SpecialQuery3.sql
-- Top 10 days by total sales (sum of order totals), descending

-- Replace table/column names if different: orders, order_timestamp, total_price

SELECT
  order_timestamp::date AS order_date,
  SUM(total_price) AS total_sales
FROM orders
GROUP BY order_date
ORDER BY total_sales DESC
LIMIT 10;
