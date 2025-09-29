--Special Query #1: "Weekly Sales History"

--pseudocode: select count of orders grouped by week
--about: given a specific week, how many orders were placed?
--example: "week 1 has 98765 orders"

SELECT 
    EXTRACT(WEEK FROM order_date) AS order_week,
    COUNT(order_id) AS total_orders
FROM
    orders
GROUP BY
    EXTRACT(WEEK FROM order_date)
ORDER BY
    order_week;