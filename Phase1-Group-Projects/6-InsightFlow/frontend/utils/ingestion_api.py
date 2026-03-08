import os
import requests
import streamlit as st
from utils.backend_auth import get_auth_header

BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")

def upload_csv(file, date_str: str) -> dict:
    """
    Upload a CSV file to the backend.
    """
    files = {"file": (file.name, file.getvalue(), "text/csv")}
    data = {"date": date_str}

    try:
        resp = requests.post(
            f"{BACKEND_URL}/api/ingest/csv",
            files=files,
            data=data,
            headers=get_auth_header(),
            timeout=30
        )
    except requests.exceptions.ConnectionError:
        raise ConnectionError("Cannot reach the backend.")

    if resp.status_code == 200:
        return resp.json()["data"]

    try:
        detail = resp.json().get("message") or resp.text
    except Exception:
        detail = f"HTTP {resp.status_code}"
    raise ValueError(detail)

def get_ingestion_jobs() -> list:
    """
    Fetch the list of ingestion jobs for the current user.
    """
    try:
        resp = requests.get(
            f"{BACKEND_URL}/api/ingest/jobs",
            headers=get_auth_header(),
            timeout=10
        )
    except requests.exceptions.ConnectionError:
        raise ConnectionError("Cannot reach the backend.")

    if resp.status_code == 200:
        return resp.json()["data"]

    return []

def get_job_details(job_id: int) -> dict:
    """
    Fetch details for a specific ingestion job.
    """
    try:
        resp = requests.get(
            f"{BACKEND_URL}/api/ingest/jobs/{job_id}",
            headers=get_auth_header(),
            timeout=10
        )
    except requests.exceptions.ConnectionError:
        raise ConnectionError("Cannot reach the backend.")

    if resp.status_code == 200:
        return resp.json()["data"]

    return {}
