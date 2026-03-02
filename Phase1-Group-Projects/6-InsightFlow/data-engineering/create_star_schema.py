"""Create Star Schema for InsightFlow Retail Analytics"""
from sqlalchemy import create_engine, text
from config import DATABASE_URL

engine = create_engine(DATABASE_URL)

SCHEMA_SQL = """
-- Dimension: Date
CREATE TABLE IF NOT EXISTS dim_date (
    date_key SERIAL PRIMARY KEY,
    full_date DATE UNIQUE NOT NULL,
    year INTEGER, quarter INTEGER, month INTEGER,
    day INTEGER, day_of_week INTEGER, week_of_year INTEGER,
    month_name VARCHAR(20), day_name VARCHAR(20),
    is_weekend BOOLEAN
);

-- Dimension: Product
CREATE TABLE IF NOT EXISTS dim_product (
    product_key SERIAL PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    unit_price NUMERIC(10,2)
);

-- Dimension: Customer
CREATE TABLE IF NOT EXISTS dim_customer (
    customer_key SERIAL PRIMARY KEY,
    customer_id INTEGER UNIQUE NOT NULL,
    segment VARCHAR(50)
);

-- Dimension: Region
CREATE TABLE IF NOT EXISTS dim_region (
    region_key SERIAL PRIMARY KEY,
    region_name VARCHAR(255) NOT NULL,
    country VARCHAR(100)
);

-- Fact: Sales
CREATE TABLE IF NOT EXISTS fact_sales (
    sale_key SERIAL PRIMARY KEY,
    date_key INTEGER REFERENCES dim_date(date_key),
    product_key INTEGER REFERENCES dim_product(product_key),
    customer_key INTEGER REFERENCES dim_customer(customer_key),
    region_key INTEGER REFERENCES dim_region(region_key),
    quantity INTEGER,
    unit_price NUMERIC(10,2),
    total_amount NUMERIC(12,2),
    source VARCHAR(50)
);

-- TODO: Add fact_feedback table for customer satisfaction analytics
-- TODO: Add dim_channel for online vs in-store tracking
"""

def create_schema():
    with engine.connect() as conn:
        for stmt in SCHEMA_SQL.split(';'):
            stmt = stmt.strip()
            if stmt and not stmt.startswith('--'):
                conn.execute(text(stmt))
        conn.commit()
    print("Star schema created successfully!")

if __name__ == "__main__":
    create_schema()
