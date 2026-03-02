"""Generate sample data for InsightFlow retail analytics"""
import pandas as pd
import random
from datetime import datetime, timedelta

def generate_sales_data(rows=500):
    """Generate sample sales CSV data (point-of-sale transactions)."""
    products = ["Widget A", "Widget B", "Gadget X", "Gadget Y", "Service Pro"]
    regions = ["Ghana-Accra", "Ghana-Kumasi", "Rwanda-Kigali", "Rwanda-Huye"]

    data = []
    base_date = datetime.now() - timedelta(days=90)
    for i in range(rows):
        data.append({
            "date": (base_date + timedelta(days=random.randint(0, 90))).strftime("%Y-%m-%d"),
            "product": random.choice(products),
            "region": random.choice(regions),
            "quantity": random.randint(1, 50),
            "unit_price": round(random.uniform(10.0, 200.0), 2),
            "customer_id": random.randint(1000, 9999),
        })

    df = pd.DataFrame(data)
    df["total"] = df["quantity"] * df["unit_price"]
    df.to_csv("sample_sales.csv", index=False)
    print(f"Generated {len(df)} rows -> sample_sales.csv")
    return df


def generate_feedback_data(rows=200):
    """Generate sample customer feedback survey CSV data."""
    products = ["Widget A", "Widget B", "Gadget X", "Gadget Y", "Service Pro"]
    channels = ["email", "in-store", "online"]

    data = []
    base_date = datetime.now() - timedelta(days=90)
    for i in range(rows):
        data.append({
            "date": (base_date + timedelta(days=random.randint(0, 90))).strftime("%Y-%m-%d"),
            "customer_id": random.randint(1000, 9999),
            "product": random.choice(products),
            "rating": random.randint(1, 5),
            "channel": random.choice(channels),
            "comment": random.choice([
                "Great product", "Needs improvement", "Average experience",
                "Excellent service", "Would recommend", "Not satisfied",
                "Good value for money", "Delivery was slow", "",
            ]),
        })

    df = pd.DataFrame(data)
    df.to_csv("sample_feedback.csv", index=False)
    print(f"Generated {len(df)} rows -> sample_feedback.csv")
    return df


# TODO: Generate sample online orders JSON data (API feed simulation)
# TODO: Generate sample inventory CSV data


if __name__ == "__main__":
    generate_sales_data()
    generate_feedback_data()
