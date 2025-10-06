--Special Query #1: "Weekly Sales History"

--pseudocode: select count of orders grouped by week
--about: given a specific week, how many orders were placed?
--example: "week 1 has 98765 orders"

-- Version 1: Filter by specific week (replace ? with desired week number 1-53)
SELECT 
    EXTRACT(WEEK FROM TO_DATE(SUBSTRING(datetime_col, 1, 8), 'MMDDYYYY')) AS order_week,
    COUNT(*) AS total_orders
FROM
    orders
WHERE
    EXTRACT(WEEK FROM TO_DATE(SUBSTRING(datetime_col, 1, 8), 'MMDDYYYY')) = 42
GROUP BY
    EXTRACT(WEEK FROM TO_DATE(SUBSTRING(datetime_col, 1, 8), 'MMDDYYYY'));

