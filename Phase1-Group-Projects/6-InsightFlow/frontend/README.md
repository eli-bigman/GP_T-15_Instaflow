# InsightFlow – Frontend

A Streamlit-based data pipeline & analytics platform that authenticates users
via a Spring Boot REST backend and persists all user data in PostgreSQL.

---

## Table of Contents

1. [Project Structure](#project-structure)
2. [Prerequisites](#prerequisites)
3. [Environment Variables](#environment-variables)
4. [Installation & Running](#installation--running)
5. [Authentication Flow](#authentication-flow)
6. [Pages](#pages)
7. [API Reference](#api-reference)
8. [Security Notes](#security-notes)

---

## Project Structure

```
frontend/
├── app.py                  # Entry point – Login / Register page
├── .env                    # Secret values – NEVER commit this file
├── requirements.txt
├── assets/
│   └── style.css
├── components/
│   ├── navbar.py
│   └── sidebar.py
├── pages/
│   ├── 1_Login.py          # Standalone login page (sidebar nav)
│   ├── 2_Analytics.py      # Protected analytics page
│   └── 3_Dashboard.py      # Protected dashboard page
└── utils/
    ├── auth.py             # require_auth() guard
    └── backend_auth.py     # Spring Boot API calls (login/register)
```

---

## Prerequisites

| Tool                | Version                                                              |
| ------------------- | -------------------------------------------------------------------- |
| Python              | 3.10+                                                                |
| Spring Boot backend | running on port 8080                                                 |
| PostgreSQL          | running (configured in backend)                                      |

---

## Environment Variables

Create a `.env` file in the frontend directory:

```bash
touch .env
```

**Example `.env` file:**

```properties
BACKEND_URL=http://localhost:8080
COOKIE_NAME=insightflow_auth_cookie
COOKIE_KEY=your_random_secret_key_here
COOKIE_EXPIRY_DAYS=30
```

| Variable               | Description                                                       |
| ---------------------- | ----------------------------------------------------------------- |
| `BACKEND_URL`          | Base URL of the Spring Boot API (default `http://localhost:8080`) |
| `COOKIE_NAME`          | Name of the re-auth cookie stored in the browser                  |
| `COOKIE_KEY`           | Secret key used to sign the re-auth cookie                        |
| `COOKIE_EXPIRY_DAYS`   | Days before the re-auth cookie expires                            |

> **.env is listed in .gitignore** – never commit real secrets.

---

## Installation & Running

### 1. Create and activate a virtual environment

```bash
python3 -m venv .venv
source .venv/bin/activate        # Windows: .venv\Scripts\activate
```

### 2. Install dependencies

```bash
pip install -r requirements.txt
```

### 3. Start the Spring Boot backend (separate terminal)

```bash
cd ../backend
./mvnw spring-boot:run
```

### 4. Start the Streamlit frontend

```bash
streamlit run app.py
```

The app opens at **http://localhost:8501**.

---

## Testing Before Push

Run these checks locally before pushing to catch errors early:

### Check for syntax errors

```bash
python -m py_compile app.py pages/*.py utils/*.py components/*.py
```

### Run linter

```bash
flake8 . --count --select=E9,F63,F7,F82 --show-source --statistics --exclude=venv,.venv,__pycache__,.git
```

---

## Authentication Flow

```
User                Streamlit (app.py)         Spring Boot (/api/auth)    PostgreSQL
 │                        │                            │                       │
 │── email + password ───▶│                            │                       │
 │                        │── POST /api/auth/login ───▶│                       │
 │                        │                            │── verify BCrypt ─────▶│
 │                        │                            │◀─ user row ───────────│
 │                        │◀── { token, name, role } ──│                       │
 │                        │                            │                       │
 │                        │  set_session(data)         │                       │
 │                        │  st.session_state:         │                       │
 │                        │    authentication_status=True                      │
 │                        │    jwt_token = <JWT>       │                       │
 │                        │    name, email, roles      │                       │
 │◀── redirect to Dashboard│                            │                       │
```

Every protected page calls `require_auth()` at the top, which checks
`st.session_state["authentication_status"]` and redirects to `app.py` if
the user is not logged in.

---

## Pages

| Page             | Route                  | Access         |
| ---------------- | ---------------------- | -------------- |
| Login / Register | `app.py`               | Public         |
| Login (nav)      | `pages/1_Login.py`     | Public         |
| Analytics        | `pages/2_Analytics.py` | Requires login |
| Dashboard        | `pages/3_Dashboard.py` | Requires login |

---

## API Reference

The backend exposes the following endpoints (Spring Boot, port 8080):

| Method | Path                 | Body                        | Response                            |
| ------ | -------------------- | --------------------------- | ----------------------------------- |
| `POST` | `/api/auth/register` | `{ name, email, password }` | `{ token, email, name, role }`      |
| `POST` | `/api/auth/login`    | `{ email, password }`       | `{ token, email, name, role }`      |
| `GET`  | `/api/pipelines`     | –                           | List of pipelines (JWT required)    |
| `GET`  | `/api/datasources`   | –                           | List of data sources (JWT required) |

Full Swagger UI: **http://localhost:8080/swagger-ui.html**

### Default Test Credentials

**Admin User:**
- Email: `admin@amalitech.com`
- Password: `password123`

**Regular User:**
- Email: `user@amalitech.com`
- Password: `password123`

---

## Security Notes

- Passwords are hashed with **BCrypt** by Spring Boot before storage – the frontend never stores or transmits plain passwords beyond the initial form submission.
- The JWT token is stored **only in `st.session_state`** (server-side memory), not in cookies or localStorage.
- The `.env` file is git-ignored. Use environment variables or a secrets manager (e.g. HashiCorp Vault, AWS Secrets Manager) in production.
- Set `COOKIE_KEY` to a long random string in production (`python -c "import secrets; print(secrets.token_hex(32))"`).
