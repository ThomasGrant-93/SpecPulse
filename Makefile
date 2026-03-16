# SpecPulse Makefile
# Управление проектом SpecPulse

.PHONY: help dev build test clean docker docker-up docker-down logs backend frontend install-deps lint format

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

# Colors for output
COLOR_RESET := \033[0m
COLOR_GREEN := \033[32m
COLOR_YELLOW := \033[33m
COLOR_BLUE := \033[34m
COLOR_RED := \033[31m

# ==============================================================================
# Главная цель
# ==============================================================================

help: ## Показать справку по доступным командам
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
# Разработка
# ==============================================================================

dev: ## Запустить backend и frontend для разработки (в фоне)
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

dev-foreground: ## Запустить backend и frontend в foreground режиме
	@echo "$(COLOR_GREEN)Запуск в foreground режиме...$(COLOR_RESET)"
	@echo "$(COLOR_YELLOW)Нажмите Ctrl+C для остановки$(COLOR_RESET)"
	@cd $(FRONTEND_DIR) && npm run dev &
	@./gradlew :$(BACKEND_DIR):bootRun

stop: ## Остановить все запущенные процессы
	@echo "$(COLOR_YELLOW)Остановка всех процессов...$(COLOR_RESET)"
	@pkill -f "gradlew.*bootRun" 2>/dev/null || true
	@pkill -f "npm.*dev" 2>/dev/null || true
	@pkill -f "vite" 2>/dev/null || true
	@echo "$(COLOR_GREEN)Все процессы остановлены$(COLOR_RESET)"

backend: ## Запустить только backend
	@echo "$(COLOR_GREEN)Запуск backend...$(COLOR_RESET)"
	@./gradlew :$(BACKEND_DIR):bootRun

frontend: ## Запустить только frontend
	@echo "$(COLOR_GREEN)Запуск frontend...$(COLOR_RESET)"
	@cd $(FRONTEND_DIR) && npm run dev

# ==============================================================================
# Сборка
# ==============================================================================

build: build-backend build-frontend ## Собрать backend и frontend

build-backend: ## Собрать только backend
	@echo "$(COLOR_GREEN)Сборка backend...$(COLOR_RESET)"
	@./gradlew :$(BACKEND_DIR):build -x test
	@echo "$(COLOR_GREEN)Backend собран: $(BACKEND_DIR)/build/libs/$(COLOR_RESET)"

build-frontend: ## Собрать только frontend
	@echo "$(COLOR_GREEN)Сборка frontend...$(COLOR_RESET)"
	@cd $(FRONTEND_DIR) && npm run build
	@echo "$(COLOR_GREEN)Frontend собран: $(FRONTEND_DIR)/dist/$(COLOR_RESET)"

build-docker: ## Собрать Docker образ
	@echo "$(COLOR_GREEN)Сборка Docker образа...$(COLOR_RESET)"
	@docker build -t $(PROJECT_NAME):latest .
	@echo "$(COLOR_GREEN)Docker образ собран: $(PROJECT_NAME):latest$(COLOR_RESET)"

# ==============================================================================
# Тесты
# ==============================================================================

test: test-backend test-frontend ## Запустить все тесты

test-backend: ## Запустить тесты backend
	@echo "$(COLOR_GREEN)Запуск тестов backend...$(COLOR_RESET)"
	@./gradlew :$(BACKEND_DIR):test

test-frontend: ## Запустить тесты frontend
	@echo "$(COLOR_GREEN)Запуск тестов frontend...$(COLOR_RESET)"
	@cd $(FRONTEND_DIR) && npm test -- --run

test-coverage: test-coverage-backend test-coverage-frontend ## Запустить тесты с покрытием

test-coverage-backend: ## Запустить тесты backend с покрытием
	@echo "$(COLOR_GREEN)Запуск тестов backend с покрытием...$(COLOR_RESET)"
	@./gradlew :$(BACKEND_DIR):test jacocoTestReport

test-coverage-frontend: ## Запустить тесты frontend с покрытием
	@echo "$(COLOR_GREEN)Запуск тестов frontend с покрытием...$(COLOR_RESET)"
	@cd $(FRONTEND_DIR) && npm run test:coverage

test-ui: ## Запустить тесты frontend в UI режиме
	@echo "$(COLOR_GREEN)Запуск тестов frontend в UI режиме...$(COLOR_RESET)"
	@cd $(FRONTEND_DIR) && npm run test:ui

# ==============================================================================
# Установка зависимостей
# ==============================================================================

install-deps: install-deps-backend install-deps-frontend ## Установить все зависимости

install-deps-backend: ## Установить зависимости backend
	@echo "$(COLOR_GREEN)Установка зависимостей backend...$(COLOR_RESET)"
	@./gradlew :$(BACKEND_DIR):dependencies

install-deps-frontend: ## Установить зависимости frontend
	@echo "$(COLOR_GREEN)Установка зависимостей frontend...$(COLOR_RESET)"
	@cd $(FRONTEND_DIR) && npm install

# ==============================================================================
# Линтинг и форматирование
# ==============================================================================

lint: lint-backend lint-frontend ## Запустить линтеры

lint-backend: ## Запустить линтер backend
	@echo "$(COLOR_GREEN)Линтинг backend...$(COLOR_RESET)"
	@./gradlew :$(BACKEND_DIR):checkstyleMain :$(BACKEND_DIR):checkstyleTest

lint-frontend: ## Запустить линтер frontend
	@echo "$(COLOR_GREEN)Линтинг frontend...$(COLOR_RESET)"
	@cd $(FRONTEND_DIR) && npm run lint

format: format-frontend ## Отформатировать код

format-frontend: ## Отформатировать frontend код
	@echo "$(COLOR_GREEN)Форматирование frontend...$(COLOR_RESET)"
	@cd $(FRONTEND_DIR) && npx prettier --write "src/**/*.{ts,tsx,js,jsx,css,json}"

# ==============================================================================
# Docker
# ==============================================================================

docker-up: ## Запустить Docker Compose
	@echo "$(COLOR_GREEN)Запуск Docker Compose...$(COLOR_RESET)"
	@docker-compose -f $(COMPOSE_FILE) up -d --build
	@echo ""
	@echo "$(COLOR_GREEN)Сервисы запущены:$(COLOR_RESET)"
	@echo "  - Backend: http://localhost:$(BACKEND_PORT)"
	@echo "  - Frontend: http://localhost:$(FRONTEND_PORT)"
	@echo "  - Database: localhost:$(DB_PORT)"
	@echo ""
	@echo "$(COLOR_YELLOW)Для остановки: make docker-down$(COLOR_RESET)"

docker-down: ## Остановить Docker Compose
	@echo "$(COLOR_YELLOW)Остановка Docker Compose...$(COLOR_RESET)"
	@docker-compose -f $(COMPOSE_FILE) down

docker-logs: ## Показать логи Docker контейнеров
	@docker-compose -f $(COMPOSE_FILE) logs -f

docker-restart: docker-down docker-up ## Перезапустить Docker Compose

docker-clean: ## Очистить Docker ресурсы
	@echo "$(COLOR_YELLOW)Очистка Docker ресурсов...$(COLOR_RESET)"
	@docker-compose -f $(COMPOSE_FILE) down -v
	@docker system prune -f

# ==============================================================================
# База данных
# ==============================================================================

db-start: ## Запустить PostgreSQL
	@echo "$(COLOR_GREEN)Запуск PostgreSQL...$(COLOR_RESET)"
	@docker run --name $(DB_NAME)-db \
		-e POSTGRES_DB=$(DB_NAME) \
		-e POSTGRES_USER=$(DB_USER) \
		-e POSTGRES_PASSWORD=$(DB_PASSWORD) \
		-p $(DB_PORT):5432 \
		-d postgres:15
	@echo "$(COLOR_GREEN)PostgreSQL запущен на порту $(DB_PORT)$(COLOR_RESET)"

db-stop: ## Остановить PostgreSQL
	@echo "$(COLOR_YELLOW)Остановка PostgreSQL...$(COLOR_RESET)"
	@docker stop $(DB_NAME)-db && docker rm $(DB_NAME)-db

db-reset: db-stop db-start ## Перезапустить PostgreSQL с очисткой данных

# ==============================================================================
# Очистка
# ==============================================================================

clean: clean-backend clean-frontend clean-test ## Очистить все артефакты сборки

clean-backend: ## Очистить backend
	@echo "$(COLOR_YELLOW)Очистка backend...$(COLOR_RESET)"
	@./gradlew :$(BACKEND_DIR):clean

clean-frontend: ## Очистить frontend
	@echo "$(COLOR_YELLOW)Очистка frontend...$(COLOR_RESET)"
	@cd $(FRONTEND_DIR) && npm run clean

clean-test: ## Очистить тестовые артефакты
	@echo "$(COLOR_YELLOW)Очистка тестовых артефактов...$(COLOR_RESET)"
	@rm -rf $(BACKEND_DIR)/build/test-results
	@rm -rf $(BACKEND_DIR)/build/reports/tests
	@rm -rf $(FRONTEND_DIR)/coverage

# ==============================================================================
# Проверка статуса
# ==============================================================================

status: ## Проверить статус сервисов
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
# API утилиты
# ==============================================================================

api-docs: ## Открыть Swagger UI
	@echo "$(COLOR_GREEN)Открытие Swagger UI...$(COLOR_RESET)"
	@xdg-open http://localhost:$(BACKEND_PORT)/swagger-ui.html 2>/dev/null || \
		open http://localhost:$(BACKEND_PORT)/swagger-ui.html 2>/dev/null || \
		echo "Откройте http://localhost:$(BACKEND_PORT)/swagger-ui.html в браузере"

api-services: ## Получить список сервисов
	@echo "$(COLOR_GREEN)Список сервисов:$(COLOR_RESET)"
	@curl -s http://localhost:$(BACKEND_PORT)/api/v1/registry | jq '.' 2>/dev/null || \
		curl -s http://localhost:$(BACKEND_PORT)/api/v1/registry

# ==============================================================================
# CI/CD
# ==============================================================================

ci: install-deps lint test build ## Полный CI пайплайн

pr: clean ci ## Подготовка к PR (очистка + CI)
