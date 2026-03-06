"""
backend_auth.py
~~~~~~~~~~~~~~~
Thin wrapper around the InsightFlow Spring Boot REST API.

Endpoints used:
  POST /api/auth/login     – AuthRequest  { email, password }
  POST /api/auth/register  – RegisterRequest { name, email, password }

Both return AuthResponse { token, email, name, role }.
"""

import os
import requests
import streamlit as st

# Read backend URL from environment (set via .env / docker-compose)
BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")


# ─── API calls ────────────────────────────────────────────────────────────────

def login(email: str, password: str) -> dict:
    """
    Authenticate against the Spring Boot backend.

    Returns the AuthResponse dict on success.
    Raises ValueError (bad credentials / server error) or
           ConnectionError (backend not reachable).
    """
    try:
        resp = requests.post(
            f"{BACKEND_URL}/api/auth/login",
            json={"email": email, "password": password},
            timeout=10,
        )
    except requests.exceptions.ConnectionError:
        raise ConnectionError(
            f"Cannot reach the backend at {BACKEND_URL}. "
            "Make sure the Spring Boot server is running."
        )

    if resp.status_code == 200:
        return resp.json()

    # Surface the error message from the backend
    try:
        msg = resp.json()
        detail = msg if isinstance(msg, str) else (msg.get("message") or msg.get("error") or str(msg))
    except Exception:
        detail = resp.text or f"HTTP {resp.status_code}"
    raise ValueError(detail)


def register(name: str, email: str, password: str) -> dict:
    """
    Register a new user via the Spring Boot backend.

    Returns the AuthResponse dict (includes JWT) on success.
    Raises ValueError or ConnectionError on failure.
    """
    try:
        resp = requests.post(
            f"{BACKEND_URL}/api/auth/register",
            json={"name": name, "email": email, "password": password},
            timeout=10,
        )
    except requests.exceptions.ConnectionError:
        raise ConnectionError(
            f"Cannot reach the backend at {BACKEND_URL}. "
            "Make sure the Spring Boot server is running."
        )

    if resp.status_code == 200:
        return resp.json()

    try:
        msg = resp.json()
        detail = msg if isinstance(msg, str) else (msg.get("message") or msg.get("error") or str(msg))
    except Exception:
        detail = resp.text or f"HTTP {resp.status_code}"
    raise ValueError(detail)


# ─── Session helpers ──────────────────────────────────────────────────────────

def set_session(data: dict) -> None:
    """
    Populate st.session_state from an AuthResponse payload.

    Keys written:
      authentication_status – True
      jwt_token             – Bearer token for subsequent API requests
      name                  – User's display name
      email                 – User's email address
      roles                 – List with the user's role string
    """
    st.session_state["authentication_status"] = True
    st.session_state["jwt_token"] = data.get("token")
    st.session_state["name"] = data.get("name")
    st.session_state["email"] = data.get("email")
    st.session_state["roles"] = [data.get("role")] if data.get("role") else []


def clear_session() -> None:
    """Remove all auth-related keys from session state (logout)."""
    for key in ["authentication_status", "jwt_token", "name", "email", "roles"]:
        st.session_state.pop(key, None)


def get_auth_header() -> dict:
    """
    Return an Authorization header dict for use with the requests library.

    Example:
        resp = requests.get(url, headers=get_auth_header())
    """
    token = st.session_state.get("jwt_token")
    if not token:
        return {}
    return {"Authorization": f"Bearer {token}"}
