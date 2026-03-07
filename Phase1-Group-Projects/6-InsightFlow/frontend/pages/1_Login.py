"""
1_Login.py
~~~~~~~~~~
Standalone login page (accessible from Streamlit's sidebar nav).
Redirects to the dashboard if the user is already authenticated.
"""

import streamlit as st
from dotenv import load_dotenv
from utils.backend_auth import login, register, set_session

load_dotenv()

st.set_page_config(
    page_title="InsightFlow – Login",
    page_icon="📊",
    layout="centered",
)

# Already authenticated → bounce to dashboard
if st.session_state.get("authentication_status"):
    st.switch_page("pages/3_Dashboard.py")

st.title("📊 InsightFlow")
st.caption("Data pipeline & analytics platform")
st.divider()

tab_login, tab_register = st.tabs(["Sign In", "Create Account"])

# ── Login tab ─────────────────────────────────────────────────────────────────
with tab_login:
    with st.form("login_form", clear_on_submit=False):
        st.subheader("Sign in to your account")
        email = st.text_input("Email", placeholder="you@example.com")
        password = st.text_input("Password", type="password", placeholder="••••••••")
        submitted = st.form_submit_button("Login", use_container_width=True)

    if submitted:
        if not email or not password:
            st.error("Please fill in both email and password.")
        else:
            with st.spinner("Authenticating…"):
                try:
                    data = login(email, password)
                    set_session(data)
                    st.success(f"Welcome back, {data['name']}!")
                    st.switch_page("pages/3_Dashboard.py")
                except ConnectionError as e:
                    st.error(str(e))
                except ValueError as e:
                    st.error(f"Login failed: {e}")

# ── Register tab ──────────────────────────────────────────────────────────────
with tab_register:
    with st.form("register_form", clear_on_submit=True):
        st.subheader("Create a new account")
        reg_name = st.text_input("Full Name", placeholder="John Smith")
        reg_email = st.text_input("Email", placeholder="you@example.com", key="reg_email")
        reg_password = st.text_input("Password", type="password", placeholder="min. 6 characters", key="reg_pass")
        reg_password2 = st.text_input("Confirm Password", type="password", placeholder="repeat password", key="reg_pass2")
        reg_submitted = st.form_submit_button("Register", use_container_width=True)

    if reg_submitted:
        if not all([reg_name, reg_email, reg_password, reg_password2]):
            st.error("Please fill in all fields.")
        elif reg_password != reg_password2:
            st.error("Passwords do not match.")
        elif len(reg_password) < 6:
            st.error("Password must be at least 6 characters.")
        else:
            with st.spinner("Creating account…"):
                try:
                    data = register(reg_name, reg_email, reg_password)
                    set_session(data)
                    st.success(f"Account created! Welcome, {data['name']}!")
                    st.switch_page("pages/3_Dashboard.py")
                except ConnectionError as e:
                    st.error(str(e))
                except ValueError as e:
                    st.error(f"Registration failed: {e}")

