-- Special Query #2: "Realistic Sales History"

-- pseudocode: select count of orders, sum of order total grouped by hour
-- about: given a specific hour of the day, how many orders were placed and what was the total sum of the orders?
-- example: e.g., "12pm has 12345 orders totaling $86753"

-- Version 1: Filter by specific hour (replace ? with desired hour 0-23)
SELECT 
    CAST(SUBSTRING(datetime_col, 9, 2) AS INTEGER) AS order_hour,
    COUNT(*) AS total_orders,
    SUM(total_price) AS total_sales
FROM
    orders
WHERE
    CAST(SUBSTRING(datetime_col, 9, 2) AS INTEGER) = 10
GROUP BY
    CAST(SUBSTRING(datetime_col, 9, 2) AS INTEGER);





