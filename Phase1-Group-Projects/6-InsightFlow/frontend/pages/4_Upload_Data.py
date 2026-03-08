import streamlit as st
from datetime import date
from utils.ingestion_api import upload_csv
import pandas as pd
import time

st.set_page_config(page_title="InsightFlow – Upload POS Data", page_icon="📤", layout="wide")

# ── Check Authentication ──────────────────────────────────────────────────────
if not st.session_state.get("authentication_status"):
    st.warning("Please login to access this page.")
    st.stop()

st.title("📤 Upload Daily POS CSV File")
st.caption("As a Sales Manager, upload your daily store sales data for ingestion.")
st.divider()

# ── Upload Form ───────────────────────────────────────────────────────────────
with st.container():
    col1, col2 = st.columns([1, 2])

    with col1:
        st.subheader("Upload Details")
        upload_date = st.date_input(
            "Select Sales Date",
            value=date.today(),
            max_value=date.today(), # SM-01: Cannot upload for a future date
            help="The date the sales occurred."
        )

        uploaded_file = st.file_uploader(
            "Choose POS CSV file",
            type=["csv"], # SM-01: Accepts only CSV
            help="Select the daily CSV file from your device."
        )

        if st.button("Upload and Process", use_container_width=True, type="primary"):
            if not uploaded_file:
                st.error("Please select a CSV file to upload.")
            else:
                with st.spinner("Uploading and processing file..."):
                    try:
                        # SM-01: Upload logic
                        result = upload_csv(uploaded_file, upload_date.isoformat())
                        st.session_state["last_upload_result"] = result
                        st.success(f"Confirmation: File '{uploaded_file.name}' has been received.") # SM-01
                        st.balloons()
                        time.sleep(1)
                        st.rerun()
                    except Exception as e:
                        # SM-04: Immediate on-screen error message
                        st.error(f"Upload Failed: {str(e)}")

    with col2:
        # ── SM-02: View Upload Status & Validation Results ────────────────────────
        if "last_upload_result" in st.session_state:
            res = st.session_state["last_upload_result"]
            st.subheader("Latest Upload Status")

            # Summary Metrics
            m1, m2, m3 = st.columns(3)
            total = res['recordsProcessed'] + res['recordsFailed']
            m1.metric("Total Rows", total)
            m2.metric("Accepted", res['recordsProcessed'])
            m3.metric("Rejected", res['recordsFailed'], delta=-res['recordsFailed'] if res['recordsFailed'] > 0 else 0, delta_color="inverse")

            # Status Badge
            status = res['status']
            if status == "COMPLETED":
                st.success("✅ All rows accepted! The data is now ready for reporting.") # SM-02
            elif status == "PARTIAL_SUCCESS":
                st.warning("⚠️ Partial Success: Some rows failed validation.") # SM-04
            else:
                st.error("❌ Upload Failed: No data was ingested.") # SM-04

            # Validation Errors Table
            if res.get('validationErrors'):
                st.subheader("Validation Errors")
                errors = res['validationErrors']

                # Parse errors into a table (Row # | Reason)
                error_data = []
                for err in errors:
                    parts = err.split(": ", 1)
                    if len(parts) == 2:
                        error_data.append({"Row": parts[0].replace("Row ", ""), "Reason": parts[1]})
                    else:
                        error_data.append({"Row": "N/A", "Reason": err})

                df_errors = pd.DataFrame(error_data)
                st.table(df_errors) # SM-02: Rejected rows listed with row number and reason

                # SM-02: Download error report
                csv_errors = df_errors.to_csv(index=False).encode('utf-8')
                st.download_button(
                    label="📥 Download Error Report",
                    data=csv_errors,
                    file_name=f"error_report_{res['jobId']}.csv",
                    mime="text/csv",
                )

            if st.button("Clear Results"):
                del st.session_state["last_upload_result"]
                st.rerun()
        else:
            st.info("Upload a file to see the validation results here.")

st.divider()
st.info("💡 **Tip:** Ensure your CSV includes headers: `transaction_id`, `product_id`, `quantity`, `sku`, `payment_method`.")
