# McWendyQueen

Spring Boot learning project that models a fast-food restaurant domain: menu items, recipes, condiments, orders, and Kafka-backed order events.

Built as a hands-on exercise in layered services, JPA persistence, REST APIs, OpenAPI docs, and local infrastructure with Docker (PostgreSQL + Kafka).

## Stack

| Layer | Choice |
|-------|--------|
| Language | Java 17 |
| Framework | Spring Boot 3.5 |
| Persistence | Spring Data JPA + PostgreSQL (H2 for tests) |
| Messaging | Spring Kafka |
| API docs | springdoc-openapi (Swagger UI) |
| Templates | Thymeleaf |
| Build | Maven Wrapper |

## Features

- Menu, recipe, condiment, and order domain services with DTOs and validation
- REST controllers with OpenAPI documentation
- Kafka listeners / admin hooks for order-related events
- Scheduled order processing support
- Unit tests for services and controllers

## Quick start

### Prerequisites

- JDK 17+
- Docker Desktop (PostgreSQL + Kafka for full local stack)
- IntelliJ IDEA (recommended) or Maven CLI

Detailed workstation setup (Windows tools, Docker, Kafka UI, IntelliJ) lives in [`LOCAL_DEV_SETUP.md`](LOCAL_DEV_SETUP.md).

### Run the app

```powershell
.\mvnw.cmd spring-boot:run
```

Or run the Spring Boot main class from IntelliJ after starting Postgres/Kafka containers per `LOCAL_DEV_SETUP.md`.

### Tests

```powershell
.\mvnw.cmd test
```

## Project layout

```
src/main/java/com/mcwendyqueen/
  controller/     # REST endpoints
  service/        # Business logic
  model/          # Entities, DTOs, repositories
src/test/java/    # Service and controller tests
```

Package naming uses reverse-DNS style: `com.mcwendyqueen.<layer>.<domain>`.

## Related docs

- [`LOCAL_DEV_SETUP.md`](LOCAL_DEV_SETUP.md) — recreate the Windows + Docker + IntelliJ environment
- `System Design.excalidraw` — high-level design sketch

## Author

[David Helman](https://github.com/dhelman999) — personal learning / portfolio project.
