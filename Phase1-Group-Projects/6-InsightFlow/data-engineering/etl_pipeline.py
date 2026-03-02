"""ETL Pipeline for InsightFlow - Retail Analytics Processing"""
import pandas as pd
from sqlalchemy import create_engine, text
from config import DATABASE_URL

engine = create_engine(DATABASE_URL)


def extract_sales_csv(filepath="sample_sales.csv"):
    """Extract sales data from CSV file."""
    try:
        df = pd.read_csv(filepath)
        print(f"Extracted {len(df)} rows from {filepath}")
        return df
    except FileNotFoundError:
        print(f"File {filepath} not found. Run sample_data.py first.")
        return pd.DataFrame()


def transform_sales_data(df):
    """Clean and transform sales data for star schema loading."""
    if df.empty:
        return df
    df = df.dropna(subset=["date", "product", "quantity"])
    df = df.drop_duplicates()
    df["date"] = pd.to_datetime(df["date"])
    df["total"] = df["quantity"] * df["unit_price"]
    # TODO: Handle data quality issues (negative quantities, future dates, etc.)
    # TODO: Standardize product names (trim, title case)
    # TODO: Validate customer_id references
    print(f"Transformed {len(df)} clean records")
    return df


def load_dim_date(df):
    """Populate date dimension from sales dates."""
    dates = df["date"].dt.date.unique()
    dim_records = []
    for d in dates:
        dt = pd.Timestamp(d)
        dim_records.append({
            "full_date": d,
            "year": dt.year, "quarter": (dt.month - 1) // 3 + 1,
            "month": dt.month, "day": dt.day,
            "day_of_week": dt.dayofweek, "week_of_year": dt.isocalendar()[1],
            "month_name": dt.strftime("%B"), "day_name": dt.strftime("%A"),
            "is_weekend": dt.dayofweek >= 5,
        })
    dim_df = pd.DataFrame(dim_records)
    dim_df.to_sql("dim_date", engine, if_exists="replace", index=False)
    print(f"Loaded {len(dim_df)} dates into dim_date")


def load_dim_product(df):
    """Populate product dimension."""
    products = df[["product"]].drop_duplicates().rename(columns={"product": "product_name"})
    # TODO: Add category classification logic
    products["category"] = "General"
    products["unit_price"] = df.groupby("product")["unit_price"].mean().values
    products.to_sql("dim_product", engine, if_exists="replace", index=False)
    print(f"Loaded {len(products)} products into dim_product")


def load_dim_region(df):
    """Populate region dimension."""
    regions = df[["region"]].drop_duplicates().rename(columns={"region": "region_name"})
    regions["country"] = regions["region_name"].apply(
        lambda r: r.split("-")[0] if "-" in r else "Unknown"
    )
    regions.to_sql("dim_region", engine, if_exists="replace", index=False)
    print(f"Loaded {len(regions)} regions into dim_region")


# TODO: Implement load_dim_customer()
# TODO: Implement load_fact_sales() - join dimensions and populate fact table
# TODO: Implement compute_daily_revenue() - aggregate by date
# TODO: Implement compute_top_products() - rank by total sales
# TODO: Implement compute_regional_performance() - compare Ghana vs Rwanda


def run_pipeline():
    print("=" * 50)
    print("Starting InsightFlow ETL Pipeline")
    print("=" * 50)

    # Extract
    sales_df = extract_sales_csv()
    if sales_df.empty:
        print("No data to process. Exiting.")
        return

    # Transform
    clean_df = transform_sales_data(sales_df)

    # Load dimensions
    load_dim_date(clean_df)
    load_dim_product(clean_df)
    load_dim_region(clean_df)

    # TODO: Load customer dimension
    # TODO: Load fact_sales table
    # TODO: Generate daily revenue metrics
    # TODO: Generate top products report
    # TODO: Process feedback surveys for satisfaction trends

    print("=" * 50)
    print("ETL Pipeline complete!")
    print("=" * 50)


if __name__ == "__main__":
    run_pipeline()
