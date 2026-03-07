# InsightFlow Backend

A Spring Boot backend for InsightFlow — a multi-channel data pipeline and insight reporting platform for ShopSmart Ghana.

---

## Tech Stack

- Java 25
- Spring Boot 4.0.3
- PostgreSQL 16
- Spring Security + JWT
- Spring RestClient (HTTP client for external API integration)
- SpringDoc OpenAPI / Swagger UI
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
git clone  https://github.com/eli-bigman/GP_T-15_Instaflow.git
cd GP_T-15_Instaflow/Phase1-Group-Projects/6-InsightFlow/backend
```

### 2. Set Up Environment Variables

Copy the example env file and fill in the values:

```bash
cp .env.example .env
cp .env.docker.example .env
```

Open `.env` and `.env.docker` and fill in your values:

```properties
# Database
APP_DB_URL=jdbc:postgresql://localhost:5432/insightflow_app
APP_DB_USER=insightflow
APP_DB_PASSWORD=insightflow_pass

# JWT
APP_JWT_SECRET=your_generated_secret_here
APP_ACCESS_TOKEN_EXPIRATION=86400000
APP_REFRESH_TOKEN_EXPIRATION=604800000 # Will only be use when we decide to add OAuth

# Google OAuth
# Will only be use when we decide to add OAuth
APP_GOOGLE_CLIENT_ID=your_google_client_id
APP_GOOGLE_CLIENT_SECRET=your_google_client_secre++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++t
APP_REDIRECT_URL=http://localhost:3000/login
```

> **Note:** Never commit your `.env` file to Git. It is already in `.gitignore`.

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
#OR
docker compose up -d pgadmin # Use this if you're using the current version of docker installation
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
./mvn spring-boot:run 
#OR
./mvnw spring-boot:run #Make sure mvnw is present
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
│   │   ├── config/                     # Security (JWT filter, SecurityConfig)
│   │   ├── controller/
│   │   │   ├── AuthController          # POST /api/auth/**
│   │   │   ├── DataSourceController    # CRUD for data source configurations
│   │   │   ├── OrderFeedController     # Orders sync + push endpoints
│   │   │   ├── FeedbackWebhookController  # Feedback sync + push endpoints
│   │   │   └── ProductCatalogueController # Products sync + push endpoints
│   │   ├── model/
│   │   │   ├── enums/
│   │   │   │   ├── OrderStatus         # PENDING | FULFILLED | CANCELLED | RETURNED
│   │   │   │   ├── FeedbackCategory    # Delivery Speed | Product Quality | App Experience | Packaging | Overall
│   │   │   │   ├── DataSourceType      # API | JSON | CSV | DATABASE
│   │   │   │   └── SourceType          # API | JSON | CSV | DATABASE
│   │   │   ├── Order                   # orders table — PII-masked before staging
│   │   │   ├── OrderItem               # order_items table — cascaded from Order
│   │   │   ├── FeedbackSubmission      # feedback_submissions table
│   │   │   ├── ProductCatalogue        # product_catalogue table
│   │   │   └── DataSource              # data_sources table — ingestion audit log
│   │   ├── dto/
│   │   │   ├── OrderDto                # Maps external API order JSON (order_items array)
│   │   │   ├── OrderItemDto            # Maps nested order_items inside orders
│   │   │   ├── FeedbackSubmissionDto   # Maps external API feedback JSON
│   │   │   └── ProductCatalogueDto     # Maps external API product JSON
│   │   ├── repository/
│   │   │   ├── OrderRepository
│   │   │   ├── OrderItemRepository
│   │   │   ├── FeedbackSubmissionRepository
│   │   │   ├── ProductCatalogueRepository
│   │   │   └── DataSourceRepository
│   │   ├── service/
│   │   │   ├── ApiFeedService          # Orders ingestion pipeline (pull + push)
│   │   │   ├── FeedbackIngestionService # Feedback ingestion pipeline (pull + push)
│   │   │   ├── ProductCatalogueService # Product catalogue ingestion pipeline (pull + push)
│   │   │   ├── StandardizationService  # DTO → entity mapping, validation, PII masking
│   │   │   └── DataSourceService       # Data source CRUD + ingestion audit recording
│   │   └── exception/                  # AppException, GlobalExceptionHandler
│   └── resources/
│       └── application.properties
docker/
└── db/
    ├── Dockerfile          # Custom postgres image with schemas
    └── init.sql            # Database schema definitions
```

