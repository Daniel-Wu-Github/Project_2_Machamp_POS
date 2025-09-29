
SELECT 
    EXTRACT(HOUR FROM order_time) as hour_of_day,
    CASE 
        WHEN EXTRACT(HOUR FROM order_time) = 0 THEN '12am'
        WHEN EXTRACT(HOUR FROM order_time) < 12 THEN CONCAT(EXTRACT(HOUR FROM order_time), 'am')
        WHEN EXTRACT(HOUR FROM order_time) = 12 THEN '12pm'
        ELSE CONCAT(EXTRACT(HOUR FROM order_time) - 12, 'pm')
    END as formatted_hour,
    COUNT(*) as order_count,
    SUM(total_price) as total_sales,
    ROUND(AVG(total_price), 2) as average_order_value
FROM orders
GROUP BY EXTRACT(HOUR FROM order_time)
ORDER BY EXTRACT(HOUR FROM order_time);
