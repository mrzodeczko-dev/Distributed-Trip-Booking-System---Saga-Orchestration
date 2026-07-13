# Distributed Trip Booking System - Saga Orchestration

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F.svg?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot) [![Java](https://img.shields.io/badge/Java-25-ED8B00.svg?logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0id2hpdGUiPjxwYXRoIGQ9Ik04Ljg1MSAxOC41NnMtLjkxNy41MzQuNjUzLjcxNGMxLjkwMi4yMTggMi44NzQuMTg3IDQuOTY5LS4yMTEgMCAwIC41NTIuMzQ2IDEuMzIxLjY0Ni00LjY5OCAyLjAxMy0xMC42MzMtLjExOC02Ljk0My0xLjE0OU04LjI3NiAxNS45MzNzLTEuMDI4Ljc2Mi41NDIuOTI0YzIuMDMyLjIwOSAzLjYzNi4yMjcgNi40MTMtLjMwOCAwIDAgLjM4NC4zODkuOTg3LjYwMi01LjY3OSAxLjY2MS0xMi4wMDcuMTMtNy45NDItMS4yMThNMTMuMTE2IDExLjQ3NWMxLjE1OCAxLjMzMy0uMzA0IDIuNTMzLS4zMDQgMi41MzNzMi45MzktMS41MTggMS41ODktMy40MThjLTEuMjYxLTEuNzcyLTIuMjI4LTIuNjUyIDMuMDA3LTUuNjg4IDAgMC04LjIxNiAyLjA1MS00LjI5MiA2LjU3M00xOS4zMyAyMC41MDRzLjY3OS41NTktLjc0Ny45OTFjLTIuNzEyLjgyMi0xMS4yODggMS4wNjktMTMuNjY5LjAzMy0uODU2LS4zNzMuNzUtLjg5IDEuMjU0LS45OTguNTI3LS4xMTQuODI4LS4wOTMuODI4LS4wOTMtLjk1My0uNjcxLTYuMTU2IDEuMzE3LTIuNjQzIDEuODg3IDkuNTggMS41NTMgMTcuNDYyLS43IDE0Ljk3NS0xLjgyTTkuMjkyIDEzLjIxcy00LjM2MiAxLjAzNi0xLjU0NCAxLjQxMmMxLjE4OS4xNTkgMy41NjEuMTIzIDUuNzctLjA2MiAxLjgwNi0uMTUyIDMuNjE4LS40NzcgMy42MTgtLjQ3N3MtLjYzNy4yNzItMS4wOTguNTg3Yy00LjQyOSAxLjE2NS0xMi45ODYuNjIzLTEwLjUyMi0uNTY5IDIuMDgyLTEuMDA2IDMuNzc2LS44OTEgMy43NzYtLjg5MU0xNy4xMTYgMTcuNTg0YzQuNTAzLTIuMzQgMi40MjEtNC41ODkuOTY4LTQuMjg1LS4zNTUuMDc0LS41MTUuMTM4LS41MTUuMTM4cy4xMzItLjIwNy4zODUtLjI5N2MyLjg3NS0xLjAxMSA1LjA4NiAyLjk4MS0uOTI5IDQuNTYyIDAgMCAuMDctLjA2Mi4wOTEtLjExOE0xNC40MDEgMHMyLjQ5NCAyLjQ5NC0yLjM2NSA2LjMzYy0zLjg5NiAzLjA3Ny0uODg5IDQuODMyIDAgNi44MzYtMi4yNzQtMi4wNTMtMy45NDMtMy44NTgtMi44MjQtNS41NCAxLjY0NC0yLjQ2OSA2LjE5Ny0zLjY2NSA1LjE4OS03LjYyNk05LjczNCAyMy45MjRjNC4zMjIuMjc3IDEwLjk1OS0uMTU0IDExLjExNi0yLjE5OCAwIDAtLjMwMi43NzUtMy41NzIgMS4zOTEtMy42ODguNjk0LTguMjM5LjYxMy0xMC45MzcuMTY4IDAgMCAuNTUzLjQ1NyAzLjM5My42MzkiLz48L3N2Zz4K)](https://openjdk.org/) [![RabbitMQ](https://img.shields.io/badge/RabbitMQ-Cluster-FF6600.svg?logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com/) [![Liquibase](https://img.shields.io/badge/Liquibase-Migrations-2962FF.svg?logo=liquibase&logoColor=white)](https://www.liquibase.org/) [![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg?logo=docker&logoColor=white)](https://www.docker.com/) [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

[![Booking Tests](https://img.shields.io/github/actions/workflow/status/mrzodeczko-dev/saga-orchestration/ci.yml?branch=master&label=booking-service)](https://github.com/mrzodeczko-dev/saga-orchestration/actions/workflows/ci.yml) [![Flight Tests](https://img.shields.io/github/actions/workflow/status/mrzodeczko-dev/saga-orchestration/ci.yml?branch=master&label=flight-service)](https://github.com/mrzodeczko-dev/saga-orchestration/actions/workflows/ci.yml) [![Hotel Tests](https://img.shields.io/github/actions/workflow/status/mrzodeczko-dev/saga-orchestration/ci.yml?branch=master&label=hotel-service)](https://github.com/mrzodeczko-dev/saga-orchestration/actions/workflows/ci.yml) [![Payment Tests](https://img.shields.io/github/actions/workflow/status/mrzodeczko-dev/saga-orchestration/ci.yml?branch=master&label=payment-service)](https://github.com/mrzodeczko-dev/saga-orchestration/actions/workflows/ci.yml) [![E2E Tests](https://img.shields.io/github/actions/workflow/status/mrzodeczko-dev/saga-orchestration/e2e.yml?branch=master&label=e2e%20tests)](https://github.com/mrzodeczko-dev/saga-orchestration/actions/workflows/e2e.yml)

Monorepo for a distributed trip booking system implementing the **Saga Orchestration** pattern with **Spring Boot 4.1.0**, **Java 25**, and **Hexagonal Architecture**. The orchestrator coordinates a multi-step booking (flight, hotel, payment) across independent microservices via a 3-node **RabbitMQ quorum queue** cluster, with **Transactional Outbox**, **idempotent consumers**, and **automatic compensating transactions** on failure.

## Services

| Service | Port | Role | Description |
|---------|------|------|-------------|
| **Booking Service** | 8080 | Orchestrator | Starts sagas, tracks step state, dispatches commands, handles replies, triggers compensation |
| **Flight Service** | 8081 | Participant | Reserves / cancels seat reservations per saga |
| **Hotel Service** | 8082 | Participant | Reserves / cancels cabin reservations per saga |
| **Payment Service** | 8083 | Participant | Charges / refunds payments per saga |

## Architecture

```mermaid
graph LR
    Client(["🖥️ Client"])

    subgraph ORCH["Booking Service :8080"]
        direction TB
        API["REST API"]
        SAG["Saga Orchestrator"]
        OB1[("Outbox")]
        LB1["Liquibase"]
        API --> SAG --> OB1
    end

    subgraph MQ["RabbitMQ Cluster · 3 nodes"]
        direction TB
        CMD["x.saga.commands"]
        RPL["x.saga.replies"]
        DLX["x.saga.dlx"]
    end

    subgraph PART["Saga Participants"]
        direction TB
        F["✈️ Flight :8081"]
        H["🏨 Hotel :8082"]
        P["💳 Payment :8083"]
        LB2["Liquibase"]
    end

    B_DB[("Booking DB")]
    F_DB[("Flight DB")]
    H_DB[("Hotel DB")]
    P_DB[("Payment DB")]

    Client -- "POST /bookings" --> API

    OB1 -. "publish" .-> CMD

    CMD -- "flight.command" --> F
    CMD -- "hotel.command" --> H
    CMD -- "payment.command" --> P

    F -. "reply" .-> RPL
    H -. "reply" .-> RPL
    P -. "reply" .-> RPL

    RPL -- "saga.reply" --> SAG

    LB1 -. "migrations" .-> B_DB
    SAG --- B_DB
    LB2 -. "migrations" .-> F_DB
    LB2 -. "migrations" .-> H_DB
    LB2 -. "migrations" .-> P_DB
    F --- F_DB
    H --- H_DB
    P --- P_DB

    style Client fill:#e1f5fe,stroke:#0277bd,color:#000
    style ORCH fill:#fff3e0,stroke:#ef6c00,color:#000
    style MQ fill:#fce4ec,stroke:#c62828,color:#000
    style PART fill:#f3e5f5,stroke:#6a1b9a,color:#000
    style B_DB fill:#fff3e0,stroke:#ef6c00,color:#000
    style F_DB fill:#f3e5f5,stroke:#6a1b9a,color:#000
    style H_DB fill:#f3e5f5,stroke:#6a1b9a,color:#000
    style P_DB fill:#f3e5f5,stroke:#6a1b9a,color:#000

    linkStyle 0,1,2,3,4,5,10 stroke:#ef6c00
    linkStyle 6,7,8,9 stroke:#6a1b9a
```

### Saga Flow

A trip booking saga executes three steps sequentially: **FLIGHT** → **HOTEL** → **PAYMENT**. Each step follows a reserve/cancel contract. If any step fails, the orchestrator automatically compensates all previously reserved steps in reverse order.

```mermaid
flowchart TD
    START(["POST /bookings"]) --> FL

    subgraph HAPPY["Happy Path"]
        direction LR
        FL["✈️ Reserve Flight"]
        HT["🏨 Reserve Hotel"]
        PM["💳 Charge Payment"]
        FL -- "reserved ✓" --> HT -- "reserved ✓" --> PM
    end

    PM -- "reserved ✓" ---> OK(["✅ COMPLETED"])

    FL -. "failed ✗" .-> CANCEL(["❌ CANCELLED"])

    HT -. "failed ✗" .-> CF1["✈️ Cancel Flight"] --> CANCEL

    PM -. "failed ✗" .-> CH["🏨 Cancel Hotel"] --> CF2["✈️ Cancel Flight"] --> CANCEL

    style HAPPY fill:#e8f5e9,stroke:#2e7d32,color:#000
    style OK fill:#c8e6c9,stroke:#2e7d32,color:#000
    style CANCEL fill:#ffcdd2,stroke:#c62828,color:#000
    style START fill:#e1f5fe,stroke:#0277bd,color:#000
    style CF1 fill:#ffebee,stroke:#c62828,color:#000
    style CF2 fill:#ffebee,stroke:#c62828,color:#000
    style CH fill:#ffebee,stroke:#c62828,color:#000
```

### Messaging Topology

The system uses three RabbitMQ exchanges: `x.saga.commands` (direct) routes commands by service-specific routing keys (`flight.command`, `hotel.command`, `payment.command`), `x.saga.replies` (direct) routes all participant replies back to the orchestrator via `saga.reply`, and `x.saga.dlx` (direct) handles dead-lettered messages. Each participant has its own command queue and DLQ. All queues are quorum queues by default (replicated across the 3-node cluster).

### Reliability Guarantees

All services use the **Transactional Outbox** pattern — messages are persisted to a local outbox table within the same database transaction as the business operation, then published asynchronously by a ShedLock-coordinated poller. This ensures exactly-once semantics between the database write and the message publish. Participant services additionally implement **idempotent consumers** via a `processed_messages` table — duplicate commands (same `sagaId:action` key) are silently skipped.

## Quick Start (Docker Compose)

```bash
cp .env.example .env
# Fill in secrets (RabbitMQ passwords, DB passwords)
docker compose up -d --build
curl http://localhost:8080/actuator/health
```

### Start a Booking

```bash
curl -X POST http://localhost:8080/bookings \
  -H "Content-Type: application/json" \
  -d '{"customerName":"John","destination":"Mars","amount":9999.99}'
```

### Check Saga Status

```bash
curl http://localhost:8080/bookings/{sagaId}
```

### Check Participant State

```bash
curl http://localhost:8081/reservations/{sagaId}   # Flight
curl http://localhost:8082/reservations/{sagaId}   # Hotel
curl http://localhost:8083/payments/{sagaId}        # Payment
```

## Repository Structure

```
saga-orchestration/
├── booking-service/                   # Saga orchestrator
│   ├── src/main/java/com/rzodeczko/
│   │   ├── application/
│   │   │   ├── command/               # StartTripBookingCommand
│   │   │   ├── dto/                   # SagaInstanceDto, SagaStepDto
│   │   │   ├── event/                 # SagaReply, SagaAction, ReplyStatus
│   │   │   ├── port/in/              # StartTripBookingUseCase, HandleSagaReplyUseCase, GetSagaUseCase
│   │   │   ├── port/out/             # SagaCommandPort, SagaInstanceRepository
│   │   │   └── service/              # SagaOrchestratorImpl, SagaQueryServiceImpl
│   │   ├── domain/
│   │   │   ├── exception/            # InvalidSagaStateException, SagaNotFoundException
│   │   │   └── model/saga/           # SagaInstance, SagaStep, SagaStepName, SagaStatus, SagaStepStatus
│   │   ├── infrastructure/
│   │   │   ├── configuration/        # BeanConfiguration
│   │   │   ├── messaging/            # OutboxSagaCommandPublisher, SagaReplyListener, SagaTopologyConfig
│   │   │   ├── outbox/               # OutboxEvent, OutboxEventService, OutboxEventPublisher
│   │   │   ├── persistence/          # SagaInstanceEntity, SagaStepEntity, JPA adapter, mapper
│   │   │   └── tx/                   # TransactionalSagaOrchestrator, TransactionalSagaQueryService
│   │   └── presentation/
│   │       ├── controller/           # BookingController
│   │       ├── dto/                  # StartTripBookingRequestDto, BookingResponseDto
│   │       └── exception/            # GlobalExceptionHandler
│   ├── src/main/resources/
│   │   ├── application.yaml
│   │   └── db/changelog/             # Liquibase migrations (per-service)
│   ├── Dockerfile
│   └── pom.xml
├── flight-service/                    # Saga participant (seat reservations)
│   ├── src/main/java/com/rzodeczko/
│   │   ├── application/              # FlightCommandService, ports, events
│   │   ├── domain/model/             # SeatReservation, ReservationOutcome, ReservationStatus
│   │   ├── infrastructure/
│   │   │   ├── idempotency/          # ProcessedMessageEntity, JpaProcessedMessageRepository
│   │   │   ├── messaging/            # FlightCommandListener, OutboxSagaReplyPublisher
│   │   │   ├── outbox/               # OutboxEventEntity, OutboxEventService, OutboxEventPublisher
│   │   │   └── persistence/          # SeatReservationEntity, JPA adapter, mapper
│   │   └── presentation/
│   │       └── controller/           # ReservationController
│   ├── Dockerfile
│   └── pom.xml
├── hotel-service/                     # Saga participant (cabin reservations)
│   ├── src/main/java/com/rzodeczko/  # Same structure as flight-service
│   ├── Dockerfile
│   └── pom.xml
├── payment-service/                   # Saga participant (charges & refunds)
│   ├── src/main/java/com/rzodeczko/  # Same structure as flight-service
│   ├── Dockerfile
│   └── pom.xml
├── rabbitmq/                          # Custom RabbitMQ cluster image
│   ├── Dockerfile                     # Parameterized RABBITMQ_VERSION
│   ├── rabbitmq.conf                  # Quorum queues, cluster formation, Prometheus
│   ├── rabbitmq-definitions.json      # Users, vhosts, permissions
│   └── enabled_plugins
├── .github/workflows/
│   ├── ci.yml                         # Unit & contract tests (4 services)
│   ├── e2e.yml                        # End-to-end tests (Docker Compose)
│   └── dockerhub-publish-images.yml   # Publish Docker images after E2E pass
├── docker-compose.yml                 # Full stack (4 MySQL + 3 RabbitMQ + 4 services)
├── .env.example                       # All environment variables
└── .gitignore
```

## CI/CD

The project uses three GitHub Actions workflows. **`ci.yml`** has two jobs: **Flight Service**, **Hotel Service**, and **Payment Service** build in parallel — each produces Spring Cloud Contract stubs and caches them. **Booking Service** then builds and runs contract tests against all three stub sets. All jobs upload JaCoCo coverage reports and Surefire test results as artifacts. **`e2e.yml`** spins up the full Docker Compose stack and runs end-to-end saga tests. **`dockerhub-publish-images.yml`** triggers after a successful E2E run on `master`/`develop` and publishes all four service images to Docker Hub.

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 25 (virtual threads via Project Loom) |
| Framework | Spring Boot 4.1.0 |
| Messaging | RabbitMQ 4.1.1 (3-node quorum queue cluster) |
| Messaging client | Spring AMQP (publisher confirms, returns, retry) |
| Outbox | Transactional Outbox pattern (ShedLock 6.0.2 + JPA) |
| Contract Testing | Spring Cloud Contract 2025.1.0 |
| Database | MySQL 9.6 (per-service isolation) |
| Persistence | Spring Data JPA, HikariCP |
| Migrations | Liquibase (Spring Boot Starter) |
| Validation | Spring Boot Starter Validation (Hibernate Validator) |
| Code Coverage | JaCoCo 0.8.13 (80% line coverage gate) |
| Integration Testing | Testcontainers (MySQL, RabbitMQ), Awaitility |
| Observability | Spring Boot Actuator |
| Build | Maven 3.9, multi-stage Docker build with CDS extraction |
| Containerisation | Docker, Docker Compose v2+ |
| CI/CD | GitHub Actions |
| Utilities | Lombok, Jackson |

## Contact

Designed and implemented by **Michał Rzodeczko**.
GitHub: [mrzodeczko-dev](https://github.com/mrzodeczko-dev)