---

## Online Channel Integration (E-Commerce API Connector)

This module ingests orders, feedback, and product data from the ShopSmart external API into the staging PostgreSQL database.

### Data Sources

| Source | External Endpoint | Type | Schedule |
|--------|------------------|------|----------|
| Orders | `GET /api/orders` | API | Daily 01:00 UTC |
| Order Items | Embedded in orders JSON | JSON | With orders |
| Feedback | `GET /api/feedback` | API | Daily 01:05 UTC |
| Products | `GET /api/products` | API | Daily 01:10 UTC |

### Sync Endpoints (Pull)

All sync endpoints are public (no JWT required) and can be triggered manually via Swagger.

**Orders**
```
POST /api/v1/integration/orders/sync/all
POST /api/v1/integration/orders/sync/date/{date}     # e.g. 2026-02-19
POST /api/v1/integration/orders/sync/day/{day}       # e.g. tuesday
```

**Feedback**
```
POST /api/v1/ingestion/feedback/sync/all
POST /api/v1/ingestion/feedback/sync/date/{date}
POST /api/v1/ingestion/feedback/sync/day/{day}
```

**Products**
```
POST /api/v1/integration/products/sync/all
POST /api/v1/integration/products/sync/date/{date}
POST /api/v1/integration/products/sync/day/{day}
```

### Push Endpoints (Webhook)

The external ShopSmart server sends real-time push notifications to these endpoints when new records are created.

```
POST /api/feed/orders            # Receives a single order JSON
POST /api/v1/ingestion/feedback  # Receives a single feedback JSON
POST /api/v1/ingestion/products  # Receives a single product JSON
```

### Pipeline Overview

```
External API
    │
    ▼
fetchOrderList / fetchFeedbackList / fetchProductList
    │   (flexible JSON parser — handles plain arrays and wrapped objects)
    ▼
Validation & Standardization
    │   • Required field checks
    │   • Region / payment method validation (orders)
    │   • Rating range check 1–5 (feedback)
    │   • Category normalization (feedback)
    │   • Orphan check — feedback must reference a staged order
    ▼
Deduplication
    │   existsByOrderId / existsByFeedbackId / existsBySku
    ▼
PII Masking (orders only)
    │   SHA-256 hash on customer_id and delivery_address
    ▼
Persist to Staging DB
    │   Order → cascades to OrderItems
    ▼
recordIngestion → data_sources table
```

---

## Docker Setup (Full)

To run both the app and database in Docker:

```bash
docker-compose up -d
#OR
docker compose up -d # Use this if you're using the current version of docker installation
```

To stop everything:

```bash
docker-compose down
#OR
docker compose down
```

To view logs:

```bash
docker-compose logs -f app
#OR
docker compose logs -f app
```

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
docker-compose down #OR docker compose down
docker-compose up -d db #OR docker compose up -d db
```

---

## Environment Variables Reference

| Variable | Description |
|---|---|
| `APP_DB_URL` | PostgreSQL JDBC connection URL |
| `APP_DB_USER` | PostgreSQL username |
| `APP_DB_PASSWORD` | PostgreSQL password |
| `APP_JWT_SECRET` | Base64-encoded JWT signing secret |
| `APP_ACCESS_TOKEN_EXPIRATION` | Access token expiry in milliseconds |
| `APP_REFRESH_TOKEN_EXPIRATION` | Refresh token expiry in milliseconds |
| `APP_GOOGLE_CLIENT_ID` | Google OAuth client ID |
| `APP_GOOGLE_CLIENT_SECRET` | Google OAuth client secret |
| `APP_REDIRECT_URL` | OAuth redirect URL after login |
| `PGADMIN_EMAIL` | pgAdmin login email |
| `PGADMIN_PASSWORD` | pgAdmin login password |

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

**Feedback sync returns 0 saved / all orphans**
- Run orders sync first (`POST /api/v1/integration/orders/sync/all`) — feedback requires parent orders to exist in the staging DB

---

## Contributing

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Commit your changes: `git commit -m "Add your feature"`
3. Push to the branch: `git push origin feature/your-feature`
4. Open a Pull Request