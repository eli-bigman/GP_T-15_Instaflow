-- Users
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL DEFAULT 'USER',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Data Sources
CREATE TABLE IF NOT EXISTS data_sources (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    type            VARCHAR(50) NOT NULL,
    source_type     VARCHAR(50),
    connection_url  VARCHAR(500),
    file_path       VARCHAR(500),
    is_active       BOOLEAN,
    record_count    INTEGER,
    last_ingestion  TIMESTAMP,
    created_by      BIGINT REFERENCES users(id),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

<<<<<<< Updated upstream
-- Product Catalogue
CREATE TABLE IF NOT EXISTS product_catalogue (
    product_id      BIGSERIAL       PRIMARY KEY,            -- e.g. PRD-A12. Universal key across all channels.
    sku             VARCHAR(20)     NOT NULL UNIQUE,     
    product_name    VARCHAR(200)    NOT NULL,            
    category        VARCHAR(100)    NOT NULL,            
    brand           VARCHAR(100),                       
    unit_of_measure VARCHAR(20),                         
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE 
);

-- SKU Mappings
CREATE TABLE IF NOT EXISTS sku_mappings (
    mapping_id      BIGSERIAL       PRIMARY KEY,
    master_sku      VARCHAR(20)     NOT NULL REFERENCES product_catalogue(sku),
    pos_sku         VARCHAR(20),                         
    ecom_sku        VARCHAR(20),                        
    notes           TEXT                                
);

-- Store Reference
-- For creating all 25 stores
CREATE TABLE IF NOT EXISTS store_reference (
    store_id        VARCHAR(10)     PRIMARY KEY,         -- Example: ACC-01, KUM-01. Must match POS CSV store_id exactly.
    store_name      VARCHAR(100)    NOT NULL,            -- Example: ShopSmart Accra Central
    city            VARCHAR(50)     NOT NULL,            -- Accra | Kumasi | Takoradi | Cape Coast
    region          VARCHAR(50)     NOT NULL             -- Greater Accra | Ashanti | Western | Central
);

-- Warehouses
CREATE TABLE IF NOT EXISTS warehouses (
    warehouse_id    VARCHAR(20)     PRIMARY KEY,         -- Example. WH-ACC-01, WH-KUM-01
    region          VARCHAR(50)     NOT NULL,            -- Example: Greater Accra | Ashanti | Western | Central
    city            VARCHAR(50)     NOT NULL,            -- Accra | Kumasi | Takoradi | Cape Coast
    address         TEXT                                
);

-- POS Transactions
CREATE TABLE IF NOT EXISTS pos_transactions (
    transaction_id  VARCHAR(20)     NOT NULL,            -- Example: ORD-1001.
    sku             VARCHAR(20)     NOT NULL REFERENCES product_catalogue(sku),
    store_id        VARCHAR(10)     NOT NULL REFERENCES store_reference(store_id),
    quantity        INT             NOT NULL CHECK (quantity > 0),
    unit_price_ghs  DECIMAL(10,2)   NOT NULL CHECK (unit_price_ghs > 0),
    line_total_ghs  DECIMAL(10,2)   NOT NULL,            
    payment_method  VARCHAR(20)     NOT NULL CHECK (payment_method IN ('CASH', 'MOMO', 'CARD')),
    cashier_id      VARCHAR(10)     NOT NULL,            -- Example. CASH-04
    timestamp_local TIMESTAMP       NOT NULL,            -- Ghana time (GMT+0)
    ingested_at     TIMESTAMP       NOT NULL DEFAULT NOW(),
    PRIMARY KEY (transaction_id, sku)                    
);

-- Inventory Stock
CREATE TABLE IF NOT EXISTS inventory_stock (
    warehouse_id        VARCHAR(20)     NOT NULL REFERENCES warehouses(warehouse_id),
    sku                 VARCHAR(20)     NOT NULL REFERENCES product_catalogue(sku),
    stock_available     INT             NOT NULL CHECK (stock_available >= 0),
    reorder_threshold   INT             NOT NULL CHECK (reorder_threshold > 0),
    last_restock_date   DATE,                           
    last_extracted_at   TIMESTAMP       NOT NULL,        -- added by ETL on read.
    PRIMARY KEY (warehouse_id, sku)
);

-- Orders
CREATE TABLE IF NOT EXISTS orders (
    order_id                VARCHAR(20)     PRIMARY KEY,   
    customer_id             VARCHAR(20)     NOT NULL,        
    order_status            VARCHAR(20)     NOT NULL CHECK (order_status IN ('PENDING', 'FULFILLED', 'CANCELLED', 'RETURNED')),
    delivery_region         VARCHAR(50)     NOT NULL,        -- Greater Accra | Ashanti | Western | Central
    delivery_city           VARCHAR(50)     NOT NULL,
    delivery_address        TEXT,                           
    payment_method          VARCHAR(20)     NOT NULL CHECK (payment_method IN ('MOMO', 'CARD', 'COD')),
    total_order_value_ghs   DECIMAL(10,2)   NOT NULL CHECK (total_order_value_ghs > 0),
    order_timestamp_utc     TIMESTAMP       NOT NULL,        -- UTC from API. ETL converts to GMT+0.
    ingested_at             TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- Order Items
CREATE TABLE IF NOT EXISTS order_items (
    order_item_id   VARCHAR(30)     PRIMARY KEY,         
    order_id        VARCHAR(20)     NOT NULL REFERENCES orders(order_id),
    sku             VARCHAR(20)     NOT NULL REFERENCES product_catalogue(sku),
    quantity        INT             NOT NULL CHECK (quantity > 0),
    unit_price_ghs  DECIMAL(10,2)   NOT NULL CHECK (unit_price_ghs > 0), 
    line_total_ghs  DECIMAL(10,2)   NOT NULL            
);

-- Feedback Submissions
CREATE TABLE IF NOT EXISTS feedback_submissions (
    feedback_id         VARCHAR(20)     PRIMARY KEY,     -- Example. FB-882
    order_id            VARCHAR(20)     NOT NULL REFERENCES orders(order_id),
    customer_id         VARCHAR(20),                     
    delivery_region     VARCHAR(50)     NOT NULL,       
    sku                 VARCHAR(20)     REFERENCES product_catalogue(sku), 
    rating              SMALLINT        NOT NULL CHECK (rating BETWEEN 1 AND 5),
    category            VARCHAR(50)     NOT NULL CHECK (category IN ('Delivery Speed', 'Product Quality', 'App Experience', 'Packaging', 'Overall')),
    comment             TEXT,                            
    submitted_at        TIMESTAMP       NOT NULL,       
    ingested_at         TIMESTAMP       NOT NULL DEFAULT NOW()
);
