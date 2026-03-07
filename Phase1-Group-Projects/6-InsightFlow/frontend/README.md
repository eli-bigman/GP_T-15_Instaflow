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
7. [Connecting to the Spring Boot Backend](#connecting-to-the-spring-boot-backend)
8. [API Reference](#api-reference)
9. [Security Notes](#security-notes)

---

## Project Structure

```
frontend/
├── app.py                  # Entry point – Login / Register page
├── config.yaml             # streamlit-authenticator fallback config (no secrets)
├── .env                    # Secret values – NEVER commit this file
├── .env.example            # Template for new devs
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
    ├── auth.py             # require_auth() guard + streamlit-authenticator helpers
    ├── backend_auth.py     # Spring Boot API calls (login / register /
```

---

## Prerequisites

| Tool                | Version                                                              |
| ------------------- | -------------------------------------------------------------------- |
| Python              | 3.10+                                                                |
| Spring Boot backend | running on port 8080                                                 |
| PostgreSQL          | running (configured in `backend/src/main/resources/application.yml`) |

---

## Environment Variables

Copy `.env.example` to `.env` and fill in the values:

```bash
cp .env.example .env
```

| Variable               | Description                                                       |
| ---------------------- | ----------------------------------------------------------------- |
| `BACKEND_URL`          | Base URL of the Spring Boot API (default `http://localhost:8080`) |
| `COOKIE_NAME`          | Name of the re-auth cookie stored in the browser                  |
| `COOKIE_KEY`           | Secret key used to sign the re-auth cookie                        |
| `COOKIE_EXPIRY_DAYS`   | Days before the re-auth cookie expires                            |
| `GOOGLE_CLIENT_ID`     | Google OAuth2 client ID (optional)                                |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 client secret (optional)                            |
| `GOOGLE_REDIRECT_URI`  | OAuth2 redirect URI (default `http://localhost:8501`)             |

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
cd frontend
streamlit run app.py
```

The app opens at **http://localhost:8501**.

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

## Connecting to the Spring Boot Backend

All API calls live in `utils/backend_auth.py`.

### Login

```python
from utils.backend_auth import login, set_session

data = login(email="user@example.com", password="secret")
set_session(data)   # populates st.session_state
```

### Register

```python
from utils.backend_auth import register, set_session

data = register(name="John Smith", email="john@example.com", password="secret")
set_session(data)
```

### Making authenticated API calls from other pages

After login, use the stored JWT for any subsequent requests to the backend:

```python
from utils.backend_auth import get_auth_header
import requests

resp = requests.get(
    "http://localhost:8080/api/pipelines",
    headers=get_auth_header(),   # adds Authorization: Bearer <token>
)
data = resp.json()
```

### Logout

```python
from utils.backend_auth import clear_session

clear_session()         # wipes session state
st.switch_page("app.py")
```

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

---

## Security Notes

- Passwords are hashed with **BCrypt** by Spring Boot before storage – the frontend never stores or transmits plain passwords beyond the initial form submission.
- The JWT token is stored **only in `st.session_state`** (server-side memory), not in cookies or localStorage.
- The `.env` file is git-ignored. Use environment variables or a secrets manager (e.g. HashiCorp Vault, AWS Secrets Manager) in production.
- Set `COOKIE_KEY` to a long random string in production (`python -c "import secrets; print(secrets.token_hex(32))"`).

//////////////////////////////////

# InsightFlow Backend

A Spring Boot backend for InsightFlow — a data pipeline and insight reporting platform.

---

## Tech Stack

- Java 25
- Spring Boot 3.2.0
- PostgreSQL 16
- Spring Security + JWT
- Spring GraphQL
- MapStruct
- Lombok
- Docker + Docker Compose
- pgAdmin 4

---

## Prerequisites

Make sure you have the following installed:

- [Java 25](https://adoptium.net/)
- [Maven](https://maven.apache.org/)
- [Docker](https://www.docker.com/products/docker-desktop)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/) (recommended)

---

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/insightflow.git
cd insightflow/backend
```

### 2. Set Up Environment Variables

This project uses two env files depending on how you run the app:

| File          | Used when                           |
| ------------- | ----------------------------------- |
| `.env`        | Running app locally in IntelliJ     |
| `.env.docker` | Running app via `docker-compose up` |

Copy the example files and fill in the values:

```bash
cp .env.example .env
cp .env.docker.example .env.docker
```

**`.env`** (for local development in IntelliJ):

```properties
# Database - uses localhost since app runs outside Docker
APP_DB_URL=jdbc:postgresql://localhost:5432/insightflow_app
APP_DB_USER=insightflow
APP_DB_PASSWORD=insightflow_pass

# pgAdmin
PGADMIN_EMAIL=admin@admin.com
PGADMIN_PASSWORD=admin

# JWT
APP_JWT_SECRET=your_generated_secret_here
APP_ACCESS_TOKEN_EXPIRATION=86400000
APP_REFRESH_TOKEN_EXPIRATION=604800000

# Google OAuth
APP_GOOGLE_CLIENT_ID=your_google_client_id
APP_GOOGLE_CLIENT_SECRET=your_google_client_secret
APP_REDIRECT_URL=http://localhost:3000/login
```

**`.env.docker`** (for running via Docker Compose):

```properties
# Database - uses "db" service name since app runs inside Docker
APP_DB_URL=jdbc:postgresql://db:5432/insightflow_app
APP_DB_USER=insightflow
APP_DB_PASSWORD=insightflow_pass

# pgAdmin
PGADMIN_EMAIL=admin@admin.com
PGADMIN_PASSWORD=admin

# JWT
APP_JWT_SECRET=your_generated_secret_here
APP_ACCESS_TOKEN_EXPIRATION=86400000
APP_REFRESH_TOKEN_EXPIRATION=604800000

# Google OAuth
APP_GOOGLE_CLIENT_ID=your_google_client_id
APP_GOOGLE_CLIENT_SECRET=your_google_client_secret
APP_REDIRECT_URL=http://localhost:3000/login
```

> **Note:** Never commit `.env` or `.env.docker` to Git. Both are already in `.gitignore`.

To generate a secure JWT secret, run:

```bash
openssl rand -base64 64 | tr -d '\n'
```

---

### 3. Start the Database

Pull and start the PostgreSQL container (all schemas are pre-built inside the image):

```bash
docker-compose up -d db
```

This starts:

- **PostgreSQL** on port `5432` with all tables already created
- You can optionally also start pgAdmin:

```bash
docker-compose up -d pgadmin
```

Then access pgAdmin at **http://localhost:5050**

- Email: value of `PGADMIN_EMAIL` in your `.env`
- Password: value of `PGADMIN_PASSWORD` in your `.env`

To connect pgAdmin to the database:

- Host: `localhost`
- Port: `5432`
- Username: value of `APP_DB_USER`
- Password: value of `APP_DB_PASSWORD`

---

### 4. Run the Application

Run from IntelliJ by pressing the **Run** button, or via terminal:

```bash
./mvnw spring-boot:run
```

The app will start on **http://localhost:8080**

---

## API Documentation

Once the app is running, access the Swagger UI at:

```
http://localhost:8080/swagger-ui.html
```

API docs (JSON) available at:

```
http://localhost:8080/api-docs
```

---

## Project Structure

```
src/
├── main/
│   ├── java/com/insightflow/
│   │   ├── config/         # Security, JWT, filters
│   │   ├── controller/     # REST controllers
│   │   ├── model/          # JPA entities
│   │   ├── repository/     # Spring Data repositories
│   │   ├── service/        # Business logic
│   │   └── dto/            # Data transfer objects
│   └── resources/
│       ├── application.properties
│       └── db/migration/   # Flyway migration scripts
docker/
└── db/
    ├── Dockerfile          # Custom postgres image with schemas
    └── init.sql            # Database schema definitions
```

---

## Docker Setup (Full)

To run both the app and database in Docker:

```bash
docker-compose up -d
```

To stop everything:

```bash
docker-compose down
```

To view logs:

```bash
docker-compose logs -f app
```

---

## Database Schema Updates (For Team Leads)

When the database schema changes, rebuild and push the Docker image:

```bash
# 1. Update docker/db/init.sql with new tables/columns

# 2. Rebuild and push the image
docker build -t gdakore/insightflow-db:latest ./docker/db
docker push gdakore/insightflow-db:latest
```

Teammates then pull the latest image and recreate their container:

```bash
docker pull gdakore/insightflow-db:latest
docker-compose down
docker-compose up -d db
```

---

## Environment Variables Reference

| Variable                       | Description                          |
| ------------------------------ | ------------------------------------ |
| `APP_DB_URL`                   | PostgreSQL JDBC connection URL       |
| `APP_DB_USER`                  | PostgreSQL username                  |
| `APP_DB_PASSWORD`              | PostgreSQL password                  |
| `APP_JWT_SECRET`               | Base64-encoded JWT signing secret    |
| `APP_ACCESS_TOKEN_EXPIRATION`  | Access token expiry in milliseconds  |
| `APP_REFRESH_TOKEN_EXPIRATION` | Refresh token expiry in milliseconds |
| `APP_GOOGLE_CLIENT_ID`         | Google OAuth client ID               |
| `APP_GOOGLE_CLIENT_SECRET`     | Google OAuth client secret           |
| `APP_REDIRECT_URL`             | OAuth redirect URL after login       |
| `PGADMIN_EMAIL`                | pgAdmin login email                  |
| `PGADMIN_PASSWORD`             | pgAdmin login password               |

---

## Common Issues

**App fails to start with `password authentication failed`**

- Check that your `.env` credentials match what was used to create the DB
- Make sure the Docker container is running: `docker ps`

**`Could not resolve placeholder` error**

- Your `.env` file is missing a required variable
- Check that `spring.config.import=optional:file:.env[.properties]` is in `application.properties`

**Port 5432 already in use**

- You have a local PostgreSQL instance running alongside Docker
- Stop it with: `sudo systemctl stop postgresql`

---

## Contributing

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Commit your changes: `git commit -m "Add your feature"`
3. Push to the branch: `git push origin feature/your-feature`
4. Open a Pull Request
