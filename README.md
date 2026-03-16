# SpecPulse

[![Build](https://img.shields.io/github/actions/workflow/status/your-org/specpulse/ci.yml?branch=main)](https://github.com/your-org/specpulse/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17-blue)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61dafb)](https://react.dev/)

**OpenAPI specification management system with pull-model architecture**

SpecPulse automatically fetches, stores, compares, and tests OpenAPI specifications from registered services. It helps
teams track API changes, detect breaking changes, and maintain API documentation across multiple microservices.

![SpecPulse Dashboard](docs/images/dashboard.png)

## ✨ Features

- **📋 Service Registry** — Register and manage services with OpenAPI endpoints
- **🔄 Automated Pull** — Scheduled fetching of OpenAPI specifications (configurable interval)
- **📦 Version Storage** — Store and track spec versions with SHA-256 fingerprints
- **🔍 Diff Engine** — Compare versions and detect breaking changes using openapi-diff
- **🧪 API Testing** — Generate and run tests from OpenAPI specs
- **📊 Audit Log** — Track all system events and changes
- **👥 Service Groups** — Hierarchical grouping (environments, domains, teams)
- **🌐 Web UI** — Modern React 19 + TypeScript frontend

## 🏗️ Architecture

```
┌─────────────────┐      ┌──────────────────┐      ┌─────────────────┐
│   Frontend      │      │    Backend       │      │   PostgreSQL    │
│  React 19 + TS  │◄────►│ Spring Boot 3.4  │◄────►│     Database    │
│   (Port 3000)   │      │   (Port 8080)    │      │   (Port 5432)   │
└─────────────────┘      └──────────────────┘      └─────────────────┘
                                │
                                ▼
                        ┌──────────────────┐
                        │  External APIs   │
                        │  (OpenAPI Specs) │
                        └──────────────────┘
```

## 📦 Tech Stack

### Backend

| Technology              | Version | Purpose               |
|-------------------------|---------|-----------------------|
| **Java**                | 17      | Core language         |
| **Spring Boot**         | 3.4.3   | Application framework |
| **PostgreSQL**          | 15      | Database              |
| **Flyway**              | 11.4.0  | Database migrations   |
| **OpenAPI Parser**      | 2.1.25  | OpenAPI validation    |
| **openapi-diff**        | 2.1.0   | Diff engine           |
| **Quartz**              | -       | Scheduling            |
| **Apache HttpClient 5** | 5.4.3   | HTTP client           |
| **Testcontainers**      | 1.20.5  | Integration tests     |

### Frontend

| Technology         | Version | Purpose      |
|--------------------|---------|--------------|
| **React**          | 19.0.0  | UI framework |
| **TypeScript**     | 5.7.2   | Type safety  |
| **Vite**           | 6.1.0   | Build tool   |
| **TailwindCSS**    | 3.4.17  | Styling      |
| **TanStack Query** | 5.66.0  | Server state |
| **Axios**          | 1.7.9   | HTTP client  |
| **React Router**   | 7.1.5   | Routing      |
| **Vitest**         | 3.0.5   | Testing      |

## 🚀 Quick Start

### Prerequisites

- **Java 17+** (OpenJDK or Oracle JDK)
- **Node.js 18+** and npm
- **Docker** and **Docker Compose** (for database)
- **Git**

### Option 1: Using Makefile (Recommended)

```bash
# Clone the repository
git clone https://github.com/ThomasGrant-93/specpulse.git
cd specpulse

# Start PostgreSQL database
make db-up

# Start development servers (backend + frontend)
make dev

# Open browser
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### Option 2: Manual Setup

```bash
# 1. Start PostgreSQL
docker run --name specpulse-db \
  -e POSTGRES_DB=specpulse \
  -e POSTGRES_USER=specpulse \
  -e POSTGRES_PASSWORD=specpulse \
  -p 5432:5432 -d postgres:15

# 2. Run backend (port 8080)
./gradlew :specpulse-backend:bootRun

# 3. Run frontend (port 3000)
cd specpulse-frontend
npm install
npm run dev
```

### Option 3: Docker Compose (Production-like)

```bash
# Build and run all services
docker-compose up -d --build

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

## 📖 Usage

### 1. Register a Service

```bash
curl -X POST http://localhost:8080/api/v1/registry \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my-service",
    "openApiUrl": "https://api.example.com/openapi.json",
    "description": "My awesome service",
    "enabled": true
  }'
```

### 2. Pull OpenAPI Spec

```bash
# Pull single service
curl -X POST http://localhost:8080/api/v1/pull/service/1

# Pull all services
curl -X POST http://localhost:8080/api/v1/pull/all
```

### 3. View Version History

```bash
curl http://localhost:8080/api/v1/versions/service/1
```

### 4. Check for Breaking Changes

```bash
curl http://localhost:8080/api/v1/diffs/service/1
```

## 🔧 Configuration

### Environment Variables

| Variable                                    | Default                                      | Description             |
|---------------------------------------------|----------------------------------------------|-------------------------|
| `SPRING_DATASOURCE_URL`                     | `jdbc:postgresql://localhost:5432/specpulse` | Database URL            |
| `SPRING_DATASOURCE_USERNAME`                | `specpulse`                                  | Database user           |
| `SPRING_DATASOURCE_PASSWORD`                | `specpulse`                                  | Database password       |
| `SPECPULSE_SCHEDULER_ENABLED`               | `true`                                       | Enable scheduler        |
| `SPECPULSE_SCHEDULER_PULL_INTERVAL_SECONDS` | `300`                                        | Pull interval (seconds) |
| `LOGGING_LEVEL_ROOT`                        | `INFO`                                       | Root log level          |
| `LOGGING_LEVEL_COM_SPECPULSE`               | `DEBUG`                                      | App log level           |

### Application Settings

Access application settings via API:

```bash
# Get all settings
curl http://localhost:8080/api/v1/settings

# Get category settings
curl http://localhost:8080/api/v1/settings/scheduler

# Update setting
curl -X PUT http://localhost:8080/api/v1/settings/scheduler/pull_interval \
  -H "Content-Type: application/json" \
  -d '{"value": 600}'
```

## 🧪 Testing

```bash
# Run all tests
make test

# Backend tests
make test-backend

# Frontend tests
make test-frontend

# With coverage
make test-coverage

# Interactive UI (frontend)
make test-ui
```

## 📊 API Endpoints

### Registry (`/api/v1/registry`)

| Method | Endpoint             | Description       |
|--------|----------------------|-------------------|
| GET    | `/registry`          | List all services |
| GET    | `/registry/{id}`     | Get service by ID |
| POST   | `/registry`          | Create service    |
| PUT    | `/registry/{id}`     | Update service    |
| DELETE | `/registry/{id}`     | Delete service    |
| GET    | `/registry/search`   | Search services   |
| POST   | `/registry/validate` | Validate OpenAPI  |

### Versions (`/api/v1/versions`)

| Method | Endpoint                        | Description          |
|--------|---------------------------------|----------------------|
| GET    | `/versions/service/{id}`        | Get service versions |
| GET    | `/versions/service/{id}/latest` | Get latest version   |
| GET    | `/versions/{id}`                | Get version by ID    |

### Diffs (`/api/v1/diffs`)

| Method | Endpoint              | Description            |
|--------|-----------------------|------------------------|
| GET    | `/diffs/service/{id}` | Get diff history       |
| GET    | `/diffs/{id}`         | Get diff by ID         |
| POST   | `/diffs/compare`      | Compare specifications |

### Groups (`/api/v1/groups`)

| Method | Endpoint                | Description           |
|--------|-------------------------|-----------------------|
| GET    | `/groups`               | Get all groups        |
| POST   | `/groups`               | Create group          |
| PUT    | `/groups/{id}`          | Update group          |
| DELETE | `/groups/{id}`          | Delete group          |
| POST   | `/groups/{id}/services` | Add services to group |

For full API documentation, see [Swagger UI](http://localhost:8080/swagger-ui.html).

## 📁 Project Structure

```
specpulse/
├── specpulse-backend/       # Spring Boot backend
│   ├── src/main/java/
│   │   └── com/specpulse/
│   │       ├── api/         # REST controllers
│   │       ├── service/     # Business logic
│   │       ├── repository/  # JPA repositories
│   │       ├── entity/      # JPA entities
│   │       ├── parser/      # OpenAPI parser
│   │       ├── diff/        # Diff engine
│   │       └── scheduler/   # Quartz scheduler
│   ├── src/main/resources/
│   │   └── db/migration/    # Flyway migrations
│   └── src/test/            # Tests
│
├── specpulse-frontend/      # React frontend
│   ├── src/
│   │   ├── components/      # React components
│   │   ├── pages/           # Page components
│   │   ├── hooks/           # Custom hooks
│   │   └── services/        # API clients
│   └── src/**/*.test.tsx    # Tests
│
├── docker-compose.yml       # Docker Compose config
├── Dockerfile              # Multi-stage build
└── Makefile                # Build automation
```

## 🤝 Contributing

We welcome contributions! Here's how you can help:

### Getting Started

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Run tests (`make test`)
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

### Development Guidelines

- Follow existing code style (Google Java Style / ESLint + Prettier)
- Write tests for new features
- Update documentation as needed
- Use conventional commits

### Code Review Checklist

- [ ] Tests added/updated
- [ ] Linting passes (`make lint`)
- [ ] No `console.log` in production code
- [ ] Specific exception handling
- [ ] TypeScript types (no `any`)

## 📄 License

This project is licensed under the [MIT License](LICENSE).

## 🙏 Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot)
- [React](https://react.dev/)
- [OpenAPI Specification](https://www.openapis.org/)
- [openapi-diff](https://github.com/OpenAPITools/openapi-diff)
- [Testcontainers](https://www.testcontainers.org/)

## 📞 Support

- **Documentation:** [Swagger UI](http://localhost:8080/swagger-ui.html)
- **Issues:** [GitHub Issues](https://github.com/your-org/specpulse/issues)
- **Discussions:** [GitHub Discussions](https://github.com/your-org/specpulse/discussions)
