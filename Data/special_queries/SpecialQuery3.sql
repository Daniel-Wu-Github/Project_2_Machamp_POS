-- Special Query #3: "Peak Sales Day"

-- pseudocode: select top 10 sums of order total grouped by day in descending order by order total
-- about: given a specific day, what was the sum of the top 10 order totals?
-- example: "30 August has $12345 of top sales"

-- Version 1: Filter by specific month and day (replace ? with month (1-12) and day (1-31))
SELECT 
    TO_DATE(SUBSTRING(datetime_col, 1, 8), 'MMDDYYYY') AS order_date,
    SUM(total_price) AS top_10_sales
FROM
    orders
WHERE
    EXTRACT(MONTH FROM TO_DATE(SUBSTRING(datetime_col, 1, 8), 'MMDDYYYY')) = 1 AND
    EXTRACT(DAY FROM TO_DATE(SUBSTRING(datetime_col, 1, 8), 'MMDDYYYY')) = 7
GROUP BY
    TO_DATE(SUBSTRING(datetime_col, 1, 8), 'MMDDYYYY')
ORDER BY
    top_10_sales DESC;

