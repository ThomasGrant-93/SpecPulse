# SpecPulse Makefile
# SpecPulse Project Management

.PHONY: help dev build test clean docker docker-up docker-down logs backend frontend install-deps lint format \
	build-docker docker-login docker-push docker-publish

# ==============================================================================
# Переменные
# ==============================================================================

# Backend
BACKEND_DIR := specpulse-backend
BACKEND_PORT := 8080

# Frontend
FRONTEND_DIR := specpulse-frontend
FRONTEND_PORT := 3000

# Database
DB_PORT := 5432
DB_NAME := specpulse
DB_USER := specpulse
DB_PASSWORD := specpulse

# Docker
COMPOSE_FILE := docker-compose.yml
PROJECT_NAME := specpulse

# Docker publishing
DOCKERHUB_USERNAME ?=
DOCKERHUB_TOKEN ?=
# Full Docker Hub repo name: "<username>/<repository>".
DOCKERHUB_IMAGE ?= $(if $(DOCKERHUB_USERNAME),$(DOCKERHUB_USERNAME)/$(PROJECT_NAME),)
# Tag to build/push.
# Defaults to current branch name (slashes replaced with dashes).
# If detached HEAD, falls back to "local".
DOCKER_TAG ?= $(shell BR=$$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo HEAD); if [ "$$BR" = "HEAD" ] || [ -z "$$BR" ]; then echo local; else echo $$BR | tr '/' '-'; fi)

