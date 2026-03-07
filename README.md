# InsightFlow - Multi-Source Business Intelligence Pipeline

![CI/CD](https://github.com/eli-bigman/GP_T-15_Instaflow/actions/workflows/ci.yml/badge.svg)

## Overview

InsightFlow is a multi-source BI pipeline for retail companies with a Spring Boot backend and Streamlit frontend.
It provides authentication, data ingestion, and analytics capabilities with automated CI/CD deployment.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Streamlit Frontend                        │
│                    (Port 8501)                               │
│              Login/Register + Dashboard                      │
└──────────────────────┬──────────────────────────────────────┘
                       │ REST API (JWT Auth)
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Spring Boot Backend (Port 8080)                 │
│   AuthController │ DataSourceController │ PipelineController│
│   JWT Security │ Validation │ File Ingestion                │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              PostgreSQL Database (Port 5432)                 │
│   users │ data_sources │ pipelines │ ingestion_jobs         │
└─────────────────────────────────────────────────────────────┘
```

## Tech Stack

### Backend
- Java 25 (Temurin)
- Spring Boot 4.0.3
- Spring Security + JWT
- PostgreSQL 16
- Docker

### Frontend
- Python 3.11
- Streamlit
- Requests (REST API client)

### DevOps
- Docker & Docker Compose
- GitHub Actions (CI/CD)
- Docker Hub (Image Registry)

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Java 25 / Maven 3.9+
- Python 3.10+

### 1. Clone Repository

```bash
git clone https://github.com/eli-bigman/GP_T-15_Instaflow.git
cd GP_T-15_Instaflow/Phase1-Group-Projects/6-InsightFlow
```

### 2. Start Database

```bash
cd backend
docker compose up -d db
```

### 3. Run Backend

```bash
./mvnw spring-boot:run
```

Backend runs at **http://localhost:8080**

### 4. Run Frontend

```bash
cd ../frontend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
streamlit run app.py
```

Frontend runs at **http://localhost:8501**

### 5. Login

Use default credentials:
- **Email:** `admin@amalitech.com`
- **Password:** `password123`

## API Endpoints

| Method | Endpoint              | Description          | Auth Required |
| ------ | --------------------- | -------------------- | ------------- |
| POST   | /api/auth/register    | Register new user    | No            |
| POST   | /api/auth/login       | Login and get JWT    | No            |
| GET    | /api/auth/users       | List all users       | Yes           |
| GET    | /api/datasources      | List data sources    | Yes           |
| POST   | /api/datasources      | Create data source   | Yes           |
| GET    | /api/pipelines        | List pipelines       | Yes           |
| POST   | /api/pipelines        | Create pipeline      | Yes           |
| POST   | /api/pipelines/{id}/run | Run pipeline       | Yes           |
| GET    | /actuator/health      | Health check         | No            |
| GET    | /actuator/metrics     | Application metrics  | No            |

### Swagger UI

Available at: **http://localhost:8080/swagger-ui.html**

## Docker Deployment

### Using Docker Compose

```bash
cd Phase1-Group-Projects/6-InsightFlow
docker compose up -d
```

This starts:
- PostgreSQL database (port 5432)
- Spring Boot backend (port 8080)
- Streamlit frontend (port 8501)

### Environment Variables

Create `.env` files in backend and frontend directories. See respective README files for details.

## CI/CD Pipeline

### Continuous Integration (CI)

Triggered on push/PR to `main` or `dev` branches:

1. **Backend Tests**
   - Sets up PostgreSQL service
   - Compiles Java code
   - Runs unit tests

2. **Frontend Tests**
   - Syntax validation
   - Linting with flake8

### Continuous Deployment (CD)

Triggered after successful CI on `main` branch:

1. **Build & Push Images**
   - Builds Docker images for backend and frontend
   - Pushes to Docker Hub with `:latest` and `:commit-sha` tags

2. **Integration Testing**
   - Deploys all services with docker-compose
   - Tests backend health endpoint
   - Tests frontend accessibility
   - Cleans up containers

### GitHub Secrets Required

- `DOCKER_USERNAME` - Docker Hub username
- `DOCKERHUB_TOKEN` - Docker Hub access token
- `APP_DB_USER` - Database username
- `APP_DB_PASSWORD` - Database password
- `APP_JWT_SECRET` - JWT signing secret
- `APP_ACCESS_TOKEN_EXPIRATION` - Token expiry (ms)
- `FRONTEND_COOKIE_NAME` - Cookie name
- `FRONTEND_COOKIE_KEY` - Cookie signing key
- `FRONTEND_COOKIE_EXPIRY_DAYS` - Cookie expiry (days)

## Project Structure

```
Phase1-Group-Projects/6-InsightFlow/
├── backend/
│   ├── src/main/java/com/insightflow/
│   │   ├── config/          # Security, JWT
│   │   ├── controller/      # REST endpoints
│   │   ├── service/         # Business logic
│   │   ├── model/           # JPA entities
│   │   ├── repository/      # Data access
│   │   └── dto/             # Data transfer objects
│   ├── docker/db/           # Database Docker image
│   ├── pom.xml
│   └── README.md
├── frontend/
│   ├── pages/               # Streamlit pages
│   ├── utils/               # Auth helpers
│   ├── app.py               # Main entry point
│   ├── requirements.txt
│   └── README.md
└── docker-compose.yml       # Multi-service orchestration
```

## Development Workflow

1. Create feature branch: `git checkout -b feature/your-feature`
2. Make changes and test locally
3. Run tests:
   - Backend: `./mvnw test`
   - Frontend: `python -m py_compile app.py pages/*.py utils/*.py`
4. Commit and push: `git push origin feature/your-feature`
5. Create Pull Request
6. CI runs automatically
7. Merge to `main` triggers CD

## Documentation

- [Backend README](Phase1-Group-Projects/6-InsightFlow/backend/README.md)
- [Frontend README](Phase1-Group-Projects/6-InsightFlow/frontend/README.md)
- [API Documentation](http://localhost:8080/swagger-ui.html)

## License

MIT
