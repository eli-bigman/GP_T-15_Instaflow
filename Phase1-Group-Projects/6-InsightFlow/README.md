# InsightFlow - Multi-Source Business Intelligence Pipeline

## Overview

InsightFlow is a multi-source BI pipeline for retail companies.
It ingests data from POS systems (CSV), online stores (API feeds), and internal databases,
then transforms and loads it into a star-schema data warehouse for analytics and reporting.

## Architecture & Data Flow

```
POS(CSV) + OnlineStore(API) + InternalDB(JDBC)
         |              |              |
         v              v              v
   Spring Boot Backend (port 8080)
   FileUpload + ApiFeed + DBConnector(TODO)
            |         |         |
            v         v         v
         Validation Service
                |
   Staging Area (postgres-app:5432)
                |
   ETL Pipeline (Python)
   Extract -> Transform -> Load
                |
   Data Warehouse (postgres-warehouse:5433)
   fact_sales, fact_feedback, dim_product,
   dim_customer, dim_store, dim_date
                |
   Streamlit Reporting Dashboard
```

## Role Split

### Java Backend Developers (2-3)
| Developer | Responsibility | Key Files |
|-----------|---------------|-----------|
| **Dev A** | File ingestion, CSV parsing, validation | FileIngestionService, ValidationService, FileUploadController |
| **Dev B** | API feed connectors, data standardization | ApiFeedService, StandardizationService, OrderFeedController |
| **Dev C** | Pipeline orchestration, auth, metrics API | AuthService, PipelineService, MetricsService, controllers |

### Data Engineers (2-3)
| Engineer | Responsibility | Key Files |
|----------|---------------|-----------|
| **DE 1** | Warehouse schema, ETL pipeline core | schema.sql, etl_pipeline.py, create_warehouse.py |
| **DE 2** | Data transformations, quality checks | transformations.py, quality_checks.py, quality_scoring.py |
| **DE 3** | Business metrics, reporting dashboard | business_metrics.py, reporting_dashboard.py |

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 21+ / Maven 3.9+
- Python 3.10+

### 1. Start Infrastructure
```bash
docker-compose up -d
```

### 2. Run Backend
```bash
cd backend && mvn spring-boot:run
```

### 3. Set Up Data Engineering
```bash
cd data-engineering
pip install -r requirements.txt
python warehouse/create_warehouse.py
python sample_data/generate_data.py
```

### 4. Run Dashboard
```bash
streamlit run data-engineering/dashboards/reporting_dashboard.py
```

## API Endpoints

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | /api/auth/register | Register new user | Implemented |
| POST | /api/auth/login | Login and get JWT | Implemented |
| POST | /api/ingest/csv | Upload CSV file | Implemented |
| POST | /api/ingest/json | Upload JSON data | Stub |
| GET | /api/ingest/jobs | List ingestion jobs | Implemented |
| GET | /api/ingest/jobs/{id} | Get job details | Implemented |
| POST | /api/feed/orders | Receive order feed | Stub |
| POST | /api/feed/inventory | Receive inventory updates | Stub |
| POST | /api/pipeline/trigger | Trigger ETL pipeline | Stub |
| GET | /api/pipeline/status | Get pipeline status | Stub |
| GET | /api/pipeline/history | Get run history | Stub |
| GET | /api/metrics/revenue?days=30 | Daily revenue data | Stub |
| GET | /api/metrics/top-products?limit=10 | Top selling products | Stub |
| GET | /api/metrics/satisfaction | Customer satisfaction | Stub |
| GET | /api/metrics/inventory | Inventory turnover | Stub |

### Swagger UI
Available at: http://localhost:8080/swagger-ui.html