# Colors for output
COLOR_RESET := \033[0m
COLOR_GREEN := \033[32m
COLOR_YELLOW := \033[33m
COLOR_BLUE := \033[34m
COLOR_RED := \033[31m

# ==============================================================================
# Main Goal
# ==============================================================================

help: ## Show help for available commands
	@echo "$(COLOR_BLUE)SpecPulse - OpenAPI Management System$(COLOR_RESET)"
	@echo ""
	@echo "$(COLOR_YELLOW)Основные команды:$(COLOR_RESET)"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(COLOR_GREEN)%-20s$(COLOR_RESET) %s\n", $$1, $$2}'
	@echo ""
	@echo "$(COLOR_YELLOW)Примеры использования:$(COLOR_RESET)"
	@echo "  make dev              # Запустить backend и frontend для разработки"
	@echo "  make test             # Запустить все тесты"
	@echo "  make build            # Собрать проект"
	@echo "  make docker-up        # Запустить в Docker"

# ==============================================================================
# Development
# ==============================================================================

dev: ## Start backend and frontend for development (in background)
	@echo "$(COLOR_GREEN)Запуск режима разработки...$(COLOR_RESET)"
	@echo "$(COLOR_BLUE)Backend: http://localhost:$(BACKEND_PORT)$(COLOR_RESET)"
	@echo "$(COLOR_BLUE)Frontend: http://localhost:$(FRONTEND_PORT)$(COLOR_RESET)"
	@echo ""
	@# Запуск backend
	@./gradlew :$(BACKEND_DIR):bootRun --quiet &
	@echo "$(COLOR_GREEN)Backend запущен на порту $(BACKEND_PORT)$(COLOR_RESET)"
	@sleep 3
	@# Запуск frontend
	@cd $(FRONTEND_DIR) && npm run dev &
	@echo "$(COLOR_GREEN)Frontend запущен на порту $(FRONTEND_PORT)$(COLOR_RESET)"
	@echo ""
	@echo "$(COLOR_YELLOW)Для остановки: make stop$(COLOR_RESET)"

dev-foreground: ## Start backend and frontend in foreground mode
	@echo "$(COLOR_GREEN)Запуск в foreground режиме...$(COLOR_RESET)"
	@echo "$(COLOR_YELLOW)Нажмите Ctrl+C для остановки$(COLOR_RESET)"
	@cd $(FRONTEND_DIR) && npm run dev &
	@./gradlew :$(BACKEND_DIR):bootRun

stop: ## Stop all running processes
	@echo "$(COLOR_YELLOW)Остановка всех процессов...$(COLOR_RESET)"
	@pkill -f "gradlew.*bootRun" 2>/dev/null || true
	@pkill -f "npm.*dev" 2>/dev/null || true
	@pkill -f "vite" 2>/dev/null || true
	@echo "$(COLOR_GREEN)Все процессы остановлены$(COLOR_RESET)"

backend: ## Run only backend
	@echo "$(COLOR_GREEN)Запуск backend...$(COLOR_RESET)"
	@./gradlew :$(BACKEND_DIR):bootRun

frontend: ## Run only frontend
	@echo "$(COLOR_GREEN)Запуск frontend...$(COLOR_RESET)"
	@cd $(FRONTEND_DIR) && npm run dev

# ==============================================================================
# Build
# ==============================================================================

build: build-frontend build-backend ## Build backend and frontend

build-backend: build-frontend ## Build only backend
	@echo "$(COLOR_GREEN)Сборка backend...$(COLOR_RESET)"
	@./gradlew :$(BACKEND_DIR):build -x test
	@echo "$(COLOR_GREEN)Backend собран: $(BACKEND_DIR)/build/libs/$(COLOR_RESET)"

build-frontend: ## Build only frontend
	@echo "$(COLOR_GREEN)Сборка frontend...$(COLOR_RESET)"
	@cd $(FRONTEND_DIR) && npm run build
	@echo "$(COLOR_GREEN)Frontend собран: $(FRONTEND_DIR)/dist/$(COLOR_RESET)"

build-docker: ## Build Docker image locally
	@echo "$(COLOR_GREEN)Сборка Docker образа...$(COLOR_RESET)"
	@docker build -t $(PROJECT_NAME):$(DOCKER_TAG) .
	@echo "$(COLOR_GREEN)Docker образ собран: $(PROJECT_NAME):$(DOCKER_TAG)$(COLOR_RESET)"

docker-login: ## Log in to Docker Hub (use DOCKERHUB_USERNAME and DOCKERHUB_TOKEN)
	@if [ -z "$(DOCKERHUB_USERNAME)" ] || [ -z "$(DOCKERHUB_TOKEN)" ]; then \
		echo "$(COLOR_RED)Set DOCKERHUB_USERNAME and DOCKERHUB_TOKEN first$(COLOR_RESET)"; \
		exit 1; \
	fi
	@echo "$(COLOR_YELLOW)Logging into Docker Hub...$(COLOR_RESET)"
	@echo "$(DOCKERHUB_TOKEN)" | docker login -u "$(DOCKERHUB_USERNAME)" --password-stdin

docker-push: ## Tag and push the image to Docker Hub
	@if [ -z "$(DOCKERHUB_IMAGE)" ]; then \
		echo "$(COLOR_RED)Set DOCKERHUB_IMAGE (or DOCKERHUB_USERNAME) first$(COLOR_RESET)"; \
		exit 1; \
	fi
	@echo "$(COLOR_GREEN)Pushing $(DOCKERHUB_IMAGE):$(DOCKER_TAG)$(COLOR_RESET)"
	@docker tag $(PROJECT_NAME):$(DOCKER_TAG) $(DOCKERHUB_IMAGE):$(DOCKER_TAG)
	@docker push $(DOCKERHUB_IMAGE):$(DOCKER_TAG)

docker-publish: build-docker docker-login docker-push ## Build and publish image to Docker Hub
	@echo "$(COLOR_GREEN)Docker image published to Docker Hub$(COLOR_RESET)"

# ==============================================================================
# Tests
# ==============================================================================

test: test-backend test-frontend ## Run all tests

test-backend: ## Run backend tests
	@echo "$(COLOR_GREEN)Запуск тестов backend...$(COLOR_RESET)"
	@./gradlew :$(BACKEND_DIR):test

test-frontend: ## Run frontend tests
	@echo "$(COLOR_GREEN)Запуск тестов frontend...$(COLOR_RESET)"
	@cd $(FRONTEND_DIR) && npm test -- --run

test-coverage: test-coverage-backend test-coverage-frontend ## Run tests with coverage

test-coverage-backend: ## Run backend tests with coverage
	@echo "$(COLOR_GREEN)Запуск тестов backend с покрытием...$(COLOR_RESET)"
	@./gradlew :$(BACKEND_DIR):test jacocoTestReport

test-coverage-frontend: ## Run frontend tests with coverage
	@echo "$(COLOR_GREEN)Запуск тестов frontend с покрытием...$(COLOR_RESET)"
	@cd $(FRONTEND_DIR) && npm run test:coverage

test-ui: ## Run frontend tests in UI mode
	@echo "$(COLOR_GREEN)Запуск тестов frontend в UI режиме...$(COLOR_RESET)"
	@cd $(FRONTEND_DIR) && npm run test:ui

# ==============================================================================
# Dependency Installation
# ==============================================================================

install-deps: install-deps-backend install-deps-frontend ## Install all dependencies

install-deps-backend: ## Install backend dependencies
	@echo "$(COLOR_GREEN)Установка зависимостей backend...$(COLOR_RESET)"
	@./gradlew :$(BACKEND_DIR):dependencies

install-deps-frontend: ## Install frontend dependencies
	@echo "$(COLOR_GREEN)Установка зависимостей frontend...$(COLOR_RESET)"
	@cd $(FRONTEND_DIR) && npm install

# ==============================================================================
# Linting and Formatting
# ==============================================================================

lint: lint-backend lint-frontend ## Run linters

lint-backend: ## Run backend linter
	@echo "$(COLOR_GREEN)Линтинг backend...$(COLOR_RESET)"
	@./gradlew :$(BACKEND_DIR):checkstyleMain :$(BACKEND_DIR):checkstyleTest

lint-frontend: ## Run frontend linter
	@echo "$(COLOR_GREEN)Линтинг frontend...$(COLOR_RESET)"
	@cd $(FRONTEND_DIR) && npm run lint

format: format-frontend ## Format code

format-frontend: ## Format frontend code
	@echo "$(COLOR_GREEN)Форматирование frontend...$(COLOR_RESET)"
	@cd $(FRONTEND_DIR) && npx prettier --write "src/**/*.{ts,tsx,js,jsx,css,json}"

# ==============================================================================
# Docker
# ==============================================================================

docker-up: ## Start Docker Compose
	@echo "$(COLOR_GREEN)Запуск Docker Compose...$(COLOR_RESET)"
	@docker-compose -f $(COMPOSE_FILE) up -d --build
	@echo ""
	@echo "$(COLOR_GREEN)Сервисы запущены:$(COLOR_RESET)"
	@echo "  - Backend: http://localhost:$(BACKEND_PORT)"
	@echo "  - Frontend: http://localhost:$(FRONTEND_PORT)"
	@echo "  - Database: localhost:$(DB_PORT)"
	@echo ""
	@echo "$(COLOR_YELLOW)Для остановки: make docker-down$(COLOR_RESET)"

docker-down: ## Stop Docker Compose
	@echo "$(COLOR_YELLOW)Остановка Docker Compose...$(COLOR_RESET)"
	@docker-compose -f $(COMPOSE_FILE) down

docker-logs: ## Show Docker container logs
	@docker-compose -f $(COMPOSE_FILE) logs -f

docker-restart: docker-down docker-up ## Restart Docker Compose

docker-clean: ## Clean Docker resources
	@echo "$(COLOR_YELLOW)Очистка Docker ресурсов...$(COLOR_RESET)"
	@docker-compose -f $(COMPOSE_FILE) down -v
	@docker system prune -f

# ==============================================================================
# Database
# ==============================================================================

db-start: ## Start PostgreSQL
	@echo "$(COLOR_GREEN)Запуск PostgreSQL...$(COLOR_RESET)"
	@docker run --name $(DB_NAME)-db \
		-e POSTGRES_DB=$(DB_NAME) \
		-e POSTGRES_USER=$(DB_USER) \
		-e POSTGRES_PASSWORD=$(DB_PASSWORD) \
		-p $(DB_PORT):5432 \
		-d postgres:15
	@echo "$(COLOR_GREEN)PostgreSQL запущен на порту $(DB_PORT)$(COLOR_RESET)"

db-stop: ## Stop PostgreSQL
	@echo "$(COLOR_YELLOW)Остановка PostgreSQL...$(COLOR_RESET)"
	@docker stop $(DB_NAME)-db && docker rm $(DB_NAME)-db

db-reset: db-stop db-start ## Restart PostgreSQL and clear data

# ==============================================================================
# Cleanup
# ==============================================================================

clean: clean-backend clean-frontend clean-test ## Remove build artifacts

clean-backend: ## Clean backend
	@echo "$(COLOR_YELLOW)Очистка backend...$(COLOR_RESET)"
	@./gradlew :$(BACKEND_DIR):clean

clean-frontend: ## Clean frontend
	@echo "$(COLOR_YELLOW)Очистка frontend...$(COLOR_RESET)"
	@cd $(FRONTEND_DIR) && npm run clean

clean-test: ## Clean test artifacts
	@echo "$(COLOR_YELLOW)Очистка тестовых артефактов...$(COLOR_RESET)"
	@rm -rf $(BACKEND_DIR)/build/test-results
	@rm -rf $(BACKEND_DIR)/build/reports/tests
	@rm -rf $(FRONTEND_DIR)/coverage

# ==============================================================================
# Status Check
# ==============================================================================

status: ## Check status of services
	@echo "$(COLOR_BLUE)Проверка статуса сервисов...$(COLOR_RESET)"
	@echo ""
	@echo "$(COLOR_YELLOW)Backend (порт $(BACKEND_PORT)):$(COLOR_RESET)"
	@curl -s http://localhost:$(BACKEND_PORT)/actuator/health > /dev/null 2>&1 && \
		echo "  $(COLOR_GREEN)✓ Работает$(COLOR_RESET)" || \
		echo "  $(COLOR_RED)✗ Не работает$(COLOR_RESET)"
	@echo ""
	@echo "$(COLOR_YELLOW)Frontend (порт $(FRONTEND_PORT)):$(COLOR_RESET)"
	@curl -s http://localhost:$(FRONTEND_PORT) > /dev/null 2>&1 && \
		echo "  $(COLOR_GREEN)✓ Работает$(COLOR_RESET)" || \
		echo "  $(COLOR_RED)✗ Не работает$(COLOR_RESET)"
	@echo ""
	@echo "$(COLOR_YELLOW)Database (порт $(DB_PORT)):$(COLOR_RESET)"
	@docker ps | grep -q $(DB_NAME)-db && \
		echo "  $(COLOR_GREEN)✓ Работает$(COLOR_RESET)" || \
		echo "  $(COLOR_YELLOW)- Не запущен в Docker$(COLOR_RESET)"

# ==============================================================================
# API Utilities
# ==============================================================================

api-docs: ## Open Swagger UI
	@echo "$(COLOR_GREEN)Открытие Swagger UI...$(COLOR_RESET)"
	@xdg-open http://localhost:$(BACKEND_PORT)/swagger-ui.html 2>/dev/null || \
		open http://localhost:$(BACKEND_PORT)/swagger-ui.html 2>/dev/null || \
		echo "Откройте http://localhost:$(BACKEND_PORT)/swagger-ui.html в браузере"

api-services: ## Get list of services
	@echo "$(COLOR_GREEN)Список сервисов:$(COLOR_RESET)"
	@curl -s http://localhost:$(BACKEND_PORT)/api/v1/registry | jq '.' 2>/dev/null || \
		curl -s http://localhost:$(BACKEND_PORT)/api/v1/registry

# ==============================================================================
# CI/CD
# ==============================================================================

ci: install-deps lint test build ## Full CI pipeline

pr: clean ci ## Prepare PR (cleanup + CI)
