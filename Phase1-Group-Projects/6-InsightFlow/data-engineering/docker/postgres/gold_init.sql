-- -- =============================================================
-- -- GOLD LAYER INIT — Star Schema
-- -- Dimensional model for analytics
-- -- Source: postgres-silver
-- -- =============================================================


-- -- =============================================================
-- -- DIMENSION TABLES
-- -- =============================================================


-- -- -------------------------------------------------------------
-- -- DIM: Product
-- -- Source: silver_product_catalogue + silver_sku_mappings
-- -- -------------------------------------------------------------
-- CREATE TABLE IF NOT EXISTS dim_product (
--     product_sk          SERIAL          PRIMARY KEY,     -- surrogate key
--     product_id          BIGINT          NOT NULL,        -- natural key
--     sku                 VARCHAR(20)     NOT NULL UNIQUE,
--     product_name        VARCHAR(200)    NOT NULL,
--     category            VARCHAR(100)    NOT NULL,
--     brand               VARCHAR(100),
--     unit_of_measure     VARCHAR(20),
--     pos_sku             VARCHAR(20),                     -- from sku_mappings
--     ecom_sku            VARCHAR(20),                     -- from sku_mappings
--     is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
--     -- Gold metadata
--     _silver_source      VARCHAR(50)     DEFAULT 'silver_product_catalogue',
--     _gold_loaded_at     TIMESTAMP       DEFAULT NOW()
-- );


-- -- -------------------------------------------------------------
-- -- DIM: Store
-- -- Source: silver_store_reference
-- -- -------------------------------------------------------------
-- CREATE TABLE IF NOT EXISTS dim_store (
--     store_sk            SERIAL          PRIMARY KEY,     -- surrogate key
--     store_id            VARCHAR(10)     NOT NULL UNIQUE, -- natural key
--     store_name          VARCHAR(100)    NOT NULL,
--     city                VARCHAR(50)     NOT NULL,
--     region              VARCHAR(50)     NOT NULL,
--     -- Gold metadata
--     _silver_source      VARCHAR(50)     DEFAULT 'silver_store_reference',
--     _gold_loaded_at     TIMESTAMP       DEFAULT NOW()
-- );


-- -- -------------------------------------------------------------
-- -- DIM: Warehouse
-- -- Source: silver_warehouses
-- -- -------------------------------------------------------------
-- CREATE TABLE IF NOT EXISTS dim_warehouse (
--     warehouse_sk        SERIAL          PRIMARY KEY,     -- surrogate key
--     warehouse_id        VARCHAR(20)     NOT NULL UNIQUE, -- natural key
--     city                VARCHAR(50)     NOT NULL,
--     region              VARCHAR(50)     NOT NULL,
--     address             TEXT,
--     -- Gold metadata
--     _silver_source      VARCHAR(50)     DEFAULT 'silver_warehouses',
--     _gold_loaded_at     TIMESTAMP       DEFAULT NOW()
-- );


-- -- -------------------------------------------------------------
-- -- DIM: Customer
-- -- Source: silver_orders (customer_id extracted — no customer table in source)
-- -- -------------------------------------------------------------
-- CREATE TABLE IF NOT EXISTS dim_customer (
--     customer_sk         SERIAL          PRIMARY KEY,     -- surrogate key
--     customer_id         VARCHAR(20)     NOT NULL UNIQUE, -- natural key
--     delivery_region     VARCHAR(50),                     -- last known region
--     delivery_city       VARCHAR(50),                     -- last known city
--     preferred_payment   VARCHAR(20),                     -- most used payment method
--     total_orders        INT             DEFAULT 0,       -- derived
--     -- Gold metadata
--     _silver_source      VARCHAR(50)     DEFAULT 'silver_orders',
--     _gold_loaded_at     TIMESTAMP       DEFAULT NOW()
-- );


