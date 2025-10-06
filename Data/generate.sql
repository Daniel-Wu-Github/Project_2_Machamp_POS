CREATE TABLE orders (
    datetime_col VARCHAR(12) NOT NULL,        -- Format: MMDDYYYYHHMM (e.g., 092620240901)
    order_id VARCHAR(20) NOT NULL PRIMARY KEY, -- Format: MMDDYYYY_N (e.g., 09262024_1)
    customer_id BIGINT NOT NULL,              -- Large integer for customer ID (e.g., 7002313919)
    menu_items TEXT NOT NULL,                 -- JSON-like string containing menu items and quantities
    total_price DECIMAL(10,2) NOT NULL        -- Price with 2 decimal places (e.g., 10.75)
);

\copy orders FROM 'Data/orders.csv' DELIMITER ',' CSV HEADER;


-- -- Create index on customer_id for faster queries
-- CREATE INDEX idx_orders_customer_id ON orders(customer_id);

-- -- Create index on datetime for time-based queries
-- CREATE INDEX idx_orders_datetime ON orders(datetime_col);