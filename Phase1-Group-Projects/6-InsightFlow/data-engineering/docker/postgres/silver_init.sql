-- SILVER LAYER Schema
-- Cleaned, standardized, typed correctly
-- Source: insightflow-db (Bronze Layer)

-- SILVER layer for Product Catalogue
CREATE TABLE IF NOT EXISTS silver_product_catalogue (
    product_id          BIGINT          NOT NULL,
    sku                 VARCHAR(20)     NOT NULL UNIQUE,
    product_name        VARCHAR(200)    NOT NULL,
    category            VARCHAR(100)    NOT NULL,
    brand               VARCHAR(100),
    unit_of_measure     VARCHAR(20),
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    -- metadata
    _source_table       VARCHAR(50)     DEFAULT 'product_catalogue',
    _ingested_at        TIMESTAMP,
    _silver_loaded_at   TIMESTAMP       DEFAULT NOW(),
    PRIMARY KEY (sku)
);


-- SILVER layer for SKU Mappings
CREATE TABLE IF NOT EXISTS silver_sku_mappings (
    mapping_id          BIGINT          NOT NULL,
    master_sku          VARCHAR(20)     NOT NULL,
    pos_sku             VARCHAR(20),
    ecom_sku            VARCHAR(20),
    notes               TEXT,
    -- metadata
    _source_table       VARCHAR(50)     DEFAULT 'sku_mappings',
    _silver_loaded_at   TIMESTAMP       DEFAULT NOW(),
    PRIMARY KEY (mapping_id)
);


-- SILVER layer for Store Reference
CREATE TABLE IF NOT EXISTS silver_store_reference (
    store_id            VARCHAR(10)     NOT NULL,
    store_name          VARCHAR(100)    NOT NULL,
    city                VARCHAR(50)     NOT NULL,
    region              VARCHAR(50)     NOT NULL,
    -- metadata
    _source_table       VARCHAR(50)     DEFAULT 'store_reference',
    _silver_loaded_at   TIMESTAMP       DEFAULT NOW(),
    PRIMARY KEY (store_id)
);

-- SILVER layer for Warehouses
CREATE TABLE IF NOT EXISTS silver_warehouses (
    warehouse_id        VARCHAR(20)     NOT NULL,
    region              VARCHAR(50)     NOT NULL,
    city                VARCHAR(50)     NOT NULL,
    address             TEXT,
    -- metadata
    _source_table       VARCHAR(50)     DEFAULT 'warehouses',
    _silver_loaded_at   TIMESTAMP       DEFAULT NOW(),
    PRIMARY KEY (warehouse_id)
);


-- SILVER layer for POS Transactions
-- Transformations applied:
--   - order_timestamp_utc converted to GMT+0 (Ghana time)
--   - payment_method standardized to title case
--   - line_total_ghs validated against quantity * unit_price_ghs
CREATE TABLE IF NOT EXISTS silver_pos_transactions (
    transaction_id      VARCHAR(20)     NOT NULL,
    sku                 VARCHAR(20)     NOT NULL,
    store_id            VARCHAR(10)     NOT NULL,
    quantity            INT             NOT NULL,
    unit_price_ghs      DECIMAL(10,2)   NOT NULL,
    line_total_ghs      DECIMAL(10,2)   NOT NULL,
    line_total_check    BOOLEAN,                        -- True if quantity * unit_price = line_total
    payment_method      VARCHAR(20)     NOT NULL,       -- Standardized: Cash | Momo | Card
    cashier_id          VARCHAR(10)     NOT NULL,
    timestamp_local     TIMESTAMP       NOT NULL,       -- Already GMT+0
    ingested_at         TIMESTAMP,
    -- metadata
    _source_table       VARCHAR(50)     DEFAULT 'pos_transactions',
    _silver_loaded_at   TIMESTAMP       DEFAULT NOW(),
    PRIMARY KEY (transaction_id, sku)
);