-- -- -------------------------------------------------------------
-- -- DIM: Date
-- -- Standalone — populated by Airflow date spine task
-- -- Used by all fact tables
-- -- -------------------------------------------------------------
-- CREATE TABLE IF NOT EXISTS dim_date (
--     date_sk             INT             PRIMARY KEY,     -- YYYYMMDD e.g. 20240101
--     full_date           DATE            NOT NULL UNIQUE,
--     day_of_week         VARCHAR(10)     NOT NULL,        -- Monday | Tuesday etc.
--     day_of_month        INT             NOT NULL,
--     month               INT             NOT NULL,
--     month_name          VARCHAR(10)     NOT NULL,        -- January | February etc.
--     quarter             INT             NOT NULL,        -- 1 | 2 | 3 | 4
--     year                INT             NOT NULL,
--     is_weekend          BOOLEAN         NOT NULL DEFAULT FALSE,
--     -- Gold metadata
--     _gold_loaded_at     TIMESTAMP       DEFAULT NOW()
-- );


-- -- -------------------------------------------------------------
-- -- DIM: Payment Method
-- -- Conformed dimension shared across POS and Ecom fact tables
-- -- -------------------------------------------------------------
-- CREATE TABLE IF NOT EXISTS dim_payment_method (
--     payment_sk          SERIAL          PRIMARY KEY,
--     payment_method      VARCHAR(20)     NOT NULL UNIQUE, -- Cash | Momo | Card | COD
--     payment_category    VARCHAR(20),                     -- Digital | Physical
--     -- Gold metadata
--     _gold_loaded_at     TIMESTAMP       DEFAULT NOW()
-- );


-- -- -------------------------------------------------------------
-- -- DIM: Data Source  (for lineage)
-- -- Source: silver_data_sources
-- -- -------------------------------------------------------------
-- CREATE TABLE IF NOT EXISTS dim_data_source (
--     source_sk           SERIAL          PRIMARY KEY,
--     source_id           BIGINT          NOT NULL UNIQUE,
--     source_name         VARCHAR(255)    NOT NULL,
--     source_type         VARCHAR(50),
--     is_active           BOOLEAN,
--     last_ingestion      TIMESTAMP,
--     -- Gold metadata
--     _silver_source      VARCHAR(50)     DEFAULT 'silver_data_sources',
--     _gold_loaded_at     TIMESTAMP       DEFAULT NOW()
-- );


-- -- =============================================================
-- -- FACT TABLES
-- -- =============================================================


-- -- -------------------------------------------------------------
-- -- FACT: POS Sales
-- -- Grain: one row per transaction line item (transaction_id + sku)
-- -- Source: silver_pos_transactions
-- -- -------------------------------------------------------------
-- CREATE TABLE IF NOT EXISTS fact_pos_sales (
--     pos_sale_sk         SERIAL          PRIMARY KEY,
--     -- Foreign keys to dimensions
--     product_sk          INT             REFERENCES dim_product(product_sk),
--     store_sk            INT             REFERENCES dim_store(store_sk),
--     payment_sk          INT             REFERENCES dim_payment_method(payment_sk),
--     date_sk             INT             REFERENCES dim_date(date_sk),
--     -- Degenerate dimensions (no dim table needed)
--     transaction_id      VARCHAR(20)     NOT NULL,
--     cashier_id          VARCHAR(10)     NOT NULL,
--     -- Measures
--     quantity            INT             NOT NULL,
--     unit_price_ghs      DECIMAL(10,2)   NOT NULL,
--     line_total_ghs      DECIMAL(10,2)   NOT NULL,
--     -- Gold metadata
--     _silver_source      VARCHAR(50)     DEFAULT 'silver_pos_transactions',
--     _gold_loaded_at     TIMESTAMP       DEFAULT NOW()
-- );


