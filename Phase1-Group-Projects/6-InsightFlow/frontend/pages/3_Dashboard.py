import streamlit as st
from utils.auth import require_auth
from utils.backend_auth import clear_session

st.set_page_config(
    page_title="InsightFlow – Dashboard",
    page_icon="🗂️",
    layout="wide",
)

# Guard: redirect to login if not authenticated
require_auth()

# ── Sidebar: user info + logout ──────────────────────────────────────────────
with st.sidebar:
    st.markdown(f"**Logged in as:** {st.session_state.get('name')}")
    st.caption(f"📧 {st.session_state.get('email', '')}")
    roles = st.session_state.get("roles") or []
    if roles:
        st.caption(f"Role: {', '.join(roles)}")
    if st.button("Logout", key="dashboard_logout", use_container_width=True):
        clear_session()
        st.switch_page("app.py")

# ── Page content ─────────────────────────────────────────────────────────────
st.title("🗂️ Dashboard")
st.write(f"Welcome back, *{st.session_state.get('name')}*!")
st.divider()
st.info("Your dashboard content will appear here.")