-- SILVER layer for Inventory Stock
-- Transformations applied:
--   - last_restock_date cast to DATE
--   - stock_level_status derived: OK | LOW | CRITICAL
CREATE TABLE IF NOT EXISTS silver_inventory_stock (
    warehouse_id            VARCHAR(20)     NOT NULL,
    sku                     VARCHAR(20)     NOT NULL,
    stock_available         INT             NOT NULL,
    reorder_threshold       INT             NOT NULL,
    stock_level_status      VARCHAR(20),                -- Derived: OK | LOW | CRITICAL
    last_restock_date       DATE,
    last_extracted_at       TIMESTAMP       NOT NULL,
    -- metadata
    _source_table           VARCHAR(50)     DEFAULT 'inventory_stock',
    _silver_loaded_at       TIMESTAMP       DEFAULT NOW(),
    PRIMARY KEY (warehouse_id, sku)
);


-- SILVER layer for Orders
-- Transformations applied:
--   - order_timestamp_utc converted to GMT+0
--   - delivery_region and delivery_city stripped and title cased
--   - payment_method standardized
CREATE TABLE IF NOT EXISTS silver_orders (
    order_id                VARCHAR(20)     NOT NULL,
    customer_id             VARCHAR(20)     NOT NULL,
    order_status            VARCHAR(20)     NOT NULL,   
    delivery_region         VARCHAR(50)     NOT NULL,
    delivery_city           VARCHAR(50)     NOT NULL,
    delivery_address        TEXT,
    payment_method          VARCHAR(20)     NOT NULL,  
    total_order_value_ghs   DECIMAL(10,2)   NOT NULL,
    order_timestamp_utc     TIMESTAMP       NOT NULL,
    order_timestamp_gmt     TIMESTAMP       NOT NULL,   
    ingested_at             TIMESTAMP,
    -- Silver metadata
    _source_table           VARCHAR(50)     DEFAULT 'orders',
    _silver_loaded_at       TIMESTAMP       DEFAULT NOW(),
    PRIMARY KEY (order_id)
);

-- SILVER layer for Order Items
CREATE TABLE IF NOT EXISTS silver_order_items (
    order_item_id       VARCHAR(30)     NOT NULL,
    order_id            VARCHAR(20)     NOT NULL,
    sku                 VARCHAR(20)     NOT NULL,
    quantity            INT             NOT NULL,
    unit_price_ghs      DECIMAL(10,2)   NOT NULL,
    line_total_ghs      DECIMAL(10,2)   NOT NULL,
    line_total_check    BOOLEAN,                      
    -- metadata
    _source_table       VARCHAR(50)     DEFAULT 'order_items',
    _silver_loaded_at   TIMESTAMP       DEFAULT NOW(),
    PRIMARY KEY (order_item_id)
);

-- SILVER layer for Feedback Submissions
-- Transformations applied:
--   - rating validated between 1-5
--   - category standardized
--   - comment stripped of whitespace
CREATE TABLE IF NOT EXISTS silver_feedback_submissions (
    feedback_id         VARCHAR(20)     NOT NULL,
    order_id            VARCHAR(20)     NOT NULL,
    customer_id         VARCHAR(20),
    delivery_region     VARCHAR(50)     NOT NULL,
    sku                 VARCHAR(20),
    rating              SMALLINT        NOT NULL,
    category            VARCHAR(50)     NOT NULL,
    comment             TEXT,
    submitted_at        TIMESTAMP       NOT NULL,
    ingested_at         TIMESTAMP,
    -- metadata
    _source_table       VARCHAR(50)     DEFAULT 'feedback_submissions',
    _silver_loaded_at   TIMESTAMP       DEFAULT NOW(),
    PRIMARY KEY (feedback_id)
);


-- SILVER: Data Sources  (for lineage tracking)
CREATE TABLE IF NOT EXISTS silver_data_sources (
    id                  BIGINT          NOT NULL,
    name                VARCHAR(255)    NOT NULL,
    description         TEXT,
    type                VARCHAR(50)     NOT NULL,
    source_type         VARCHAR(50),
    connection_url      VARCHAR(500),
    file_path           VARCHAR(500),
    is_active           BOOLEAN,
    record_count        INTEGER,
    last_ingestion      TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL,
    -- metadata
    _source_table       VARCHAR(50)     DEFAULT 'data_sources',
    _silver_loaded_at   TIMESTAMP       DEFAULT NOW(),
    PRIMARY KEY (id)
);