-- -- -------------------------------------------------------------
-- -- FACT: Ecom Sales
-- -- Grain: one row per order line item (order_item_id)
-- -- Source: silver_orders + silver_order_items
-- -- -------------------------------------------------------------
-- CREATE TABLE IF NOT EXISTS fact_ecom_sales (
--     ecom_sale_sk        SERIAL          PRIMARY KEY,
--     -- Foreign keys to dimensions
--     customer_sk         INT             REFERENCES dim_customer(customer_sk),
--     product_sk          INT             REFERENCES dim_product(product_sk),
--     payment_sk          INT             REFERENCES dim_payment_method(payment_sk),
--     date_sk             INT             REFERENCES dim_date(date_sk),
--     -- Degenerate dimensions
--     order_id            VARCHAR(20)     NOT NULL,
--     order_item_id       VARCHAR(30)     NOT NULL,
--     order_status        VARCHAR(20)     NOT NULL,        -- PENDING | FULFILLED | CANCELLED | RETURNED
--     delivery_region     VARCHAR(50)     NOT NULL,
--     delivery_city       VARCHAR(50)     NOT NULL,
--     -- Measures
--     quantity            INT             NOT NULL,
--     unit_price_ghs      DECIMAL(10,2)   NOT NULL,
--     line_total_ghs      DECIMAL(10,2)   NOT NULL,
--     total_order_value_ghs DECIMAL(10,2) NOT NULL,        -- from orders table
--     -- Gold metadata
--     _silver_source      VARCHAR(50)     DEFAULT 'silver_order_items',
--     _gold_loaded_at     TIMESTAMP       DEFAULT NOW()
-- );


-- -- -------------------------------------------------------------
-- -- FACT: Inventory
-- -- Grain: one row per warehouse + sku snapshot
-- -- Source: silver_inventory_stock
-- -- -------------------------------------------------------------
-- CREATE TABLE IF NOT EXISTS fact_inventory (
--     inventory_sk        SERIAL          PRIMARY KEY,
--     -- Foreign keys to dimensions
--     warehouse_sk        INT             REFERENCES dim_warehouse(warehouse_sk),
--     product_sk          INT             REFERENCES dim_product(product_sk),
--     date_sk             INT             REFERENCES dim_date(date_sk),
--     -- Measures
--     stock_available     INT             NOT NULL,
--     reorder_threshold   INT             NOT NULL,
--     stock_level_status  VARCHAR(20),                     -- OK | LOW | CRITICAL
--     last_restock_date   DATE,
--     last_extracted_at   TIMESTAMP       NOT NULL,
--     -- Gold metadata
--     _silver_source      VARCHAR(50)     DEFAULT 'silver_inventory_stock',
--     _gold_loaded_at     TIMESTAMP       DEFAULT NOW()
-- );


-- -- -------------------------------------------------------------
-- -- FACT: Feedback
-- -- Grain: one row per feedback submission
-- -- Source: silver_feedback_submissions
-- -- -------------------------------------------------------------
-- CREATE TABLE IF NOT EXISTS fact_feedback (
--     feedback_sk         SERIAL          PRIMARY KEY,
--     -- Foreign keys to dimensions
--     customer_sk         INT             REFERENCES dim_customer(customer_sk),
--     product_sk          INT             REFERENCES dim_product(product_sk),
--     date_sk             INT             REFERENCES dim_date(date_sk),
--     -- Degenerate dimensions
--     feedback_id         VARCHAR(20)     NOT NULL,
--     order_id            VARCHAR(20)     NOT NULL,
--     delivery_region     VARCHAR(50)     NOT NULL,
--     category            VARCHAR(50)     NOT NULL,        -- Delivery Speed | Product Quality etc.
--     -- Measures
--     rating              SMALLINT        NOT NULL,        -- 1 to 5
--     comment             TEXT,
--     -- Gold metadata
--     _silver_source      VARCHAR(50)     DEFAULT 'silver_feedback_submissions',
--     _gold_loaded_at     TIMESTAMP       DEFAULT NOW()
-- );


-- -- =============================================================
-- -- SEED: Payment Method dimension
-- -- Static values known upfront — no ETL needed
-- -- =============================================================
-- INSERT INTO dim_payment_method (payment_method, payment_category) VALUES
--     ('Cash',    'Physical'),
--     ('Momo',    'Digital'),
--     ('Card',    'Digital'),
--     ('COD',     'Physical')
-- ON CONFLICT (payment_method) DO NOTHING;