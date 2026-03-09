import streamlit as st
from datetime import date, timedelta
from utils.ingestion_api import get_ingestion_jobs
import pandas as pd

st.set_page_config(page_title="InsightFlow – Upload History", page_icon="📜", layout="wide")

# ── Check Authentication ──────────────────────────────────────────────────────
if not st.session_state.get("authentication_status"):
    st.warning("Please login to access this page.")
    st.stop()

st.title("📜 My Upload History")
st.caption("View and monitor your past CSV uploads for the last 30 days.")
st.divider()

# ── Fetch History ─────────────────────────────────────────────────────────────
try:
    jobs = get_ingestion_jobs()
except Exception as e:
    st.error(f"Failed to fetch history: {str(e)}")
    st.stop()

if not jobs:
    st.info("No upload history found for the last 30 days.")
else:
    # ── SM-03: View My Upload History ─────────────────────────────────────────
    # Convert to DataFrame for easier manipulation
    df = pd.DataFrame(jobs)

    # Format columns for display
    df['Date'] = pd.to_datetime(df['salesDate']).dt.date
    df['Time'] = pd.to_datetime(df['startedAt']).dt.strftime('%H:%M:%S')
    df['Filename'] = df['fileName']
    df['Accepted'] = df['recordsProcessed']
    df['Rejected'] = df['recordsFailed']
    df['Status'] = df['status']

    # SM-13: Status indicators
    def format_status(row):
        if row['Status'] == "COMPLETED": return "✅ Fully Accepted"
        if row['Accepted'] > 0: return "⚠️ Partially Accepted"
        return "❌ Failed"

    df['Status Display'] = df.apply(format_status, axis=1)

    # Display Table
    st.subheader("Recent Uploads")
    display_cols = ['Date', 'Time', 'Filename', 'Accepted', 'Rejected', 'Status Display']
    st.dataframe(df[display_cols], use_container_width=True, hide_index=True)

    # ── SM-14: Missing Date Detection ─────────────────────────────────────────
    st.divider()
    st.subheader("📅 Missing Uploads (Last 30 Days)")

    # Generate list of last 30 days
    today = date.today()
    last_30_days = [today - timedelta(days=i) for i in range(30)]
    uploaded_dates = set(df['Date'].tolist())

    missing_dates = [d for d in last_30_days if d not in uploaded_dates]

    if not missing_dates:
        st.success("Great! You have successfully uploaded data for every day in the last 30 days.")
    else:
        st.warning(f"You are missing uploads for {len(missing_dates)} days.")

        # Display missing dates in a grid
        cols = st.columns(5)
        for i, d in enumerate(missing_dates):
            cols[i % 5].info(f"📅 {d.strftime('%b %d, %Y')}")

# ── Navigation ────────────────────────────────────────────────────────────────
st.sidebar.divider()
if st.sidebar.button("📤 Go to Upload Page"):
    st.switch_page("pages/4_Upload_Data.py")
