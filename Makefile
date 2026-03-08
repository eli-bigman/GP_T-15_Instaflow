COMPOSE_FILE = docker-compose.dataeng.yml
DATA_ENG_DIR = Phase1-Group-Projects/6-InsightFlow/data-engineering

.PHONY: up down restart logs ps build clean insightflow-up

# This will start the source db first, then data eng pipeline
source-db-up:
	@echo "Starting InsightFlow..."
	cd Phase1-Group-Projects/6-InsightFlow/backend && docker compose up -d db
	@echo "Waiting for InsightFlow network to be ready..."
	@sleep 5

# Start data eng pipeline
up: source-db-up
	@echo "Starting Data Engineering Pipeline..."
	cd $(DATA_ENG_DIR) && docker compose -f $(COMPOSE_FILE) up -d
	@echo "Pipeline is up!"

# Stop data eng pipeline
down:
	@echo "Stopping Data Engineering Pipeline..."
	cd $(DATA_ENG_DIR) && docker compose -f $(COMPOSE_FILE) down

# Stop and remove volumes
clean:
	@echo "Stopping and removing volumes..."
	cd $(DATA_ENG_DIR) && docker compose -f $(COMPOSE_FILE) down -v
	@echo "Clean complete!"

# Restart
restart: down up

# Rebuild images
build:
	@echo "Rebuilding images..."
	cd $(DATA_ENG_DIR) && docker compose -f $(COMPOSE_FILE) up -d --build

# View all logs
logs:
	cd $(DATA_ENG_DIR) && docker compose -f $(COMPOSE_FILE) logs -f

# View logs for specific service — usage: make logs-service s=airflow-scheduler
logs-service:
	cd $(DATA_ENG_DIR) && docker compose -f $(COMPOSE_FILE) logs -f $(s)

# Check running containers
ps:
	cd $(DATA_ENG_DIR) && docker compose -f $(COMPOSE_FILE) ps

# Stop insightflow
insightflow-down:
	@echo "Stopping InsightFlow..."
	cd insightflow && docker compose down