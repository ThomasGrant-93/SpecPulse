# Contributing to SpecPulse

Thank you for your interest in contributing to SpecPulse! This document provides guidelines and instructions for
contributing.

## 🎯 How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check existing issues. When creating a bug report, include:

- **Clear title and description**
- **Steps to reproduce** the behavior
- **Expected vs actual behavior**
- **Screenshots** if applicable
- **Environment details** (OS, Java version, Node version)

**Example:**

```markdown
**Bug Summary**
Service group assignment fails when creating a new service

**Steps to Reproduce**

1. Go to 'Add Service'
2. Fill in service details
3. Select a group from dropdown
4. Click 'Save'

**Expected:** Service created with group assignment
**Actual:** Error: "Group not found"

**Environment:**

- OS: Ubuntu 22.04
- Java: OpenJDK 17.0.5
- Node: 18.16.0
```

### Suggesting Features

Feature suggestions are welcome! Please provide:

- **Use case** - Why is this feature needed?
- **Proposed solution** - How should it work?
- **Alternatives considered** - Other approaches you've thought about
- **Additional context** - Screenshots, mockups, etc.

### Pull Requests

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Run tests (`make test`)
5. Commit your changes (`git commit -m 'feat: add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

## 🏗️ Development Setup

### Prerequisites

- Java 17+
- Node.js 18+
- Docker and Docker Compose
- Git

### Initial Setup

```bash
# Clone your fork
git clone https://github.com/ThomasGrant-93/specpulse.git
cd specpulse

# Start database
make db-up

# Start development servers
make dev

# Run tests
make test
```

## 📋 Coding Guidelines

### Java Backend

- Follow **Google Java Style Guide**
- Use **Lombok** for boilerplate reduction
- Write **unit tests** for new services and controllers
- Use **specific exception handling** (avoid generic `Exception`)
- Add **JavaDoc** for public APIs
- Keep methods **small and focused** (< 50 lines preferred)

**Example:**

```java
/**
 * Create a new service with group assignment
 *
 * @param request service creation request
 * @return created service DTO
 * @throws DuplicateResourceException if service name already exists
 */
@Transactional
public ServiceDTO createService(CreateServiceRequest request) {
    log.info("Creating new service: name={}, groupId={}",
            request.name(), request.groupId());

    if (repository.existsByName(request.name())) {
        throw new DuplicateResourceException("Service already exists");
    }

    // Implementation...
}
```

### TypeScript Frontend

- Use **strict TypeScript** (no `any` types)
- Follow **ESLint + Prettier** rules
- Write **tests** for components and hooks
- Use **functional components** with hooks
- Prefer **named exports** over default exports
- Add **JSDoc** for complex functions

**Example:**

```typescript
interface ServiceFormProps {
    onSubmit: (data: CreateServiceRequest) => void;
    onCancel: () => void;
    initialData?: Service;
}

/**
 * Form component for creating/editing services
 */
export default function ServiceForm({
                                        onSubmit,
                                        onCancel,
                                        initialData
                                    }: ServiceFormProps) {
    // Implementation...
}
```

### Commit Messages

We follow **Conventional Commits**:

```
feat: add service group filtering
fix: resolve NPE in diff comparison
docs: update API documentation
refactor: simplify version comparison logic
test: add integration tests for groups API
chore: update dependencies
```

**Types:**

- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation changes
- `refactor` - Code refactoring
- `test` - Adding tests
- `chore` - Maintenance tasks

### Code Review Checklist

Before submitting a PR, ensure:

- [ ] Tests added/updated (aim for >80% coverage)
- [ ] Linting passes (`make lint`)
- [ ] Formatting correct (`make format`)
- [ ] No `console.log` in production code
- [ ] No `@Transactional` on read-only methods
- [ ] No `any` types in TypeScript
- [ ] Documentation updated
- [ ] CHANGELOG.md updated (for features/fixes)

## 🧪 Testing

### Backend Tests

```bash
# Run all backend tests
make test-backend

# Run specific test class
./gradlew :specpulse-backend:test --tests "com.specpulse.api.RegistryControllerTest"

# With coverage
make test-backend-coverage
```

### Frontend Tests

```bash
# Run all frontend tests
make test-frontend

# Interactive UI mode
make test-ui

# With coverage
make test-frontend-coverage
```

### Integration Tests

Uses Testcontainers for PostgreSQL:

```java

@Testcontainers
@SpringBootTest
class IntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void shouldCreateService() {
        // Test implementation...
    }
}
```

## 📦 Architecture

### Backend Layers

```
Controller → Service → Repository
     ↓          ↓          ↓
   DTO       Entity     JPA
```

### Frontend Structure

```
pages/          # Page components
components/     # Reusable components
hooks/          # Custom React hooks
services/       # API clients
types/          # TypeScript types
utils/          # Utility functions
```

## 🔒 Security

- Never commit sensitive information (API keys, passwords)
- Use environment variables for configuration
- Validate all user inputs
- Use parameterized queries (JPA handles this)
- Implement rate limiting for production

## 📖 Documentation

- Update README.md for new features
- Add JavaDoc/JSDoc for public APIs
- Update Swagger/OpenAPI documentation
- Include examples in documentation

## 🚀 Release Process

1. Update version in `build.gradle` and `package.json`
2. Update CHANGELOG.md
3. Create git tag: `git tag -a v0.1.0 -m "Release v0.1.0"`
4. Push tag: `git push origin v0.1.0`
5. Create GitHub release

## 💬 Questions?

- **General questions:** [GitHub Discussions](https://github.com/ThomasGrant-93/specpulse/discussions)
- **Bug reports:** [GitHub Issues](https://github.com/ThomasGrant-93/specpulse/issues)

## 🙏 Thank You!

Every contribution makes SpecPulse better. Whether it's a bug report, feature suggestion, or code contribution - we
appreciate your help!

---

**SpecPulse Contributors**
