-- Query 1: Total Revenue
-- Purpose: Get the total revenue from all orders
SELECT SUM(total_price) AS total_revenue
FROM orders;

-- Query 2: Average Order Value
-- Purpose: Calculate the average amount spent per order
SELECT AVG(total_price) AS average_order_value
FROM orders;

-- Query 3: Order Count by Customer
-- Purpose: Find how many orders each customer has made
SELECT customer_id, COUNT(*) AS order_count
FROM orders
GROUP BY customer_id
ORDER BY order_count DESC;

-- Query 4: Most Expensive Order
-- Purpose: Find the highest single order value
SELECT order_id, customer_id, total_price, datetime_col
FROM orders
WHERE total_price = (SELECT MAX(total_price) FROM orders);

-- Query 5: Orders Above Specific Amount
-- Purpose: Find all orders above a certain dollar amount (replace ? with amount)
SELECT order_id, customer_id, total_price, datetime_col
FROM orders
WHERE total_price > 10
ORDER BY total_price DESC;

-- Query 6: Customer Order History
-- Purpose: Get all orders for a specific customer (replace ? with customer_id)
SELECT order_id, datetime_col, total_price, menu_items
FROM orders
WHERE customer_id = 7002313919
ORDER BY datetime_col DESC;

-- Query 7: Daily Order Count
-- Purpose: Count how many orders were placed each day
SELECT 
    TO_DATE(SUBSTRING(datetime_col, 1, 8), 'MMDDYYYY') AS order_date,
    COUNT(*) AS orders_per_day
FROM orders
GROUP BY TO_DATE(SUBSTRING(datetime_col, 1, 8), 'MMDDYYYY')
ORDER BY order_date;

-- Query 8: Orders Within Date Range
-- Purpose: Find orders between two specific dates (replace ? with start and end dates)
SELECT order_id, customer_id, total_price, datetime_col
FROM orders
WHERE TO_DATE(SUBSTRING(datetime_col, 1, 8), 'MMDDYYYY') 
    BETWEEN TO_DATE('10/11/2024', 'MM/DD/YYYY') AND TO_DATE('12/11/2024', 'MM/DD/YYYY')
ORDER BY datetime_col;

-- Query 9: Top Spending Customers
-- Purpose: Find the customers who have spent the most money (top 10)
SELECT 
    customer_id,
    COUNT(*) AS total_orders,
    SUM(total_price) AS total_spent,
    AVG(total_price) AS avg_order_value
FROM orders
GROUP BY customer_id
ORDER BY total_spent DESC
LIMIT 10;
