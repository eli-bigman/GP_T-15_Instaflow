import os
import streamlit as st
import streamlit_authenticator as stauth
import yaml
from yaml.loader import SafeLoader
from dotenv import load_dotenv

# Load .env into os.environ (safe to call multiple times)
load_dotenv()

# Resolve config.yaml relative to this utils/ directory (one level up)
_FRONTEND_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONFIG_PATH = os.path.join(_FRONTEND_DIR, "config.yaml")


def load_config() -> dict:
    """Load the YAML config file."""
    with open(CONFIG_PATH) as f:
        return yaml.load(f, Loader=SafeLoader)


def save_config(config: dict) -> None:
    """Persist changes (hashed passwords, login state, etc.) back to config.yaml."""
    with open(CONFIG_PATH, "w") as f:
        yaml.dump(config, f, default_flow_style=False, allow_unicode=True)


def get_authenticator() -> tuple[stauth.Authenticate, dict]:
    """
    Return a cached (authenticator, config) pair stored in session state.

    Cookie settings are read from environment variables (set via .env) so
    that secrets are never hard-coded in config.yaml.
    """
    if "authenticator" not in st.session_state or "config" not in st.session_state:
        config = load_config()

        # Override cookie settings from .env when available
        cookie_name = os.getenv("COOKIE_NAME", config["cookie"]["name"])
        cookie_key = os.getenv("COOKIE_KEY", config["cookie"]["key"])
        cookie_expiry = int(os.getenv("COOKIE_EXPIRY_DAYS", config["cookie"]["expiry_days"]))

        authenticator = stauth.Authenticate(
            config["credentials"],
            cookie_name,
            cookie_key,
            cookie_expiry,
        )
        st.session_state["authenticator"] = authenticator
        st.session_state["config"] = config

    return st.session_state["authenticator"], st.session_state["config"]


def require_auth() -> None:
    """
    Guard helper for all protected pages.

    Supports two auth modes:
      1. Backend JWT mode  – set when the user logged in via the Spring Boot API
         (st.session_state contains 'jwt_token').
      2. Local YAML mode   – set when the user logged in via streamlit-authenticator.

    In both cases 'authentication_status' must be True; otherwise the user is
    redirected to the login page.
    """
    if not st.session_state.get("authentication_status"):
        # Try refreshing a stauth cookie session (local mode only)
        try:
            authenticator, _ = get_authenticator()
            authenticator.login(location="unrendered")
        except Exception:
            pass

    if not st.session_state.get("authentication_status"):
        st.warning("Please log in to access this page.")
        st.switch_page("app.py")
        st.stop()
