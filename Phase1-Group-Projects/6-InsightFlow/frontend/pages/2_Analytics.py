import streamlit as st
from utils.auth import require_auth
from utils.backend_auth import clear_session

st.set_page_config(
    page_title="InsightFlow – Analytics",
    page_icon="📈",
    layout="wide",
)

# Guard: redirect to login if not authenticated
require_auth()

# ── Sidebar: user info + logout ──────────────────────────────────────────────
with st.sidebar:
    st.markdown(f"**Logged in as:** {st.session_state.get('name')}")
    st.caption(f"📧 {st.session_state.get('email', '')}")
    if st.button("Logout", key="analytics_logout", use_container_width=True):
        clear_session()
        st.switch_page("app.py")

# ── Page content ─────────────────────────────────────────────────────────────
st.title("📈 Analytics")
st.write(f"Welcome, *{st.session_state.get('name')}*!")
st.divider()
st.info("Your analytics content will appear here.")
